package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.AgentLifeType;

public record CharacterSnapshot(Long characterId, String name, String mind, String responseStyle, String job,
        AgentLifeType lifeType, Integer romanceStyleScore, CharacterTraitSnapshot traits) {
    public CharacterSnapshot {
        if (characterId == null) throw new IllegalArgumentException("character.characterId is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("character.name is required");
        if (romanceStyleScore == null || romanceStyleScore < 0 || romanceStyleScore > 100) throw new IllegalArgumentException("character.romanceStyleScore must be between 0 and 100");
        if (traits == null) throw new IllegalArgumentException("character.traits is required");
    }
    public Long getId(){return characterId;} public String getName(){return name;} public String getMind(){return mind;}
    public String getResponseStyle(){return responseStyle;} public String getJob(){return job;}
    public AgentLifeType getLifeType(){return lifeType;}
    public Integer getRomanceStyleScore(){return romanceStyleScore;}
}
