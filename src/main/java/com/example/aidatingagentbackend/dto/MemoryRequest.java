package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.MemoryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemoryRequest {

    private Long characterId;

    private MemoryType type;

    private String summary;

    private Integer importance;
}
