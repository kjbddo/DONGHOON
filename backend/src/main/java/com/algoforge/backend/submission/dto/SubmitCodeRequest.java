package com.algoforge.backend.submission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitCodeRequest(
        @NotNull Long problemId,

        @NotBlank
        @Size(min = 2, max = 20)
        String language,

        @NotBlank
        @Size(min = 1, max = 200_000)
        String code
) {}
