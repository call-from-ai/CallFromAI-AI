package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.ConversationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationEventRepository extends JpaRepository<ConversationEvent, Long> {

    List<ConversationEvent> findTop8ByUserIdOrderByCreatedAtDesc(Long userId);
}
