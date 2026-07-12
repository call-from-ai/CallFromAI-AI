package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.RelationshipStage;

public record RelationshipSnapshot(Long relationshipId, RelationshipStage relationshipStage,
        Integer relationshipTemperatureScore, Integer trust, Integer closeness, Integer conflictLevel,
        Integer repairProgress, Integer breakupRisk, Integer daysTogether, RelationshipStrategy strategy) {
    public RelationshipSnapshot {
        if (relationshipStage == null) throw new IllegalArgumentException("relationship.relationshipStage is required");
        range(relationshipTemperatureScore, "relationshipTemperatureScore"); range(trust, "trust");
        range(closeness, "closeness"); range(conflictLevel, "conflictLevel"); range(repairProgress, "repairProgress"); range(breakupRisk, "breakupRisk");
        if (daysTogether == null || daysTogether < 0) throw new IllegalArgumentException("relationship.daysTogether must be non-negative");
        if (strategy == null) strategy = RelationshipStrategy.NORMAL;
    }
    private static void range(Integer value, String name) {
        if (value == null || value < 0 || value > 100) throw new IllegalArgumentException("relationship." + name + " must be between 0 and 100");
    }
    public Integer getTrust(){return trust;} public Integer getCloseness(){return closeness;}
    public Integer getConflictLevel(){return conflictLevel;} public Integer getRepairProgress(){return repairProgress;}
    public Integer getBreakupRisk(){return breakupRisk;} public Integer getDaysTogether(){return daysTogether;}
    public String getRelationshipStage(){return relationshipStage.name();}
    public Integer getRelationshipTemperatureScore(){return relationshipTemperatureScore;}
}
