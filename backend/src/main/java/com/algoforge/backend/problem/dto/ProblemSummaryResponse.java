package com.algoforge.backend.problem.dto;

import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.domain.ProblemCategory;
import com.algoforge.backend.problem.domain.ProblemTag;

import java.util.List;

public record ProblemSummaryResponse(
        Long id,
        String slug,
        String title,
        String difficulty,
        boolean aiGenerated,
        List<String> categories,
        List<String> tags,
        ProblemUserStatus userStatus
) {
    public static ProblemSummaryResponse from(Problem p) {
        return from(p, null);
    }

    public static ProblemSummaryResponse from(Problem p, ProblemUserStatus userStatus) {
        return new ProblemSummaryResponse(
                p.getId(),
                p.getSlug(),
                p.getTitle(),
                p.getDifficulty().name(),
                p.isAiGenerated(),
                p.getCategories().stream().map(ProblemCategory::getName).toList(),
                p.getTags().stream().map(ProblemTag::getName).toList(),
                userStatus
        );
    }
}
