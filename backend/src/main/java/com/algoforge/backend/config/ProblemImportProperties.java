package com.algoforge.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 외부 문제 가져오기(라이선스) 정책.
 *
 *   algoforge.problem.import.licensed-allowed-hosts: 비어 있으면
 *     - LICENSED_IMPORT 는 licenseAck + (선택) sourceUrl 만 검사.
 *   값이 있으면 sourceUrl의 호스트가 목록에 하나라도 맞지 않으면 {@code IMPORT_BLOCKED_BY_POLICY}
 */
@ConfigurationProperties(prefix = "algoforge.problem.import")
public record ProblemImportProperties(
        List<String> licensedAllowedHosts
) {
    public List<String> licensedAllowedHostsOrEmpty() {
        return licensedAllowedHosts == null ? List.of() : licensedAllowedHosts;
    }
}
