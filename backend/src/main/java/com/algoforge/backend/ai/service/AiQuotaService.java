package com.algoforge.backend.ai.service;

import com.algoforge.backend.ai.repository.AiCallLogRepository;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.config.AiQuotaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * AI 호출 직전에 호출하여 일일 사용량 한도를 검증한다.
 *
 *  - 사용자 한도 초과: ErrorCode.AI_QUOTA_EXCEEDED (HTTP 429)
 *  - 글로벌 한도 초과: 동일 ErrorCode (운영 비용 보호)
 *
 * 한도 비활성화(<=0)는 검사 스킵.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private final AiCallLogRepository repository;
    private final AiQuotaProperties properties;

    @Transactional(readOnly = true)
    public void assertWithinDailyQuota(Long userId) {
        OffsetDateTime since = startOfTodayUtc();

        int perUser = properties.dailyPerUserOrDefault();
        if (perUser > 0 && userId != null) {
            long used = repository.countSuccessfulByUserSince(userId, since);
            if (used >= perUser) {
                log.info("[AiQuota] user {} 한도 초과 ({} / {})", userId, used, perUser);
                throw new BusinessException(
                        ErrorCode.AI_QUOTA_EXCEEDED,
                        "오늘 사용 가능한 AI 호출 한도(" + perUser + "회)를 초과했습니다."
                );
            }
        }

        int global = properties.dailyGlobalOrDefault();
        if (global > 0) {
            long used = repository.countSuccessfulSince(since);
            if (used >= global) {
                log.warn("[AiQuota] 글로벌 일일 한도 초과 ({} / {})", used, global);
                throw new BusinessException(
                        ErrorCode.AI_QUOTA_EXCEEDED,
                        "전체 AI 사용량이 오늘 한도에 도달했습니다. 잠시 후 다시 시도하세요."
                );
            }
        }
    }

    /**
     * 사용자에게 남은 호출 수를 반환 (대시보드 표시용). 한도 비활성화 시 -1.
     */
    @Transactional(readOnly = true)
    public int remainingForUser(Long userId) {
        int perUser = properties.dailyPerUserOrDefault();
        if (perUser <= 0 || userId == null) return -1;
        long used = repository.countSuccessfulByUserSince(userId, startOfTodayUtc());
        return Math.max(0, perUser - (int) Math.min(used, Integer.MAX_VALUE));
    }

    private OffsetDateTime startOfTodayUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
    }
}
