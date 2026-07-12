package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.springframework.stereotype.Service;

@Service
public class SettingsDefaultPolicy {

    public RelationshipStage defaultRelationshipStage() {
        return RelationshipStage.CRUSH;
    }

    public String defaultRelationshipStageValue() {
        return defaultRelationshipStage().name();
    }

    public Integer defaultRelationshipTemperatureScore() {
        return 50;
    }
}
