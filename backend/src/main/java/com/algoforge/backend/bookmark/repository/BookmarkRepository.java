package com.algoforge.backend.bookmark.repository;

import com.algoforge.backend.bookmark.domain.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Bookmark.Pk> {

    /** 사용자가 북마크한 problem_id 페이지(최신순) — N+1 방지를 위해 problemId만 우선 조회 */
    @Query("""
            SELECT b.id.problemId
              FROM Bookmark b
             WHERE b.id.userId = :userId
             ORDER BY b.createdAt DESC, b.id.problemId DESC
            """)
    Page<Long> findProblemIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 입력된 problemId 집합 중 사용자가 이미 북마크한 것만 반환 (목록 페이지에서 표시용) */
    @Query("""
            SELECT b.id.problemId
              FROM Bookmark b
             WHERE b.id.userId = :userId
               AND b.id.problemId IN :problemIds
            """)
    List<Long> findBookmarkedProblemIds(@Param("userId") Long userId,
                                        @Param("problemIds") List<Long> problemIds);

    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.id.userId = :userId AND b.id.problemId = :problemId")
    int deleteByUserAndProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);

    long countByIdUserId(Long userId);
}
