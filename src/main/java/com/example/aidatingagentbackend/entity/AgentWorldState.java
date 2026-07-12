package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Column;
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
@Table(name = "agent_world_states")
@Getter
@Setter
@NoArgsConstructor
public class AgentWorldState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long characterId;

    private String currentActivity;

    private String location;

    private String timeContext;

    private String mood;

    private Integer energy;

    private Integer stress;

    private Integer loneliness;

    @Column(columnDefinition = "TEXT")
    private String pendingThought;

    private LocalDateTime lastUpdatedAt;

    @PrePersist
    @PreUpdate
    public void refreshUpdatedAt() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
