package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_self_state_logs", indexes =
        @Index(name = "idx_agent_self_state_logs_character_id", columnList = "character_id"))
@Getter
@Setter
@NoArgsConstructor
public class AgentSelfStateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false)
    private Long characterId;

    private Double previousHurt;

    private Double nextHurt;

    private Double previousTrust;

    private Double nextTrust;

    private Double previousAnger;

    private Double nextAnger;

    private Double previousInsecurity;

    private Double nextInsecurity;

    private String eventType;

    private Double severity;

    @Column(columnDefinition = "TEXT")
    private String deltaReason;

    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
