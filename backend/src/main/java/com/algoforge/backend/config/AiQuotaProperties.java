package com.algoforge.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 호출 일일 quota 설정. (UTC 기준 자정 리셋)
 *
 *   algoforge.ai.quota.daily-per-user : 사용자 1인 일일 호출 한도
 *   algoforge.ai.quota.daily-global   : 전체 일일 호출 한도 (비용 보호)
 *
 * 0 또는 음수면 해당 한도 비활성화.
 */
@ConfigurationProperties(prefix = "algoforge.ai.quota")
public record AiQuotaProperties(
        Integer dailyPerUser,
        Integer dailyGlobal
) {
    public int dailyPerUserOrDefault() {
        return dailyPerUser == null ? 30 : dailyPerUser;
    }

    public int dailyGlobalOrDefault() {
        return dailyGlobal == null ? 1000 : dailyGlobal;
    }
}
