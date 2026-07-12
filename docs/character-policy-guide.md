# RomanticAgent 캐릭터 정책집

> 기준: 2026-07-12 현재 `src/main`과 `src/test`의 실제 구현. 이 문서에서 **의도**는 주석·프롬프트 문구·기존 설계 문서가 말하는 방향이고, **실제**는 런타임 호출 경로에서 실행되는 동작이다. 코드로 확인되지 않은 내용은 **확인 필요** 또는 미구현으로 표시한다.

## 1. 문서 목적과 한눈에 보는 결론

캐릭터는 하나의 클래스가 아니라 다음 파이프라인의 합성 결과다.

`Character/Keyword(고정 입력) → CharacterTraitProfile(계산·저장) → Relationship stage/score(관계 설정) → EventAnalysis/Signal → AgentSelfState(현재 내부 감정) → State/Relationship/World/Goal(맥락) → Memory·CharacterExample 검색 → PromptBuilder → Gemini → ResponseStylePostProcessor`

- 성격 키워드는 기본 5인 10개 trait에 delta를 누적하고 0~10으로 clamp한다. 저장 설정을 바꿀 때만 다시 계산한다.
- stage는 표현 허용 범위와 일부 감정 delta를 바꾼다. score는 5개 말투 band, 예시 선별, 프롬프트와 일부 후처리를 바꾼다.
- 현재 감정의 실제 원천은 주로 `AgentSelfState`; `State.emotion`은 별도 legacy 대표 감정이다.
- Memory는 과거 사실, CharacterExample은 말투 참고다. 프롬프트가 둘을 명시적으로 분리한다.
- Java 코드가 정책 객체를 병합하는 강제 우선순위 엔진은 없다. 프롬프트의 뒤 블록도 앞 블록을 구조적으로 override하지 않는다. 명시적 filter/replace와 LLM 지시 준수의 조합이다.
- 같은 입력도 identity, 저장 trait, 관계 상태, 감정, 검색 결과, history가 달라 서로 다른 응답이 된다.

## 2. 전체 캐릭터 형성 구조

아래의 “기본값”은 엔티티 필드 자체가 아니라 실제 생성/조회 fallback까지 포함한다.

| 요소 | 저장 위치 | 기본값 | 변경 시점·주체 | 지속성 | 응답 영향·관계 |
|---|---|---|---|---|---|
| Character identity (`name`, `mind`, `values`, `habit`, `responseStyle`) | `Character`, `characters` | 엔티티 기본 없음 | Character API의 생성/수정 | DB 영속 | Prompt에는 name, mind→Core(140자), responseStyle→Style(140자)만 들어간다. **values와 habit은 현재 PromptBuilder 미사용** |
| PersonalityKeyword | `CharacterTraitProfile.personalityKeywords`, element collection | 빈 set | trait profile/character settings 저장 시 사용자 요청 | DB 영속 | 직접 prompt에 노출되지 않고 trait 계산 입력으로만 사용 |
| CharacterTrait 10종 | `CharacterTraitProfile` | 각 5 | keyword 설정 저장 시 `PersonalityTraitResolver`와 `CharacterTraitProfileService` | DB 영속; profile이 없으면 비저장 default 객체 | 감정 modifier(일부), example/memory rerank(일부), trait prompt, 후처리(표현성만) |
| RelationshipStage | `Relationship.relationshipStage` 문자열 | `CRUSH`; 잘못된 값의 조회도 CRUSH fallback | relationship settings 저장/관계 API | DB 영속 | stage prompt, example hard filter, 감정 modifier, proactive policy |
| RelationshipTemperatureScore | `Relationship.relationshipTemperatureScore` | 정책 기본 50. 단, `ContextLoader.createDefaultRelationship()`은 score를 세팅하지 않아 resolver가 50 사용 | settings 저장 | DB 영속 또는 runtime fallback | 5 band prompt, example filter/rank, postprocess |
| AgentSelfState | `agent_self_states` | affection .55, trust .60, hurt/anger/disappointment 0, insecurity/distance .15, calm | 매 사용자 메시지 전 `EmotionUpdateService`; decay/event/signal | DB 영속, `@Version` | 현재 감정 표현 전략과 품질 평가, goal/world/proactive에 영향 |
| State | `states` | neutral, intensity 0, energy 50, stress 20 | `ContextUpdater.updateBeforeResponse`의 legacy `EmotionEngine` | DB 영속 | relationship context의 CurrentMood, Memory emotion bonus. AgentSelfState와 별도 source |
| Relationship 수치 | `relationships` | trust 50, closeness 30, conflict/repair/breakup 0, days 0 | `ContextUpdater`의 `RelationshipEngine`; settings는 stage/score | DB 영속 | prompt는 conflict/breakupRisk를 low/medium/high로만 사용; 다른 서비스의 goal/world 판단에도 사용 |
| AgentWorldState | `agent_world_states` | 쉬는 중/방/calm/energy55/stress25/loneliness30 등 | 매 응답 전 시간대·profile·관계·self state·reflection 기반 서비스 갱신 | DB 영속, 매 turn 재작성 | life 관련 질문 또는 goal 존재 시만 prompt에 일부 포함 |
| AgentGoal | `agent_goals` | 없음; 선택 시 CHECK_IN 등 | 매 응답 전 `selectCurrentGoal` | DB 영속 ACTIVE/완료 | Initiative와 proactive 정책에 영향. Prompt에 goal 자체 블록은 없고 Life State 포함 조건·initiative를 통해 간접 영향 |
| AgentInitiative | DTO record | 없음 | Context load 때 매번 `plan` | 비영속 | `[Turn Intent]`; direct answer 우선이라는 약한 지시 |
| AgentLifeEvent | `agent_life_events` | 서비스가 필요 시 생성 | `ensureAndFindForPrompt` | DB 영속 | 과거/자기 이야기 trigger 때 1건, 160자까지 prompt |
| ConversationEvent | `conversation_events` | 없음 | 매 메시지 전 detector가 중요 event를 저장 | DB 영속 | 최근 8개 조회, prompt에는 2개를 shared facts로 사용 |
| CharacterPreference | `character_preferences` | 없음 | preference 질문 계획 및 생성 응답 후 저장 | DB 영속 | 최대 2개 prompt; active preference plan이면 Memory 검색/삽입을 건너뜀 |
| Memory | `memories` | 없음 | 응답 후 `ContextUpdater.updateMemoryAfterResponse`; 검색 시 metadata 갱신 | DB 영속 | 사실 RAG. 검색 top 5지만 prompt는 1건만 사용 |
| CharacterExample | `character_examples` | 없음(별도 sample 생성 API 존재) | 관리 API/seed | DB 영속 | 스타일 RAG. event/stage/score/trait/tone rerank 후 최대 5개 prompt |

`AgentGoal`이 PromptBuilder 필드로 전달되지만 `[Agent Goal]` 블록은 없다. `AgentWorldState` 노출 조건과 `AgentInitiativeService` 계획에 간접 사용될 뿐이다. 이는 설계상 “목표가 prompt에 포함된다”는 표현과 실제 구현의 차이다.

## 3. 고정 성격과 가변 상태

| 요소 | 고정/반고정/가변 | 변경 조건 | 저장 위치 | 응답 영향 |
|---|---|---|---|---|
| Character identity | 반고정 | 관리 API 수정 | Character DB | identity/style |
| PersonalityKeyword | 반고정 | 설정 저장 | trait profile collection | trait 재계산 원천 |
| CharacterTrait | 반고정 | keyword 저장 때만 | trait profile DB | emotion/RAG/prompt |
| RelationshipStage | 반고정 | settings 저장 | Relationship DB | 허용 표현/filter/modifier |
| TemperatureScore | 반고정 | settings 저장 | Relationship DB | band/RAG/prompt/postprocess |
| AgentSelfState | 가변 | 매 사용자 turn+시간 decay | self state DB | 강한 현재 감정 근거 |
| State | 가변 | 매 turn legacy engine | State DB | 대표 mood/memory bonus |
| Relationship 수치 | 가변 | 매 turn relationship engine | Relationship DB | conflict/risk/goal |
| AgentWorldState | 가변 | 매 turn/시간대 | world state DB | 조건부 life staging |
| AgentGoal | 가변 | 매 turn 선택/교체 | goal DB | initiative/proactive 간접 영향 |
| AgentInitiative | 가변 | 매 context load | 비영속 DTO | turn intent |
| Memory | 누적 가변 | 응답 후 생성, 검색 때 usage 갱신 | Memory DB | 사실 context |
| CharacterExample | 반고정 | 관리/seed | Example DB | 말투 샘플 |

실제로 매 turn 자주 변하는 것은 AgentSelfState, State, Relationship 수치, WorldState, Goal/Initiative, retrieval metadata다. identity/keyword/trait/stage/score/example은 설정·관리 작업이 없으면 거의 변하지 않는다.

## Character Create policy (Trait rule V2)

`POST /api/characters`는 기존 서술 필드와 함께 `name`, `gender`, `age`, `job`, `spiceLevel`,
`mbti`, `speechStyle`, `relationshipStage`, `traits[{trait, priority}]`를 받는다. `preferTime`은 DTO와
생성 흐름 어디에도 존재하지 않으며 새로 추가하지 않았다. traits는 1~5개, keyword/priority 중복 금지,
null 금지, 1부터 개수까지 연속 priority만 허용한다. 입력 배열은 priority로 정렬한다.

Trait 기본값은 모두 3이고 범위는 0~10이다. priority 가중치는 1부터 순서대로
`1.0, 0.9, 0.8, 0.7, 0.6`이다. Trait별 양수와 음수 delta를 각각 절댓값 내림차순으로 정렬한 뒤
첫 항 100%, 둘째 75%, 셋째 이후 50%를 적용한다. 이후 MBTI 보정과 clamp를 수행한다.
정책의 단일 원천은 `PersonalityTraitResolver.KEYWORD_DELTAS`와 `PRIORITY_WEIGHTS`이다.

MBTI 보정은 E/I, N/S, T/F, J/P 축별 지정된 최대 ±1.0의 약한 보정이다. job, lifeType,
speechStyle, spiceLevel은 Trait 계산 입력이 아니며 HOMEBODY도 lifeType을 변경하지 않는다.

외부 `spiceLevel`은 내부 `Character.romanceStyleScore`로 저장되며 현재 친밀도나 감정이 아니라
연애 성향의 표현 강도를 뜻한다. band는 0~20 MILD, 21~40 SOFT, 41~60 BALANCED,
61~80 SPICY, 81~100 EXTRA_SPICY다. 기존 관계 엔진의 relationship temperature는 채팅 호환을
위해 그대로 두되 Character Create와 Trait 계산에서는 사용하지 않는다.

### Keyword → Trait 전체 표

| Keyword | Delta |
|---|---|
| HUMOROUS | Humor +7, Playfulness +3, EmotionalStability +1 |
| PLAYFUL | Playfulness +7, Humor +3, Confidence +2 |
| CUTE | Affection +7, Expressiveness +6, Dominance -3 |
| HIGH_JEALOUSY | Jealousy +8, Attachment +5, EmotionalStability -6, Confidence -1 |
| TALKATIVE | Expressiveness +8, Affection +2, Dominance +1 |
| DAD_JOKE | Humor +7, Confidence +3, Playfulness +2 |
| HOMEBODY | EmotionalStability +4, Affection +2 |
| TEASING | Playfulness +7, Dominance +4, Confidence +4, Empathy -2 |
| CLINGY | Attachment +8, Jealousy +5, EmotionalStability -7, Confidence -2 |
| TSUNDERE | Affection +5, Expressiveness -7, Confidence +3, Dominance +1 |
| EXPRESSIVE | Expressiveness +8, Affection +4, EmotionalStability -1 |
| NICKNAME_LOVER | Affection +6, Expressiveness +5 |
| POSSESSIVE | Attachment +7, Jealousy +7, Dominance +4, EmotionalStability -5, Empathy -2 |
| QUIRKY | Humor +5, Playfulness +5, Confidence +1 |
| EASY_GOING | EmotionalStability +8, Jealousy -6, Attachment -4, Dominance -2 |
| OPENLY_JEALOUS | Jealousy +7, Expressiveness +7, EmotionalStability -4 |
| SHY | Confidence -8, Expressiveness -6, Dominance -4, Affection +3, Empathy +1 |
| SMOOTH | Confidence +8, Humor +4, Playfulness +4, EmotionalStability +1 |
| FREQUENT_CONTACT_CHECKER | Attachment +7, Affection +3, EmotionalStability -3 |
| GOOD_LISTENER | Empathy +8, EmotionalStability +4, Dominance -2, Expressiveness -1 |
| COMPLIMENT_GIVER | Affection +5, Expressiveness +5, Empathy +3 |

예: SHY(priority 1)와 EXPRESSIVE(priority 2)는 Expressiveness에서 -6과 +7.2가 충돌한다.
priority를 뒤집으면 -5.4와 +8이 되어 결과도 달라진다. 선택과 priority는
`character_trait_selections`에 보존된다.

## 4. PersonalityKeyword → Trait 정책

모든 trait 기본값은 5다. 아래 delta를 중복 제거된 `LinkedHashSet` 순서로 합산한 뒤 각 trait를 0~10 clamp한다.

| PersonalityKeyword | 영향 Trait | Delta | 최종 clamp |
|---|---|---:|---|
| HUMOROUS | humor / playfulness | +5 / +2 | 0~10 |
| PLAYFUL | playfulness / humor / confidence | +5 / +2 / +1 | 0~10 |
| CUTE | affection / expressiveness | +5 / +4 | 0~10 |
| HIGH_JEALOUSY | jealousy / attachment / emotionalStability | +5 / +3 / -2 | 0~10 |
| TALKATIVE | expressiveness / affection | +5 / +1 | 0~10 |
| DAD_JOKE | humor / confidence | +4 / +2 | 0~10 |
| HOMEBODY | emotionalStability / affection | +2 / +1 | 0~10 |
| TEASING | playfulness / dominance / confidence | +4 / +2 / +2 | 0~10 |
| CLINGY | attachment / jealousy / emotionalStability | +5 / +2 / -3 | 0~10 |
| TSUNDERE | affection / expressiveness / confidence | +2 / -2 / +2 | 0~10 |
| EXPRESSIVE | expressiveness / affection | +5 / +3 | 0~10 |
| NICKNAME_LOVER | affection / expressiveness | +4 / +3 | 0~10 |
| POSSESSIVE | attachment / jealousy / dominance | +4 / +4 / +1 | 0~10 |
| QUIRKY | humor / playfulness | +3 / +3 | 0~10 |
| EASY_GOING | emotionalStability / jealousy / attachment | +5 / -2 / -1 | 0~10 |
| OPENLY_JEALOUS | jealousy / expressiveness | +4 / +4 | 0~10 |
| SHY | confidence / expressiveness / affection | -4 / -2 / +2 | 0~10 |
| SMOOTH | confidence / humor / playfulness | +5 / +2 / +2 | 0~10 |
| FREQUENT_CONTACT_CHECKER | attachment / affection | +4 / +2 | 0~10 |
| GOOD_LISTENER | empathy / emotionalStability | +5 / +2 | 0~10 |
| COMPLIMENT_GIVER | affection / expressiveness / empathy | +3 / +3 / +2 | 0~10 |

- null/empty set은 전부 5. set 안 null은 무시한다. 같은 enum은 set에서 중복 제거된다.
- `calculationVersion=1`, `calculatedAt=now`; DB에 keyword와 계산 trait를 함께 저장한다.
- 재계산은 `CharacterTraitProfileService.save/saveForCharacter`에서 설정 저장 시뿐이다. 채팅마다 저장 재계산하지 않는다. profile이 없으면 default profile을 메모리에서 만들지만 저장하지 않는다.
- delta 합산은 순서 독립적이며 마지막에만 clamp한다.

## 5. Trait별 실제 행동 정책

공통 범위는 0~10. Prompt high는 `>=8`, low는 `<=2`; 3~7에는 개별 trait 지시가 거의 없다. Emotion modifier는 기준 5에서 배율 1.0이며 `positive=1+(trait-5)/5×.3`(0.7~1.3), inverse는 반대(1.3~0.7)다.

### Humor

- 낮음/중간: 별도 금지·지시 없음. 높음: 가벼운 상황 농담 가능.
- Emotion/Memory: 영향 없음. Example: NORMAL 상황의 relevant trait이며 playful tag에서 high bonus(주 bonus의 0.6).
- Prompt: high 한 줄. PostProcessor: 없음. 실제 위치: `TraitInstructionResolver`, `CharacterExampleRelevantTraitPolicy`, `CharacterExampleToneTagPolicy`.

### Playfulness

- 높음: 상대 반응을 보며 장난. empathy도 높고 고민 키워드가 있으면 공감 우선.
- Emotion/Memory: 영향 없음. Example: NORMAL 기본 relevant trait, playful 계열 tone bonus.
- Prompt만 표현 정책. PostProcessor 없음. `TraitInstructionResolver`, example policy들.

### Affection

- 높음: 호감·애정 표현. expressiveness 낮음과 충돌 시 챙김/농담으로 간접 표현.
- Emotion: positive event/recovery의 affection delta에 0.7~1.3 배율. 부정 event를 상쇄하지 않는다.
- Memory: high(>=8)이고 애정 키워드 memory면 +1.5(약한 관련성 없으면 절반). Example: AFFECTION 상황과 warm tag.
- PostProcessor 없음. `EmotionTraitModifier`, retrieval, prompt/example policy.

### Empathy

- 높음: 고민에서 해결보다 감정 확인, playfulness와 충돌 시 공감 우선.
- Emotion: 직접 delta modifier 없음. Memory: 고민 계열 +1.5. Example: concern/갈등 relevant, warm/repair tag bonus.
- Prompt 표현 정책. PostProcessor 없음.

### Attachment

- 높음: 관계·연락 관심, 통제 금지. LONG_TERM이면 일상 지연 과잉 불안 금지.
- Emotion: breakup/retraction/cold/affection/return event에서 hurt·insecurity·distance 배율. recovery delta에도 이 배율이 먼저 적용될 수 있으나 recovery branch에서는 stability 배율로 대체되는 구조여서 attachment 배율이 최종 recovery에 보존되지 않는다.
- Memory: 약속/연락/답장/갈등/화해 +1.5. Example: jealousy/affection relevant.
- PostProcessor 없음.

### Jealousy

- 높음: 실제 경쟁 상대가 있을 때만 질투 가능. 높은 값만으로 감정을 발명하지 않도록 prompt와 example filter가 방어한다.
- Emotion: EventAnalysis의 emotion/summary에 질투·다른 사람·전 애인이 있을 때만 hurt/insecurity(positive), anger(mild) 배율.
- Memory: 실제 jealousy context와 관련 memory가 동시에 있어야 +1.5. Example: jealousy context에서 relevant; score 81+의 jealous tag는 근거 없으면 제외.
- PostProcessor 없음.

### Dominance

- 높음: 질문 반복 대신 먼저 제안/리드.
- Emotion/Memory: 영향 없음. Example: 전화/리드 context의 relevant trait, confident/spicy/dominant tag bonus×.7.
- Prompt 표현과 example 검색만 적용. PostProcessor 없음.

### Confidence

- 높음: 여유 있고 확신 있게 응답.
- Emotion/Memory: 영향 없음. Example: NORMAL 또는 전화/리드 관련, playful bonus×.6 및 spicy tag 주 bonus.
- PostProcessor 없음.

### Expressiveness

- 낮음: 돌려 말하고 큰 느낌표를 하나로 제한. 높음: 실제 감정을 직접 표현. hurt>=.6이면 각각 직접 서운함/말수 감소로 갈린다.
- Emotion/Memory: 내부 감정 수치를 바꾸지 않음. Example: affection/jealousy/conflict relevant tag bonus.
- PostProcessor: `<=2`일 때 `!!` 이상을 `!`로 축소. 이 trait만 직접 후처리에 연결된다.

### EmotionalStability

- 낮음: 상처 시 흔들릴 수 있으나 위협 금지. 높음: 작은 갈등 확대 금지.
- Emotion: 부정 event의 hurt/anger/insecurity/disappointment를 inverse 배율, recovery와 시간 decay를 positive 배율. breakup 자체는 제거하지 않는다.
- Example: conflict/cold/concern relevant. Memory direct bonus 없음. PostProcessor 없음.

정리하면 내부 감정값을 실제로 바꾸는 trait는 affection, attachment, jealousy, emotionalStability 네 개다. 나머지는 prompt/example 표현 선택이며 empathy만 Memory에도, expressiveness만 후처리에도 연결된다.

## 6. RelationshipStage 정책

| 정책 | CRUSH | EARLY_DATING | LONG_TERM | 실제 적용 |
|---|---|---|---|---|
| 애정/애칭 | 호감 가능; 확정 연인·과한 애칭/사랑 제한 | 보고 싶음·애칭·플러팅 허용 | 편안한 배려, 과장된 설렘 반복 억제 | prompt; CRUSH 후처리 `자기야/내 사랑→너` |
| 플러팅 | 약하게 | 자연스럽게 | 편안한 장난 | prompt만 |
| 질투 | possessive example hard 제외 | 사건 근거 정책은 공통 | 사건 근거 정책은 공통 | example filter+공통 prompt |
| 답장 지연 | low severity cold insecurity ×1.15 | 별도 없음 | ×0.75 | emotion policy |
| 갈등 | 별도 stage 문구 없음 | breakup hurt ×1.05 | 저강도 cold 완화 | emotion policy |
| 전화 제안 | 명시 허용 없음 | 명시 허용 | 별도 명시 없음 | prompt/example context; 실제 전화 연결 없음 |
| 선제 연락 | low score+고압 goal 차단 | 일반 policy | 일반 policy | proactive policy만; scheduler/outbound job 없음 |
| Example | strong possessive 제외 | stage 일치 | stage 일치 | hard filter, 일치 +12/무지정 +4 |
| positive affection | ×1.10 | ×1.15 | ×1.0 | emotion policy |

전화 제안은 텍스트 행동일 뿐 call/WebSocket/TTS 연동은 없다. 애칭 빈도, 지연 시간 자체, stage별 문장 길이의 deterministic 규칙도 없다. 이것들은 의도 또는 LLM 지시이지 강제 구현이 아니다.

## 7. RelationshipTemperatureScore 정책

| Score | Prompt 말투/행동 | Example 우선 tag | PostProcessor |
|---|---|---|---|
| 0~20 CALM | 차분·안정, 짧은 배려, 과한 flirt 제한 | calm/stable/considerate/neutral/soft +8 | `ㅋㅋ` 묶음 최대 1회 |
| 21~40 FRIENDLY_AFFECTION | 다정·부드러운 애정 | warm/gentle/friendly/soft/affection +8 | score 전용 없음 |
| 41~60 PLAYFUL_FLIRTING | 자연스러운 장난·flirt, 집착 금지 | playful/flirty/teasing/pushpull/spicy +7 | 없음 |
| 61~80 ACTIVE_AFFECTION_JEALOUSY | 적극 애정·강한 장난, 근거 있을 때만 직접 질투 | expressive/teasing/affection/spicy/flirty +8; 질투 맥락 jealous +6 | 없음 |
| 81~100 SPICY_LEADING | 짧고 자신감, 도발·밀당·리드; hurt 높으면 쉽게 풀리지 않음 | confident/provocative/dominant/spicy/pushpull +10; 근거 있는 jealous +6 | 질문 줄 종결 `?` 1개 초과 제거, 마침표 축소, 일부 존댓말 완화 |

애칭, 반말, 축약어, 오타, `ㅎㅎ` 빈도는 score band가 직접 강제하지 않는다. 문장 길이도 81+ prompt의 “짧게”뿐이다. 쉽게 풀리는 정도는 81+ prompt와 AgentSelfState 정책에 의존한다.

legacy enum 호환: FRIENDLY→35, NEUTRAL→50, SPICY→85, CONFLICT_REPAIR→50. score가 있으면 항상 우선한다. 역변환은 0~40 FRIENDLY, 41~60 NEUTRAL, 61~100 SPICY다. `CONFLICT_REPAIR`는 band가 아니라 request가 넘기는 별도 상황 전략이며 후처리에서만 특정 문구 replace를 한다. 그러나 Context의 score가 저장되어 있으면 enum은 score 결정에 쓰이지 않고, Initiative와 legacy postprocess 분기에 남아 두 source가 동시에 작동할 수 있다.

## 8. 감정 형성 정책

실제 순서:

`ChatService → AIProcessingService.prepare → EmotionUpdateService.updateBeforeResponse(낙관적 잠금 최대 2회 재시도) → state/trait/stage load → 시간 decay → EventAnalyzer(실패 시 EventDetector) → MessageSignalDetector → base event delta → EmotionTraitModifier → RelationshipStageEmotionPolicy → side effect/floor → signal transition에도 trait/stage modifier → 0~1 clamp → AgentSelfStateLog → ReflectionCandidate → saveAndFlush`

그 뒤 별도로 `ConversationEventService`, `ContextUpdater(legacy State/Relationship)`, WorldState, Goal, Context/RAG가 실행된다.

| EventType | base delta (affection, trust, hurt, anger, insecurity, disappointment, distance) | Trait/Stage | 예외 |
|---|---|---|---|
| BREAKUP_DECLARATION | 0,-.30,+.70,+.35,+.60,+.45,+.40 | attachment/stability; EARLY hurt×1.05 | lastEmotion hurt, breakup event 기록; 안정성이 높아도 사라지지 않음 |
| BREAKUP_RETRACTION | 0,0,-.10,-.05,-.10,0,0 | attachment relevant이나 recovery branch는 stability recovery 적용 | 직전 breakup이면 hurt>=.55, anger>=.25, distance>=.35 floor |
| APOLOGY | 0,+.05,-.20,-.15,-.08,-.10,0 | stability recovery | 기존 hurt>=.45면 hurt>=.35, distance>=.25 floor |
| AFFECTION | +.08,+.03,0,0,-.04,0,-.04 | affection/attachment; CRUSH ×1.10, EARLY ×1.15 | hurt>=.5이면 conflicted |
| INSULT | 0,-.15,+.35,+.30,0,+.25,+.20 | stability inverse | upset |
| IGNORE_OR_COLD | 0,0,0,0,+.20,+.15,+.15 | attachment/stability; low severity CRUSH insecurity×1.15, LONG_TERM×.75 | distant |
| NORMAL | 0 | signal transition만 가능 | threshold로 lastEmotion 재평가 |

- decay: 경과 시간당 `.01×stabilityModifier`, 최대 .25. hurt/anger 전량, insecurity×.8, disappointment×.7, distance×.4 감소. affection/trust는 decay 없음.
- recovery: 음수 부정감정 delta에 stability positive modifier. affection positive는 positive event일 때만.
- severity는 stage의 “low severity cold(<.6)” 판정에만 쓰인다. base delta scaling에는 쓰이지 않는다.
- sincerity, `isJoke`, `isManipulative`는 EventAnalyzer가 만들고 log reason에 기록하지만 **delta 계산에 사용하지 않는다**. 따라서 joke/manipulation별 예외는 현재 미구현이다.
- trait=5이면 모든 trait 배율이 1.0이라 기존 delta 유지. stage 배율과 side-effect floor는 별도로 적용된다.

## 9. 감정과 표현 분리 사례

| 사례 | 내부 상태 | Prompt / Example | 후처리 | 최종 경향 |
|---|---|---|---|---|
| hurt 높음+expressiveness 높음 | expressiveness는 hurt를 안 바꿈 | 직접 서운함; conflict relevant examples | 별도 없음 | 직접적이되 즉시 용서 금지 |
| hurt 높음+expressiveness 낮음 | 동일 | 말수 감소/우회 | `!!→!` | 간접·짧은 서운함 |
| jealousy 높음+event 없음 | 변화 없음 | 근거 없이 질투 금지; 81+ jealous example 제외 | 없음 | 질투 발명 억제 |
| jealousy 높음+event 있음 | hurt/insecurity 최대1.3, anger 최대1.15 배율 | jealous examples/직접 표현 가능 | 없음 | 더 민감한 질투 |
| stability 높음+breakup | negative delta 최대 .7배이나 큰 base 유지 | 작은 갈등 확대 금지 | 없음 | 차분하지만 상처와 floor 유지 |
| affection 높음+부정 event | 부정 delta를 상쇄하지 않음 | 애정 성향 지시는 있을 수 있으나 self-state 우선 문구 | 없음 | 애정 많은 캐릭터도 상처 표현 |
| playfulness 높음+사용자 고민 | 내부 변화 없음(별 signal 가능) | empathy도 높을 때만 명시적 공감 우선; example relevant traits는 concern이면 empathy/stability | 없음 | empathy가 8 미만이면 장난 억제가 보장되지 않음 |
| attachment 높음+LONG_TERM | cold 관련 내부 민감도와 stage .75가 곱해짐 | 일상 지연 과잉 불안 금지 | 없음 | attachment가 매우 높으면 완전 상쇄되지 않을 수 있음 |

## 10. CharacterExample 정책

후보는 repository의 `findCandidateStyleExamples(characterId,eventType)`에서 가져온다(정확한 JPQL은 `CharacterExampleRepository` 참고). active, stage 일치/무지정, min/max score를 hard filter한다. CRUSH의 strong possessive text/tag, score>=81이면서 jealousy 근거가 없는 jealous tag도 제외한다.

점수는 `priority×0.4 + event(일치30/null8) + stage(일치12/무지정4) + trait/tone/temperature score`. high trait bonus는 `max(0,trait-5)×2`이고 relevant trait에만 준다. assistant text 정규화로 중복 제거하고 tone family diversity를 적용하여 top 5. 결과가 비면 legacy `(character,event,enum)` query top 5로 fallback한다. 이 fallback은 새 stage/score hard filter를 우회할 가능성이 있다.

| 상황 | 우선 Trait | 우선 toneTag | 제외 조건 |
|---|---|---|---|
| 질투 | jealousy, attachment, expressiveness | jealous | score81+에서 질투 근거 없으면 제외; CRUSH strong possessive 제외 |
| 고민 | empathy, stability | warm/soft/repair 계열 | stage/score 불일치 |
| 장난/일상 | humor, playfulness, confidence | playful/teasing/flirty | stage/score 불일치 |
| 애정 | affection, attachment, expressiveness | affection/warm | CRUSH possessive 제외 |
| 갈등 | stability, expressiveness, empathy | repair/hurt/boundary | stage/score 불일치 |
| 전화 제안 | dominance, confidence, affection | confident/dominant | context에 전화/call/lead 문자열 필요 |

Prompt는 “style only, 현재 사실로 취급 금지, 문장 그대로 복사 금지”를 명시한다. 다만 LLM 준수에 의존하며 구조적 사실 격리는 아니다.

## 11. Memory RAG 정책

```text
score = importance*0.5
      + cosine(queryEmbedding,memoryEmbedding)*70
      + (summary가 State.emotion 포함 ? 8 : 0)
      + 공통 token 개수*1
      - [최근사용 penalty + min(8,retrievalCount*.8)]
      + min(6, traitBonus)
      + max 2 stageBonus
```

최근사용 penalty는 10분 미만 18, 30분 미만 10, 120분 미만 4, 이후 0. Trait bonus는 attachment/empathy/jealousy/affection 각 조건당 1.5이며 token 약한 관련성이 전혀 없으면 총 bonus가 절반이다. LONG_TERM이고 importance>=7, 생성 후 14일 이상이면 +2. 오래됐다는 이유로 감점하지 않는다. importance는 선형 0.5배이며 엔티티 차원의 범위 검증은 없다.

전체 character memory를 정렬해 top 5를 반환하고 그 5개의 `lastRetrievedAt`, `retrievalCount`를 즉시 저장한다. 그러나 prompt에는 첫 1개만 들어가므로 나머지 4개도 사용된 것으로 계수되는 불일치가 있다. embedding이 없으면 검색 중 생성·저장한다. 검색 실패를 잡는 fallback은 없다. characterId null이면 모든 memory 조회가 가능하지만 정상 chat은 id 필수다.

Trait 최대 6, stage 최대 2라 cosine 최대 70보다 작지만 semantic 점수 차가 작으면 순위를 뒤집을 수 있다. Memory는 사실, Example은 말투다. active preference 질문이면 Memory retrieval 자체를 건너뛴다.

## 12. PromptBuilder 정책

실제 `build()` 순서와 제한:

| 순서 | 블록 | 포함 조건·제한 | 충돌 처리 |
|---:|---|---|---|
| 1 | system rules | 항상 | answer first, 질문 최대1, memory 직접 관련, 경계·위협 금지 |
| 2 | Character | 존재 시; mind/style 140자 | values/habit 제외 |
| 3 | Relationship Context | 항상 header; mood/stage/band/conflict/risk/event | dialogue content로 쓰지 말라는 지시 |
| 4 | Stage | 항상 | 표현 허용/제한 |
| 5 | Temperature | 항상 | message와 실제 감정 우선 명시 |
| 6 | Trait | resolver 결과 최대 8 | resolver가 일부 conflict 문구 추가 |
| 7 | AgentSelfState | 존재 시 | hurt/anger/insecurity guard |
| 8 | Topic | plan 존재 | topic 유지 |
| 9 | Preference | active plan 또는 저장 preference; preference 2개 | active면 memory 제외 |
| 10 | Initiative | 존재 시 | direct answer 우선, lightly 사용 |
| 11 | Life/World | life 질문 또는 goal 존재; event 1개 | 실제 물리 사실로 단정 금지 |
| 12 | ConversationEvent | 최대2, 각120자 | shared facts |
| 13 | Memory | 최대1, 140자 | 직접 관련일 때만 |
| 14 | CharacterExample | 최대5; U80/A120 | style only/no verbatim |
| 15 | Chat History | 일반6, streaming compact4; 각160자 | repository 최신순을 그대로 받아 역순 여부 **확인 필요** |
| 16 | current user | 전체 | 마지막 블록 |

프롬프트 최대 총 길이 제한은 없다. 각 블록 절단만 있다. AgentGoal 전용 블록은 없으며 AgentWorldState도 조건부다.

축약 예시:

```text
You are the user's romantic chat partner...
Rules: answer first; max one follow-up question; ... no threats/coercion.

[Character]
Name=하린 Core=낯을 가리지만 상대를 세심하게 살핀다 Style=짧고 자연스러운 메신저 말투

[Relationship Context]
CurrentMood=neutral Stage=CRUSH TemperatureBand=warm Conflict=low BreakupRisk=low
Use relationship context as policy, not as dialogue content.

[Relationship Stage Behavior]
- 호감 표현은 가능하지만 확정적인 연인처럼 말하지 않는다.
...
[Character Trait Behavior]
- 사용자가 힘든 이야기를 하면 해결보다 감정 확인을 우선한다.
...
[Optional Memory]
- 사용자는 월요일 발표를 걱정하고 있다.
Use only if directly relevant...
[Style Examples]
U: 오늘 힘들었어
A: 많이 힘들었겠다. 무슨 일 있었어?
Examples are style references only...
[Recent Chat]
USER: 오늘 발표 끝났어
[User Message]
생각보다 잘한 것 같아
```

## 13. 정책 우선순위와 충돌

코드상 강제력 기준 우선순위는 다음이 더 정확하다.

1. Java hard filter/validation/clamp와 후처리 replace
2. system 첫 규칙 및 재생성 quality rules
3. 현재 user message와 EventAnalysis에서 갱신된 AgentSelfState
4. stage hard filter/지시
5. temperature score hard filter/지시
6. trait conflict/self-state 지시
7. relationship/world/goal/initiative context
8. Memory 사실
9. CharacterExample 스타일
10. history/identity 일반 말투

단, 2~10은 LLM 텍스트 지시여서 실제 parser 기반 override 우선순위가 아니다.

- Playfulness vs 고민: empathy도 high이고 고민 keyword가 있을 때 공감 지시가 추가된다. empathy가 낮으면 미해결.
- Jealousy vs 근거 없음: prompt 금지+81 score example filter. 80 이하 jealous example은 hard 금지가 없어 부분적이다.
- 높은 score vs CRUSH: CRUSH possessive example filter와 애칭 replace가 우선하지만, spicy 일반 도발은 허용될 수 있다.
- Affection vs hurt: self-state 즉시 용서 금지가 명시되며 affection은 부정 delta를 낮추지 않는다.
- Attachment vs LONG_TERM: prompt와 stage emotion .75가 과잉 반응을 완화하되 attachment 배율과 곱해진다.
- Confidence vs 낮은 expressiveness: 직접 conflict rule이 없어 “확신 있게”와 “돌려 말하기”가 병존한다.
- Example vs 현재 감정: prompt상 현재 감정 우선, 구조적 보장은 quality evaluator/재생성에 한정.
- Memory vs Example: 사실/스타일 역할을 명시적으로 나누지만 LLM 입력 문자열에는 함께 존재한다.

## 14. ResponseStylePostProcessor 정책

항상 줄 끝의 일부 마침표를 제거한다. legacy SPICY면 전역 문자열 replace로 `습니다→''`, `해요→해`, `이에요/예요→임`, 특정 문장을 축약한다. CONFLICT_REPAIR는 `괜찮아, 다행이야→말은 들을게`, `고마워.→고마워`. score 81+는 질문 pileup, 마침표, 일부 존댓말을 줄이고, 0~20은 `ㅋㅋ` 묶음을 최대 1개로 줄인다. expressiveness<=2는 복수 느낌표 축소, CRUSH는 `자기야/내 사랑→너`, anger<.3은 `짜증나→좀 그렇네`다.

마침표·웃음·느낌표·질문부호 축소는 주로 의미 보존이다. 반면 `습니다`를 빈 문자열로 제거하는 전역 replace, `그렇구나→그래?`, `괜찮았어?→괜찮았냐`, conflict 문구 및 `짜증나` replace는 태도·화행을 바꿀 수 있어 위험하다. 형태소/단어 경계를 보지 않아 단어 내부도 치환할 수 있다. 애칭을 새로 만들거나 오타/ㅎㅎ/반말을 score별로 생성하는 규칙은 없다.

## 15. 캐릭터 형성 시나리오

### Scenario A — CRUSH, 30, SHY+GOOD_LISTENER+COMPLIMENT_GIVER

1. 최종 trait: humor5, playfulness5, affection10, empathy10, attachment5, jealousy5, dominance5, confidence1, expressiveness6, stability7.
2. CRUSH: 확정 연인/과한 애칭·소유 제한.
3. score30: 다정하고 부드러운 애정.
4. 감정: affection high라 positive affection delta 최대1.3; stability 7이 부정 반응을 .88배·recovery/decay 1.12배. empathy는 내부 delta를 안 바꿈.
5. Example: 고민/애정에서 empathy/affection tag 우대; CRUSH possessive 제외.
6. Memory: empathy·affection 관련 +1.5씩 가능.
7. Prompt: 애정, 공감 지시; expressiveness 6이라 “수줍게”라는 직접 지시는 없음.
8. 응답 경향: 부드럽고 잘 듣고 칭찬하지만 confidence=1 자체의 low instruction이 없어서 예상보다 덜 수줍을 수 있음.
9. proactive: CRUSH+30은 `EXPRESS_AFFECTION` 또는 `ASK_ABOUT_PAST_EVENT` goal이면 차단된다. hurt/anger가 각각 .65 이상이어도 정책 조건에 따라 차단된다.
10. keyword SHY의 낮은 confidence가 prompt에서 직접 소비되지 않는 것이 기대 차이.

### Scenario B — EARLY_DATING, 65, PLAYFUL+TEASING+EXPRESSIVE

1. trait: humor7, playfulness10, affection8, empathy5, attachment5, jealousy5, dominance7, confidence8, expressiveness10, stability5.
2. 애칭/flirt/전화 제안 허용.
3. 적극 애정·강한 장난, 근거 있을 때 질투.
4. positive affection은 trait1.18×stage1.15. playfulness/expressiveness는 내부 감정 미변경.
5. playful/teasing/flirty 및 affection examples 우대.
6. affection memory만 조건부 bonus; playfulness memory bonus 없음.
7. playful, affection, confidence, expressiveness 지시.
8. 직접적이고 장난스러운 연애 초반 말투.
9. proactive는 goal/self-state gate 통과 시 가능하나 실제 정기 scheduler는 없음.
10. dominance=7은 high 기준 미달이라 리드 trait 지시 없음.

### Scenario C — EARLY_DATING, 90, HIGH_JEALOUSY+OPENLY_JEALOUS+SMOOTH+POSSESSIVE

1. trait: humor7, playfulness7, affection5, empathy5, attachment10, jealousy10, dominance6, confidence10, expressiveness9, stability3.
2. early dating 애정/flirt 허용; CRUSH 제한 없음.
3. spicy-leading, 짧고 자신감·도발·리드. 질투는 실제 사건 근거 필요.
4. jealousy event면 hurt/insecurity×1.3, anger×1.15; attachment relevant도 ×1.3, stability3은 부정×1.12. 곱연산으로 민감도가 커짐.
5. jealousy context가 있을 때 jealous tone 강한 우대; 없으면 score81+ jealous tag hard 제외.
6. attachment memory +1.5; jealousy memory는 사건이 있을 때만 +1.5.
7. attachment/jealousy/confidence/expressiveness 지시와 근거 없는 질투 금지.
8. 사건이 있으면 직접적이고 자신감 있는 질투, 없으면 일반 spicy-leading.
9. high jealousy/high score만으로 proactive affection을 강제하지 않는다.
10. POSSESSIVE delta가 있어도 dominance6은 high 지시가 없다. 낮은 stability3도 low(<=2) 문구가 없다.

### Scenario D — LONG_TERM, 20, EASY_GOING+GOOD_LISTENER+HOMEBODY

1. trait: humor5, playfulness5, affection6, empathy10, attachment4, jealousy3, dominance5, confidence5, expressiveness5, stability10.
2. 일상 관심·현실 배려·편안한 장난, 과장 설렘 반복 억제.
3. calm: 차분, 짧은 배려, 낮은 flirt; laugh 1회.
4. negative delta×.7, recovery/decay×1.3; low severity cold insecurity에 추가×.75.
5. 고민에서 empathy/stable, calm/soft tag 우대.
6. empathy memory +1.5; LONG_TERM 중요도7+·14일+ memory +2.
7. empathy/stability 및 stage/calm 지시.
8. 안정적이고 짧은 생활형 배려.
9. proactive는 낮은 score 자체보다 goal/hurt/anger/stage policy에 따름.
10. HOMEBODY는 WorldState 생활 유형을 직접 결정하지 않고 trait 두 개만 바꾼다.

## 16. 구현 현황 점검

아래 집계 기준으로 IMPLEMENTED 8, PARTIALLY_IMPLEMENTED 4, 미구현/선언/legacy 4다.

| 정책 | 상태 | 실제 코드 위치 | 문제점 | 권장 조치 |
|---|---|---|---|---|
| Keyword→Trait | IMPLEMENTED | `PersonalityTraitResolver` | 계수 하드코딩 | 정책 테이블 단일화 |
| Trait 저장 | IMPLEMENTED | `CharacterTraitProfileService` | 없는 profile default는 비저장 | 의도 명시 |
| Stage 저장 | IMPLEMENTED | Relationship/settings service·resolver | 문자열 저장, invalid 조회 silent CRUSH | enum column/validation 통일 |
| Temperature 저장 | IMPLEMENTED | Relationship settings·score resolver | legacy enum 병존 | score를 단일 source로 축소 |
| Emotion modifier | PARTIALLY_IMPLEMENTED | `EmotionTraitModifier`, stage policy | severity/sincerity/joke/manipulation 미적용 | 분석 필드 연결 또는 제거 |
| CharacterExample reranking | IMPLEMENTED | Service/Reranker/ToneTagPolicy | legacy fallback이 새 filter 우회 가능 | fallback에도 동일 guard |
| Memory bonus | IMPLEMENTED | `MemoryRetrievalService` | top5 usage 갱신, prompt1 | 실제 삽입 건만 mark |
| Prompt instruction | IMPLEMENTED | `PromptBuilder`, trait resolver | 총 길이 cap·강제 merge 없음 | token budget/precedence 명문화 |
| Trait conflict resolution | PARTIALLY_IMPLEMENTED | `TraitInstructionResolver` | 네 조합만 처리 | 조합 테스트 확대 |
| PostProcessor | PARTIALLY_IMPLEMENTED | `ResponseStylePostProcessor` | 의미 변경 replace 위험 | token-aware 최소 변환 |
| Proactive Contact | PARTIALLY_IMPLEMENTED | proactive services/policy | SSE 연결 사용자 대상, 실제 schedule/outbound 없음 | backend scheduler 연동 |
| 전화 연동 | NOT_IMPLEMENTED | prompt/example의 “전화” 텍스트뿐 | call session/STT/TTS 없음 | 별도 shared AI DTO 설계 |
| Backend↔AI DTO | DECLARED_BUT_UNUSED | 기존 설계 문서; `ChatRequest`는 로컬 DTO | 분리 backend 계약 없음 | versioned DTO 구현 |
| optimistic locking | IMPLEMENTED | `AgentSelfState.@Version`, EmotionUpdate retry | self state만 보호 | 관계/state ownership 검토 |
| requestId idempotency | NOT_IMPLEMENTED | 없음 | 중복 요청 시 side effect 중복 | backend request ledger |
| legacy RelationshipTemperature | LEGACY_ONLY | enum/resolver/postprocessor/initiative | score와 병존 | CONFLICT_REPAIR만 별도 strategy로 분리 |

## 17. 정책상 문제점과 충돌

### HIGH

1. `severity/sincerity/isJoke/isManipulative`를 정교하게 분석하지만 감정 delta 크기/예외에 거의 연결하지 않는다. joke breakup도 event type에 따라 큰 고정 delta가 가능하다.
2. `State`/`RelationshipEngine`과 `AgentSelfState`/EventAnalysis가 병렬 감정 source다. prompt의 CurrentMood와 self emotion이 충돌할 수 있다.
3. legacy enum과 score가 동시에 Initiative, example fallback, postprocessor에 관여한다. 요청 enum SPICY와 저장 score 20 같은 충돌이 가능하다.
4. postprocessor의 전역 deterministic replace가 존댓말 형태·질문·갈등 태도의 의미를 바꿀 수 있다.
5. requestId idempotency가 없어 재시도 시 self-state/log/event/chat/memory side effect가 중복될 수 있다.

### MEDIUM

- Example legacy fallback이 새 stage/score/jealousy filter를 우회할 수 있다.
- Memory top5 모두 retrievalCount를 올리지만 prompt는 1개만 사용한다.
- trait/stage/temperature 계수와 threshold가 resolver, prompt, reranker, postprocessor에 분산됐다.
- Character identity의 values/habit, AgentGoal 자체가 prompt에 직접 연결되지 않는다.
- Prompt 전체 token cap이 없고 history 최신순을 대화 순서로 반전하는 코드가 보이지 않는다.
- trait는 매 요청 재계산되지는 않지만 여러 서비스가 relationship/self/profile을 반복 조회한다.
- high/low 기준이 대체로 8/2라 중간값 변화가 감정 modifier 외 표현에 거의 안 보인다.

### LOW

- Temperature가 trait 저장값 자체를 변경하지는 않는다. 이는 문제 목록에서 **발견되지 않음**.
- Trait가 질투를 발명하지 않도록 여러 guard가 있으나 score<=80 example에는 hard guard가 약하다.
- CharacterExample을 사실처럼, Memory를 말투처럼 쓰지 말라는 prompt guard는 있으나 LLM 의존이다.
- `relationshipTemperatureScore`가 default Relationship entity 생성 시 null로 남고 resolver가 50을 보충한다.

## 18. 최종 정책 요약

### 캐릭터를 결정하는 것

identity와 저장된 10개 trait가 장기 성격을 결정한다. keyword는 직접 행동하지 않고 trait로 변환된다.

### 관계별로 달라지는 것

stage는 허용 범위·저강도 감정 민감도, score는 말투 band·example·일부 후처리를 바꾼다.

### 매 대화마다 달라지는 것

EventAnalysis, AgentSelfState, legacy State/Relationship 수치, world/goal/initiative, Memory/Example 검색 결과와 history다.

### 응답 생성에서 가장 우선하는 것

강제로는 validation/filter/clamp/postprocess, 의미 정책으로는 안전 규칙과 현재 user event/self-state다.

### 현재 구현된 핵심 차별점

고정 trait와 현재 감정을 분리하고, Memory 사실과 Example 스타일을 서로 다른 검색·prompt 블록으로 다룬다.

### 아직 약한 부분

분석 필드 미연결, 이중 감정 source, enum/score 이중 source, 의미 변경 후처리, idempotency/전화 연동 부재다.

### 다음 개선 우선순위

새 기능 제안 차원의 우선순위는 (1) event 분석 필드와 delta 일치, (2) 감정 source 단일화, (3) score/strategy 분리, (4) idempotency, (5) 안전한 후처리다. 본 작업에서는 구현하지 않았다.

## 19. 검증 결과와 근거 파일

- 실제 존재 확인: `Character`, `PersonalityKeyword`, `CharacterTrait/Profile`, `RelationshipStage/Temperature/Band`, `AgentSelfState`, `State`, `Relationship`, `AgentWorldState`, `AgentGoal/LifeEvent`, `ConversationEvent`, `CharacterPreference`, `Memory`, `CharacterExample`.
- 실제 메서드 확인: `PersonalityTraitResolver.resolve`, `EmotionUpdateService.updateBeforeResponse`, `MemoryRetrievalService.retrieve`, `CharacterExampleReranker.rerank`, `PromptBuilder.Builder.build`, `ResponseStylePostProcessor.process`, `AIProcessingService.prepare/process`.
- enum 확인: keyword 21개, event 7개, stage 3개, legacy temperature 4개, score band 5개.
- 수치 확인: trait 0~10/default5, self-state 0~1, score 0~100, keyword delta, emotion base delta, memory 점수·penalty, example 점수와 top K.
- 테스트 근거: `PersonalityTraitResolverTests`, `CharacterTraitProfileServiceTests`, `EmotionTraitModifierPolicyTests`, `EmotionUpdateServiceTests/IntegrationStyleTests`, `MemoryRetrievalServiceTraitTests`, `CharacterExampleServiceTests/RerankerTests`, `PromptBuilderTraitInstructionTests`, `ResponseStylePostProcessorTests`, `ProactiveContactPolicyServiceTests`, `RelationshipSettingsServiceTests`.
- **확인 필요**: `ChatMessageRepository.findTop20...Desc` 결과를 PromptBuilder가 역순 없이 쓰는 것이 의도인지; proactive 실제 실행 주기/외부 배포 환경. 관련 파일: `ChatMessageRepository.java`, `ProactiveChatService.java`. CharacterExample 후보/legacy 정렬은 `CharacterExampleRepository` JPQL에서 event 일치→priority→id 및 event/temperature 일치→priority→id 순으로 확인했다.
- 테스트는 주요 mapping/modifier/rerank/prompt/postprocess 사례를 보장하지만 모든 21-keyword 조합, 모든 trait 충돌, 전체 prompt snapshot, 동시성·idempotency, 실제 Gemini 준수까지 보장하지 않는다.

## 부록: 주요 코드 위치

- 성격: `entity/PersonalityKeyword.java`, `domain/CharacterTrait.java`, `service/PersonalityTraitResolver.java`, `service/CharacterTraitProfileService.java`
- 감정: `engine/EventAnalyzer.java`, `engine/EventDetector.java`, `service/EmotionUpdateService.java`, `service/EmotionTraitModifier.java`, `service/RelationshipStageEmotionPolicy.java`
- RAG: `context/MemoryRetrievalService.java`, `service/CharacterExampleService.java`, `service/CharacterExampleReranker.java`, `service/CharacterExampleToneTagPolicy.java`
- 조립/생성: `service/AIProcessingService.java`, `context/ContextLoader.java`, `prompt/PromptBuilder.java`, `prompt/TraitInstructionResolver.java`
- 출력/선제: `service/ResponseStylePostProcessor.java`, `service/ProactiveContactPolicyService.java`, `service/ProactiveChatService.java`
