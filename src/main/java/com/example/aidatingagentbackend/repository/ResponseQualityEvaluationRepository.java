package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponseQualityEvaluationRepository extends JpaRepository<ResponseQualityEvaluation, Long> {

    List<ResponseQualityEvaluation> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
