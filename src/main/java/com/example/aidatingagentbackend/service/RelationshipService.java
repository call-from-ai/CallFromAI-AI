package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.RelationshipRequest;
import com.example.aidatingagentbackend.dto.RelationshipResponse;
import com.example.aidatingagentbackend.dto.RelationshipSettingsRequest;
import com.example.aidatingagentbackend.dto.RelationshipSettingsResponse;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final RelationshipStageResolver relationshipStageResolver;
    private final RelationshipTemperatureScoreResolver relationshipTemperatureScoreResolver;

    public RelationshipService(
            RelationshipRepository relationshipRepository,
            RelationshipStageResolver relationshipStageResolver,
            RelationshipTemperatureScoreResolver relationshipTemperatureScoreResolver
    ) {
        this.relationshipRepository = relationshipRepository;
        this.relationshipStageResolver = relationshipStageResolver;
        this.relationshipTemperatureScoreResolver = relationshipTemperatureScoreResolver;
    }

    @Transactional
    public RelationshipResponse create(RelationshipRequest request) {
        Relationship relationship = new Relationship();
        applyRequest(relationship, request);
        return RelationshipResponse.from(relationshipRepository.save(relationship));
    }

    @Transactional(readOnly = true)
    public List<RelationshipResponse> findAll() {
        return relationshipRepository.findAll()
                .stream()
                .map(RelationshipResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RelationshipResponse findById(Long id) {
        return RelationshipResponse.from(findRelationship(id));
    }

    @Transactional
    public RelationshipResponse update(Long id, RelationshipRequest request) {
        Relationship relationship = findRelationship(id);
        applyRequest(relationship, request);
        return RelationshipResponse.from(relationshipRepository.save(relationship));
    }

    @Transactional
    public RelationshipSettingsResponse updateSettings(Long id, RelationshipSettingsRequest request) {
        Relationship relationship = findRelationship(id);
        relationship.setRelationshipStage(normalizeStage(request.getRelationshipStage()));
        relationship.setRelationshipTemperatureScore(resolveTemperatureScore(request));
        Relationship saved = relationshipRepository.save(relationship);
        return settingsResponse(saved);
    }

    @Transactional(readOnly = true)
    public RelationshipSettingsResponse findSettings(Long id) {
        return settingsResponse(findRelationship(id));
    }

    @Transactional
    public void delete(Long id) {
        Relationship relationship = findRelationship(id);
        relationshipRepository.delete(relationship);
    }

    private Relationship findRelationship(Long id) {
        return relationshipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship not found. id=" + id));
    }

    private void applyRequest(Relationship relationship, RelationshipRequest request) {
        relationship.setCharacterId(request.getCharacterId());
        relationship.setTrust(request.getTrust());
        relationship.setCloseness(request.getCloseness());
        relationship.setConflictLevel(request.getConflictLevel());
        relationship.setRepairProgress(request.getRepairProgress());
        relationship.setBreakupRisk(request.getBreakupRisk());
        relationship.setRelationshipStage(normalizeStage(request.getRelationshipStage()));
        relationship.setRelationshipTemperatureScore(resolveTemperatureScore(request.getRelationshipTemperatureScore()));
        relationship.setDaysTogether(request.getDaysTogether());
    }

    private String normalizeStage(String relationshipStage) {
        try {
            return relationshipStageResolver.normalize(relationshipStage);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid relationshipStage.", exception);
        }
    }

    private Integer resolveTemperatureScore(Integer relationshipTemperatureScore) {
        try {
            return relationshipTemperatureScoreResolver.resolveScore(relationshipTemperatureScore, null);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private Integer resolveTemperatureScore(RelationshipSettingsRequest request) {
        try {
            return relationshipTemperatureScoreResolver.resolveScore(
                    request.getRelationshipTemperatureScore(),
                    request.getRelationshipTemperature()
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private RelationshipSettingsResponse settingsResponse(Relationship relationship) {
        String stage = relationshipStageResolver.resolve(relationship.getRelationshipStage()).name();
        Integer temperatureScore = relationshipTemperatureScoreResolver.resolveScore(
                relationship.getRelationshipTemperatureScore(),
                null
        );
        return RelationshipSettingsResponse.from(relationship, stage, temperatureScore);
    }
}
