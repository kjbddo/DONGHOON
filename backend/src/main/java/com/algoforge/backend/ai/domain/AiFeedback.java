package com.algoforge.backend.ai.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(
        name = "ai_feedbacks",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_feedbacks_submission_level",
                columnNames = {"submission_id", "feedback_level"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "feedback_level", nullable = false)
    private Short feedbackLevel;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "direction_hint", columnDefinition = "TEXT")
    private String directionHint;

    @Column(name = "counter_example_hint", columnDefinition = "TEXT")
    private String counterExampleHint;

    @Column(name = "complexity_hint", columnDefinition = "TEXT")
    private String complexityHint;

    @Column(name = "runtime_error_hint", columnDefinition = "TEXT")
    private String runtimeErrorHint;

    @Column(name = "compile_error_hint", columnDefinition = "TEXT")
    private String compileErrorHint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_ai_response", columnDefinition = "jsonb")
    private JsonNode rawAiResponse;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    private AiFeedback(Long submissionId, Long userId, Long problemId,
                       Short feedbackLevel,
                       String summary, String directionHint, String counterExampleHint,
                       String complexityHint, String runtimeErrorHint, String compileErrorHint,
                       JsonNode rawAiResponse,
                       String modelName, String promptVersion, Integer tokenUsage) {
        this.submissionId = submissionId;
        this.userId = userId;
        this.problemId = problemId;
        this.feedbackLevel = feedbackLevel;
        this.summary = summary;
        this.directionHint = directionHint;
        this.counterExampleHint = counterExampleHint;
        this.complexityHint = complexityHint;
        this.runtimeErrorHint = runtimeErrorHint;
        this.compileErrorHint = compileErrorHint;
        this.rawAiResponse = rawAiResponse;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.tokenUsage = tokenUsage;
        this.createdAt = OffsetDateTime.now();
    }
}
