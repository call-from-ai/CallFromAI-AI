package com.example.aidatingagentbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {

    private Long userId;

    private String message;
}
