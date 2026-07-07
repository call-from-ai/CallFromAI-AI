package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.ReflectionCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReflectionCandidateRepository extends JpaRepository<ReflectionCandidate, Long> {

    List<ReflectionCandidate> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    List<ReflectionCandidate> findTop50ByProcessedFalseOrderByCreatedAtAsc();
}
