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

@Getter
@Entity
@Table(name = "submission_test_case_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubmissionTestCaseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "test_case_id", nullable = false)
    private Long testCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubmissionStatus status;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "memory_used_kb")
    private Integer memoryUsedKb;

    @Column(name = "output_excerpt", columnDefinition = "TEXT")
    private String outputExcerpt;

    @Builder
    private SubmissionTestCaseResult(Long submissionId, Long testCaseId,
                                     SubmissionStatus status,
                                     Integer executionTimeMs, Integer memoryUsedKb,
                                     String outputExcerpt) {
        this.submissionId = submissionId;
        this.testCaseId = testCaseId;
        this.status = status;
        this.executionTimeMs = executionTimeMs;
        this.memoryUsedKb = memoryUsedKb;
        this.outputExcerpt = outputExcerpt;
    }
}
