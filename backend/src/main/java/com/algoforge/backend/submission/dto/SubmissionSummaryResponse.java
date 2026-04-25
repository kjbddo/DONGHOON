package com.algoforge.backend.submission.dto;

import com.algoforge.backend.submission.domain.Submission;

import java.time.OffsetDateTime;

public record SubmissionSummaryResponse(
        Long id,
        Long problemId,
        Long languageId,
        String status,
        Integer executionTimeMs,
        Integer memoryUsedKb,
        OffsetDateTime submittedAt,
        OffsetDateTime judgedAt
) {
    public static SubmissionSummaryResponse from(Submission s) {
        return new SubmissionSummaryResponse(
                s.getId(),
                s.getProblemId(),
                s.getLanguageId(),
                s.getStatus().name(),
                s.getExecutionTimeMs(),
                s.getMemoryUsedKb(),
                s.getSubmittedAt(),
                s.getJudgedAt()
        );
    }
}
