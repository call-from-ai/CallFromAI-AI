package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.repository.CharacterExampleRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CharacterExampleServiceTests {

    @Test
    void legacyEnumSearchIsUsedWhenRerankCandidatesAreEmpty() {
        CharacterExampleRepository repository = mock(CharacterExampleRepository.class);
        CharacterTraitProfileService traitProfileService = mock(CharacterTraitProfileService.class);
        RelationshipRepository relationshipRepository = mock(RelationshipRepository.class);
        CharacterExample legacy = example(1L, AgentEventType.NORMAL, RelationshipTemperature.SPICY, "spicy-short");

        when(repository.findCandidateStyleExamples(1L, AgentEventType.NORMAL)).thenReturn(List.of());
        when(repository.findRelevantStyleExamples(
                eq(1L),
                eq(AgentEventType.NORMAL),
                eq(RelationshipTemperature.SPICY),
                any(Pageable.class)
        )).thenReturn(List.of(legacy));
        when(relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.empty());
        when(traitProfileService.findEntityOrDefault(1L)).thenReturn(new CharacterTraitProfile());

        CharacterExampleService service = service(repository, traitProfileService, relationshipRepository);

        List<CharacterExample> result = service.findRelevantEntities(
                1L,
                AgentEventType.NORMAL,
                RelationshipTemperature.SPICY
        );

        assertThat(result).containsExactly(legacy);
    }

    @Test
    void conflictRepairLegacyTemperatureStillWorks() {
        CharacterExampleRepository repository = mock(CharacterExampleRepository.class);
        CharacterTraitProfileService traitProfileService = mock(CharacterTraitProfileService.class);
        RelationshipRepository relationshipRepository = mock(RelationshipRepository.class);
        CharacterExample repair = example(
                2L,
                AgentEventType.APOLOGY,
                RelationshipTemperature.CONFLICT_REPAIR,
                "repair-soft-boundary"
        );

        when(repository.findCandidateStyleExamples(1L, AgentEventType.APOLOGY)).thenReturn(List.of());
        when(repository.findRelevantStyleExamples(
                eq(1L),
                eq(AgentEventType.APOLOGY),
                eq(RelationshipTemperature.CONFLICT_REPAIR),
                any(Pageable.class)
        )).thenReturn(List.of(repair));
        when(relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.empty());
        when(traitProfileService.findEntityOrDefault(1L)).thenReturn(new CharacterTraitProfile());

        CharacterExampleService service = service(repository, traitProfileService, relationshipRepository);

        List<CharacterExample> result = service.findRelevantEntities(
                1L,
                AgentEventType.APOLOGY,
                RelationshipTemperature.CONFLICT_REPAIR
        );

        assertThat(result).containsExactly(repair);
    }

    @Test
    void emptyCandidatesAndEmptyLegacySearchReturnsEmptyList() {
        CharacterExampleRepository repository = mock(CharacterExampleRepository.class);
        CharacterTraitProfileService traitProfileService = mock(CharacterTraitProfileService.class);
        RelationshipRepository relationshipRepository = mock(RelationshipRepository.class);

        when(repository.findCandidateStyleExamples(1L, AgentEventType.NORMAL)).thenReturn(List.of());
        when(repository.findRelevantStyleExamples(
                eq(1L),
                eq(AgentEventType.NORMAL),
                eq(RelationshipTemperature.NEUTRAL),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.empty());
        when(traitProfileService.findEntityOrDefault(1L)).thenReturn(new CharacterTraitProfile());

        CharacterExampleService service = service(repository, traitProfileService, relationshipRepository);

        List<CharacterExample> result = service.findRelevantEntities(
                1L,
                AgentEventType.NORMAL,
                RelationshipTemperature.NEUTRAL
        );

        assertThat(result).isEmpty();
    }

    @Test
    void relationshipSettingsAreUsedForAdvancedReranking() {
        CharacterExampleRepository repository = mock(CharacterExampleRepository.class);
        CharacterTraitProfileService traitProfileService = mock(CharacterTraitProfileService.class);
        RelationshipRepository relationshipRepository = mock(RelationshipRepository.class);
        Relationship relationship = new Relationship();
        relationship.setCharacterId(1L);
        relationship.setRelationshipStage("LONG_TERM");
        relationship.setRelationshipTemperatureScore(90);
        CharacterTraitProfile traits = new CharacterTraitProfile();
        traits.setConfidence(10);
        traits.setDominance(10);
        traits.setHumor(5);
        traits.setPlayfulness(5);
        traits.setAffection(5);
        traits.setEmpathy(5);
        traits.setAttachment(5);
        traits.setJealousy(5);
        traits.setExpressiveness(5);
        traits.setEmotionalStability(5);
        CharacterExample confident = example(1L, AgentEventType.NORMAL, RelationshipTemperature.NEUTRAL, "confident-dominant");
        CharacterExample warm = example(2L, AgentEventType.NORMAL, RelationshipTemperature.NEUTRAL, "warm-soft");

        when(repository.findCandidateStyleExamples(1L, AgentEventType.NORMAL)).thenReturn(List.of(warm, confident));
        when(relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.of(relationship));
        when(traitProfileService.findEntityOrDefault(1L)).thenReturn(traits);

        CharacterExampleService service = service(repository, traitProfileService, relationshipRepository);

        List<CharacterExample> result = service.findRelevantEntities(
                1L,
                AgentEventType.NORMAL,
                RelationshipTemperature.FRIENDLY
        );

        assertThat(result).extracting(CharacterExample::getToneTag)
                .startsWith("confident-dominant");
    }

    private CharacterExampleService service(
            CharacterExampleRepository repository,
            CharacterTraitProfileService traitProfileService,
            RelationshipRepository relationshipRepository
    ) {
        SettingsDefaultPolicy settingsDefaultPolicy = new SettingsDefaultPolicy();
        RelationshipTemperatureScoreResolver temperatureScoreResolver =
                new RelationshipTemperatureScoreResolver(settingsDefaultPolicy);
        return new CharacterExampleService(
                repository,
                traitProfileService,
                relationshipRepository,
                new RelationshipStageResolver(settingsDefaultPolicy),
                temperatureScoreResolver,
                new CharacterExampleReranker(
                        new CharacterExampleToneTagPolicy(),
                        temperatureScoreResolver,
                        new CharacterExampleRelevantTraitPolicy()
                )
        );
    }

    private CharacterExample example(
            Long id,
            AgentEventType eventType,
            RelationshipTemperature relationshipTemperature,
            String toneTag
    ) {
        CharacterExample example = new CharacterExample();
        example.setId(id);
        example.setCharacterId(1L);
        example.setEventType(eventType);
        example.setRelationshipTemperature(relationshipTemperature);
        example.setToneTag(toneTag);
        example.setPriority(80);
        example.setUserExample("user");
        example.setAssistantExample("assistant-" + toneTag);
        example.setActive(true);
        return example;
    }
}
