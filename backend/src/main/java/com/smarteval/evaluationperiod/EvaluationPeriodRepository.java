package com.smarteval.evaluationperiod;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationPeriodRepository extends JpaRepository<EvaluationPeriod, Long> {
    List<EvaluationPeriod> findAllByOrderByCreatedAtDesc();
    Optional<EvaluationPeriod> findFirstByOrderByCreatedAtDesc();
}
