package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class CharacterTraitProfileResponse {

    private Long id;

    private Long characterId;

    private Set<PersonalityKeyword> personalityKeywords;

    private Integer humor;

    private Integer playfulness;

    private Integer affection;

    private Integer empathy;

    private Integer attachment;

    private Integer jealousy;

    private Integer dominance;

    private Integer confidence;

    private Integer expressiveness;

    private Integer emotionalStability;

    private Integer calculationVersion;

    private LocalDateTime calculatedAt;

    private LocalDateTime updatedAt;

    public static CharacterTraitProfileResponse from(CharacterTraitProfile profile) {
        CharacterTraitProfileResponse response = new CharacterTraitProfileResponse();
        response.setId(profile.getId());
        response.setCharacterId(profile.getCharacterId());
        response.setPersonalityKeywords(profile.getPersonalityKeywords());
        response.setHumor(profile.getHumor());
        response.setPlayfulness(profile.getPlayfulness());
        response.setAffection(profile.getAffection());
        response.setEmpathy(profile.getEmpathy());
        response.setAttachment(profile.getAttachment());
        response.setJealousy(profile.getJealousy());
        response.setDominance(profile.getDominance());
        response.setConfidence(profile.getConfidence());
        response.setExpressiveness(profile.getExpressiveness());
        response.setEmotionalStability(profile.getEmotionalStability());
        response.setCalculationVersion(profile.getCalculationVersion());
        response.setCalculatedAt(profile.getCalculatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }
}
