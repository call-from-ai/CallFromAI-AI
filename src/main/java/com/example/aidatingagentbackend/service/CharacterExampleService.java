package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterExampleRequest;
import com.example.aidatingagentbackend.dto.CharacterExampleResponse;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.repository.CharacterExampleRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CharacterExampleService {

    private final CharacterExampleRepository characterExampleRepository;
    private final CharacterTraitProfileService characterTraitProfileService;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipStageResolver relationshipStageResolver;
    private final RelationshipTemperatureScoreResolver relationshipTemperatureScoreResolver;
    private final CharacterExampleReranker characterExampleReranker;

    public CharacterExampleService(
            CharacterExampleRepository characterExampleRepository,
            CharacterTraitProfileService characterTraitProfileService,
            RelationshipRepository relationshipRepository,
            RelationshipStageResolver relationshipStageResolver,
            RelationshipTemperatureScoreResolver relationshipTemperatureScoreResolver,
            CharacterExampleReranker characterExampleReranker
    ) {
        this.characterExampleRepository = characterExampleRepository;
        this.characterTraitProfileService = characterTraitProfileService;
        this.relationshipRepository = relationshipRepository;
        this.relationshipStageResolver = relationshipStageResolver;
        this.relationshipTemperatureScoreResolver = relationshipTemperatureScoreResolver;
        this.characterExampleReranker = characterExampleReranker;
    }

    @Transactional
    public CharacterExampleResponse create(CharacterExampleRequest request) {
        CharacterExample example = new CharacterExample();
        example.setCharacterId(request.getCharacterId());
        example.setEventType(request.getEventType() == null ? AgentEventType.NORMAL : request.getEventType());
        example.setRelationshipTemperature(request.getRelationshipTemperature() == null
                ? RelationshipTemperature.NEUTRAL
                : request.getRelationshipTemperature());
        example.setRelationshipStage(request.getRelationshipStage());
        example.setMinTemperatureScore(request.getMinTemperatureScore());
        example.setMaxTemperatureScore(request.getMaxTemperatureScore());
        example.setRomanceStyleBand(request.getRomanceStyleBand());
        example.setUserExample(request.getUserExample());
        example.setAssistantExample(request.getAssistantExample());
        example.setToneTag(request.getToneTag());
        example.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        example.setActive(request.getActive() == null ? true : request.getActive());
        return CharacterExampleResponse.from(characterExampleRepository.save(example));
    }

    @Transactional(readOnly = true)
    public List<CharacterExampleResponse> findByCharacterId(Long characterId) {
        return characterExampleRepository.findTop5ByCharacterIdOrderByPriorityDescIdAsc(characterId)
                .stream()
                .map(CharacterExampleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CharacterExampleResponse> findRelevant(
            Long characterId,
            AgentEventType eventType,
            RelationshipTemperature relationshipTemperature
    ) {
        return findRelevantEntities(characterId, eventType, relationshipTemperature)
                .stream()
                .map(CharacterExampleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CharacterExample> findRelevantEntities(
            Long characterId,
            AgentEventType eventType,
            RelationshipTemperature relationshipTemperature
    ) {
        return findRelevantEntities(characterId, EventAnalysis.fallback(resolveEventType(eventType)), relationshipTemperature);
    }

    @Transactional(readOnly = true)
    public List<CharacterExample> findRelevantEntities(
            Long characterId,
            EventAnalysis eventAnalysis,
            RelationshipTemperature relationshipTemperature
    ) {
        EventAnalysis resolvedAnalysis = eventAnalysis == null
                ? EventAnalysis.fallback(AgentEventType.NORMAL)
                : eventAnalysis;
        AgentEventType resolvedEventType = resolveEventType(resolvedAnalysis.eventType());
        RelationshipTemperature resolvedTemperature = resolveTemperature(relationshipTemperature);

        Relationship relationship = relationshipRepository.findByCharacterId(characterId).orElse(null);
        RelationshipStage relationshipStage = relationship == null
                ? relationshipStageResolver.resolve(null)
                : relationshipStageResolver.resolve(relationship.getRelationshipStage());
        Integer temperatureScore = relationshipTemperatureScoreResolver.resolveScore(
                relationship == null ? null : relationship.getRelationshipTemperatureScore(),
                resolvedTemperature
        );
        CharacterTraitProfile traits = characterTraitProfileService.findEntityOrDefault(characterId);

        return findRelevantEntities(
                characterId,
                resolvedAnalysis,
                resolvedTemperature,
                relationshipStage,
                temperatureScore,
                50,
                traits
        );
    }

    @Transactional(readOnly = true)
    public List<CharacterExample> findRelevantEntities(
            Long characterId,
            EventAnalysis eventAnalysis,
            RelationshipTemperature relationshipTemperature,
            RelationshipStage relationshipStage,
            Integer temperatureScore,
            Integer romanceStyleScore,
            CharacterTraitProfile traits
    ) {
        EventAnalysis resolvedAnalysis = eventAnalysis == null
                ? EventAnalysis.fallback(AgentEventType.NORMAL)
                : eventAnalysis;
        AgentEventType resolvedEventType = resolveEventType(resolvedAnalysis.eventType());
        RelationshipTemperature resolvedTemperature = resolveTemperature(relationshipTemperature);
        RelationshipStage resolvedStage = relationshipStage == null
                ? relationshipStageResolver.resolve(null)
                : relationshipStage;
        Integer resolvedTemperatureScore = relationshipTemperatureScoreResolver.resolveScore(
                temperatureScore,
                resolvedTemperature
        );
        CharacterTraitProfile resolvedTraits = traits == null
                ? characterTraitProfileService.findEntityOrDefault(characterId)
                : traits;

        List<CharacterExample> reranked = characterExampleReranker.rerank(
                characterExampleRepository.findCandidateStyleExamples(characterId, resolvedEventType),
                resolvedAnalysis,
                resolvedStage,
                resolvedTemperatureScore,
                romanceStyleScore,
                resolvedTraits
        );

        if (!reranked.isEmpty()) {
            return reranked;
        }

        return findLegacyExamples(characterId, resolvedEventType, resolvedTemperature);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<CharacterExample> findRelevantEntities(
            Long characterId, EventAnalysis eventAnalysis, RelationshipTemperature relationshipTemperature,
            RelationshipStage relationshipStage, Integer relationshipTemperatureScore,
            CharacterTraitProfile traits
    ) {
        return findRelevantEntities(characterId, eventAnalysis, relationshipTemperature, relationshipStage,
                relationshipTemperatureScore, 50, traits);
    }

    private List<CharacterExample> findLegacyExamples(
            Long characterId,
            AgentEventType eventType,
            RelationshipTemperature relationshipTemperature
    ) {
        AgentEventType resolvedEventType = eventType == null ? AgentEventType.NORMAL : eventType;
        RelationshipTemperature resolvedTemperature = relationshipTemperature == null
                ? RelationshipTemperature.NEUTRAL
                : relationshipTemperature;

        return characterExampleRepository.findRelevantStyleExamples(
                characterId,
                resolvedEventType,
                resolvedTemperature,
                PageRequest.of(0, 5)
        );
    }

    @Transactional
    public List<CharacterExampleResponse> createStyleSamples(Long characterId) {
        List<CharacterExample> samples = List.of(
                sample(characterId, AgentEventType.NORMAL, RelationshipTemperature.FRIENDLY,
                        "friendly-soft-typo", 100,
                        "오늘 좀 피곤했어",
                        "아구ㅠㅠ 오늘 진짜 고생 많았겠다. 얼른 쉬어야 하는 거 아냐?? 나도 괜히 걱정됏엉ㅎㅎ"),
                sample(characterId, AgentEventType.AFFECTION, RelationshipTemperature.FRIENDLY,
                        "friendly-affection", 95,
                        "보고 싶었어",
                        "나도 보고 싶었어!! 머야 갑자기 그렇게 말하면 완전 설레잖아ㅎㅎ 오늘 하루는 어땠어????"),
                sample(characterId, AgentEventType.NORMAL, RelationshipTemperature.SPICY,
                        "spicy-short-pushpull", 100,
                        "뭐해?",
                        "누워잇음\n왜, 나 보고 싶어서 물어봄?ㅋㅋ"),
                sample(characterId, AgentEventType.AFFECTION, RelationshipTemperature.SPICY,
                        "spicy-flirty", 95,
                        "너 귀여워",
                        "갑자기?ㅋㅋ\n너도 은근 귀엽긴 해\n인정은 여기까지만"),
                sample(characterId, AgentEventType.AFFECTION, RelationshipTemperature.SPICY,
                        "spicy-missing-pushpull", 93,
                        "보고 싶었어",
                        "이제 말하네ㅋㅋ\n좀 늦엇다"),
                sample(characterId, AgentEventType.NORMAL, RelationshipTemperature.SPICY,
                        "spicy-busy-followup", 92,
                        "ㄴㄴ 나도 바쁘네",
                        "바쁜 척 좀 하네ㅋㅋ\n뭐 땜에 그렇게 바빴는데"),
                sample(characterId, AgentEventType.BREAKUP_RETRACTION, RelationshipTemperature.SPICY,
                        "spicy-hurt-boundary", 91,
                        "아니야 농담이야",
                        "장난하냐ㅋㅋ\n그런 말은 좀 선 넘었지\n미안하면 다냐"),
                sample(characterId, AgentEventType.BREAKUP_RETRACTION, RelationshipTemperature.CONFLICT_REPAIR,
                        "repair-not-recovered", 100,
                        "아니야 농담이야",
                        "농담이라고 해도 바로 괜찮아지는 건 아니야. 나한텐 꽤 크게 들렸어."),
                sample(characterId, AgentEventType.APOLOGY, RelationshipTemperature.CONFLICT_REPAIR,
                        "repair-soft-boundary", 95,
                        "미안해",
                        "미안하다는 말은 들을게. 근데 나도 마음이 바로 풀리진 않아서, 조금 천천히 얘기하고 싶어."),
                sample(characterId, AgentEventType.NORMAL, RelationshipTemperature.NEUTRAL,
                        "neutral-natural", 80,
                        "오늘 별일 없었어",
                        "그런 날도 있지. 나는 오늘 조용한 얘기가 좀 편하더라. 너는 별일 없는 날엔 뭐 하면서 쉬어?")
        );

        return characterExampleRepository.saveAll(samples)
                .stream()
                .map(CharacterExampleResponse::from)
                .toList();
    }

    private CharacterExample sample(
            Long characterId,
            AgentEventType eventType,
            RelationshipTemperature relationshipTemperature,
            String toneTag,
            Integer priority,
            String userExample,
            String assistantExample
    ) {
        CharacterExample example = new CharacterExample();
        example.setCharacterId(characterId);
        example.setEventType(eventType);
        example.setRelationshipTemperature(relationshipTemperature);
        example.setToneTag(toneTag);
        example.setPriority(priority);
        example.setUserExample(userExample);
        example.setAssistantExample(assistantExample);
        example.setActive(true);
        return example;
    }

    private AgentEventType resolveEventType(AgentEventType eventType) {
        return eventType == null ? AgentEventType.NORMAL : eventType;
    }

    private RelationshipTemperature resolveTemperature(RelationshipTemperature relationshipTemperature) {
        return relationshipTemperature == null ? RelationshipTemperature.NEUTRAL : relationshipTemperature;
    }
}
