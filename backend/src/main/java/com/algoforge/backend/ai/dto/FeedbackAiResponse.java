package com.algoforge.backend.ai.dto;

public record FeedbackAiResponse(
        Integer feedbackLevel,
        String summary,
        String directionHint,
        String counterExampleHint,
        String complexityHint,
        String runtimeErrorHint,
        String compileErrorHint,
        Boolean shouldRevealAnswerCode
) {}
