package com.example.aidatingagentbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CharacterExampleRequest {

    private Long characterId;

    private String userExample;

    private String assistantExample;

    private String toneTag;

    private Integer priority;
}
