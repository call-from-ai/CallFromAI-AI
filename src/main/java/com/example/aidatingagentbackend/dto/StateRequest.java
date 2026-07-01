package com.example.aidatingagentbackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StateRequest {

    private String emotion;

    private Integer emotionIntensity;

    private Integer energy;

    private Integer stress;

    private String thinking;

    private String goal;
}
