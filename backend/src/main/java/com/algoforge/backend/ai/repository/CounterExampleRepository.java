package com.algoforge.backend.ai.repository;

import com.algoforge.backend.ai.domain.CounterExample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounterExampleRepository extends JpaRepository<CounterExample, Long> {

    List<CounterExample> findAllBySubmissionIdOrderByIdAsc(Long submissionId);

    long countBySubmissionId(Long submissionId);
}
