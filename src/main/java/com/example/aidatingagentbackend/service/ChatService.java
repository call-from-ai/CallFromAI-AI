package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final AIProcessingService aiProcessingService;
    private final GeminiService geminiService;

    public ChatService(
            AIProcessingService aiProcessingService,
            GeminiService geminiService
    ) {
        this.aiProcessingService = aiProcessingService;
        this.geminiService = geminiService;
    }

    public ChatResponse chat(ChatRequest request) {
        AIProcessingService.CompletedAIProcessing completed = aiProcessingService.process(request);
        return new ChatResponse(completed.reply());
    }

    public SseEmitter sendMessageStream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

        CompletableFuture.runAsync(() -> {
            long startedAt = System.currentTimeMillis();
            long firstTokenAt = -1L;
            AtomicBoolean firstChunkSent = new AtomicBoolean(false);
            StringBuilder streamedReply = new StringBuilder();

            geminiService.resetCallCount();
            try {
                AIProcessingService.PreparedAIProcessing prepared =
                        aiProcessingService.prepare(request, true);

                sendEvent(emitter, "meta", Map.of(
                        "eventType", prepared.eventAnalysis().eventType().name(),
                        "compactPrompt", true
                ));

                final long[] firstTokenHolder = {firstTokenAt};
                geminiService.generateStream(prepared.prompt(), chunk -> {
                    if (firstChunkSent.compareAndSet(false, true)) {
                        firstTokenHolder[0] = System.currentTimeMillis();
                    }
                    streamedReply.append(chunk);
                    sendEvent(emitter, "chunk", Map.of("text", chunk));
                });
                firstTokenAt = firstTokenHolder[0];

                aiProcessingService.finishGeneratedReply(prepared, streamedReply.toString(), false);

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
                log.warn("chat.stream failed characterId={}", request.resolveCharacterId(), exception);
                sendEvent(emitter, "error", Map.of("message", exception.getMessage() == null ? "stream failed" : exception.getMessage()));
                emitter.completeWithError(exception);
            } finally {
                geminiService.clearCallCount();
            }
        });

        return emitter;
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
