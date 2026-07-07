package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }

    @PostMapping(value = {"/chat/stream", "/api/chat/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        return chatService.sendMessageStream(request);
    }
}
