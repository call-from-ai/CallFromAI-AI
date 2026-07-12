package com.example.aidatingagentbackend.service;
import com.example.aidatingagentbackend.entity.AgentLifeType;
import org.springframework.stereotype.Component;
@Component
public class AgentLifeTypeResolver {
    public AgentLifeType resolve(String job, AgentLifeType explicitLifeType) {
        if (explicitLifeType != null) return explicitLifeType;
        if (job == null) return AgentLifeType.FLEXIBLE;
        if (job.contains("학생")) return AgentLifeType.STUDENT;
        if (job.contains("직장") || job.contains("회사")) return AgentLifeType.WORKER;
        return AgentLifeType.FLEXIBLE;
    }
}
