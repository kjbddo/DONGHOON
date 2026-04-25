package com.algoforge.backend.ai.service;

import com.algoforge.backend.ai.client.AiClient;
import com.algoforge.backend.ai.domain.AiCallPurpose;
import com.algoforge.backend.ai.dto.GenerateProblemAiRequest;
import com.algoforge.backend.ai.dto.GeneratedProblemAiResponse;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.problem.domain.Example;
import com.algoforge.backend.problem.domain.ProblemSourceType;
import com.algoforge.backend.problem.dto.admin.AdminProblemDetailResponse;
import com.algoforge.backend.problem.dto.admin.AiProblemCreateCommand;
import com.algoforge.backend.problem.dto.admin.TestCaseDto;
import com.algoforge.backend.problem.service.AdminProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * AI 서버를 호출하여 문제를 생성하고, AdminProblemService를 통해 DRAFT 상태의 Problem으로 저장한다.
 *
 * - sourceType은 AI 응답이 우선이지만, 안전을 위해 AI_GENERATED 또는 AI_REWRITTEN_SOURCE_BASED만 허용.
 * - difficulty의 RUBY는 도메인 enum에 없으므로 DIAMOND로 매핑.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProblemGenerationService {

    private static final String DEFAULT_MODEL = "gemini";
    private static final String DEFAULT_PROMPT_VERSION = "v1";

    private final AiClient aiClient;
    private final AdminProblemService adminProblemService;
    private final AiQuotaService quotaService;
    private final AiCallLogger callLogger;

    public AdminProblemDetailResponse generate(GenerateProblemAiRequest req, Long requesterUserId) {
        // 관리자 호출이라도 비용 보호 차원에서 일일 quota 적용
        quotaService.assertWithinDailyQuota(requesterUserId);

        GeneratedProblemAiResponse ai = invokeWithLogging(requesterUserId, req);

        AiProblemCreateCommand cmd = toCommand(ai, requesterUserId);
        return adminProblemService.createFromAi(cmd);
    }

    private GeneratedProblemAiResponse invokeWithLogging(Long userId, GenerateProblemAiRequest req) {
        long startedAt = System.nanoTime();
        try {
            GeneratedProblemAiResponse res = await(aiClient.generateProblem(req));
            callLogger.logSuccess(AiCallPurpose.PROBLEM_GEN, userId,
                    DEFAULT_MODEL, DEFAULT_PROMPT_VERSION,
                    req, res, elapsedMs(startedAt));
            return res;
        } catch (RuntimeException ex) {
            callLogger.logFailure(AiCallPurpose.PROBLEM_GEN, userId,
                    DEFAULT_MODEL, DEFAULT_PROMPT_VERSION,
                    req, elapsedMs(startedAt), ex);
            throw ex;
        }
    }

    private long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    // ===== private =====
    private AiProblemCreateCommand toCommand(GeneratedProblemAiResponse ai, Long userId) {
        Difficulty difficulty = mapDifficulty(ai.difficulty());
        ProblemSourceType sourceType = mapSourceType(ai.sourceType());

        List<Example> examples = ai.examples() == null ? List.of()
                : ai.examples().stream()
                .map(e -> new Example(e.input(), e.output(), e.explanation()))
                .toList();

        List<TestCaseDto> testCases = ai.testCases() == null ? List.of()
                : ai.testCases().stream()
                .map(t -> new TestCaseDto(
                        null,
                        Objects.requireNonNullElse(t.input(), ""),
                        Objects.requireNonNullElse(t.output(), ""),
                        Boolean.TRUE.equals(t.isHidden())))
                .toList();

        if (testCases.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED, "AI 응답에 테스트 케이스가 없습니다.");
        }

        int timeLimitMs = ai.timeLimit() == null ? 2000 : ai.timeLimit() * 1000;
        int memoryLimitMb = ai.memoryLimit() == null ? 256 : ai.memoryLimit();

        return new AiProblemCreateCommand(
                ai.title(),
                null, // slug 자동 생성
                ai.description(),
                ai.inputDescription(),
                ai.outputDescription(),
                ai.constraints(),
                examples,
                timeLimitMs,
                memoryLimitMb,
                difficulty,
                sourceType,
                ai.category() == null ? List.of() : List.of(ai.category()),
                List.of(),       // tags는 프롬프트 변경 시 채워질 수 있음
                testCases,
                DEFAULT_MODEL,
                DEFAULT_PROMPT_VERSION,
                userId
        );
    }

    private Difficulty mapDifficulty(String d) {
        if (d == null) return Difficulty.SILVER;
        return switch (d.toUpperCase()) {
            case "BRONZE" -> Difficulty.BRONZE;
            case "SILVER" -> Difficulty.SILVER;
            case "GOLD" -> Difficulty.GOLD;
            case "PLATINUM" -> Difficulty.PLATINUM;
            case "DIAMOND", "RUBY" -> Difficulty.DIAMOND;
            default -> Difficulty.SILVER;
        };
    }

    private ProblemSourceType mapSourceType(String s) {
        if (s == null) return ProblemSourceType.AI_GENERATED;
        return switch (s) {
            case "AI_REWRITTEN_SOURCE_BASED" -> ProblemSourceType.AI_REWRITTEN_SOURCE_BASED;
            default -> ProblemSourceType.AI_GENERATED;
        };
    }

    private <T> T await(java.util.concurrent.CompletableFuture<T> f) {
        try {
            return f.get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED, "AI 호출이 중단되었습니다.");
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof BusinessException be) throw be;
            log.warn("AI 호출 실패", cause);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED, "AI 호출 실패: " + cause.getMessage());
        }
    }

    /** topicHint를 sourceMetadata에 포함시키고 싶을 때 사용 가능한 헬퍼 */
    @SuppressWarnings("unused")
    private GenerateProblemAiRequest withMetadata(GenerateProblemAiRequest base, Map<String, Object> metadata) {
        return new GenerateProblemAiRequest(base.category(), base.difficulty(), base.topicHint(), metadata);
    }
}
