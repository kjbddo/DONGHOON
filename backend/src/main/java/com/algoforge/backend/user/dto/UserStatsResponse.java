package com.algoforge.backend.user.dto;

import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.submission.domain.SubmissionStatus;

import java.util.Map;

/**
 * 사용자 통계 응답.
 *
 *  - solvedCount     : 정답 처리된 distinct 문제 수
 *  - attemptedCount  : 1회 이상 제출한 distinct 문제 수
 *  - totalSubmissions: 총 제출 건수
 *  - acceptanceRate  : ACCEPTED / totalSubmissions (소수점 4자리)
 *  - bookmarkCount   : 북마크한 문제 수
 *  - rank            : 푼 문제 수 기준 랭킹 (1-based, 동률은 같은 랭크)
 *  - solvedByDifficulty : 난이도별 해결 수
 *  - submissionsByStatus: 상태별 제출 수
 *  - languageUsage   : 언어id별 제출 수 (TOP n)
 */
public record UserStatsResponse(
        Long userId,
        String username,
        long solvedCount,
        long attemptedCount,
        long totalSubmissions,
        double acceptanceRate,
        long bookmarkCount,
        long rank,
        Map<Difficulty, Long> solvedByDifficulty,
        Map<SubmissionStatus, Long> submissionsByStatus,
        Map<Long, Long> languageUsage
) {}
