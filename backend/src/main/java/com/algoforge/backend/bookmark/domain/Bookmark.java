package com.algoforge.backend.bookmark.domain;

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

/**
 * 사용자 ↔ 문제 북마크. PK는 (user_id, problem_id) 복합키.
 * 동일 (사용자, 문제)에 대해 1행만 존재.
 */
@Getter
@Entity
@Table(name = "bookmarks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {

    @EmbeddedId
    private Pk id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Bookmark(Long userId, Long problemId) {
        this.id = new Pk(userId, problemId);
        this.createdAt = OffsetDateTime.now();
    }

    public Long getUserId() { return id.getUserId(); }
    public Long getProblemId() { return id.getProblemId(); }

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
