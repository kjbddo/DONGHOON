package com.algoforge.backend.user.dto;

import com.algoforge.backend.user.domain.User;

import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String username,
        String profileImageUrl,
        String status,
        List<String> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getProfileImageUrl(),
                user.getStatus().name(),
                user.getRoleNames()
        );
    }
}
