package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.service.ProactiveChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ProactiveChatController {

    private final ProactiveChatService proactiveChatService;

    public ProactiveChatController(ProactiveChatService proactiveChatService) {
        this.proactiveChatService = proactiveChatService;
    }

    @GetMapping(value = "/api/chat/proactive/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam Long userId) {
        return proactiveChatService.subscribe(userId);
    }

    @PostMapping("/api/chat/proactive/send")
    public void sendNow(@RequestParam Long userId) {
        proactiveChatService.sendNow(userId);
    }
}
