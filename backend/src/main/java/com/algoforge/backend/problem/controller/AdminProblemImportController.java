package com.algoforge.backend.problem.controller;

import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.problem.dto.admin.AdminProblemDetailResponse;
import com.algoforge.backend.problem.dto.admin.ProblemImportRequest;
import com.algoforge.backend.problem.service.ProblemImportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin/Import", description = "JSON 메타데이터 문제 가져오기 (정책/라이선스)")
@RestController
@RequestMapping("/api/admin/problems/import")
@RequiredArgsConstructor
public class AdminProblemImportController {

    private final ProblemImportService problemImportService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminProblemDetailResponse>> importJson(
            @RequestBody @Valid ProblemImportRequest body
    ) {
        AdminProblemDetailResponse created = problemImportService.importProblem(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }
}
