package com.algoforge.backend.problem.repository;

import com.algoforge.backend.problem.domain.ProblemTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemTagRepository extends JpaRepository<ProblemTag, Long> {
    Optional<ProblemTag> findByName(String name);
    List<ProblemTag> findByNameIn(List<String> names);
}
