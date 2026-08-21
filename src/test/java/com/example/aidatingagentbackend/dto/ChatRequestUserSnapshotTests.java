package com.example.aidatingagentbackend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ChatRequestUserSnapshotTests {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void readsGenderAndCalculatesAgeFromNestedUserSnapshot() throws Exception {
        ChatRequest request = mapper.readValue("""
                {
                  "user": {
                    "birth": "2000-08-12",
                    "gender": "FEMALE",
                    "job": "EMPLOYEE",
                    "mbti": "INTJ"
                  },
                  "localDateTime": "2026-08-11T12:00:00+09:00"
                }
                """, ChatRequest.class);

        assertThat(request.getUserGender()).isEqualTo("FEMALE");
        assertThat(request.getUserAge()).isEqualTo(25);
    }

    @Test
    void legacyTopLevelValuesTakePriority() {
        ChatRequest request = new ChatRequest();
        request.setUser(new UserSnapshot(LocalDate.of(2000, 1, 1), "FEMALE", null, null));
        request.setUserAge(30);
        request.setUserGender("MALE");

        assertThat(request.getUserAge()).isEqualTo(30);
        assertThat(request.getUserGender()).isEqualTo("MALE");
    }

    @Test
    void treatsLegacyZeroAgeAsMissing() {
        ChatRequest request = new ChatRequest();
        request.setUserAge(0);

        assertThat(request.getUserAge()).isNull();
    }

    @Test
    void derivesAgeFromBirthWhenLegacyAgeIsZero() {
        ChatRequest request = new ChatRequest();
        request.setUserAge(0);
        request.setUser(new UserSnapshot(LocalDate.of(2000, 8, 12), "FEMALE", null, null));
        request.setLocalDateTime(java.time.OffsetDateTime.parse("2026-08-11T12:00:00+09:00"));

        assertThat(request.getUserAge()).isEqualTo(25);
    }
}
