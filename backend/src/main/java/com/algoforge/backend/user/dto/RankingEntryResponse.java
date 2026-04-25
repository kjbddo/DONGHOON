package com.algoforge.backend.user.dto;

/**
 * 랭킹 페이지의 1행. rank는 페이지 + 동률 처리를 반영한 1-based 표시 순위.
 */
public record RankingEntryResponse(
        long rank,
        Long userId,
        String username,
        String profileImageUrl,
        long solvedCount
) {}
