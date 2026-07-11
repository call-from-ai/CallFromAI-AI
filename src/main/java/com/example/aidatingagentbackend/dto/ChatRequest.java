package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {

    private Long characterId;

    private Long userId;

    private String message;

    private RelationshipTemperature relationshipTemperature;

    public Long resolveCharacterId() {
        return characterId == null ? userId : characterId;
    }
}
