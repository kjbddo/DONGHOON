package com.algoforge.backend.problem.dto.admin;

import com.algoforge.backend.problem.domain.ProblemImportMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 문제 가져오기. {@code payload}는 {@link AdminProblemCreateRequest}와 동일 스키마이나
 * {@code sourceType}은 {@code mode}에 의해 덮어쓴다.
 */
public record ProblemImportRequest(
        @NotNull ProblemImportMode mode,
        @NotNull @Valid AdminProblemCreateRequest payload,
        /** LICENSED_IMPORT 일 때 필수 */
        Boolean licenseAcknowledged,
        /** 출처 페이지 (정책상 도메인 검사에 사용) */
        String sourceUrl,
        /** 선택: 사이트 표기명 (로그/관리용) */
        String sourceSite
) {}
