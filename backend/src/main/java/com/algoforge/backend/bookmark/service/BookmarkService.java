package com.algoforge.backend.bookmark.service;

import com.algoforge.backend.bookmark.domain.Bookmark;
import com.algoforge.backend.bookmark.repository.BookmarkRepository;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.domain.ProblemStatus;
import com.algoforge.backend.problem.dto.ProblemSummaryResponse;
import com.algoforge.backend.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 북마크 도메인 서비스.
 *
 *  정책:
 *   - PUBLIC 상태인 문제만 새로 북마크 가능 (DRAFT/HIDDEN/REPORTED은 거부, 단 기존 북마크는 유지)
 *   - 동일 (사용자, 문제) 중복 등록은 idempotent (이미 있으면 무시하고 성공)
 *   - 목록 조회 시 북마크가 가리키는 문제가 DELETED여도 노출은 막지 않고 클라이언트가 처리하게 둠
 */
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ProblemRepository problemRepository;

    @Transactional
    public boolean addBookmark(Long userId, Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        if (problem.getStatus() != ProblemStatus.PUBLIC) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_PUBLIC,
                    "공개 상태의 문제만 북마크할 수 있습니다.");
        }

        Bookmark.Pk pk = new Bookmark.Pk(userId, problemId);
        if (bookmarkRepository.existsById(pk)) {
            return false;
        }
        try {
            bookmarkRepository.save(new Bookmark(userId, problemId));
            return true;
        } catch (DataIntegrityViolationException dup) {
            return false;
        }
    }

    @Transactional
    public boolean removeBookmark(Long userId, Long problemId) {
        return bookmarkRepository.deleteByUserAndProblem(userId, problemId) > 0;
    }

    @Transactional(readOnly = true)
    public Page<ProblemSummaryResponse> listBookmarks(Long userId, Pageable pageable) {
        Page<Long> idPage = bookmarkRepository.findProblemIdsByUserId(userId, pageable);
        List<Long> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }

        Map<Long, Problem> byId = new HashMap<>();
        for (Problem p : problemRepository.findByIdIn(ids)) {
            byId.put(p.getId(), p);
        }

        List<ProblemSummaryResponse> items = ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(ProblemSummaryResponse::from)
                .toList();

        return new PageImpl<>(items, pageable, idPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long problemId) {
        return bookmarkRepository.existsById(new Bookmark.Pk(userId, problemId));
    }
}
