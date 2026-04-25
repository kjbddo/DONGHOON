package com.algoforge.backend.user.service;

import com.algoforge.backend.bookmark.repository.BookmarkRepository;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.submission.domain.SubmissionStatus;
import com.algoforge.backend.submission.repository.SubmissionRepository;
import com.algoforge.backend.submission.repository.UserSolvedProblemRepository;
import com.algoforge.backend.user.domain.User;
import com.algoforge.backend.user.dto.UserStatsResponse;
import com.algoforge.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 풀이 통계를 한 번에 모아서 반환한다.
 *
 *  쿼리 수: 7회 (페이지 단위가 아니라 1인분 통계이므로 비용 작음)
 *  - rank: 동률 처리는 (countUsersAheadOf + 1)로 1-based.
 *  - languageUsage: 키는 languageId. 표시용 이름은 프런트에서 별도 매핑.
 */
@Service
@RequiredArgsConstructor
public class UserStatsService {

    /** languageUsage 응답에 포함할 최대 항목 수 */
    private static final int LANGUAGE_USAGE_LIMIT = 10;

    private final UserRepository userRepository;
    private final UserSolvedProblemRepository solvedRepository;
    private final SubmissionRepository submissionRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional(readOnly = true)
    public UserStatsResponse getStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        long solvedCount = solvedRepository.countByIdUserId(userId);
        long attemptedCount = submissionRepository.countDistinctProblemsByUser(userId);
        long totalSubmissions = submissionRepository.countByUserId(userId);
        long bookmarkCount = bookmarkRepository.countByIdUserId(userId);

        Map<SubmissionStatus, Long> submissionsByStatus = toEnumMap(
                submissionRepository.countByUserGroupedByStatus(userId),
                SubmissionStatus.class,
                row -> (SubmissionStatus) row[0],
                row -> ((Number) row[1]).longValue()
        );

        long acceptedCount = submissionsByStatus.getOrDefault(SubmissionStatus.ACCEPTED, 0L);
        double acceptanceRate = totalSubmissions == 0
                ? 0.0
                : Math.round(((double) acceptedCount / totalSubmissions) * 10000.0) / 10000.0;

        Map<Difficulty, Long> solvedByDifficulty = toEnumMap(
                solvedRepository.countSolvedByDifficulty(userId),
                Difficulty.class,
                row -> (Difficulty) row[0],
                row -> ((Number) row[1]).longValue()
        );

        Map<Long, Long> languageUsage = new LinkedHashMap<>();
        List<Object[]> langRows = submissionRepository.countByUserGroupedByLanguage(userId);
        for (int i = 0; i < Math.min(langRows.size(), LANGUAGE_USAGE_LIMIT); i++) {
            Object[] row = langRows.get(i);
            languageUsage.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        long rank = solvedCount == 0 ? 0 : solvedRepository.countUsersAheadOf(userId) + 1;

        return new UserStatsResponse(
                user.getId(),
                user.getUsername(),
                solvedCount,
                attemptedCount,
                totalSubmissions,
                acceptanceRate,
                bookmarkCount,
                rank,
                solvedByDifficulty,
                submissionsByStatus,
                languageUsage
        );
    }

    private <K extends Enum<K>> Map<K, Long> toEnumMap(List<Object[]> rows,
                                                       Class<K> keyType,
                                                       java.util.function.Function<Object[], K> keyFn,
                                                       java.util.function.Function<Object[], Long> valueFn) {
        Map<K, Long> map = new EnumMap<>(keyType);
        for (Object[] row : rows) {
            K key = keyFn.apply(row);
            if (key == null) continue;
            map.put(key, valueFn.apply(row));
        }
        return map;
    }
}
