package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.RomanceStyleBand;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.SpeechStyle;
import com.example.aidatingagentbackend.entity.AgentLifeType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CharacterResponse {

    private Long id;

    private String name;

    private String mind;

    private String values;

    private String habit;

    private String responseStyle;
    private Integer romanceStyleScore;
    private RomanceStyleBand romanceStyleBand;
    private RelationshipStage relationshipStage;
    private SpeechStyle speechStyle;
    private AgentLifeType lifeType;
    private String job;

    public static CharacterResponse from(Character character) {
        CharacterResponse response = new CharacterResponse();
        response.setId(character.getId());
        response.setName(character.getName());
        response.setMind(character.getMind());
        response.setValues(character.getValues());
        response.setHabit(character.getHabit());
        response.setResponseStyle(character.getResponseStyle());
        response.setRomanceStyleScore(character.getRomanceStyleScore());
        response.setRomanceStyleBand(character.getRomanceStyleScore() == null ? null : RomanceStyleBand.from(character.getRomanceStyleScore()));
        response.setRelationshipStage(character.getRelationshipStage());
        response.setSpeechStyle(character.getSpeechStyle());
        response.setLifeType(character.getLifeType());
        response.setJob(character.getJob());
        return response;
    }
}
