package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_self_states")
@Getter
@Setter
@NoArgsConstructor
public class AgentSelfState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long characterId;

    private Double affection;

    private Double trust;

    private Double hurt;

    private Double anger;

    private Double insecurity;

    private Double disappointment;

    private Double emotionalDistance;

    private String lastEmotion;

    private String lastSignificantEvent;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void refreshUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
