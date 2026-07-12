package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.RelationshipRequest;
import com.example.aidatingagentbackend.dto.RelationshipResponse;
import com.example.aidatingagentbackend.dto.RelationshipSettingsRequest;
import com.example.aidatingagentbackend.dto.RelationshipSettingsResponse;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelationshipSettingsServiceTests {

    private final SettingsDefaultPolicy defaultPolicy = new SettingsDefaultPolicy();
    private final RelationshipService service = new RelationshipService(
            repository(),
            new RelationshipStageResolver(defaultPolicy),
            new RelationshipTemperatureScoreResolver(defaultPolicy)
    );

    @Test
    void relationshipStageAndTemperatureSettingsAreSaved() {
        RelationshipSettingsRequest request = new RelationshipSettingsRequest();
        request.setRelationshipStage("EARLY_DATING");
        request.setRelationshipTemperatureScore(72);

        RelationshipSettingsResponse response = service.updateSettings(1L, request);

        assertThat(response.getRelationshipId()).isEqualTo(1L);
        assertThat(response.getRelationshipStage()).isEqualTo("EARLY_DATING");
        assertThat(response.getRelationshipTemperatureScore()).isEqualTo(72);
    }

    @Test
    void temperatureRangeErrorBecomesBadRequest() {
        RelationshipSettingsRequest request = new RelationshipSettingsRequest();
        request.setRelationshipStage("EARLY_DATING");
        request.setRelationshipTemperatureScore(101);

        assertThatThrownBy(() -> service.updateSettings(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void missingRelationshipSettingsReturnDefaults() {
        RelationshipSettingsResponse response = service.findSettings(2L);

        assertThat(response.getRelationshipStage()).isEqualTo("CRUSH");
        assertThat(response.getRelationshipTemperatureScore()).isEqualTo(50);
    }

    @Test
    void legacyRelationshipTemperatureCanInferScore() {
        RelationshipSettingsRequest request = new RelationshipSettingsRequest();
        request.setRelationshipStage("LONG_TERM");
        request.setRelationshipTemperature(RelationshipTemperature.SPICY);

        RelationshipSettingsResponse response = service.updateSettings(1L, request);

        assertThat(response.getRelationshipStage()).isEqualTo("LONG_TERM");
        assertThat(response.getRelationshipTemperatureScore()).isEqualTo(85);
    }

    @Test
    void existingRelationshipCreateStillWorks() {
        RelationshipRequest request = new RelationshipRequest();
        request.setCharacterId(12L);
        request.setTrust(50);
        request.setRelationshipStage("CRUSH");

        RelationshipResponse response = service.create(request);

        assertThat(response.getCharacterId()).isEqualTo(12L);
        assertThat(response.getTrust()).isEqualTo(50);
        assertThat(response.getRelationshipStage()).isEqualTo("CRUSH");
        assertThat(response.getRelationshipTemperatureScore()).isEqualTo(50);
    }

    private RelationshipRepository repository() {
        RelationshipRepository repository = mock(RelationshipRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(relationship(1L, 12L, null, null)));
        when(repository.findById(2L)).thenReturn(Optional.of(relationship(2L, 12L, null, null)));
        when(repository.save(any(Relationship.class))).thenAnswer(invocation -> {
            Relationship relationship = invocation.getArgument(0);
            if (relationship.getId() == null) {
                relationship.setId(99L);
            }
            return relationship;
        });
        return repository;
    }

    private Relationship relationship(Long id, Long characterId, String stage, Integer temperatureScore) {
        Relationship relationship = new Relationship();
        relationship.setId(id);
        relationship.setCharacterId(characterId);
        relationship.setRelationshipStage(stage);
        relationship.setRelationshipTemperatureScore(temperatureScore);
        return relationship;
    }
}
