package com.algoforge.backend.problem.service;

import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.domain.ProblemStatus;
import com.algoforge.backend.problem.domain.TestCase;
import com.algoforge.backend.problem.dto.ProblemDetailResponse;
import com.algoforge.backend.problem.dto.ProblemSummaryResponse;
import com.algoforge.backend.problem.repository.ProblemRepository;
import com.algoforge.backend.problem.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    public Page<ProblemSummaryResponse> listPublic(
            Difficulty difficulty,
            Boolean aiOnly,
            String keyword,
            Pageable pageable
    ) {
        return problemRepository
                .searchPublic(ProblemStatus.PUBLIC, difficulty, aiOnly, blankToNull(keyword), pageable)
                .map(ProblemSummaryResponse::from);
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
