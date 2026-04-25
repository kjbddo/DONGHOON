package com.algoforge.backend.bookmark.controller;

import com.algoforge.backend.bookmark.service.BookmarkService;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.problem.dto.ProblemSummaryResponse;
import com.algoforge.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 북마크 API. 모든 엔드포인트는 로그인 필요.
 */
@Tag(name = "Bookmark", description = "내 북마크 추가/삭제/목록")
@RestController
@RequestMapping("/api/users/me/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    public ApiResponse<Page<ProblemSummaryResponse>> list(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(bookmarkService.listBookmarks(requireUserId(currentUser), pageable));
    }

    @PostMapping("/{problemId}")
    public ResponseEntity<ApiResponse<Boolean>> add(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long problemId
    ) {
        boolean created = bookmarkService.addBookmark(requireUserId(currentUser), problemId);
        // 새로 만든 경우 201, 이미 있어 idempotent로 처리된 경우 200
        return ResponseEntity
                .status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.ok(created));
    }

    @DeleteMapping("/{problemId}")
    public ApiResponse<Boolean> remove(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long problemId
    ) {
        // 200 + false: 이미 없음(멱등 삭제) — axios 등 클라이언트가 404로 실패하지 않도록
        return ApiResponse.ok(bookmarkService.removeBookmark(requireUserId(currentUser), problemId));
    }

    @GetMapping("/{problemId}/exists")
    public ApiResponse<Boolean> exists(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long problemId
    ) {
        return ApiResponse.ok(bookmarkService.isBookmarked(requireUserId(currentUser), problemId));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.userId();
    }
}
