package com.algoforge.backend.problem.dto.admin;

import com.algoforge.backend.problem.domain.Problem;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminProblemSummaryResponse(
        Long id,
        String slug,
        String title,
        String difficulty,
        String status,
        String sourceType,
        boolean aiGenerated,
        BigDecimal qualityScore,
        int reportCount,
        OffsetDateTime createdAt
) {
    public static AdminProblemSummaryResponse from(Problem p) {
        return new AdminProblemSummaryResponse(
                p.getId(),
                p.getSlug(),
                p.getTitle(),
                p.getDifficulty().name(),
                p.getStatus().name(),
                p.getSourceType().name(),
                p.isAiGenerated(),
                p.getQualityScore(),
                p.getReportCount(),
                p.getCreatedAt()
        );
    }
}
