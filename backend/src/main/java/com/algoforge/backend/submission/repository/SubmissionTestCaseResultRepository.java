package com.algoforge.backend.submission.repository;

import com.algoforge.backend.submission.domain.SubmissionTestCaseResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionTestCaseResultRepository extends JpaRepository<SubmissionTestCaseResult, Long> {

    List<SubmissionTestCaseResult> findBySubmissionIdOrderByIdAsc(Long submissionId);

    @Modifying
    @Query("DELETE FROM SubmissionTestCaseResult r WHERE r.submissionId = :submissionId")
    int deleteAllBySubmissionId(@Param("submissionId") Long submissionId);
}
