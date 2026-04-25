package com.algoforge.judge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * worker 측 채점 설정.
 *
 *  - workdirBase  : 컴파일/실행을 위한 임시 디렉터리의 부모. (Linux 권장: /var/lib/algoforge/judge)
 *  - dockerBinary : 보통 "docker". 환경에 따라 절대 경로로 지정 가능.
 *  - outputLimitBytes : stdout 캡처 상한 (초과 시 강제 종료).
 *  - compileTimeoutMs : 컴파일 단계 호스트 측 타임아웃.
 *  - shortCircuit : true면 첫 실패 케이스에서 멈춤(채점 시간 절약).
 *  - parallelTestCases : 1이면 기존 순차. 2 이상이고 shortCircuit=false일 때 케이스를 병렬 샌드박스 실행.
 *  - readCgroupMemory: Linux에서 컨테이너 exit 후 cgroup memory.peak 읽기 시도 (false면 0, OOM 시 한도로 보정).
 */
@ConfigurationProperties(prefix = "algoforge.judge")
public record JudgeProperties(
        String submissionQueue,
        String resultQueue,
        String dlxExchange,
        String workdirBase,
        String dockerBinary,
        long outputLimitBytes,
        Integer compileTimeoutMs,
        Boolean shortCircuit,
        Integer parallelTestCases,
        Boolean readCgroupMemory
) {
    public int compileTimeoutMsOrDefault() {
        return compileTimeoutMs == null || compileTimeoutMs <= 0 ? 30_000 : compileTimeoutMs;
    }

    public boolean shortCircuitOrDefault() {
        return shortCircuit == null || shortCircuit;
    }

    public int parallelTestCasesOrDefault() {
        if (parallelTestCases == null || parallelTestCases < 1) {
            return 1;
        }
        return Math.min(parallelTestCases, 32);
    }

    public boolean readCgroupMemoryOrDefault() {
        return readCgroupMemory == null || readCgroupMemory;
    }
}
