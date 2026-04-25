package com.algoforge.judge.dto;

import java.util.List;

public record JudgeResultMessage(
        Long submissionId,
        String status,                 // ACCEPTED / WRONG_ANSWER / TLE / MLE / RUNTIME_ERROR / COMPILE_ERROR / SYSTEM_ERROR
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
