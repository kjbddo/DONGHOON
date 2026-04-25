package com.algoforge.backend.ai.controller;

import com.algoforge.backend.ai.dto.GenerateProblemAiRequest;
import com.algoforge.backend.ai.service.AiProblemGenerationService;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.problem.dto.admin.AdminProblemDetailResponse;
import com.algoforge.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 - AI를 통해 새로운 문제(DRAFT 상태)를 생성한다.
 * 생성된 문제는 별도의 PUBLISH 단계를 거쳐 공개된다.
 */
@Tag(name = "Admin/AI", description = "관리자: AI 기반 문제 생성")
@RestController
@RequestMapping("/api/admin/problems/ai")
@RequiredArgsConstructor
public class AdminAiProblemController {

    private final AiProblemGenerationService aiProblemGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<AdminProblemDetailResponse>> generate(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody(required = false) GenerateProblemAiRequest req
    ) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        GenerateProblemAiRequest payload = req == null
                ? new GenerateProblemAiRequest(null, null, null, null)
                : req;
        AdminProblemDetailResponse created = aiProblemGenerationService.generate(payload, currentUser.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }
}
