# RomanticAgent Character Behavior Model

## Current Architecture

The current AI-side pipeline keeps character behavior as a layered model:

1. Character core profile
2. Relationship settings
3. Agent self emotion state
4. Event and message signal analysis
5. Memory and style example retrieval
6. Prompt behavior policy
7. LLM generation
8. Post-processing and quality evaluation

The implementation is intentionally incremental. It does not mirror the future service backend ERD inside this project.

## Source Of Truth

| Domain | Source of truth | Notes |
| --- | --- | --- |
| Character core personality | `PersonalityKeyword`, `CharacterTraitProfile` | Keywords are user/config input. `CharacterTraitProfile` stores calculated final trait values. |
| Calculated traits | `CharacterTraitProfile` | Recalculated only when settings are saved. Chat requests reuse stored values. |
| Relationship stage | `Relationship.relationshipStage` | Defaults to `CRUSH` only in context when missing. |
| Relationship temperature | `Relationship.relationshipTemperatureScore` | 0-100 source of truth. Legacy enum is adapter input. |
| Current agent self emotion | `AgentSelfState` | Multidimensional self state used before response generation. |
| Representative emotion | `State.emotion` | Legacy/current representative state. |
| Relationship metrics | `Relationship` | Trust, closeness, conflict, repair, breakup risk. |
| Past facts | `Memory` | Actual extracted user/conversation facts. |
| Style examples | `CharacterExample` | Style reference only, never factual memory. |

## PersonalityKeyword To Trait Mapping

`PersonalityTraitResolver` maps keywords to a bounded `CharacterTrait`:

- `HUMOROUS`, `DAD_JOKE`, `QUIRKY`: raise humor.
- `PLAYFUL`, `TEASING`, `SMOOTH`: raise playfulness/confidence.
- `CUTE`, `EXPRESSIVE`, `NICKNAME_LOVER`, `COMPLIMENT_GIVER`: raise affection/expressiveness.
- `GOOD_LISTENER`: raises empathy and emotional stability.
- `HIGH_JEALOUSY`, `OPENLY_JEALOUS`, `POSSESSIVE`: raise jealousy and attachment.
- `CLINGY`, `FREQUENT_CONTACT_CHECKER`: raise attachment.
- `EASY_GOING`, `HOMEBODY`: raise emotional stability or low-pressure behavior.
- `SHY`, `TSUNDERE`: reduce direct expressiveness/confidence while preserving affection.

All final values are clamped to 0-10 and stored in `CharacterTraitProfile`.

## CharacterTrait Definition

Traits:

- `humor`
- `playfulness`
- `affection`
- `empathy`
- `attachment`
- `jealousy`
- `dominance`
- `confidence`
- `expressiveness`
- `emotionalStability`

Runtime components should use `CharacterTraitProfile` when present. If a future backend sends both `personalityKeywords` and final `characterTraits`, final `characterTraits` should win. Keywords can be used for consistency checks or fallback calculation.

## RelationshipStage

- `CRUSH`: allows interest, limits strong lover language, excessive nicknames, and possessive wording.
- `EARLY_DATING`: allows affection, flirting, nicknames, and call suggestions.
- `LONG_TERM`: favors daily care, schedule awareness, comfortable teasing, and less exaggerated excitement.

`RelationshipStageEmotionPolicy` also modifies emotion deltas:

- CRUSH can be slightly more sensitive to low-severity coldness.
- EARLY_DATING can amplify positive affection and severe breakup hurt.
- LONG_TERM reduces overreaction to low-severity delayed/cold replies.

## Relationship Temperature

`relationshipTemperatureScore` is 0-100:

- 0-20: calm, stable, short care, low flirting.
- 21-40: warm and gentle affection.
- 41-60: playful, light flirting.
- 61-80: active affection, stronger teasing, jealousy only when context supports it.
- 81-100: confident, leading, teasing, and direct style, without abuse or coercion.

Legacy `RelationshipTemperature` enum remains:

- `FRIENDLY`: legacy input/style compatibility, maps to score 35.
- `NEUTRAL`: legacy input/style compatibility, maps to score 50.
- `SPICY`: legacy input/style compatibility, maps to score 85.
- `CONFLICT_REPAIR`: situation strategy, maps to score 50 but is not a temperature band.

`CONFLICT_REPAIR` must remain a repair strategy and not become a score band.

## Emotion Modifier

`EmotionUpdateService` updates `AgentSelfState` before response generation. It calls:

- `EventAnalyzer` with rule fallback
- `MessageSignalDetector`
- `EmotionTraitModifier`
- `RelationshipStageEmotionPolicy`
- `AgentSelfStateLog`
- `ReflectionCandidateService`

Traits do not invent emotions. They only modify event deltas when relevant.

Examples:

- Attachment amplifies relationship-threat sensitivity.
- Jealousy applies only when the event analysis indicates jealousy/competition.
- Emotional stability reduces negative overreaction and improves decay/recovery.
- High-severity breakup is still preserved even in stable or long-term relationships.

## CharacterExample Retrieval

`CharacterExample` remains a style reference:

- Candidate query uses `characterId` and current `eventType`.
- Java reranking applies stage, temperature score, relevant traits, tone tag, priority, duplicate removal, and diversity.
- Fallback uses legacy enum-based search.
- Empty result is valid.

Prompt rules explicitly state that examples are not facts and must not be copied verbatim.

## Memory Retrieval

`MemoryRetrievalService` keeps the original scoring structure:

```text
score =
  importance * 0.5
  + cosineSimilarity * 70
  + emotionBonus
  + tokenOverlap
  - recentUsePenalty
  + traitBonus
  + stageBonus
```

Trait and stage are weak signals only:

- trait bonus max: 6.0
- stage bonus max: 2.0

Attachment, empathy, jealousy, and affection can lightly boost relevant memories. Jealousy bonus requires an actual jealousy/competition event. Memory is factual context; it is not style guidance.

## Prompt Behavior

`PromptBuilder` orders prompt sections as:

1. System rules
2. Character identity
3. Relationship context
4. RelationshipStage behavior
5. Temperature behavior
6. CharacterTrait behavior
7. AgentSelfState expression strategy
8. Topic/preference/initiative/life state
9. Conversation events
10. Memory
11. CharacterExample
12. Chat history
13. Current user message

`TraitInstructionResolver` converts final calculated traits into behavior instructions and resolves conflicts:

- affection high + expressiveness low: indirect affection
- jealousy high + emotional stability high: direct explanation without explosion
- playfulness high + empathy high: stop joking in serious concern contexts
- attachment high + long term: avoid overreacting to normal delays

## Response Style Post Processing

`ResponseStylePostProcessor` does not create new meaning. It only adjusts:

- punctuation
- informal endings
- question pileup
- laugh marker frequency
- overly strong pet names in CRUSH
- conflict repair wording

Existing SPICY behavior remains as compatibility behavior.

## Proactive Contact Policy

`ProactiveChatService` sends scheduled proactive messages to connected users, but `ProactiveContactPolicyService` prevents high-pressure proactive contact:

- high hurt blocks proactive contact unless the goal is repair
- high anger blocks proactive contact
- high temperature + high jealousy does not force proactive affection
- CRUSH + low temperature blocks high-pressure affection/past-event goals

The policy does not schedule real outbound jobs. Future actual delivery belongs to the service backend `outbound_schedule`.

## Current Chat Flow

```text
ChatController
-> ChatService
-> AIProcessingService
-> EmotionUpdateService
-> EventAnalyzer / EventDetector fallback
-> AgentSelfState update and log
-> ConversationEventService
-> ContextUpdater
-> AgentWorldStateService
-> AgentGoalService
-> ContextLoader
-> MemoryRetrievalService
-> CharacterExampleService
-> PromptBuilder
-> GeminiService
-> ResponseStylePostProcessor
-> ResponseQualityEvaluatorService when needed
-> ChatMessage save
-> Memory update
```

Streaming uses the same context and prompt path, then saves the accumulated answer after completion.

`ChatService` is now a transport facade. `AIProcessingService` owns the shared AI processing unit:

- `prepare(request, compactPrompt)` updates pre-response state, creates one `Context`, and builds one prompt.
- synchronous chat calls `process(request)`.
- streaming chat calls `prepare(...)`, streams with the prepared prompt, then calls `finishGeneratedReply(...)`.
- both paths reuse the same `Context` for post-processing, quality evaluation, preference persistence, and memory update.

## Performance Notes

Per normal message:

- Gemini response generation: 1 call
- EventAnalyzer: conditional hybrid call
- ResponseQualityEvaluator: conditional conflict/safety call
- Regeneration: only if evaluator score is low

Known repeated reads:

- `EmotionUpdateService`, `AgentWorldStateService`, `AgentGoalService`, `ContextUpdater`, and `ContextLoader` each read relationship/self state for their own transactional updates.
- Inside `ContextLoader`, resolved relationship, traits, stage, and temperature score are reused by memory/example retrieval and prompt construction.
- `CharacterTraitProfile` is not recalculated during chat if stored. It is recalculated only on settings save.

## Future Backend ERD Mapping

Recommended mapping:

- `member` -> user
- `character` -> character core info
- `character_tag.tag_code` -> `PersonalityKeyword`
- new `character_trait_profile` -> final `CharacterTrait`
- `relationship.relationship_stage` -> `RelationshipStage`
- `relationship.relationship_temperature_score` -> temperature score
- `relationship.affinity_score` -> relationship affinity
- `relationship.emotion` -> representative emotion
- new `relationship_emotion_state` -> `AgentSelfState`
- `emotion_log` -> emotion state changes
- `relationship_status` -> recent chat/call time and statistics
- `chat_room` / `chat_message` -> raw chat
- `call` / `call_history` / `call_reservation` -> raw call data
- `outbound_schedule` -> actual proactive contact schedule

This project should not directly read those service backend tables. The backend should pass an AI processing DTO.

## Backend To AI DTO

If both `personalityKeywords` and `characterTraits` are present:

1. use `characterTraits` as calculated truth
2. use `personalityKeywords` only for fallback or consistency checks
3. compare `calculationVersion` when available

## AI To Backend DTO

AI response should return:

- generated response
- event analysis
- emotion state before/after
- representative emotion
- relationship delta
- detected events
- memory update intent
- model metadata

Final persistence belongs to the service backend.

## Chat Sequence Target

```text
Client
-> Backend saves user chat_message
-> Backend calls AI server
-> AI server handles event/emotion/RAG/prompt/LLM
-> AI server returns result
-> Backend saves assistant chat_message
-> Backend updates relationship
-> Backend updates relationship_emotion_state
-> Backend stores emotion_log
-> Backend sends SSE to client
```

## Call Sequence Target

```text
Client WebSocket
-> Backend manages call/session
-> STT utterance
-> Backend calls shared AI processing service
-> AI response
-> TTS
-> WebSocket delivery
-> call summary
-> relationship/emotion updates
-> emotion_log
```

Chat and call should eventually share an AI processing service that accepts source metadata such as `sourceType`, `sourceMessageId`, and `sourceCallId`.

## Idempotency

Future requests should carry `requestId`.

Backend responsibility:

- prevent duplicate user/assistant message persistence
- prevent duplicate relationship/emotion log writes
- own final transaction boundaries

AI server responsibility:

- return deterministic metadata for a given processing request where possible
- avoid duplicate side-effect writes if it keeps local AI-only stores
- expose before/after emotion state so backend can apply idempotent updates

Do not add an `AI_PROCESSING_REQUEST` table in this project until ownership is decided.

## Responsibilities To Move To Backend

- user and assistant chat persistence
- relationship persistence
- emotion state persistence
- emotion log persistence
- outbound schedule execution
- call/session state
- idempotency request ledger

## Responsibilities Remaining In AI Server

- event and emotion analysis
- prompt construction
- memory retrieval over AI-provided or AI-owned vector context
- style example selection
- LLM generation
- response quality evaluation
- suggested state deltas

## Open Decisions

- exact DTO versioning and `calculationVersion` format
- whether backend or AI owns vector memory storage
- idempotency ledger location
- proactive contact throttling based on `relationship_status`
- shared chat/call AI processing API boundaries
