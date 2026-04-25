package com.algoforge.backend.problem.repository;

import com.algoforge.backend.problem.domain.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblem_IdOrderBySeqAsc(Long problemId);

    List<TestCase> findByProblem_IdAndHiddenFalseOrderBySeqAsc(Long problemId);

    @Modifying
    @Query("DELETE FROM TestCase t WHERE t.problem.id = :problemId")
    int deleteAllByProblemId(@Param("problemId") Long problemId);
}
