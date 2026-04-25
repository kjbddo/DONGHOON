package com.algoforge.backend.problem.repository;

import com.algoforge.backend.problem.domain.Difficulty;
import com.algoforge.backend.problem.domain.Problem;
import com.algoforge.backend.problem.domain.ProblemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Optional<Problem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * 공개 목록(검색/필터/페이징).
     * - status는 항상 PUBLIC
     * - difficulty/aiGenerated/keyword는 null 허용 (필터 미적용)
     */
    @Query("""
            SELECT p FROM Problem p
            WHERE p.status = :status
              AND (:difficulty IS NULL OR p.difficulty = :difficulty)
              AND (:aiOnly IS NULL OR p.aiGenerated = :aiOnly)
              AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Problem> searchPublic(
            @Param("status") ProblemStatus status,
            @Param("difficulty") Difficulty difficulty,
            @Param("aiOnly") Boolean aiOnly,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 관리자 목록(상태 무관, soft delete 포함 옵션).
     */
    @Query("""
            SELECT p FROM Problem p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:difficulty IS NULL OR p.difficulty = :difficulty)
              AND (:aiOnly IS NULL OR p.aiGenerated = :aiOnly)
              AND (:includeDeleted = TRUE OR p.status <> com.algoforge.backend.problem.domain.ProblemStatus.DELETED)
              AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Problem> searchAdmin(
            @Param("status") ProblemStatus status,
            @Param("difficulty") Difficulty difficulty,
            @Param("aiOnly") Boolean aiOnly,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"categories", "tags"})
    Optional<Problem> findWithRelationsById(Long id);

    /** 카테고리/태그 fetch join — N+1 방지. id 순서는 보장되지 않으므로 호출자가 정렬해야 함. */
    @EntityGraph(attributePaths = {"categories", "tags"})
    java.util.List<Problem> findByIdIn(java.util.Collection<Long> ids);
}
