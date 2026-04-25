package com.algoforge.backend.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "submissions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "language_id", nullable = false)
    private Long languageId;

    @Column(name = "code", nullable = false, columnDefinition = "TEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubmissionStatus status;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "memory_used_kb")
    private Integer memoryUsedKb;

    @Column(name = "compile_error_message", columnDefinition = "TEXT")
    private String compileErrorMessage;

    @Column(name = "runtime_error_message", columnDefinition = "TEXT")
    private String runtimeErrorMessage;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "judged_at")
    private OffsetDateTime judgedAt;

    @Builder
    private Submission(Long userId, Long problemId, Long languageId, String code) {
        this.userId = userId;
        this.problemId = problemId;
        this.languageId = languageId;
        this.code = code;
        this.status = SubmissionStatus.PENDING;
        this.submittedAt = OffsetDateTime.now();
    }

    public void markJudging() {
        this.status = SubmissionStatus.JUDGING;
    }

    public void applyResult(SubmissionStatus next,
                            Integer executionTimeMs,
                            Integer memoryUsedKb,
                            String compileErrorMessage,
                            String runtimeErrorMessage) {
        this.status = next;
        this.executionTimeMs = executionTimeMs;
        this.memoryUsedKb = memoryUsedKb;
        this.compileErrorMessage = compileErrorMessage;
        this.runtimeErrorMessage = runtimeErrorMessage;
        this.judgedAt = OffsetDateTime.now();
    }
}
