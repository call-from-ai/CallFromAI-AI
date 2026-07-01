package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.MemoryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MemoryResponse {

    private Long id;

    private MemoryType type;

    private String summary;

    private Integer importance;

    private LocalDateTime createdAt;

    public static MemoryResponse from(Memory memory) {
        MemoryResponse response = new MemoryResponse();
        response.setId(memory.getId());
        response.setType(memory.getType());
        response.setSummary(memory.getSummary());
        response.setImportance(memory.getImportance());
        response.setCreatedAt(memory.getCreatedAt());
        return response;
    }
}
