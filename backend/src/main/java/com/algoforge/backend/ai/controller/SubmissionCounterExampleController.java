package com.algoforge.backend.ai.controller;

import com.algoforge.backend.ai.dto.CounterExampleResponse;
import com.algoforge.backend.ai.service.AiCounterExampleService;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI/CounterExample", description = "제출 단위 AI 반례 생성/조회")
@RestController
@RequestMapping("/api/submissions/{id}/counter-examples")
@RequiredArgsConstructor
public class SubmissionCounterExampleController {

    private final AiCounterExampleService service;

    @PostMapping
    public ApiResponse<CounterExampleResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable("id") Long submissionId
    ) {
        Long userId = requireUserId(currentUser);
        return ApiResponse.ok(service.getOrCreate(submissionId, userId));
    }

    @GetMapping
    public ApiResponse<CounterExampleResponse> get(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable("id") Long submissionId
    ) {
        Long userId = requireUserId(currentUser);
        return ApiResponse.ok(service.getCachedOnly(submissionId, userId));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return currentUser.userId();
    }
}
