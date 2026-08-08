package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.AgentLifeType;
import com.example.aidatingagentbackend.entity.PreferTime;

import java.util.List;

public record CharacterSnapshot(Long characterId, String name, String mind, String responseStyle, String job,
        AgentLifeType lifeType, PreferTime preferTime, Integer romanceStyleScore, List<String> keywords,
        CharacterTraitSnapshot traits) {
    public CharacterSnapshot {
        preferTime = preferTime == null ? PreferTime.ANYTIME : preferTime;
        keywords = keywords == null ? List.of() : keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::strip)
                .distinct()
                .limit(10)
                .toList();
        if (characterId == null) throw new IllegalArgumentException("character.characterId is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("character.name is required");
        if (romanceStyleScore == null || romanceStyleScore < 0 || romanceStyleScore > 100) throw new IllegalArgumentException("character.romanceStyleScore must be between 0 and 100");
        if (traits == null) throw new IllegalArgumentException("character.traits is required");
    }
    public CharacterSnapshot(Long characterId, String name, String mind, String responseStyle, String job,
            AgentLifeType lifeType, PreferTime preferTime, Integer romanceStyleScore, CharacterTraitSnapshot traits) {
        this(characterId, name, mind, responseStyle, job, lifeType, preferTime, romanceStyleScore, List.of(), traits);
    }
    public CharacterSnapshot(Long characterId, String name, String mind, String responseStyle, String job,
            AgentLifeType lifeType, Integer romanceStyleScore, CharacterTraitSnapshot traits) {
        this(characterId, name, mind, responseStyle, job, lifeType, PreferTime.ANYTIME, romanceStyleScore, List.of(), traits);
    }
    public Long getId(){return characterId;} public String getName(){return name;} public String getMind(){return mind;}
    public String getResponseStyle(){return responseStyle;} public String getJob(){return job;}
    public AgentLifeType getLifeType(){return lifeType;}
    public PreferTime getPreferTime(){return preferTime;}
    public Integer getRomanceStyleScore(){return romanceStyleScore;}
}
