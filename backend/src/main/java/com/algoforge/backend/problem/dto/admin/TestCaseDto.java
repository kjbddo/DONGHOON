package com.algoforge.backend.problem.dto.admin;

import jakarta.validation.constraints.NotNull;

public record TestCaseDto(
        Integer seq,
        @NotNull String input,
        @NotNull String expectedOutput,
        boolean hidden
) {}
