package com.algoforge.backend.ai.dto;

import java.util.List;

public record FeedbackAiRequest(
        String problemTitle,
        String description,
        String inputDescription,
        String outputDescription,
        List<String> constraints,
        String userCode,
        String language,
        String judgeStatus,
        Integer feedbackLevel,
        String failedTestExcerpt,
        String runtimeErrorMessage,
        String compileErrorMessage
) {}
