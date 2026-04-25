package com.algoforge.backend.ai.controller;

import com.algoforge.backend.ai.dto.AiFeedbackResponse;
import com.algoforge.backend.ai.service.AiFeedbackService;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 제출에 대한 AI 피드백.
 * - level 1~4 (1: 가장 약한 힌트 ~ 4: 가장 강한 힌트, but 정답 코드 미공개 정책)
 * - 같은 (제출, level)은 1회만 호출 후 캐싱됨.
 */
@Tag(name = "AI/Feedback", description = "제출 단위 AI 피드백 (레벨 1~4)")
@RestController
@RequestMapping("/api/submissions/{id}/feedback")
@RequiredArgsConstructor
public class SubmissionFeedbackController {

    private final AiFeedbackService aiFeedbackService;

    @PostMapping
    public ApiResponse<AiFeedbackResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable("id") Long submissionId,
            @RequestParam(name = "level", defaultValue = "1") int level
    ) {
        Long userId = requireUserId(currentUser);
        return ApiResponse.ok(aiFeedbackService.getOrCreate(submissionId, userId, level));
    }

    @GetMapping
    public ApiResponse<AiFeedbackResponse> get(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable("id") Long submissionId,
            @RequestParam(name = "level", defaultValue = "1") int level
    ) {
        Long userId = requireUserId(currentUser);
        return ApiResponse.ok(aiFeedbackService.getCachedOnly(submissionId, userId, level));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return currentUser.userId();
    }
}
