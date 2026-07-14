package com.example.aidatingagentbackend.dto;

public record CharacterTraitSnapshot(
        Integer humor, Integer playfulness, Integer affection, Integer empathy,
        Integer attachment, Integer jealousy, Integer dominance, Integer confidence,
        Integer expressiveness, Integer emotionalStability, Integer calculationVersion
) {
    public CharacterTraitSnapshot {
        require(humor, "humor"); require(playfulness, "playfulness"); require(affection, "affection");
        require(empathy, "empathy"); require(attachment, "attachment"); require(jealousy, "jealousy");
        require(dominance, "dominance"); require(confidence, "confidence");
        require(expressiveness, "expressiveness"); require(emotionalStability, "emotionalStability");
        if (calculationVersion != null && calculationVersion < 0) throw new IllegalArgumentException("character.traits.calculationVersion must be non-negative");
    }
    private static void require(Integer value, String name) {
        if (value == null || value < 0 || value > 10) throw new IllegalArgumentException("character.traits." + name + " must be between 0 and 10");
    }
    public Integer getHumor(){return humor;} public Integer getPlayfulness(){return playfulness;}
    public Integer getAffection(){return affection;} public Integer getEmpathy(){return empathy;}
    public Integer getAttachment(){return attachment;} public Integer getJealousy(){return jealousy;}
    public Integer getDominance(){return dominance;} public Integer getConfidence(){return confidence;}
    public Integer getExpressiveness(){return expressiveness;} public Integer getEmotionalStability(){return emotionalStability;}
}
