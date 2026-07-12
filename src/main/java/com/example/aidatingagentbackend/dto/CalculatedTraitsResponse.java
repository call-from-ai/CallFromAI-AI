package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CalculatedTraitsResponse {

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

    public static CalculatedTraitsResponse from(CharacterTraitProfile profile) {
        CalculatedTraitsResponse response = new CalculatedTraitsResponse();
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
        return response;
    }
}
