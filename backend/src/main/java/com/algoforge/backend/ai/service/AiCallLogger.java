package com.algoforge.backend.ai.service;

import com.algoforge.backend.ai.domain.AiCallLog;
import com.algoforge.backend.ai.domain.AiCallPurpose;
import com.algoforge.backend.ai.repository.AiCallLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 호출 1건당 1행을 ai_call_logs에 적재한다.
 *
 *  - 호출자(서비스)는 시작 시각만 측정해 build 단계에 latencyMs를 넣어주면 된다.
 *  - 새로운 트랜잭션({@link Propagation#REQUIRES_NEW})에서 저장하여
 *    상위 트랜잭션의 롤백/실패와 독립적으로 로그가 보존된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCallLogger {

    private static final int ERROR_MSG_MAX = 4000;

    private final AiCallLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(AiCallPurpose purpose,
                           Long userId,
                           String modelName,
                           String promptVersion,
                           Object request,
                           Object response,
                           long latencyMs) {
        try {
            repository.save(AiCallLog.builder()
                    .purpose(purpose)
                    .userId(userId)
                    .modelName(modelName == null ? "unknown" : modelName)
                    .promptVersion(promptVersion)
                    .requestPayload(toJson(request))
                    .responsePayload(toJson(response))
                    .latencyMs((int) Math.min(latencyMs, Integer.MAX_VALUE))
                    .success(true)
                    .build());
        } catch (RuntimeException e) {
            log.warn("[AiCallLogger] success log persist failed: {}", e.toString());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(AiCallPurpose purpose,
                           Long userId,
                           String modelName,
                           String promptVersion,
                           Object request,
                           long latencyMs,
                           Throwable error) {
        try {
            repository.save(AiCallLog.builder()
                    .purpose(purpose)
                    .userId(userId)
                    .modelName(modelName == null ? "unknown" : modelName)
                    .promptVersion(promptVersion)
                    .requestPayload(toJson(request))
                    .latencyMs((int) Math.min(latencyMs, Integer.MAX_VALUE))
                    .success(false)
                    .errorMessage(truncate(error == null ? null : error.toString()))
                    .build());
        } catch (RuntimeException e) {
            log.warn("[AiCallLogger] failure log persist failed: {}", e.toString());
        }
    }

    private JsonNode toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.valueToTree(obj);
        } catch (Exception e) {
            log.warn("[AiCallLogger] payload serialization failed: {}", e.toString());
            return null;
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() <= ERROR_MSG_MAX ? s : s.substring(0, ERROR_MSG_MAX) + "...[truncated]";
    }
}
