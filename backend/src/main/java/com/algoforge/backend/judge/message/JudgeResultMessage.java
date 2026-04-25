package com.algoforge.backend.judge.message;

import java.util.List;

/**
 * Judge Worker → Backend 결과 메시지.
 * judge-worker 모듈의 동일 이름 record와 필드 호환되어야 한다.
 */
public record JudgeResultMessage(
        Long submissionId,
        String status,
        Integer maxExecutionTimeMs,
        Integer maxMemoryUsedKb,
        String compileErrorMessage,
        String runtimeErrorMessage,
        List<TestCaseResult> testCaseResults
) {
    public record TestCaseResult(
            Long testCaseId,
            String status,
            Integer executionTimeMs,
            Integer memoryUsedKb,
            String outputExcerpt
    ) {}
}
