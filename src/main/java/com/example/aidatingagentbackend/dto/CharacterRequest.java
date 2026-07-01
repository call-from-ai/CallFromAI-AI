package com.example.aidatingagentbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CharacterRequest {

    private String name;

    private String mind;

    private String values;

    private String habit;

    private String responseStyle;
}
