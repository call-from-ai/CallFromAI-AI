package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.ChatResponse;
import com.example.aidatingagentbackend.dto.ChatRequest;
import com.example.aidatingagentbackend.service.ProactiveChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ProactiveChatController {

    private final ProactiveChatService proactiveChatService;

    public ProactiveChatController(ProactiveChatService proactiveChatService) {
        this.proactiveChatService = proactiveChatService;
    }

    @PostMapping("/api/chat/proactive/send")
    public ChatResponse sendNow(@RequestBody ChatRequest request, HttpServletRequest servletRequest) {
        RequestIdSupport.attach(servletRequest, request.getRequestId());
        request.validateForProactive();
        return proactiveChatService.sendNow(request);
    }
}
