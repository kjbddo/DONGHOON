package com.algoforge.backend.auth.dto;

public record SignUpResponse(
        Long userId,
        String email,
        String username
) {}
