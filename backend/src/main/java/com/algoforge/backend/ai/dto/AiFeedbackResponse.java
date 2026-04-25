package com.algoforge.backend.ai.dto;

import com.algoforge.backend.ai.domain.AiFeedback;

import java.time.OffsetDateTime;

public record AiFeedbackResponse(
        Long id,
        Long submissionId,
        Integer feedbackLevel,
        String summary,
        String directionHint,
        String counterExampleHint,
        String complexityHint,
        String runtimeErrorHint,
        String compileErrorHint,
        OffsetDateTime createdAt
) {
    public static AiFeedbackResponse from(AiFeedback f) {
        return new AiFeedbackResponse(
                f.getId(),
                f.getSubmissionId(),
                f.getFeedbackLevel() == null ? null : f.getFeedbackLevel().intValue(),
                f.getSummary(),
                f.getDirectionHint(),
                f.getCounterExampleHint(),
                f.getComplexityHint(),
                f.getRuntimeErrorHint(),
                f.getCompileErrorHint(),
                f.getCreatedAt()
        );
    }
}
