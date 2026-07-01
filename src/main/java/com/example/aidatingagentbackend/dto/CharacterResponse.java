package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.Character;
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

    public static CharacterResponse from(Character character) {
        CharacterResponse response = new CharacterResponse();
        response.setId(character.getId());
        response.setName(character.getName());
        response.setMind(character.getMind());
        response.setValues(character.getValues());
        response.setHabit(character.getHabit());
        response.setResponseStyle(character.getResponseStyle());
        return response;
    }
}
