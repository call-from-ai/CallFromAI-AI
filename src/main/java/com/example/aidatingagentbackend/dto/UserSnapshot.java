package com.example.aidatingagentbackend.dto;

import java.time.LocalDate;

/** Main backend user profile embedded in chat/proactive/call requests. */
public record UserSnapshot(
        LocalDate birth,
        String gender,
        String job,
        String mbti
) {
}
