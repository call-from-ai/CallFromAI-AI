package com.example.aidatingagentbackend.dto;
public record ChatHistoryItem(String role, String content) {
    public ChatHistoryItem {
        if (role == null || role.isBlank()) throw new IllegalArgumentException("history.role is required");
        if (content == null) throw new IllegalArgumentException("history.content is required");
    }
    public String getRole(){return role;} public String getContent(){return content;}
}
