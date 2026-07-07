package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.AgentLifeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AgentLifeEventRepository extends JpaRepository<AgentLifeEvent, Long> {

    boolean existsByUserIdAndEventDateAndTimeContext(Long userId, LocalDate eventDate, String timeContext);

    List<AgentLifeEvent> findTop8ByUserIdOrderByEventDateDescIdDesc(Long userId);
}
