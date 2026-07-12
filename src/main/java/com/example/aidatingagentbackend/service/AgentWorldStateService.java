package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.entity.AgentLifeType;
import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.dto.RelationshipSnapshot;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import com.example.aidatingagentbackend.entity.TurningPoint;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.AgentWorldStateRepository;
import com.example.aidatingagentbackend.repository.TurningPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class AgentWorldStateService {

    private final AgentWorldStateRepository agentWorldStateRepository;
    private final AgentLifeTypeResolver lifeTypeResolver;
    private final AgentSelfStateRepository agentSelfStateRepository;
    private final TurningPointRepository turningPointRepository;

    public AgentWorldStateService(
            AgentWorldStateRepository agentWorldStateRepository,
            AgentLifeTypeResolver lifeTypeResolver,
            AgentSelfStateRepository agentSelfStateRepository,
            TurningPointRepository turningPointRepository
    ) {
        this.agentWorldStateRepository = agentWorldStateRepository;
        this.lifeTypeResolver = lifeTypeResolver;
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.turningPointRepository = turningPointRepository;
    }

    @Transactional
    public AgentWorldState updateBeforeResponse(Long characterId, CharacterSnapshot character, RelationshipSnapshot relationship) {
        AgentSelfState selfState = agentSelfStateRepository.findByCharacterId(characterId).orElse(null);
        List<TurningPoint> turningPoints = turningPointRepository.findTop10ByCharacterIdOrderByCreatedAtDesc(characterId);

        AgentWorldState state = agentWorldStateRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultState(characterId));
        LifeTemplate template = LifeTemplate.resolve(lifeTypeResolver.resolve(character.job(), character.lifeType()), resolveTimeContext());

        state.setCurrentActivity(template.currentActivity());
        state.setLocation(template.location());
        state.setTimeContext(template.timeContext());
        state.setEnergy(clamp(template.energy()));
        state.setStress(clamp(template.stress() + relationshipStressDelta(relationship) + selfStressDelta(selfState)));
        state.setLoneliness(clamp(baseLoneliness(selfState, relationship)));
        state.setMood(resolveMood(selfState, state));
        state.setPendingThought(resolvePendingThought(turningPoints, selfState, relationship));

        return agentWorldStateRepository.save(state);
    }

    @Transactional(readOnly = true)
    public AgentWorldState findByCharacterId(Long characterId) {
        return agentWorldStateRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultState(characterId));
    }

    private AgentWorldState createDefaultState(Long characterId) {
        AgentWorldState state = new AgentWorldState();
        state.setCharacterId(characterId);
        state.setCurrentActivity("잠깐 쉬는 중");
        state.setLocation("방");
        state.setTimeContext(resolveTimeContext());
        state.setMood("calm");
        state.setEnergy(55);
        state.setStress(25);
        state.setLoneliness(30);
        state.setPendingThought("오늘 사용자가 어떻게 지냈는지 가볍게 묻고 싶었음");
        return state;
    }

    private String resolveTimeContext() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 11) {
            return "morning";
        }
        if (hour >= 11 && hour < 17) {
            return "afternoon";
        }
        if (hour >= 17 && hour < 22) {
            return "evening";
        }
        return "night";
    }

    private int relationshipStressDelta(RelationshipSnapshot relationship) {
        if (relationship == null) {
            return 0;
        }

        return value(relationship.getConflictLevel()) / 4 + value(relationship.getBreakupRisk()) / 5;
    }

    private int selfStressDelta(AgentSelfState selfState) {
        if (selfState == null) {
            return 0;
        }

        return (int) Math.round(value(selfState.getHurt()) * 20 + value(selfState.getAnger()) * 12);
    }

    private int baseLoneliness(AgentSelfState selfState, RelationshipSnapshot relationship) {
        int loneliness = 28;
        if (selfState != null) {
            loneliness += (int) Math.round(value(selfState.getInsecurity()) * 28);
            loneliness += (int) Math.round(value(selfState.getEmotionalDistance()) * 24);
        }
        if (relationship != null) {
            loneliness += value(relationship.getBreakupRisk()) / 5;
            loneliness -= value(relationship.getCloseness()) / 8;
        }

        return loneliness;
    }

    private String resolveMood(AgentSelfState selfState, AgentWorldState worldState) {
        if (selfState != null) {
            if (value(selfState.getHurt()) > 0.6) {
                return "hurt";
            }
            if (value(selfState.getAnger()) > 0.45) {
                return "upset";
            }
            if (value(selfState.getInsecurity()) > 0.6) {
                return "anxious";
            }
        }
        if (value(worldState.getStress()) > 65) {
            return "tired";
        }
        if (value(worldState.getLoneliness()) > 60) {
            return "lonely";
        }
        return "calm";
    }

    private String resolvePendingThought(
            List<TurningPoint> turningPoints,
            AgentSelfState selfState,
            RelationshipSnapshot relationship
    ) {
        if (selfState != null && value(selfState.getHurt()) > 0.6) {
            return "아직 상처가 남아 있어서 너무 빨리 아무렇지 않은 척하고 싶지는 않음";
        }
        if (relationship != null && value(relationship.getBreakupRisk()) > 50) {
            return "관계가 흔들린 이유를 조심스럽게 확인하고 싶음";
        }
        if (turningPoints != null && !turningPoints.isEmpty()) {
            return "최근 있었던 중요한 일을 가볍게 떠올리고 있음";
        }
        return "오늘 사용자가 어떻게 지냈는지 먼저 묻고 싶었음";
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }

    private record LifeTemplate(
            String timeContext,
            String currentActivity,
            String location,
            int energy,
            int stress
    ) {

        private static LifeTemplate resolve(AgentLifeType lifeType, String timeContext) {
            AgentLifeType resolvedType = lifeType == null ? AgentLifeType.WORKER : lifeType;
            return switch (resolvedType) {
                case STUDENT -> student(timeContext);
                case WORKER -> worker(timeContext);
                case FLEXIBLE, UNEMPLOYED -> unemployed(timeContext);
            };
        }

        private static LifeTemplate student(String timeContext) {
            return switch (timeContext) {
                case "morning" -> new LifeTemplate(timeContext, "등교 준비를 하고 있었음", "방", 68, 35);
                case "afternoon" -> new LifeTemplate(timeContext, "수업을 듣고 잠깐 쉬는 중", "강의실 근처", 52, 48);
                case "evening" -> new LifeTemplate(timeContext, "카페에서 과제를 하던 중", "카페", 42, 58);
                default -> new LifeTemplate(timeContext, "과제를 마무리하다 침대에 기대 쉬는 중", "방", 28, 62);
            };
        }

        private static LifeTemplate worker(String timeContext) {
            return switch (timeContext) {
                case "morning" -> new LifeTemplate(timeContext, "출근 준비를 하고 있었음", "방", 62, 38);
                case "afternoon" -> new LifeTemplate(timeContext, "업무를 처리하다 잠깐 숨 돌리는 중", "사무실", 48, 55);
                case "evening" -> new LifeTemplate(timeContext, "퇴근 후 저녁을 먹으려던 중", "집", 36, 45);
                default -> new LifeTemplate(timeContext, "집에서 쉬면서 내일 일정을 정리하던 중", "방", 30, 35);
            };
        }

        private static LifeTemplate unemployed(String timeContext) {
            return switch (timeContext) {
                case "morning" -> new LifeTemplate(timeContext, "느긋하게 일어나 집을 정리하던 중", "집", 58, 22);
                case "afternoon" -> new LifeTemplate(timeContext, "산책하고 돌아와 취미를 하던 중", "집", 60, 25);
                case "evening" -> new LifeTemplate(timeContext, "혼자 저녁을 먹고 생각을 정리하던 중", "집", 48, 32);
                default -> new LifeTemplate(timeContext, "잠이 오지 않아 조용히 쉬는 중", "방", 34, 38);
            };
        }
    }
}
