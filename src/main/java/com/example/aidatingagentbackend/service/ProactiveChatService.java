package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.dto.ProactiveSendRequest;
import org.springframework.stereotype.Service;

@Service
public class ProactiveChatService {

    private static final String PROACTIVE_SEED =
            "The user has not sent a new message. Send one short proactive check-in as the agent. "
                    + "Do not pretend to perform real physical actions. Do not guilt-trip, pressure, or demand a reply.";

    private final AIProcessingService aiProcessingService;
    private final GeminiService geminiService;
    private final ProactiveContactPolicyService proactiveContactPolicyService;

    public ProactiveChatService(
            AIProcessingService aiProcessingService,
            GeminiService geminiService,
            ProactiveContactPolicyService proactiveContactPolicyService
    ) {
        this.aiProcessingService = aiProcessingService;
        this.geminiService = geminiService;
        this.proactiveContactPolicyService = proactiveContactPolicyService;
    }

    public ChatResponse sendNow(ProactiveSendRequest request) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setRequestId(request.requestId());
        chatRequest.setCharacter(request.character());
        chatRequest.setRelationship(request.relationship());
        chatRequest.setHistory(request.history());
        chatRequest.setMessage(PROACTIVE_SEED);

        AIProcessingService.PreparedAIProcessing prepared = aiProcessingService.prepare(chatRequest, true);
        if (!proactiveContactPolicyService.shouldSend(prepared.context())) {
            throw new IllegalStateException("Proactive contact policy rejected this request.");
        }

        String generated = geminiService.generate(prepared.prompt());
        String reply = aiProcessingService.finishGeneratedReply(prepared, generated, true);
        ChatResponse response = new ChatResponse(reply);
        response.setRequestId(request.requestId());
        response.setRelationshipDelta(prepared.relationshipUpdate().delta());
        response.setNextRelationship(prepared.relationshipUpdate().nextRelationship());
        response.setPreviousAgentSelfState(prepared.emotionUpdateResult().previousState());
        response.setNextAgentSelfState(prepared.emotionUpdateResult().nextState());
        response.setEventAnalysis(prepared.eventAnalysis());
        return response;
    }
}
