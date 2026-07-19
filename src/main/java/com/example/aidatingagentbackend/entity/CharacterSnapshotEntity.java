package com.example.aidatingagentbackend.entity;

import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "character_snapshots", uniqueConstraints =
        @UniqueConstraint(name = "uk_character_snapshot_character", columnNames = "character_id"))
@Getter
@NoArgsConstructor
public class CharacterSnapshotEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false, updatable = false)
    private Long characterId;
    @Column(nullable = false) private String name;
    @Column(length = 4000) private String mind;
    @Column(name = "response_style", length = 2000) private String responseStyle;
    private String job;
    @Enumerated(EnumType.STRING) @Column(name = "life_type") private AgentLifeType lifeType;
    @Enumerated(EnumType.STRING) @Column(name = "prefer_time", nullable = false)
    private PreferTime preferTime = PreferTime.ANYTIME;
    @Column(name = "romance_style_score", nullable = false) private Integer romanceStyleScore;
    private Integer humor;
    private Integer playfulness;
    private Integer affection;
    private Integer empathy;
    private Integer attachment;
    private Integer jealousy;
    private Integer dominance;
    private Integer confidence;
    private Integer expressiveness;
    @Column(name = "emotional_stability") private Integer emotionalStability;
    @Column(name = "calculation_version", nullable = false) private Integer calculationVersion;

    public CharacterSnapshotEntity(CharacterSnapshot snapshot) {
        this.characterId = snapshot.characterId();
        updateFrom(snapshot);
    }

    public void updateFrom(CharacterSnapshot snapshot) {
        CharacterTraitSnapshot traits = snapshot.traits();
        name = snapshot.name(); mind = snapshot.mind(); responseStyle = snapshot.responseStyle();
        job = snapshot.job(); lifeType = snapshot.lifeType(); preferTime = snapshot.preferTime();
        romanceStyleScore = snapshot.romanceStyleScore();
        humor = traits.humor(); playfulness = traits.playfulness(); affection = traits.affection();
        empathy = traits.empathy(); attachment = traits.attachment(); jealousy = traits.jealousy();
        dominance = traits.dominance(); confidence = traits.confidence(); expressiveness = traits.expressiveness();
        emotionalStability = traits.emotionalStability(); calculationVersion = traits.calculationVersion();
    }
}
