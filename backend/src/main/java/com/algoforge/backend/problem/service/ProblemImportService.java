package com.algoforge.backend.problem.service;

import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.config.ProblemImportProperties;
import com.algoforge.backend.problem.domain.ProblemImportMode;
import com.algoforge.backend.problem.domain.ProblemSourceType;
import com.algoforge.backend.problem.dto.admin.AdminProblemCreateRequest;
import com.algoforge.backend.problem.dto.admin.AdminProblemDetailResponse;
import com.algoforge.backend.problem.dto.admin.ProblemImportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

/**
 * 외부 메타데이터 가져오기 + 운영 정책(라이선스·허용 도메인).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemImportService {

    private final AdminProblemService adminProblemService;
    private final ProblemImportProperties importProperties;

    public AdminProblemDetailResponse importProblem(ProblemImportRequest req) {
        assertPolicy(req);

        AdminProblemCreateRequest p = req.payload();
        ProblemSourceType sourceType = switch (req.mode()) {
            case METADATA_ONLY -> ProblemSourceType.ADMIN_CREATED;
            case LICENSED_IMPORT -> ProblemSourceType.LICENSED_IMPORTED;
            case AI_REWRITE_FROM_METADATA -> ProblemSourceType.AI_REWRITTEN_SOURCE_BASED;
        };

        AdminProblemCreateRequest merged = new AdminProblemCreateRequest(
                p.title(),
                p.slug(),
                p.description(),
                p.inputDescription(),
                p.outputDescription(),
                p.constraints(),
                p.examples(),
                p.timeLimitMs(),
                p.memoryLimitMb(),
                p.difficulty(),
                sourceType,
                p.categories(),
                p.tags(),
                p.testCases()
        );

        if (log.isInfoEnabled() && (req.sourceSite() != null || req.sourceUrl() != null)) {
            log.info("문제 import: mode={} sourceSite={} sourceUrl={} title={}",
                    req.mode(), req.sourceSite(), req.sourceUrl(), p.title());
        }
        return adminProblemService.create(merged);
    }

    private void assertPolicy(ProblemImportRequest req) {
        if (req.mode() == ProblemImportMode.LICENSED_IMPORT) {
            if (!Boolean.TRUE.equals(req.licenseAcknowledged())) {
                throw new BusinessException(ErrorCode.IMPORT_BLOCKED_BY_POLICY,
                        "라이선스·이용 약관에 동의한 경우에만 가져올 수 있습니다.");
            }
            List<String> allowed = importProperties.licensedAllowedHostsOrEmpty();
            if (!allowed.isEmpty()) {
                String url = req.sourceUrl();
                if (url == null || url.isBlank()) {
                    throw new BusinessException(ErrorCode.IMPORT_BLOCKED_BY_POLICY,
                            "이 환경에서는 가져올 문제의 출처 URL이 필요합니다.");
                }
                assertHostAllowed(url, allowed);
            }
        } else {
            if (importProperties.licensedAllowedHostsOrEmpty().isEmpty()) {
                return;
            }
            // 비-LICENSED 모드에서도 출처 URL이 있으면(선택) 동일 룰을 적용할지 — 여기서는
            // LICENSED 전용으로 두어 METADATA/AI_REWRITE 는 제한 없음
        }
    }

    private void assertHostAllowed(String url, List<String> allowedHosts) {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.IMPORT_BLOCKED_BY_POLICY, "출처 URL 형식이 올바르지 않습니다.");
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_BLOCKED_BY_POLICY, "URL에서 호스트를 찾을 수 없습니다.");
        }
        String h = host.toLowerCase();
        boolean ok = allowedHosts.stream().anyMatch(pattern -> {
            String p = pattern.trim().toLowerCase();
            if (p.isEmpty()) return false;
            return h.equals(p) || h.endsWith("." + p);
        });
        if (!ok) {
            throw new BusinessException(ErrorCode.IMPORT_BLOCKED_BY_POLICY,
                    "가져올 수 없는 출처 도메인입니다. 운영자에게 문의하세요.");
        }
    }
}
