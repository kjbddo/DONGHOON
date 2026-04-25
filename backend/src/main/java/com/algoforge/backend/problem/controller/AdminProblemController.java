package com.algoforge.backend.problem.controller;

import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.problem.domain.ProblemStatus;
import com.algoforge.backend.problem.dto.admin.AdminProblemCreateRequest;
import com.algoforge.backend.problem.dto.admin.AdminProblemDetailResponse;
import com.algoforge.backend.problem.dto.admin.AdminProblemSummaryResponse;
import com.algoforge.backend.problem.dto.admin.AdminProblemUpdateRequest;
import com.algoforge.backend.problem.dto.admin.ChangeStatusRequest;
import com.algoforge.backend.problem.service.AdminProblemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin/Problem", description = "관리자: 문제 CRUD / 상태 변경 / 소프트 삭제")
@RestController
@RequestMapping("/api/admin/problems")
@RequiredArgsConstructor
public class AdminProblemController {

    private final AdminProblemService adminProblemService;

    @GetMapping
    public ApiResponse<Page<AdminProblemSummaryResponse>> list(
            @RequestParam(required = false) ProblemStatus status,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false, name = "ai") Boolean aiOnly,
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(
                adminProblemService.list(status, difficulty, aiOnly, includeDeleted, keyword, pageable)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminProblemDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(adminProblemService.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminProblemDetailResponse>> create(
            @Valid @RequestBody AdminProblemCreateRequest req
    ) {
        AdminProblemDetailResponse res = adminProblemService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(res));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminProblemDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminProblemUpdateRequest req
    ) {
        return ApiResponse.ok(adminProblemService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminProblemDetailResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest req
    ) {
        return ApiResponse.ok(adminProblemService.changeStatus(id, req.status()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminProblemService.delete(id);
        return ApiResponse.ok();
    }
}
