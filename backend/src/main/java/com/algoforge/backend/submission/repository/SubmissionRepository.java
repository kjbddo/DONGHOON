package com.algoforge.backend.submission.repository;

import com.algoforge.backend.submission.domain.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Page<Submission> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Page<Submission> findByUserIdAndProblemIdOrderByIdDesc(Long userId, Long problemId, Pageable pageable);

    long countByUserId(Long userId);

    /** 사용자의 상태별 제출 수 — Object[] {status, count} */
    @Query("""
            SELECT s.status AS status, COUNT(s) AS cnt
              FROM Submission s
             WHERE s.userId = :userId
             GROUP BY s.status
            """)
    List<Object[]> countByUserGroupedByStatus(@Param("userId") Long userId);

    /** 사용자가 제출한 distinct 문제 수 (시도한 문제 수) */
    @Query("SELECT COUNT(DISTINCT s.problemId) FROM Submission s WHERE s.userId = :userId")
    long countDistinctProblemsByUser(@Param("userId") Long userId);

    /** 입력된 problemId 집합 중 사용자가 한 번이라도 제출한 problemId만 반환 */
    @Query("""
            SELECT DISTINCT s.problemId
              FROM Submission s
             WHERE s.userId = :userId
               AND s.problemId IN :problemIds
            """)
    List<Long> findAttemptedProblemIds(@Param("userId") Long userId,
                                       @Param("problemIds") List<Long> problemIds);

    /** 사용자의 언어별 제출 수 — Object[] {languageId, count} */
    @Query("""
            SELECT s.languageId AS languageId, COUNT(s) AS cnt
              FROM Submission s
             WHERE s.userId = :userId
             GROUP BY s.languageId
             ORDER BY COUNT(s) DESC
            """)
    List<Object[]> countByUserGroupedByLanguage(@Param("userId") Long userId);
}
