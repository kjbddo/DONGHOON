package com.algoforge.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(min = 2, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "username은 영문/숫자/_/-만 사용할 수 있습니다.")
        String username,

        @NotBlank @Size(min = 8, max = 100)
        String password
) {}
