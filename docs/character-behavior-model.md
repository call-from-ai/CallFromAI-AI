# RomanticAgent Character Behavior Model

> 기준: 2026-07-13의 런타임 코드. 이 문서는 캐릭터 행동 모델의 계층과 데이터 소유권을 정의한다.

## 1. 모델 개요

```text
백엔드 snapshot(character + relationship + history + message)
→ 사건·signal 분석
→ AgentSelfState 갱신
→ 관계 변화 계산
→ AI 파생 context(world/goal/life/event/preference)
→ Memory 사실 검색 + CharacterExample 스타일 검색
→ prompt 생성
→ LLM 생성·후처리·선택적 품질 평가
→ reply + 관계/self-state/event 결과 반환
```

백엔드는 캐릭터와 관계의 원본, 채팅·통화 원문, 사용자와 전송 스케줄을 소유한다. AI 서버는 이 원본을 로컬에 복제하지 않고 매 요청 snapshot을 계산 기준으로 사용한다. AI DB에는 캐릭터 행동을 연속적으로 만들기 위한 파생 데이터만 저장한다.

## 2. Source of truth

| 도메인 | Source of truth | AI 서버 처리 |
| --- | --- | --- |
| 캐릭터 정체성 | 요청의 `CharacterSnapshot` | prompt 입력으로만 사용, 원본 저장 없음 |
| 최종 trait 10개 | `CharacterSnapshot.traits` | 0~10 필수 검증 후 감정·검색·prompt·후처리에 사용 |
| 관계 단계·온도·수치 | 요청의 `RelationshipSnapshot` | 다음 관계와 delta 계산, 원본 저장 없음 |
| 대화 원문 | 요청의 `history`와 `message` | 현재 요청 context로만 사용, 메시지 행 저장 없음 |
| 현재 AI 감정 | AI 파생 `AgentSelfState` | 매 처리 전 갱신·로그 저장 |
| 과거 사실 | AI 파생 `Memory` | 검색·prompt 및 응답 후 선택적 생성 |
| 말투 예시 | AI 파생 `CharacterExample` | 현재 사건·stage·score·trait에 맞춰 선별 |
| 생활·목표·사건·취향 | AI 파생 world/goal/life/event/preference 데이터 | prompt context와 proactive 정책에 사용 |

AI 서버는 최종 trait를 자체 계산하지 않는다. 백엔드가 키워드나 MBTI를 어떤 방식으로 관리하든 AI 요청에는 이미 계산된 10개 값이 모두 있어야 한다. 누락 fallback은 없다.

## 3. Character snapshot

`CharacterSnapshot`은 다음 계층으로 사용된다.

- identity: `characterId`, `name`, `mind`, `responseStyle`, `job`, `lifeType`
- romance style: `romanceStyleScore` 0~100
- final traits: humor, playfulness, affection, empathy, attachment, jealousy, dominance, confidence, expressiveness, emotionalStability 각각 0~10
- optional metadata: `calculationVersion`

trait는 사건 자체를 생성하지 않고 이미 감지된 상황에 대한 반응 크기와 표현을 조절한다.

| 행동 축 | 주 영향 trait |
| --- | --- |
| 농담·장난 | humor, playfulness |
| 애정·공감 | affection, empathy |
| 관계 위협 민감도 | attachment, jealousy |
| 주도적 표현 | dominance, confidence |
| 직접성·감정 회복 | expressiveness, emotionalStability |

`TraitInstructionResolver`는 주로 8 이상과 2 이하에서 명시적 행동 지시를 만든다. `EmotionTraitModifier`는 affection, attachment, jealousy, emotionalStability를 관련 사건 delta에 적용한다. 질투 trait는 실제 경쟁·질투 문맥이 없으면 질투 감정을 만들지 않는다.

## 4. Relationship model

canonical stage는 `CRUSH`, `DATING`, `DEEP_LOVE`다.

- `CRUSH`: 약한 호감은 허용하지만 강한 연인·애칭·소유 표현을 제한한다.
- `DATING`: 자연스러운 애정, 플러팅, 애칭과 전화 제안 텍스트를 허용한다.
- `DEEP_LOVE`: 안정된 친밀감, 일상 배려, 편안한 장난을 우선한다.

호환 입력인 `EARLY_DATING`, `LONG_TERM`은 각각 `DATING`, `DEEP_LOVE`로 변환된다.

`relationshipTemperatureScore`는 0~100 관계 표현 온도다.

- 0~20: calm
- 21~40: friendly affection
- 41~60: playful flirting
- 61~80: active affection
- 81~100: spicy leading

`romanceStyleScore`는 캐릭터의 연애 표현 강도이므로 관계 온도와 별개다. `RelationshipStrategy.CONFLICT_REPAIR`도 온도값이 아니라 갈등 회복 상황 전략이다.

`RelationshipEngine`은 요청 snapshot의 trust, closeness, conflictLevel, repairProgress, breakupRisk를 기준으로 `EventAnalysis` 또는 키워드 규칙을 적용한다. 모든 결과를 0~100으로 clamp하고 `RelationshipDelta`와 `nextRelationship`을 반환한다. stage, 온도, daysTogether는 자동 변경하지 않는다.

## 5. Event and AgentSelfState

사건 분류는 다음 일곱 가지다.

- `BREAKUP_DECLARATION`
- `BREAKUP_RETRACTION`
- `APOLOGY`
- `AFFECTION`
- `INSULT`
- `IGNORE_OR_COLD`
- `NORMAL`

`EventAnalyzer`가 eventType, severity, sincerity, isJoke, isManipulative, primaryEmotion, summary를 분석한다. 실패하면 규칙 기반 `EventDetector` 결과를 사용한다.

현재 감정 모델은 `AgentSelfState`로 통합되어 있다. affection, trust, hurt, anger, insecurity, disappointment, emotionalDistance와 대표 텍스트 상태를 가지며 수치는 0~1이다. 매 처리에서 시간 decay → base event delta → trait modifier → stage modifier → signal transition → clamp → log 저장 순서로 갱신한다.

응답 전후 상태는 각각 `previousAgentSelfState`, `nextAgentSelfState`로 노출된다. 이 상태와 로그는 AI 파생 데이터이며 관계 원본 수치와는 별도다.

## 6. AI-owned derived context

| 모델 | 역할 |
| --- | --- |
| `AgentWorldState` | 시간대와 현재 활동·위치·mood·energy·stress·loneliness를 구성한다. |
| `AgentGoal` | 현재 관계와 상태에 맞는 활성 목표를 선택한다. |
| `AgentLifeEvent` | 캐릭터 생활 유형에 맞는 과거/당일 생활 사건을 제공한다. |
| `ConversationEvent` | 중요한 대화 signal 또는 높은 severity 사건을 공유 사건으로 남긴다. |
| `CharacterPreference` | 대화 중 정한 캐릭터 취향을 일관되게 재사용한다. |
| `TurningPoint` | 고백, 첫 데이트, 갈등, 회복, 기념일, 이별 위험을 요약한다. |
| `ResponseQualityEvaluation` | 필요한 상황에서 답변의 self-state·경계·안전 적합도를 평가한다. |

이 데이터는 백엔드의 캐릭터·관계·채팅 테이블을 대체하지 않는다.

## 7. Memory RAG

`MemoryRetrievalService`는 해당 `characterId`의 `Memory`를 다음 신호로 점수화한다.

```text
importance * 0.5
+ cosine similarity * 70
+ emotion bonus
+ token overlap
- recent-use penalty
+ trait bonus (최대 6)
+ stage bonus (최대 2)
```

최대 5개를 조회하지만 prompt에는 가장 높은 1개만 최대 140자로 넣는다. memory는 공유 사실이며 문체 예시가 아니다. 응답 후 `MemoryEngine`이 중요하다고 판단한 대화만 요약·embedding으로 저장한다.

## 8. CharacterExample RAG

`CharacterExample`은 스타일 참고 전용이다. 현재 event type으로 후보를 찾고 다음 신호로 rerank한다.

- relationship stage
- relationship temperature score
- romance style band
- relevant trait와 tone tag
- priority, duplicate 제거, 다양성

최대 5개를 prompt에 제공한다. 일치 결과가 없으면 호환용 relationship-temperature 검색을 사용한다. 예시의 내용을 실제 과거 사실로 말하거나 그대로 복사하면 안 된다.

## 9. Prompt and response behavior

`PromptBuilder`의 주요 순서는 안전 규칙, character, relationship, stage, temperature, trait, self-state, topic/preference/initiative/life, shared event, memory, example, history, 현재 메시지다.

일반 prompt는 전달된 history의 앞 6개, compact prompt는 앞 4개를 사용하며 순서를 뒤집지 않는다. `EventAnalyzer`는 앞 10개를 본다. 각 prompt history content는 160자로 제한된다.

생성 결과에는 `ResponseStylePostProcessor`가 punctuation, 과도한 질문·웃음, stage에 맞지 않는 강한 애칭, 갈등 회복 표현 등을 조정한다. `ResponseQualityEvaluatorService`가 필요한 사건만 평가하고 낮은 점수이면 동기 경로에서 한 번 재생성할 수 있다.

성공한 동기 응답은 reply, relationship delta, next relationship, 전후 self state, event analysis를 반환한다. 채팅 원문 저장과 관계 원본 반영은 백엔드 책임이다.

## 10. Proactive behavior

proactive 실행 시점은 백엔드 outbound scheduler가 결정한다. AI 서버는 `/api/chat/proactive/send`로 받은 최신 snapshot을 사용해 짧은 check-in을 계산할 뿐 자체 주기 실행이나 사용자 연결 관리를 하지 않는다.

`ProactiveContactPolicyService`는 다음 상황을 차단한다.

- hurt가 0.65 이상이고 현재 목표가 관계 회복이 아님
- anger가 0.65 이상
- 관계 온도 81 이상, jealousy trait 8 이상이며 회복/check-in 목표가 아님
- `CRUSH`, 관계 온도 40 이하에서 고압적인 애정·과거 사건 목표

`romanceStyleScore`는 전송 여부가 아니라 문구 강도에만 영향한다.

## 11. Transport and ownership exclusions

AI 서버는 브라우저 직접 호출 경로를 제공하지 않는다. 백엔드가 server-to-server로 호출하고 클라이언트 인증, CORS, chat/call 연결, SSE 중계, 메시지 저장을 담당한다.

AI 서버가 제공하는 transport는 동기 채팅, streaming 채팅 alias 두 개, proactive send다. 캐릭터·관계 CRUD, 채팅 저장·조회, 내부 timer, 구독 endpoint, call/STT/TTS는 이 모델의 범위가 아니다.

## 12. Known contract gaps

- `requestId`는 모든 요청에서 필수이며, 동일 ID·동일 body의 완료 결과는 request ledger에서 재사용한다. 처리 중인 ID 또는 동일 ID의 다른 body는 409다.
- streaming 결과는 현재 관계 delta와 self-state snapshot을 내보내지 않는다.
- history 개수, role enum, requestId의 공식 형식은 확정되지 않았다. requestId 충돌은 현재 409로 처리한다.
- AI 파생 데이터의 보존·삭제·복구 정책은 백엔드 팀과 별도 합의가 필요하다.
