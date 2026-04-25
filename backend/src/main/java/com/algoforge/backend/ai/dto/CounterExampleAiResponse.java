package com.algoforge.backend.ai.dto;

import java.util.List;

public record CounterExampleAiResponse(List<CounterExampleItemAi> counterExamples) {

    public record CounterExampleItemAi(
            String input,
            String expectedOutput,
            String reason,
            String relatedConstraint
    ) {}
}
