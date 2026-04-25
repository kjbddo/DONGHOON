package com.algoforge.judge.sandbox;

import com.algoforge.judge.config.JudgeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Docker CLI 기반 샌드박스 실행기.
 *
 * 한 번의 {@link #run(ExecutionRequest)} 호출은
 *   1) `docker run -i ...` (컨테이너는 run 종료 후 `docker rm -f`로 정리) …
 *   2) stdin을 주입한 뒤
 *   3) stdout/stderr을 캡처하면서 호스트 측 timeout을 적용한다.
 *
 * 컨테이너에 부여하는 옵션:
 *   --network=none                네트워크 차단
 *   --memory={M}m --memory-swap={M}m  메모리 한도 (초과 시 OOM kill → exit 137)
 *   --cpus=1.0 --pids-limit=64    CPU 1코어 / 프로세스 수 제한
 *   --cap-drop=ALL --security-opt=no-new-privileges  권한 박탈
 *   -v {workdir}:/work            소스/바이너리 공유 (RW)
 *
 * 출력은 {@link JudgeProperties#outputLimitBytes()} 까지만 수집하며 초과 시 OUTPUT_LIMIT으로 종료.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DockerSandboxRunner {

    private final JudgeProperties props;

    public ExecutionResult run(ExecutionRequest req) {
        String containerName = "algoforge-judge-" + UUID.randomUUID();
        try {
            return runOnce(req, containerName);
        } finally {
            removeContainerQuietly(containerName);
        }
    }

    private ExecutionResult runOnce(ExecutionRequest req, String containerName) {
        List<String> cmd = buildDockerCommand(req, containerName);
        log.debug("[sandbox] {} -> docker {}", containerName, String.join(" ", cmd));

        Process process;
        long startedAt = System.nanoTime();
        try {
            process = new ProcessBuilder(cmd)
                    .redirectErrorStream(false)
                    .start();
        } catch (IOException e) {
            log.error("Failed to start docker process", e);
            return ExecutionResult.systemError("docker start failed: " + e.getMessage());
        }

        if (req.stdin() != null) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(req.stdin().getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException ignore) {
            }
        } else {
            try { process.getOutputStream().close(); } catch (IOException ignore) { /* noop */ }
        }

        StreamReader stdoutReader = new StreamReader(process.getInputStream(), props.outputLimitBytes());
        StreamReader stderrReader = new StreamReader(process.getErrorStream(), props.outputLimitBytes());
        Thread tOut = new Thread(stdoutReader, "judge-stdout-" + containerName);
        Thread tErr = new Thread(stderrReader, "judge-stderr-" + containerName);
        tOut.setDaemon(true);
        tErr.setDaemon(true);
        tOut.start();
        tErr.start();

        boolean finished;
        try {
            finished = process.waitFor(req.timeoutMs() + 1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            killContainer(containerName);
            process.destroyForcibly();
            return ExecutionResult.systemError("interrupted");
        }
        long elapsedNs = System.nanoTime() - startedAt;
        int elapsedMs = (int) Math.min(elapsedNs / 1_000_000L, Integer.MAX_VALUE);

        if (!finished) {
            killContainer(containerName);
            process.destroyForcibly();
            joinQuietly(tOut);
            joinQuietly(tErr);
            return new ExecutionResult(
                    ExecutionStatus.TIME_LIMIT_EXCEEDED,
                    stdoutReader.asString(),
                    stderrReader.asString(),
                    req.timeoutMs(),
                    0
            );
        }

        joinQuietly(tOut);
        joinQuietly(tErr);

        int exit = process.exitValue();
        String stdout = stdoutReader.asString();
        String stderr = stderrReader.asString();

        if (stdoutReader.overflowed() || stderrReader.overflowed()) {
            killContainer(containerName);
            return new ExecutionResult(ExecutionStatus.OUTPUT_LIMIT, stdout, stderr, elapsedMs, 0);
        }

        ExecutionStatus status = switch (exit) {
            case 0 -> ExecutionStatus.OK;
            case 124 -> ExecutionStatus.TIME_LIMIT_EXCEEDED;
            case 137 -> ExecutionStatus.MEMORY_LIMIT_EXCEEDED;
            case 139 -> ExecutionStatus.RUNTIME_ERROR;
            default  -> ExecutionStatus.RUNTIME_ERROR;
        };

        int memKb = resolveMemoryUsedKb(req, status, containerName, exit);
        return new ExecutionResult(status, stdout, stderr, elapsedMs, memKb);
    }

    /**
     * OOM(137)이면 한도로 보정, 그 외 Linux+cgroup v2 memory.peak 베스트에포트.
     */
    private int resolveMemoryUsedKb(ExecutionRequest req, ExecutionStatus status, String containerName, int exit) {
        if (status == ExecutionStatus.MEMORY_LIMIT_EXCEEDED && exit == 137) {
            return Math.min(req.memoryMb() * 1024, Integer.MAX_VALUE);
        }
        if (!props.readCgroupMemoryOrDefault() || !isLinux()) {
            return 0;
        }
        long peak = readCgroupMemoryPeakBytes(containerName);
        if (peak <= 0) {
            return 0;
        }
        return (int) Math.min(peak / 1024, Integer.MAX_VALUE);
    }

    private static boolean isLinux() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("linux");
    }

    private long readCgroupMemoryPeakBytes(String containerName) {
        String id = dockerInspectId(containerName);
        if (id == null) {
            return 0L;
        }
        String[] candidates = {
                "/sys/fs/cgroup/system.slice/docker-" + id + ".scope/memory.peak",
                "/sys/fs/cgroup/system.slice/docker-" + id.substring(0, 12) + ".scope/memory.peak",
                "/sys/fs/cgroup/docker/" + id + "/memory.peak",
        };
        for (String c : candidates) {
            try {
                Path p = Path.of(c);
                if (Files.isRegularFile(p)) {
                    String s = Files.readString(p).trim();
                    if (!s.isEmpty()) {
                        return Long.parseLong(s);
                    }
                }
            } catch (Exception e) {
                log.debug("[sandbox] memory.peak read skip {}: {}", c, e.toString());
            }
        }
        return 0L;
    }

    private String dockerInspectId(String name) {
        try {
            Process p = new ProcessBuilder(props.dockerBinary(), "inspect", "-f", "{{.Id}}", name)
                    .redirectErrorStream(true)
                    .start();
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private void removeContainerQuietly(String name) {
        try {
            new ProcessBuilder(props.dockerBinary(), "rm", "-f", name)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("docker rm -f {}: {}", name, e.getMessage());
        }
    }

    /* ---------------------------------------------------------------- */
    /*  내부 헬퍼                                                         */
    /* ---------------------------------------------------------------- */

    private List<String> buildDockerCommand(ExecutionRequest req, String containerName) {
        List<String> cmd = new ArrayList<>();
        cmd.add(props.dockerBinary());
        cmd.add("run");
        cmd.add("-i");
        cmd.add("--name");
        cmd.add(containerName);
        cmd.add("--network=none");
        cmd.add("--cpus=1.0");
        cmd.add("--pids-limit=64");
        cmd.add("--memory=" + req.memoryMb() + "m");
        cmd.add("--memory-swap=" + req.memoryMb() + "m");
        cmd.add("--cap-drop=ALL");
        cmd.add("--security-opt=no-new-privileges");
        cmd.add("-v");
        cmd.add(toHostPath(req.workdir()) + ":/work");
        cmd.add("-w");
        cmd.add("/work");
        cmd.add(req.image());
        cmd.add("sh");
        cmd.add("-c");
        cmd.add(req.command());
        return cmd;
    }

    private String toHostPath(Path p) {
        return p.toAbsolutePath().toString().replace('\\', '/');
    }

    private void killContainer(String name) {
        try {
            new ProcessBuilder(props.dockerBinary(), "kill", name)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to docker kill {}: {}", name, e.getMessage());
        }
    }

    private void joinQuietly(Thread t) {
        try {
            t.join(2_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /* ---------------------------------------------------------------- */
    /*  타입 정의                                                         */
    /* ---------------------------------------------------------------- */

    public record ExecutionRequest(
            String image,
            String command,
            String stdin,
            int timeoutMs,
            int memoryMb,
            Path workdir
    ) {}

    public record ExecutionResult(
            ExecutionStatus status,
            String stdout,
            String stderr,
            int executionTimeMs,
            int memoryUsedKb
    ) {
        public static ExecutionResult systemError(String stderr) {
            return new ExecutionResult(ExecutionStatus.SYSTEM_ERROR, "", stderr, 0, 0);
        }
    }

    public enum ExecutionStatus {
        OK, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, RUNTIME_ERROR, OUTPUT_LIMIT, SYSTEM_ERROR
    }

    /**
     * 출력 사이즈 상한이 적용된 stream 리더 (overflow 시 추가 바이트는 버림).
     */
    private static final class StreamReader implements Runnable {
        private final InputStream in;
        private final long limit;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private volatile boolean overflowed = false;

        StreamReader(InputStream in, long limit) {
            this.in = in;
            this.limit = limit;
        }

        @Override
        public void run() {
            byte[] chunk = new byte[8 * 1024];
            try {
                int n;
                while ((n = in.read(chunk)) != -1) {
                    if (buf.size() + n > limit) {
                        int remain = (int) Math.max(0, limit - buf.size());
                        if (remain > 0) buf.write(chunk, 0, remain);
                        overflowed = true;
                        // drain 후 종료
                        while (in.read(chunk) != -1) { /* discard */ }
                        return;
                    }
                    buf.write(chunk, 0, n);
                }
            } catch (IOException ignore) {
                // 컨테이너 강제 종료 시 발생 가능 → 정상 흐름
            }
        }

        String asString() {
            return buf.toString(StandardCharsets.UTF_8);
        }

        boolean overflowed() {
            return overflowed;
        }
    }
}
