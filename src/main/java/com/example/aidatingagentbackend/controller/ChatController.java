package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.dto.GeminiImage;
import com.example.aidatingagentbackend.service.ChatService;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ChatController {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> SUPPORTED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, HttpServletRequest servletRequest) {
        RequestIdSupport.attach(servletRequest, request.getRequestId());
        request.validateForChat();
        return ResponseEntity.ok(chatService.chat(request));
    }

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatResponse> chatWithImage(
            @RequestPart("request") ChatRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            HttpServletRequest servletRequest
    ) {
        RequestIdSupport.attach(servletRequest, request.getRequestId());
        GeminiImage geminiImage = toGeminiImage(image);
        request.validateForChat(geminiImage != null);
        return ResponseEntity.ok(chatService.chat(request, geminiImage));
    }

    @PostMapping(value = {"/chat/stream", "/api/chat/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request, HttpServletRequest servletRequest) {
        RequestIdSupport.attach(servletRequest, request.getRequestId());
        request.validateForChat();
        return chatService.sendMessageStream(request);
    }

    @PostMapping(
            value = {"/chat/stream", "/api/chat/stream"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter chatStreamWithImage(
            @RequestPart("request") ChatRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            HttpServletRequest servletRequest
    ) {
        RequestIdSupport.attach(servletRequest, request.getRequestId());
        GeminiImage geminiImage = toGeminiImage(image);
        request.validateForChat(geminiImage != null);
        return chatService.sendMessageStream(request, geminiImage);
    }

    private GeminiImage toGeminiImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("image must be 10MB or smaller");
        }
        String contentType = image.getContentType();
        if (!SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("image must be JPEG, PNG, or WebP");
        }
        try {
            return new GeminiImage(image.getBytes(), contentType);
        } catch (IOException exception) {
            throw new IllegalArgumentException("failed to read image", exception);
        }
    }
}
