package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_self_states", uniqueConstraints =
        @UniqueConstraint(name = "uk_agent_self_state_character", columnNames = "character_id"))
@Getter
@Setter
@NoArgsConstructor
public class AgentSelfState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "character_id", nullable = false)
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
