package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "memories",
        indexes = {
                @jakarta.persistence.Index(
                        name = "idx_memories_character_type_occurred",
                        columnList = "character_id,type,occurred_at"
                )
        },
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(
                        name = "uk_memories_request_id",
                        columnNames = "request_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Memory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id")
    private Long characterId;

    @Enumerated(EnumType.STRING)
    private MemoryType type;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private MemoryChannel channel;

    @Column(name = "user_content", columnDefinition = "TEXT")
    private String userContent;

    @Column(name = "assistant_content", columnDefinition = "TEXT")
    private String assistantContent;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    private Integer importance;

    private LocalDateTime lastRetrievedAt;

    private Integer retrievalCount;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
