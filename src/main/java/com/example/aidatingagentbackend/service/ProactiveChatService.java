package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import org.springframework.stereotype.Service;
import com.example.aidatingagentbackend.exception.ProactivePolicyRejectedException;

@Service
public class ProactiveChatService {

    private final AIProcessingService aiProcessingService;
    private final GeminiService geminiService;
    private final ProactiveContactPolicyService proactiveContactPolicyService;
    private final RequestIdempotencyService requestIdempotencyService;

    public ProactiveChatService(
            AIProcessingService aiProcessingService,
            GeminiService geminiService,
            ProactiveContactPolicyService proactiveContactPolicyService,
            RequestIdempotencyService requestIdempotencyService
    ) {
        this.aiProcessingService = aiProcessingService;
        this.geminiService = geminiService;
        this.proactiveContactPolicyService = proactiveContactPolicyService;
        this.requestIdempotencyService = requestIdempotencyService;
    }

    public ChatResponse sendNow(ChatRequest request) {
        return requestIdempotencyService.execute(request, () -> process(request));
    }

    private ChatResponse process(ChatRequest request) {
        request.validateForProactive();
        ChatRequest chatRequest = request.withMessage(buildProactiveInstruction(request));

        AIProcessingService.PreparedAIProcessing prepared = aiProcessingService.prepare(chatRequest, true);
        if (!proactiveContactPolicyService.shouldSend(prepared.context())) {
            throw new ProactivePolicyRejectedException("Proactive contact policy rejected this request.");
        }

        String generated = geminiService.generate(prepared.prompt(), prepared.channel());
        String reply = aiProcessingService.finishGeneratedReply(prepared, generated, true);
        ChatResponse response = new ChatResponse(reply);
        response.setRequestId(request.getRequestId());
        response.setRelationshipDelta(prepared.relationshipUpdate().delta());
        response.setNextRelationship(prepared.relationshipUpdate().nextRelationship());
        response.setPreviousAgentSelfState(prepared.emotionUpdateResult().previousState());
        response.setNextAgentSelfState(prepared.emotionUpdateResult().nextState());
        response.setEventAnalysis(prepared.eventAnalysis());
        return response;
    }

    static String buildProactiveInstruction(ChatRequest request) {
        var relationshipState = request.getRelationshipState() == null
                ? com.example.aidatingagentbackend.dto.ProactiveRelationshipState.NORMAL
                : request.getRelationshipState();
        var contactReason = request.getContactReason() == null
                ? com.example.aidatingagentbackend.dto.ProactiveContactReason.NORMAL_CHECK_IN
                : request.getContactReason();
        var recentResponse = request.getRecentResponse() == null
                ? com.example.aidatingagentbackend.dto.RecentResponse.AMBIGUOUS
                : request.getRecentResponse();

        String statePolicy = switch (relationshipState) {
            case NORMAL -> "Write a natural, casual check-in.";
            case UPSET -> "Acknowledge the unresolved hurt or disappointment; do not ignore it or act cheerful as if nothing happened.";
            case CONFLICT -> "Write only a short message intended to calmly address and organize the conflict. Do not make unrelated small talk.";
            case REPAIRING -> "Write a careful repair-oriented check-in. Do not imply that everything is already fully resolved.";
        };
        String reasonPolicy = switch (contactReason) {
            case NORMAL_CHECK_IN -> "The purpose is a normal check-in.";
            case CALL_OFFER -> "Offer a brief call in natural Korean, with wording equivalent to \"지금 잠깐 통화할래?\".";
        };
        String responsePolicy = switch (recentResponse) {
            case POSITIVE -> "The user's latest response was positive.";
            case AMBIGUOUS -> "The user's latest response was ambiguous, so avoid overconfidence and pressure.";
            case NO_RESPONSE -> "The user did not respond recently; do not guilt-trip, chase, or demand a reply.";
        };

        return "The user has not sent a new message. Send one short proactive message as the agent. "
                + statePolicy + " " + reasonPolicy + " " + responsePolicy + " "
                + "Use romanceStyleScore only to tune how forward or reserved the wording is; never use it to decide whether to contact. "
                + "Do not pretend to perform real physical actions. Do not guilt-trip, pressure, or demand a reply.";
    }
}
