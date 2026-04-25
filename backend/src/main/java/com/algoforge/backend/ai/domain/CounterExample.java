package com.algoforge.backend.ai.domain;

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

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "counter_examples")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounterExample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "user_id")
    private Long userId;

    /** V1 스키마의 컬럼명은 `input`이다 (예약어 충돌 회피 위해 따옴표) */
    @Column(name = "input", nullable = false, columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "related_constraint", columnDefinition = "TEXT")
    private String relatedConstraint;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    private CounterExample(Long submissionId, Long problemId, Long userId,
                           String inputData, String expectedOutput,
                           String reason, String relatedConstraint, String source) {
        this.submissionId = submissionId;
        this.problemId = problemId;
        this.userId = userId;
        this.inputData = inputData;
        this.expectedOutput = expectedOutput;
        this.reason = reason;
        this.relatedConstraint = relatedConstraint;
        this.source = source == null ? "AI" : source;
        this.createdAt = OffsetDateTime.now();
    }
}
