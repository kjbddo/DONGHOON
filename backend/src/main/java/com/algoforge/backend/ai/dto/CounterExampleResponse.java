package com.algoforge.backend.ai.dto;

import com.algoforge.backend.ai.domain.CounterExample;

import java.time.OffsetDateTime;
import java.util.List;

public record CounterExampleResponse(
        Long submissionId,
        List<Item> items
) {
    public record Item(
            Long id,
            String input,
            String expectedOutput,
            String reason,
            String relatedConstraint,
            OffsetDateTime createdAt
    ) {
        public static Item from(CounterExample c) {
            return new Item(
                    c.getId(),
                    c.getInputData(),
                    c.getExpectedOutput(),
                    c.getReason(),
                    c.getRelatedConstraint(),
                    c.getCreatedAt()
            );
        }
    }

    public static CounterExampleResponse of(Long submissionId, List<CounterExample> items) {
        return new CounterExampleResponse(
                submissionId,
                items.stream().map(Item::from).toList()
        );
    }
}
