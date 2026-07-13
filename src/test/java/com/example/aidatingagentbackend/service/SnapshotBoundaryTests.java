package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.context.ContextLoader;
import com.example.aidatingagentbackend.context.ContextUpdater;
import com.example.aidatingagentbackend.dto.*;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.RelationshipEngine;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SnapshotBoundaryTests {

    @Test
    void allTenTraitsAreMandatory() {
        assertThatThrownBy(() -> new CharacterTraitSnapshot(
                null, 5, 5, 5, 5, 5, 5, 5, 5, 5, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("humor");
    }

    @Test
    void proactiveRequestRequiresBothSnapshots() {
        ChatRequest missingCharacter = request(null, relationship(), null);
        ChatRequest missingRelationship = request(character(), null, null);

        assertThatThrownBy(missingCharacter::validateForProactive)
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(missingRelationship::validateForProactive)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chatRequiresMessageButProactiveDoesNot() {
        ChatRequest request = request(character(), relationship(), null);

        assertThatThrownBy(request::validateForChat)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message is required");
        request.validateForProactive();
    }

    @Test
    void relationshipEngineCalculatesNextValuesFromSnapshot() {
        RelationshipEngine.RelationshipResult next = new RelationshipEngine().analyze(relationship(), "미안해 사과할게");
        assertThat(next.trust()).isEqualTo(58);
        assertThat(next.closeness()).isEqualTo(54);
        assertThat(next.conflictLevel()).isEqualTo(12);
        assertThat(next.repairProgress()).isEqualTo(40);
        assertThat(next.breakupRisk()).isEqualTo(10);
    }

    @Test
    void requestIdAndRelationshipResultAreReturnedByChatService() {
        AIProcessingService processing = mock(AIProcessingService.class);
        ChatRequest request = new ChatRequest();
        request.setRequestId("req-77");
        request.setCharacter(character());
        request.setRelationship(relationship());
        request.setMessage("안녕");
        RelationshipDelta delta = new RelationshipDelta(8, 4, -8, 20, -10);
        ContextUpdater.RelationshipUpdate update = new ContextUpdater.RelationshipUpdate(delta, relationship());
        EventAnalysis analysis = EventAnalysis.fallback(AgentEventType.NORMAL);
        EmotionUpdateService.EmotionUpdateResult emotion = new EmotionUpdateService.EmotionUpdateResult(null, null, analysis);
        AIProcessingService.PreparedAIProcessing prepared = new AIProcessingService.PreparedAIProcessing(
                10L, "안녕", analysis, emotion, update, null, "prompt");
        when(processing.process(request)).thenReturn(new AIProcessingService.CompletedAIProcessing(prepared, "reply"));

        ChatResponse response = new ChatService(processing, mock(GeminiService.class)).chat(request);
        assertThat(response.getRequestId()).isEqualTo("req-77");
        assertThat(response.getRelationshipDelta()).isEqualTo(delta);
        assertThat(response.getNextRelationship()).isEqualTo(relationship());
    }

    @Test
    void runtimeBoundaryHasNoRemovedPersistenceDependencies() {
        assertThat(List.of(ContextLoader.class.getDeclaredFields()))
                .noneMatch(field -> field.getType().getSimpleName().matches(
                        "CharacterRepository|RelationshipRepository|AgentProfileRepository|ChatMessageRepository"));
        assertThat(List.of(AIProcessingService.class.getDeclaredFields()))
                .noneMatch(field -> field.getType().getSimpleName().equals("ChatMessageRepository"));
    }

    private CharacterSnapshot character() {
        return new CharacterSnapshot(10L, "하나", "따뜻함", "짧게", "개발자", null, 90,
                new CharacterTraitSnapshot(5, 5, 6, 7, 5, 2, 4, 6, 7, 8, 1));
    }

    private RelationshipSnapshot relationship() {
        return new RelationshipSnapshot(20L, RelationshipStage.DATING, 35,
                50, 50, 20, 20, 20, 30, RelationshipStrategy.NORMAL);
    }

    private ChatRequest request(CharacterSnapshot character, RelationshipSnapshot relationship, String message) {
        ChatRequest request = new ChatRequest();
        request.setRequestId("r");
        request.setCharacter(character);
        request.setRelationship(relationship);
        request.setHistory(List.of());
        request.setMessage(message);
        return request;
    }
}
