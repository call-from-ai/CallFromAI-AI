package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.dto.AgentSelfStateSnapshot;
import com.example.aidatingagentbackend.dto.ErrorResponse;
import com.example.aidatingagentbackend.dto.GeminiImage;
import com.example.aidatingagentbackend.exception.GeminiCallException;
import com.example.aidatingagentbackend.exception.GeminiTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final AIProcessingService aiProcessingService;
    private final GeminiService geminiService;
    private final RequestIdempotencyService requestIdempotencyService;

    public ChatService(
            AIProcessingService aiProcessingService,
            GeminiService geminiService,
            RequestIdempotencyService requestIdempotencyService
    ) {
        this.aiProcessingService = aiProcessingService;
        this.geminiService = geminiService;
        this.requestIdempotencyService = requestIdempotencyService;
    }

    public ChatResponse chat(ChatRequest request) {
        return requestIdempotencyService.execute(request, () -> processChat(request));
    }

    public ChatResponse chat(ChatRequest request, GeminiImage image) {
        ChatRequest normalized = normalizeImageOnlyRequest(request, image);
        return requestIdempotencyService.execute(
                normalized,
                imageHash(image),
                () -> processChat(normalized, image)
        );
    }

    private ChatResponse processChat(ChatRequest request, GeminiImage image) {
        AIProcessingService.CompletedAIProcessing completed = aiProcessingService.process(request, image);
        return buildResponse(request, completed);
    }

    private ChatResponse processChat(ChatRequest request) {
        AIProcessingService.CompletedAIProcessing completed = aiProcessingService.process(request);
        return buildResponse(request, completed);
    }

    private ChatResponse buildResponse(
            ChatRequest request,
            AIProcessingService.CompletedAIProcessing completed
    ) {
        AIProcessingService.PreparedAIProcessing prepared = completed.prepared();
        ChatResponse response = new ChatResponse(completed.reply());
        response.setRequestId(request.getRequestId());
        response.setRelationshipDelta(prepared.relationshipUpdate().delta());
        response.setNextRelationship(prepared.relationshipUpdate().nextRelationship());
        response.setPreviousAgentSelfState(prepared.emotionUpdateResult().previousState());
        response.setNextAgentSelfState(prepared.emotionUpdateResult().nextState());
        response.setEventAnalysis(prepared.eventAnalysis());
        return response;
    }

    public SseEmitter sendMessageStream(ChatRequest request) {
        return sendMessageStream(request, null);
    }

    public SseEmitter sendMessageStream(ChatRequest request, GeminiImage image) {
        ChatRequest normalized = normalizeImageOnlyRequest(request, image);
        RequestIdempotencyService.Claim claim =
                requestIdempotencyService.claim(normalized, imageHash(image));
        if (claim.cachedResponse() != null) {
            return replayStream(claim.cachedResponse());
        }
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

        CompletableFuture.runAsync(() -> {
            long startedAt = System.currentTimeMillis();
            long firstTokenAt = -1L;
            AtomicBoolean firstChunkSent = new AtomicBoolean(false);
            StringBuilder streamedReply = new StringBuilder();

            geminiService.resetCallCount();
            try {
                AIProcessingService.PreparedAIProcessing prepared =
                        aiProcessingService.prepare(normalized, true);

                sendEvent(emitter, "meta", Map.of(
                        "eventType", prepared.eventAnalysis().eventType().name(),
                        "compactPrompt", true
                ));

                final long[] firstTokenHolder = {firstTokenAt};
                geminiService.generateStream(prepared.prompt(), image, prepared.channel(), chunk -> {
                    if (firstChunkSent.compareAndSet(false, true)) {
                        firstTokenHolder[0] = System.currentTimeMillis();
                    }
                    streamedReply.append(chunk);
                    sendEvent(emitter, "chunk", Map.of("text", chunk));
                });
                firstTokenAt = firstTokenHolder[0];

                String reply = aiProcessingService.finishGeneratedReply(prepared, streamedReply.toString(), false);
                ChatResponse response = buildResponse(normalized, prepared, reply);
                requestIdempotencyService.complete(claim.requestId(), response);

                long completedAt = System.currentTimeMillis();
                long firstTokenLatencyMs = firstTokenAt < 0 ? completedAt - startedAt : firstTokenAt - startedAt;
                long totalLatencyMs = completedAt - startedAt;
                int llmCallCount = geminiService.currentCallCount();
                log.info(
                        "chat.stream latency characterId={} firstTokenLatencyMs={} totalLatencyMs={} llmCallCount={}",
                        prepared.characterId(),
                        firstTokenLatencyMs,
                        totalLatencyMs,
                        llmCallCount
                );
                sendEvent(emitter, "done", Map.of(
                        "firstTokenLatencyMs", firstTokenLatencyMs,
                        "totalLatencyMs", totalLatencyMs,
                        "llmCallCount", llmCallCount
                ));
                emitter.complete();
            } catch (Exception exception) {
                requestIdempotencyService.release(claim.requestId());
                log.warn("chat.stream failed characterId={}", request.resolveCharacterId(), exception);
                sendEvent(emitter, "error", streamError(exception, request.getRequestId()));
                emitter.complete();
            } finally {
                geminiService.clearCallCount();
            }
        });

        return emitter;
    }

    private ChatRequest normalizeImageOnlyRequest(ChatRequest request, GeminiImage image) {
        if (image != null && (request.getMessage() == null || request.getMessage().isBlank())) {
            return request.withMessage(
                    "[사용자가 이미지를 보냈습니다. 이미지 내용을 이해하고 현재 대화와 관계에 맞게 자연스럽게 반응해 주세요.]"
            );
        }
        return request;
    }

    private String imageHash(GeminiImage image) {
        if (image == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(image.mimeType().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update(image.data());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private SseEmitter replayStream(ChatResponse cachedResponse) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        CompletableFuture.runAsync(() -> {
            sendEvent(emitter, "chunk", Map.of("text", cachedResponse.getReply()));
            sendEvent(emitter, "done", Map.of("cached", true));
            emitter.complete();
        });
        return emitter;
    }

    private ErrorResponse streamError(Exception exception, String requestId) {
        HttpStatus status;
        if (exception instanceof GeminiTimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
        } else if (exception instanceof GeminiCallException) {
            status = HttpStatus.BAD_GATEWAY;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = exception.getMessage() == null ? status.getReasonPhrase() : exception.getMessage();
        return new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, "/chat/stream", requestId);
    }

    private ChatResponse buildResponse(
            ChatRequest request,
            AIProcessingService.PreparedAIProcessing prepared,
            String reply
    ) {
        ChatResponse response = new ChatResponse(reply);
        response.setRequestId(request.getRequestId());
        response.setRelationshipDelta(prepared.relationshipUpdate().delta());
        response.setNextRelationship(prepared.relationshipUpdate().nextRelationship());
        response.setPreviousAgentSelfState(prepared.emotionUpdateResult().previousState());
        response.setNextAgentSelfState(prepared.emotionUpdateResult().nextState());
        response.setEventAnalysis(prepared.eventAnalysis());
        return response;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send stream event.", exception);
        }
    }
}
