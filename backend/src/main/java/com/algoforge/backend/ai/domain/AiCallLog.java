package com.algoforge.backend.ai.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * AI 서버 호출 1건당 1행. quota 산정과 비용 추적, 디버깅에 활용된다.
 *
 * - request/response payload는 JSONB로 저장하되, 민감정보는 logger 단에서 마스킹할 것.
 * - userId가 NULL이면 시스템 호출(관리자 트리거 외 백그라운드 작업).
 */
@Getter
@Entity
@Table(name = "ai_call_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PROBLEM_GEN / FEEDBACK / COUNTER_EXAMPLE — {@link AiCallPurpose} */
    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private JsonNode requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private JsonNode responsePayload;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 10, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    private AiCallLog(AiCallPurpose purpose, Long userId, String modelName, String promptVersion,
                      JsonNode requestPayload, JsonNode responsePayload,
                      Integer inputTokens, Integer outputTokens, BigDecimal costUsd,
                      Integer latencyMs, boolean success, String errorMessage) {
        this.purpose = purpose.name();
        this.userId = userId;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.costUsd = costUsd;
        this.latencyMs = latencyMs;
        this.success = success;
        this.errorMessage = errorMessage;
        this.createdAt = OffsetDateTime.now();
    }
}
