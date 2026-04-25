package com.algoforge.backend.submission.repository;

import com.algoforge.backend.submission.domain.UserSolvedProblem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSolvedProblemRepository extends JpaRepository<UserSolvedProblem, UserSolvedProblem.Pk> {

    boolean existsById(UserSolvedProblem.Pk id);

    /** 사용자가 푼 문제 수 */
    long countByIdUserId(Long userId);

    /** 사용자가 푼 problem_id 페이지 (최신 해결 순) */
    @Query("""
            SELECT s.id.problemId
              FROM UserSolvedProblem s
             WHERE s.id.userId = :userId
             ORDER BY s.firstSolvedAt DESC, s.id.problemId DESC
            """)
    Page<Long> findSolvedProblemIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 입력된 problemId 집합 중 사용자가 이미 해결한 것만 반환 */
    @Query("""
            SELECT s.id.problemId
              FROM UserSolvedProblem s
             WHERE s.id.userId = :userId
               AND s.id.problemId IN :problemIds
            """)
    List<Long> findSolvedProblemIds(@Param("userId") Long userId,
                                    @Param("problemIds") List<Long> problemIds);

    /** 난이도별 해결 수 — Object[] {difficulty, count} */
    @Query("""
            SELECT p.difficulty AS difficulty, COUNT(p) AS cnt
              FROM UserSolvedProblem s
              JOIN com.algoforge.backend.problem.domain.Problem p
                ON p.id = s.id.problemId
             WHERE s.id.userId = :userId
             GROUP BY p.difficulty
            """)
    List<Object[]> countSolvedByDifficulty(@Param("userId") Long userId);

    /** 랭킹 row: {userId, solvedCount} 상위 N — Page로 페이지네이션 */
    @Query("""
            SELECT s.id.userId AS userId, COUNT(s) AS solvedCount
              FROM UserSolvedProblem s
             GROUP BY s.id.userId
             ORDER BY COUNT(s) DESC, s.id.userId ASC
            """)
    Page<Object[]> findTopSolvers(Pageable pageable);

    /**
     * 특정 사용자보다 많이 푼 (서로 다른) 사용자 수.
     * 랭크는 (countUsersAheadOf + 1)이다. 동률은 같은 랭크(1-based)로 처리한다.
     */
    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT user_id
                  FROM user_solved_problems
                 GROUP BY user_id
                HAVING COUNT(*) > (
                    SELECT COUNT(*) FROM user_solved_problems WHERE user_id = :userId
                )
            ) sub
            """, nativeQuery = true)
    long countUsersAheadOf(@Param("userId") Long userId);

    /** 전체 푼 사용자 수 (랭킹 페이지 totalElements용) */
    @Query("SELECT COUNT(DISTINCT s.id.userId) FROM UserSolvedProblem s")
    long countDistinctSolvers();
}
