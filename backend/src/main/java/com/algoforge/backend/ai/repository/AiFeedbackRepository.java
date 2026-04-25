package com.algoforge.backend.ai.repository;

import com.algoforge.backend.ai.domain.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    Optional<AiFeedback> findBySubmissionIdAndFeedbackLevel(Long submissionId, Short feedbackLevel);

    List<AiFeedback> findAllBySubmissionIdOrderByFeedbackLevelAsc(Long submissionId);
}
