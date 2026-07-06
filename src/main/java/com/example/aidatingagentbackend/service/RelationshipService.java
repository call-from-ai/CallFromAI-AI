package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.RelationshipRequest;
import com.example.aidatingagentbackend.dto.RelationshipResponse;
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

    public RelationshipService(RelationshipRepository relationshipRepository) {
        this.relationshipRepository = relationshipRepository;
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
    public void delete(Long id) {
        Relationship relationship = findRelationship(id);
        relationshipRepository.delete(relationship);
    }

    private Relationship findRelationship(Long id) {
        return relationshipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship not found. id=" + id));
    }

    private void applyRequest(Relationship relationship, RelationshipRequest request) {
        relationship.setTrust(request.getTrust());
        relationship.setCloseness(request.getCloseness());
        relationship.setConflictLevel(request.getConflictLevel());
        relationship.setRepairProgress(request.getRepairProgress());
        relationship.setBreakupRisk(request.getBreakupRisk());
        relationship.setRelationshipStage(request.getRelationshipStage());
        relationship.setDaysTogether(request.getDaysTogether());
    }
}
