package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.config.InternalApiAuthFilter;
import com.example.aidatingagentbackend.service.CharacterDerivedDataService;
import com.example.aidatingagentbackend.service.CharacterSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

class InternalCharacterSnapshotControllerTests {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new InternalCharacterDataController(
                        mock(CharacterDerivedDataService.class), new CharacterSnapshotService(mock(com.example.aidatingagentbackend.repository.CharacterSnapshotRepository.class))))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new InternalApiAuthFilter("secret", new ObjectMapper().findAndRegisterModules()))
                .build();
    }

    @Test
    void returns204ForValidUpsert() throws Exception {
        mvc.perform(put("/internal/characters/10/snapshot").header("X-Internal-Api-Key", "secret")
                        .contentType(MediaType.APPLICATION_JSON).content(payload(10)))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns400ForPathBodyMismatch() throws Exception {
        mvc.perform(put("/internal/characters/11/snapshot").header("X-Internal-Api-Key", "secret")
                        .contentType(MediaType.APPLICATION_JSON).content(payload(10)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns401WithoutApiKey() throws Exception {
        mvc.perform(put("/internal/characters/10/snapshot").contentType(MediaType.APPLICATION_JSON).content(payload(10)))
                .andExpect(status().isUnauthorized());
    }

    private String payload(long characterId) {
        return "{\"characterId\":" + characterId + ",\"name\":\"하나\",\"mind\":\"따뜻하다\",\"responseStyle\":\"짧게\"," +
                "\"job\":\"DEVELOPER\",\"lifeType\":\"WORKER\",\"romanceStyleScore\":72,\"traits\":{" +
                "\"humor\":6,\"playfulness\":7,\"affection\":8,\"empathy\":9,\"attachment\":5,\"jealousy\":2," +
                "\"dominance\":4,\"confidence\":7,\"expressiveness\":8,\"emotionalStability\":7,\"calculationVersion\":1}}";
    }
}
