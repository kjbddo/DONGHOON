package com.algoforge.backend.user.service;

import com.algoforge.backend.submission.repository.UserSolvedProblemRepository;
import com.algoforge.backend.user.domain.User;
import com.algoforge.backend.user.dto.RankingEntryResponse;
import com.algoforge.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 푼 문제 수 기반 랭킹.
 *
 *  계산 절차:
 *   1) UserSolvedProblem GROUP BY user_id ORDER BY count DESC, user_id ASC 페이지네이션
 *   2) 페이지 내 user_id에 해당하는 User 일괄 조회
 *   3) rank = (page * size + index + 1) — 동률 처리는 단순 표시 순위만 부여
 *      (정확한 1-based 동률 랭크가 필요하면 UserStatsService.countUsersAheadOf 사용)
 *
 *  주의: solved 수가 0인 사용자는 랭킹에 포함되지 않음.
 */
@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserSolvedProblemRepository solvedRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<RankingEntryResponse> getRanking(Pageable pageable) {
        Page<Object[]> rows = solvedRepository.findTopSolvers(pageable);
        if (rows.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Long> userIds = new ArrayList<>(rows.getNumberOfElements());
        for (Object[] row : rows.getContent()) {
            userIds.add(((Number) row[0]).longValue());
        }

        Map<Long, User> users = new HashMap<>();
        for (User u : userRepository.findAllById(userIds)) {
            users.put(u.getId(), u);
        }

        long base = (long) pageable.getPageNumber() * pageable.getPageSize();
        List<RankingEntryResponse> entries = new ArrayList<>(rows.getNumberOfElements());
        for (int i = 0; i < rows.getContent().size(); i++) {
            Object[] row = rows.getContent().get(i);
            Long userId = ((Number) row[0]).longValue();
            long solved = ((Number) row[1]).longValue();
            User user = users.get(userId);
            if (user == null) continue; // soft-deleted 사용자는 스킵
            entries.add(new RankingEntryResponse(
                    base + i + 1,
                    userId,
                    user.getUsername(),
                    user.getProfileImageUrl(),
                    solved
            ));
        }

        return new PageImpl<>(entries, pageable, rows.getTotalElements());
    }
}
