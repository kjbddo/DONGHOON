package com.algoforge.backend.problem.repository;

import com.algoforge.backend.problem.domain.ProblemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemCategoryRepository extends JpaRepository<ProblemCategory, Long> {
    Optional<ProblemCategory> findByName(String name);
    List<ProblemCategory> findByNameIn(List<String> names);
}
