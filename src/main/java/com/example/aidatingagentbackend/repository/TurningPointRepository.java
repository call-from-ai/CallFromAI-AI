package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.TurningPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurningPointRepository extends JpaRepository<TurningPoint, Long> {

    List<TurningPoint> findTop10ByCharacterIdOrderByCreatedAtDesc(Long characterId);
}
