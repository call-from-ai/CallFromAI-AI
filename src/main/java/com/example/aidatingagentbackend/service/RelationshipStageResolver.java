package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.springframework.stereotype.Service;

@Service
public class RelationshipStageResolver {

    private final SettingsDefaultPolicy settingsDefaultPolicy;

    public RelationshipStageResolver(SettingsDefaultPolicy settingsDefaultPolicy) {
        this.settingsDefaultPolicy = settingsDefaultPolicy;
    }

    public RelationshipStage resolve(String value) {
        if (value == null || value.isBlank()) {
            return settingsDefaultPolicy.defaultRelationshipStage();
        }

        try {
            return RelationshipStage.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return settingsDefaultPolicy.defaultRelationshipStage();
        }
    }

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return settingsDefaultPolicy.defaultRelationshipStageValue();
        }
        return RelationshipStage.valueOf(value.strip().toUpperCase()).name();
    }
}
