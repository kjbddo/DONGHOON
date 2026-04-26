package com.algoforge.backend.problem.controller;

import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.problem.dto.ProblemDetailResponse;
import com.algoforge.backend.problem.dto.ProblemSummaryResponse;
import com.algoforge.backend.problem.service.ProblemService;
import com.algoforge.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Problem", description = "공개 문제 목록/상세 조회 (인증 불필요)")
@SecurityRequirements
@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public ApiResponse<Page<ProblemSummaryResponse>> list(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false, name = "ai") Boolean aiOnly,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = currentUser != null ? currentUser.userId() : null;
        return ApiResponse.ok(problemService.listPublic(difficulty, aiOnly, category, keyword, pageable, userId));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<ProblemDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(problemService.getPublic(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<ProblemDetailResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.ok(problemService.getPublicBySlug(slug));
    }
}
