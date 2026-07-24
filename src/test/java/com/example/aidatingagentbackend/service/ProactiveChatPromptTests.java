package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ProactiveContactReason;
import com.example.aidatingagentbackend.dto.ProactiveRelationshipState;
import com.example.aidatingagentbackend.dto.RecentResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProactiveChatPromptTests {

    @Test
    void normalStateRequestsNaturalCheckIn() {
        assertThat(instruction(ProactiveRelationshipState.NORMAL, ProactiveContactReason.NORMAL_CHECK_IN,
                RecentResponse.POSITIVE)).contains("natural, casual check-in");
    }

    @Test
    void upsetStateDoesNotIgnoreHurt() {
        assertThat(instruction(ProactiveRelationshipState.UPSET, ProactiveContactReason.NORMAL_CHECK_IN,
                RecentResponse.AMBIGUOUS)).contains("unresolved hurt", "avoid overconfidence");
    }

    @Test
    void conflictStateAllowsOnlyConflictMessage() {
        assertThat(instruction(ProactiveRelationshipState.CONFLICT, ProactiveContactReason.NORMAL_CHECK_IN,
                RecentResponse.NO_RESPONSE)).contains("only a short message", "Do not make unrelated small talk");
    }

    @Test
    void repairingStateDoesNotClaimRepairIsComplete() {
        assertThat(instruction(ProactiveRelationshipState.REPAIRING, ProactiveContactReason.NORMAL_CHECK_IN,
                RecentResponse.POSITIVE)).contains("Do not imply that everything is already fully resolved");
    }

    @Test
    void callOfferUsesBriefCallWording() {
        assertThat(instruction(ProactiveRelationshipState.NORMAL, ProactiveContactReason.CALL_OFFER,
                RecentResponse.POSITIVE)).contains("지금 잠깐 통화할래?");
    }

    private String instruction(ProactiveRelationshipState state, ProactiveContactReason reason,
                               RecentResponse response) {
        ChatRequest request = new ChatRequest();
        request.setRelationshipState(state);
        request.setContactReason(reason);
        request.setRecentResponse(response);
        return ProactiveChatService.buildProactiveInstruction(request);
    }
}
