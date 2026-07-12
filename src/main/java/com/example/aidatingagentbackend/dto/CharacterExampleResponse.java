package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.RomanceStyleBand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CharacterExampleResponse {

    private Long id;

    private Long characterId;

    private AgentEventType eventType;

    private RelationshipTemperature relationshipTemperature;

    private String relationshipStage;

    private Integer minTemperatureScore;

    private Integer maxTemperatureScore;
    private RomanceStyleBand romanceStyleBand;

    private String userExample;

    private String assistantExample;

    private String toneTag;

    private Integer priority;

    private Boolean active;

    public static CharacterExampleResponse from(CharacterExample example) {
        CharacterExampleResponse response = new CharacterExampleResponse();
        response.setId(example.getId());
        response.setCharacterId(example.getCharacterId());
        response.setEventType(example.getEventType());
        response.setRelationshipTemperature(example.getRelationshipTemperature());
        response.setRelationshipStage(example.getRelationshipStage());
        response.setMinTemperatureScore(example.getMinTemperatureScore());
        response.setMaxTemperatureScore(example.getMaxTemperatureScore());
        response.setRomanceStyleBand(example.getRomanceStyleBand());
        response.setUserExample(example.getUserExample());
        response.setAssistantExample(example.getAssistantExample());
        response.setToneTag(example.getToneTag());
        response.setPriority(example.getPriority());
        response.setActive(example.getActive());
        return response;
    }
}
