package com.algoforge.backend.ai.dto;

import java.util.List;

public record CounterExampleAiRequest(
        String problemTitle,
        String description,
        List<String> constraints,
        String userCode,
        String language,
        String failedTestExcerpt
) {}
