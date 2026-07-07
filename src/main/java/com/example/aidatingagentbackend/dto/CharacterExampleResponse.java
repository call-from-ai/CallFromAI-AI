package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.CharacterExample;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CharacterExampleResponse {

    private Long id;

    private Long characterId;

    private String userExample;

    private String assistantExample;

    private String toneTag;

    private Integer priority;

    public static CharacterExampleResponse from(CharacterExample example) {
        CharacterExampleResponse response = new CharacterExampleResponse();
        response.setId(example.getId());
        response.setCharacterId(example.getCharacterId());
        response.setUserExample(example.getUserExample());
        response.setAssistantExample(example.getAssistantExample());
        response.setToneTag(example.getToneTag());
        response.setPriority(example.getPriority());
        return response;
    }
}
