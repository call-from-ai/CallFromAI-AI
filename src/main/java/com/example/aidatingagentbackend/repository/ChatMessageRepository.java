package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // TODO: Add custom chat message queries.
}
