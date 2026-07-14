package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.entity.AiRequestLedger;
import com.example.aidatingagentbackend.entity.AiRequestStatus;
import com.example.aidatingagentbackend.exception.RequestIdConflictException;
import com.example.aidatingagentbackend.repository.AiRequestLedgerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.function.Supplier;

@Service
public class RequestIdempotencyService {

    private final JdbcTemplate jdbcTemplate;
    private final AiRequestLedgerRepository repository;
    private final ObjectMapper objectMapper;

    public RequestIdempotencyService(
            JdbcTemplate jdbcTemplate,
            AiRequestLedgerRepository repository,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ChatResponse execute(ChatRequest request, Supplier<ChatResponse> action) {
        Claim claim = claim(request);
        if (claim.cachedResponse() != null) {
            return claim.cachedResponse();
        }

        try {
            ChatResponse response = action.get();
            complete(claim.requestId(), response);
            return response;
        } catch (RuntimeException exception) {
            release(claim.requestId());
            throw exception;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Claim claim(ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getRequestId())) {
            throw new IllegalArgumentException("requestId is required");
        }

        String requestId = request.getRequestId().trim();
        String requestHash = hash(request);
        int inserted = jdbcTemplate.update(
                "INSERT IGNORE INTO ai_request_ledger " +
                        "(request_id, request_hash, status, created_at) VALUES (?, ?, 'PROCESSING', CURRENT_TIMESTAMP)",
                requestId,
                requestHash
        );

        if (inserted == 1) {
            return new Claim(requestId, null);
        }

        AiRequestLedger existing = repository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Failed to resolve request ledger entry."));
        if (!requestHash.equals(existing.getRequestHash())) {
            throw new RequestIdConflictException("requestId is already associated with a different request body");
        }
        if (existing.getStatus() == AiRequestStatus.PROCESSING) {
            throw new RequestIdConflictException("requestId is already being processed");
        }
        return new Claim(requestId, deserialize(existing.getResponseJson()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String requestId, ChatResponse response) {
        AiRequestLedger ledger = repository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Request ledger entry disappeared before completion."));
        ledger.setStatus(AiRequestStatus.COMPLETED);
        ledger.setResponseJson(serialize(response));
        ledger.setCompletedAt(LocalDateTime.now());
        repository.save(ledger);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String requestId) {
        repository.deleteById(requestId);
    }

    private String hash(ChatRequest request) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Failed to hash idempotent request.", exception);
        }
    }

    private String serialize(ChatResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize idempotent response.", exception);
        }
    }

    private ChatResponse deserialize(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, ChatResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize idempotent response.", exception);
        }
    }

    public record Claim(String requestId, ChatResponse cachedResponse) {
    }
}
