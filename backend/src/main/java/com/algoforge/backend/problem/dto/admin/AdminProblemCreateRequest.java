package com.algoforge.backend.problem.dto.admin;

import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.problem.domain.Example;
import com.algoforge.backend.problem.domain.ProblemSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminProblemCreateRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String slug,
        @NotBlank String description,
        @NotBlank String inputDescription,
        @NotBlank String outputDescription,
        @NotNull List<String> constraints,
        @NotNull @Valid List<Example> examples,
        @Min(100) Integer timeLimitMs,
        @Min(16) Integer memoryLimitMb,
        @NotNull Difficulty difficulty,
        ProblemSourceType sourceType,
        List<String> categories,
        List<String> tags,
        @NotNull @Valid List<TestCaseDto> testCases
) {}
