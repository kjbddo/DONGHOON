package com.algoforge.backend.submission.controller;

import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.security.CurrentUser;
import com.algoforge.backend.submission.dto.SubmissionDetailResponse;
import com.algoforge.backend.submission.dto.SubmissionSummaryResponse;
import com.algoforge.backend.submission.dto.SubmitCodeRequest;
import com.algoforge.backend.submission.service.SubmissionService;
import com.algoforge.backend.submission.sse.SubmissionEventPublisher;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Submission", description = "코드 제출 / 제출 목록 / SSE 실시간 채점 결과")
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionEventPublisher eventPublisher;

    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionSummaryResponse>> submit(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody SubmitCodeRequest req
    ) {
        Long userId = requireUserId(currentUser);
        SubmissionSummaryResponse res = submissionService.submit(userId, req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(res));
    }

    @GetMapping
    public ApiResponse<Page<SubmissionSummaryResponse>> listMine(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) Long problemId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = requireUserId(currentUser);
        return ApiResponse.ok(submissionService.listMine(userId, problemId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<SubmissionDetailResponse> get(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id
    ) {
        Long userId = requireUserId(currentUser);
        return ApiResponse.ok(submissionService.getDetail(userId, id));
    }

    /**
     * 채점 결과 실시간 스트림.
     * Nginx 프록시를 사용한다면 location에 X-Accel-Buffering off 설정 필요.
     */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id
    ) {
        Long userId = requireUserId(currentUser);
        // 본인 제출인지 확인 (forbidden 시 즉시 거절)
        submissionService.getDetail(userId, id);
        return eventPublisher.subscribe(id);
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.userId();
    }
}
