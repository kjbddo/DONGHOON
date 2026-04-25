package com.algoforge.judge.service;

import com.algoforge.judge.compare.OutputComparator;
import com.algoforge.judge.config.JudgeProperties;
import com.algoforge.judge.dto.JudgeRequestMessage;
import com.algoforge.judge.dto.JudgeResultMessage;
import com.algoforge.judge.meta.JudgeMetaRepository;
import com.algoforge.judge.meta.LanguageSpec;
import com.algoforge.judge.meta.ProblemMeta;
import com.algoforge.judge.meta.TestCaseRow;
import com.algoforge.judge.sandbox.DockerSandboxRunner;
import com.algoforge.judge.sandbox.DockerSandboxRunner.ExecutionRequest;
import com.algoforge.judge.sandbox.DockerSandboxRunner.ExecutionResult;
import com.algoforge.judge.sandbox.DockerSandboxRunner.ExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 채점의 핵심 흐름을 담당한다.
 *
 *   1) 언어/문제/테스트 케이스 메타 로드
 *   2) workdir 생성 + 소스 파일 작성
 *   3) (필요 시) 컴파일 — 실패 시 COMPILE_ERROR 반환
 *   4) 각 테스트 케이스 실행 → 정답 비교 → 케이스별 상태 산출
 *   5) overall 상태 집계 (첫 비-AC 케이스를 우선)
 *   6) workdir 정리
 *
 * 결과는 {@link JudgeResultMessage} 한 건으로 묶여 RabbitMQ result-queue에 publish 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final JudgeMetaRepository metaRepository;
    private final DockerSandboxRunner sandboxRunner;
    private final OutputComparator comparator;
    private final JudgeProperties props;

    public JudgeResultMessage judge(JudgeRequestMessage req) {
        long submissionId = req.submissionId();
        long problemId = req.problemId();

        LanguageSpec lang = metaRepository.findLanguageByName(req.languageName())
                .orElse(null);
        if (lang == null) {
            return systemError(submissionId, "Unknown language: " + req.languageName());
        }

        ProblemMeta problem = metaRepository.findProblemMeta(problemId).orElse(null);
        if (problem == null) {
            return systemError(submissionId, "Problem not found: " + problemId);
        }

        List<TestCaseRow> testCases = metaRepository.findTestCases(problemId);
        if (testCases.isEmpty()) {
            return systemError(submissionId, "No test cases for problem " + problemId);
        }

        int timeLimitMs = (int) Math.round(problem.timeLimitMs() * lang.timeMultiplier());
        int memoryLimitMb = problem.memoryLimitMb();

        Path workdir = createWorkdir(submissionId);
        try {
            // 소스 파일 작성
            writeSourceFile(workdir, lang, req.code());

            // 컴파일
            if (lang.compileRequired()) {
                ExecutionResult compileResult = compile(lang, workdir, memoryLimitMb);
                if (compileResult.status() != ExecutionStatus.OK) {
                    return compileError(submissionId, compileResult);
                }
            }

            // 케이스 실행
            return runTestCases(submissionId, lang, testCases, workdir, timeLimitMs, memoryLimitMb);

        } catch (IOException ioe) {
            log.error("Workdir error for submission {}", submissionId, ioe);
            return systemError(submissionId, "I/O error: " + ioe.getMessage());
        } catch (RuntimeException rex) {
            log.error("Unexpected judge failure for submission {}", submissionId, rex);
            return systemError(submissionId, "Unexpected: " + rex.getClass().getSimpleName());
        } finally {
            cleanup(workdir);
        }
    }

    /* ---------------------------------------------------------------- */
    /*  단계별 헬퍼                                                       */
    /* ---------------------------------------------------------------- */

    private Path createWorkdir(long submissionId) {
        Path base = Paths.get(props.workdirBase()).resolve("submission-" + submissionId);
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create workdir: " + base, e);
        }
        return base;
    }

    private void writeSourceFile(Path workdir, LanguageSpec lang, String code) throws IOException {
        Path source = workdir.resolve(lang.sourceFileName());
        Files.writeString(source, code == null ? "" : code, StandardCharsets.UTF_8);
    }

    private ExecutionResult compile(LanguageSpec lang, Path workdir, int memoryMb) {
        String compileCmd = lang.renderCompileCommand();
        if (compileCmd == null || compileCmd.isBlank()) {
            return new ExecutionResult(ExecutionStatus.OK, "", "", 0, 0);
        }
        return sandboxRunner.run(new ExecutionRequest(
                lang.dockerImage(),
                compileCmd,
                null,
                props.compileTimeoutMsOrDefault(),
                Math.max(memoryMb, 512),  // 컴파일은 더 여유 있게
                workdir
        ));
    }

    private JudgeResultMessage runTestCases(
            long submissionId,
            LanguageSpec lang,
            List<TestCaseRow> testCases,
            Path workdir,
            int timeLimitMs,
            int memoryLimitMb
    ) {
        String runCmd = lang.renderRunCommand(memoryLimitMb);
        boolean shortCircuit = props.shortCircuitOrDefault();
        int parallel = props.parallelTestCasesOrDefault();

        if (!shortCircuit && parallel > 1 && testCases.size() > 1) {
            return runTestCasesParallel(submissionId, lang, testCases, workdir, timeLimitMs, memoryLimitMb, runCmd);
        }

        List<JudgeResultMessage.TestCaseResult> results = new ArrayList<>(testCases.size());
        String overallStatus = "ACCEPTED";
        int maxTimeMs = 0;
        int maxMemKb = 0;
        String runtimeErrSnapshot = null;

        for (TestCaseRow tc : testCases) {
            ExecutionResult er = sandboxRunner.run(new ExecutionRequest(
                    lang.dockerImage(),
                    runCmd,
                    tc.input(),
                    timeLimitMs,
                    memoryLimitMb,
                    workdir
            ));

            String tcStatus = classifyTestCase(er, tc.expectedOutput());
            results.add(new JudgeResultMessage.TestCaseResult(
                    tc.id(),
                    tcStatus,
                    er.executionTimeMs(),
                    er.memoryUsedKb(),
                    truncate(er.stdout(), 4096)
            ));

            if (er.executionTimeMs() > maxTimeMs) maxTimeMs = er.executionTimeMs();
            if (er.memoryUsedKb() > maxMemKb) maxMemKb = er.memoryUsedKb();

            if (!"ACCEPTED".equals(tcStatus) && "ACCEPTED".equals(overallStatus)) {
                overallStatus = tcStatus;
                if ("RUNTIME_ERROR".equals(tcStatus) || "OUTPUT_LIMIT_EXCEEDED".equals(tcStatus)) {
                    runtimeErrSnapshot = truncate(er.stderr(), 4096);
                }
                if (shortCircuit) {
                    break;
                }
            }
        }

        return new JudgeResultMessage(
                submissionId,
                overallStatus,
                maxTimeMs,
                maxMemKb,
                null,
                runtimeErrSnapshot,
                results
        );
    }

    private JudgeResultMessage runTestCasesParallel(
            long submissionId,
            LanguageSpec lang,
            List<TestCaseRow> testCases,
            Path workdir,
            int timeLimitMs,
            int memoryLimitMb,
            String runCmd
    ) {
        int nThreads = Math.min(props.parallelTestCasesOrDefault(), testCases.size());
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        try {
            List<CompletableFuture<CaseOutcome>> futures = new ArrayList<>(testCases.size());
            for (TestCaseRow tc : testCases) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    ExecutionResult er = sandboxRunner.run(new ExecutionRequest(
                            lang.dockerImage(),
                            runCmd,
                            tc.input(),
                            timeLimitMs,
                            memoryLimitMb,
                            workdir
                    ));
                    String tcStatus = classifyTestCase(er, tc.expectedOutput());
                    return new CaseOutcome(tc, tcStatus, er);
                }, pool));
            }
            List<CaseOutcome> outcomes = futures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparingInt((CaseOutcome o) -> o.tc().seq())
                            .thenComparingLong(o -> o.tc().id()))
                    .toList();

            List<JudgeResultMessage.TestCaseResult> results = new ArrayList<>(outcomes.size());
            String overallStatus = "ACCEPTED";
            int maxTimeMs = 0;
            int maxMemKb = 0;
            String runtimeErrSnapshot = null;

            for (CaseOutcome o : outcomes) {
                ExecutionResult er = o.er();
                results.add(new JudgeResultMessage.TestCaseResult(
                        o.tc().id(),
                        o.tcStatus(),
                        er.executionTimeMs(),
                        er.memoryUsedKb(),
                        truncate(er.stdout(), 4096)
                ));
                if (er.executionTimeMs() > maxTimeMs) maxTimeMs = er.executionTimeMs();
                if (er.memoryUsedKb() > maxMemKb) maxMemKb = er.memoryUsedKb();
            }

            for (CaseOutcome o : outcomes) {
                if (!"ACCEPTED".equals(o.tcStatus()) && "ACCEPTED".equals(overallStatus)) {
                    overallStatus = o.tcStatus();
                    ExecutionResult er = o.er();
                    if ("RUNTIME_ERROR".equals(o.tcStatus()) || "OUTPUT_LIMIT_EXCEEDED".equals(o.tcStatus())) {
                        runtimeErrSnapshot = truncate(er.stderr(), 4096);
                    }
                    break;
                }
            }

            return new JudgeResultMessage(
                    submissionId,
                    overallStatus,
                    maxTimeMs,
                    maxMemKb,
                    null,
                    runtimeErrSnapshot,
                    results
            );
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(60, TimeUnit.MINUTES)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private record CaseOutcome(TestCaseRow tc, String tcStatus, ExecutionResult er) {}

    private String classifyTestCase(ExecutionResult er, String expectedOutput) {
        return switch (er.status()) {
            case OK -> comparator.isAccepted(expectedOutput, er.stdout())
                    ? "ACCEPTED" : "WRONG_ANSWER";
            case TIME_LIMIT_EXCEEDED -> "TIME_LIMIT_EXCEEDED";
            case MEMORY_LIMIT_EXCEEDED -> "MEMORY_LIMIT_EXCEEDED";
            case OUTPUT_LIMIT -> "OUTPUT_LIMIT_EXCEEDED";
            case RUNTIME_ERROR, SYSTEM_ERROR -> "RUNTIME_ERROR";
        };
    }

    /* ---------------------------------------------------------------- */
    /*  결과 빌더                                                         */
    /* ---------------------------------------------------------------- */

    private JudgeResultMessage compileError(long submissionId, ExecutionResult er) {
        String msg = er.stderr() == null || er.stderr().isBlank() ? er.stdout() : er.stderr();
        return new JudgeResultMessage(
                submissionId,
                "COMPILE_ERROR",
                0, 0,
                truncate(msg, 8192),
                null,
                List.of()
        );
    }

    private JudgeResultMessage systemError(long submissionId, String message) {
        return new JudgeResultMessage(
                submissionId,
                "SYSTEM_ERROR",
                0, 0,
                null,
                truncate(message, 4096),
                List.of()
        );
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "\n...[truncated]";
    }

    private void cleanup(Path workdir) {
        if (workdir == null || !Files.exists(workdir)) return;
        try (Stream<Path> walk = Files.walk(workdir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException ignore) { /* best-effort */ }
                    });
        } catch (IOException e) {
            log.warn("workdir cleanup failed: {}", workdir, e);
        }
    }
}
