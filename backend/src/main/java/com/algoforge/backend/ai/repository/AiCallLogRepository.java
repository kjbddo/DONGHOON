package com.algoforge.backend.ai.repository;

import com.algoforge.backend.ai.domain.AiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {

    /**
     * since 이후 (성공 호출만) 사용자 일일 사용량.
     * userId가 null이면 전체 사용량 집계 (시스템 호출 포함하지 않음).
     */
    @Query("""
            SELECT COUNT(l)
              FROM AiCallLog l
             WHERE l.userId = :userId
               AND l.success = true
               AND l.createdAt >= :since
            """)
    long countSuccessfulByUserSince(@Param("userId") Long userId,
                                    @Param("since") OffsetDateTime since);

    @Query("""
            SELECT COUNT(l)
              FROM AiCallLog l
             WHERE l.success = true
               AND l.createdAt >= :since
            """)
    long countSuccessfulSince(@Param("since") OffsetDateTime since);
}
