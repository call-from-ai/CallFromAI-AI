package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.AgentLifeType;
import com.example.aidatingagentbackend.entity.Mbti;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.SpeechStyle;
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

    private String gender;
    private Integer age;
    private String job;
    private AgentLifeType lifeType;
    private Integer spiceLevel;
    private Mbti mbti;
    private SpeechStyle speechStyle;
    private RelationshipStage relationshipStage;
    private java.util.List<PersonalityTraitSelection> traits;
}
