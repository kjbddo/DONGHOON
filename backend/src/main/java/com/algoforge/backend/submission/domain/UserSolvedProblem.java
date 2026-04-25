package com.algoforge.backend.submission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "user_solved_problems")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSolvedProblem {

    @EmbeddedId
    private Pk id;

    @Column(name = "first_solved_at", nullable = false)
    private OffsetDateTime firstSolvedAt;

    @Column(name = "first_accepted_submission_id", nullable = false)
    private Long firstAcceptedSubmissionId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    public UserSolvedProblem(Long userId, Long problemId, Long submissionId, int attemptCount) {
        this.id = new Pk(userId, problemId);
        this.firstSolvedAt = OffsetDateTime.now();
        this.firstAcceptedSubmissionId = submissionId;
        this.attemptCount = Math.max(1, attemptCount);
    }

    @Embeddable
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class Pk implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "problem_id")
        private Long problemId;

        public Pk(Long userId, Long problemId) {
            this.userId = userId;
            this.problemId = problemId;
        }

        public Long getUserId() { return userId; }
        public Long getProblemId() { return problemId; }
    }
}
