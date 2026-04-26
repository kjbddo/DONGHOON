package com.algoforge.backend.problem.service;

import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.domain.ProblemStatus;
import com.algoforge.backend.problem.domain.TestCase;
import com.algoforge.backend.problem.dto.ProblemDetailResponse;
import com.algoforge.backend.problem.dto.ProblemSummaryResponse;
import com.algoforge.backend.problem.dto.ProblemUserStatus;
import com.algoforge.backend.problem.repository.ProblemRepository;
import com.algoforge.backend.problem.repository.TestCaseRepository;
import com.algoforge.backend.submission.repository.SubmissionRepository;
import com.algoforge.backend.submission.repository.UserSolvedProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final UserSolvedProblemRepository userSolvedProblemRepository;
    private final SubmissionRepository submissionRepository;

    public Page<ProblemSummaryResponse> listPublic(
            Difficulty difficulty,
            Boolean aiOnly,
            String category,
            String keyword,
            Pageable pageable
    ) {
        return listPublic(difficulty, aiOnly, category, keyword, pageable, null);
    }

    /**
     * 공개 목록 조회. {@code currentUserId}가 주어지면 각 항목에 SOLVED/WRONG 상태를 채운다.
     * - SOLVED : user_solved_problems 에 (userId, problemId) 존재
     * - WRONG  : 제출 이력은 있으나 아직 SOLVED 가 아님
     * - 비로그인 또는 시도 자체가 없으면 userStatus = null
     */
    public Page<ProblemSummaryResponse> listPublic(
            Difficulty difficulty,
            Boolean aiOnly,
            String category,
            String keyword,
            Pageable pageable,
            Long currentUserId
    ) {
        Page<Problem> page = problemRepository.searchPublic(
                ProblemStatus.PUBLIC,
                difficulty,
                aiOnly,
                blankToNull(category),
                blankToNull(keyword),
                pageable
        );

        if (currentUserId == null || page.isEmpty()) {
            return page.map(ProblemSummaryResponse::from);
        }

        List<Long> ids = page.getContent().stream().map(Problem::getId).toList();
        Set<Long> solvedIds = new HashSet<>(
                userSolvedProblemRepository.findSolvedProblemIds(currentUserId, ids));
        Set<Long> attemptedIds = new HashSet<>(
                submissionRepository.findAttemptedProblemIds(currentUserId, ids));

        return page.map(p -> ProblemSummaryResponse.from(p, resolveStatus(p.getId(), solvedIds, attemptedIds)));
    }

    private ProblemUserStatus resolveStatus(Long problemId, Set<Long> solved, Set<Long> attempted) {
        if (solved.contains(problemId)) return ProblemUserStatus.SOLVED;
        if (attempted.contains(problemId)) return ProblemUserStatus.WRONG;
        return null;
    }

    public ProblemDetailResponse getPublic(Long id) {
        Problem problem = problemRepository.findWithRelationsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        if (!problem.isPubliclyVisible()) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_PUBLIC);
        }
        List<TestCase> publicCases =
                testCaseRepository.findByProblem_IdAndHiddenFalseOrderBySeqAsc(problem.getId());
        return ProblemDetailResponse.of(problem, publicCases);
    }

    public ProblemDetailResponse getPublicBySlug(String slug) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        if (!problem.isPubliclyVisible()) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_PUBLIC);
        }
        List<TestCase> publicCases =
                testCaseRepository.findByProblem_IdAndHiddenFalseOrderBySeqAsc(problem.getId());
        return ProblemDetailResponse.of(problem, publicCases);
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
