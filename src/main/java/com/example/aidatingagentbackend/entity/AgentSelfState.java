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

    private static final double DEFAULT_AFFECTION = 0.55;
    private static final double DEFAULT_TRUST = 0.60;
    private static final double DEFAULT_INSECURITY = 0.15;
    private static final double DEFAULT_EMOTIONAL_DISTANCE = 0.15;

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

    public String representativeEmotion() {
        return lastEmotion == null || lastEmotion.isBlank() ? "calm" : lastEmotion;
    }

    public int emotionIntensity() {
        double maxActivation = Math.max(
                Math.max(normalizedDeviation(affection, DEFAULT_AFFECTION), normalizedDeviation(trust, DEFAULT_TRUST)),
                Math.max(
                        Math.max(value(hurt), value(anger)),
                        Math.max(
                                Math.max(aboveBaseline(insecurity, DEFAULT_INSECURITY), value(disappointment)),
                                aboveBaseline(emotionalDistance, DEFAULT_EMOTIONAL_DISTANCE)
                        )
                )
        );
        return Math.max(0, Math.min(10, (int) Math.round(maxActivation * 8.0)));
    }

    private double normalizedDeviation(Double current, double baseline) {
        double denominator = Math.max(baseline, 1.0 - baseline);
        return Math.min(1.0, Math.abs(value(current) - baseline) / denominator);
    }

    private double aboveBaseline(Double current, double baseline) {
        return Math.min(1.0, Math.max(0.0, value(current) - baseline) / (1.0 - baseline));
    }

    private double value(Double current) {
        return current == null ? 0.0 : Math.max(0.0, Math.min(1.0, current));
    }
}
