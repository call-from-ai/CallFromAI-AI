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
@Table(name = "states")
@Getter
@Setter
@NoArgsConstructor
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String emotion;

    private Integer emotionIntensity;

    private Integer energy;

    private Integer stress;

    @Column(columnDefinition = "TEXT")
    private String thinking;

    @Column(columnDefinition = "TEXT")
    private String goal;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void refreshUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
