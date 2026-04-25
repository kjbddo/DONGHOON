package com.algoforge.backend.ai.dto;

import java.util.List;

/**
 * AI 서버가 반환하는 문제 스키마.
 * (Pydantic GeneratedProblemSchema와 동일 필드명)
 */
public record GeneratedProblemAiResponse(
        String title,
        String category,
        String difficulty,
        String description,
        String inputDescription,
        String outputDescription,
        List<String> constraints,
        List<ExampleAi> examples,
        List<ImageAi> images,
        List<TestCaseAi> testCases,
        String solutionOutline,
        SolutionCodeAi officialSolutionCode,
        Integer timeLimit,                 // 초 단위
        Integer memoryLimit,               // MB
        String sourceType,                 // AI_GENERATED / AI_REWRITTEN_SOURCE_BASED
        Boolean isAiGenerated
) {
    public record ExampleAi(String input, String output, String explanation) {}
    public record ImageAi(String url, String description) {}
    public record TestCaseAi(String input, String output, Boolean isHidden) {}
    public record SolutionCodeAi(String language, String code) {}
}
