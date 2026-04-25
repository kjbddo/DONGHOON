package com.algoforge.backend.submission.dto;

import com.algoforge.backend.submission.domain.Submission;
import com.algoforge.backend.submission.domain.SubmissionTestCaseResult;

import java.time.OffsetDateTime;
import java.util.List;

public record SubmissionDetailResponse(
        Long id,
        Long problemId,
        Long languageId,
        String code,
        String status,
        Integer executionTimeMs,
        Integer memoryUsedKb,
        String compileErrorMessage,
        String runtimeErrorMessage,
        OffsetDateTime submittedAt,
        OffsetDateTime judgedAt,
        List<TestCaseResultDto> testCaseResults
) {
    public static SubmissionDetailResponse of(Submission s, List<SubmissionTestCaseResult> results) {
        return new SubmissionDetailResponse(
                s.getId(),
                s.getProblemId(),
                s.getLanguageId(),
                s.getCode(),
                s.getStatus().name(),
                s.getExecutionTimeMs(),
                s.getMemoryUsedKb(),
                s.getCompileErrorMessage(),
                s.getRuntimeErrorMessage(),
                s.getSubmittedAt(),
                s.getJudgedAt(),
                results.stream().map(TestCaseResultDto::from).toList()
        );
    }

    public record TestCaseResultDto(
            Long testCaseId,
            String status,
            Integer executionTimeMs,
            Integer memoryUsedKb,
            String outputExcerpt
    ) {
        public static TestCaseResultDto from(SubmissionTestCaseResult r) {
            return new TestCaseResultDto(
                    r.getTestCaseId(),
                    r.getStatus().name(),
                    r.getExecutionTimeMs(),
                    r.getMemoryUsedKb(),
                    r.getOutputExcerpt()
            );
        }
    }
}
