# RomanticAgent 캐릭터 정책 가이드

> 기준: 2026-07-13의 `src/main` 실제 구현. 이 문서는 캐릭터 행동 정책의 원본이며, 코드에 없는 저장소·API·스케줄러를 전제로 하지 않는다.
> 작성중

## 1. 소유권과 실행 경계

백엔드는 사용자, 캐릭터 원본, 관계 원본, 채팅·통화 원문과 전송 일정을 소유한다. AI 서버는 매 요청에 포함된 `CharacterSnapshot`, `RelationshipSnapshot`, `ChatHistoryItem`을 해당 계산의 입력 원본으로 사용하고, 관계 변화 제안과 답변을 반환한다.

AI 서버는 캐릭터 원본, 관계 원본, 채팅 메시지를 로컬 테이블에 복제하지 않는다. 별도 프로필을 만들거나 키워드·MBTI로 trait를 보완하지도 않는다. 최종 trait 10개는 백엔드가 모든 요청에 반드시 제공해야 하며, 하나라도 없거나 0~10 범위를 벗어나면 요청은 400이다.

AI 서버의 DB에는 응답 계산을 위한 파생 데이터만 남는다. 현재 유지되는 파생 데이터는 `AgentSelfState`, `AgentSelfStateLog`, `AgentWorldState`, `AgentGoal`, `AgentLifeEvent`, `ConversationEvent`, `CharacterPreference`, `Memory`, `TurningPoint`, `CharacterExample`, `ResponseQualityEvaluation`이다. 모두 `characterId`를 중심으로 조회되며 백엔드의 원본 엔티티를 대신하지 않는다.

## 2. 요청 snapshot 정책

일반 채팅과 proactive는 하나의 `ChatRequest` 계약을 사용한다.

| 입력 | 정책 |
| --- | --- |
| `requestId` | 필수 non-blank 값. 동기·streaming·proactive 모두 AI request ledger의 correlation/idempotency key로 사용한다. |
| `character` | 모든 엔드포인트에서 필수. 캐릭터 원본의 요청 시점 snapshot이다. |
| `relationship` | 모든 엔드포인트에서 필수. 관계 원본의 요청 시점 snapshot이다. |
| `history` | 선택값. null이면 빈 목록으로 취급한다. AI 서버는 전달 순서를 바꾸지 않는다. |
| `message` | 일반 채팅과 streaming에서는 non-blank 필수. proactive에서는 없어도 되며, 있더라도 내부 proactive 지시문으로 대체된다. |

`CharacterSnapshot`은 `characterId`, `name`, `mind`, `responseStyle`, `job`, `lifeType`, `romanceStyleScore`, `traits`를 전달한다. `characterId`, non-blank `name`, 0~100의 `romanceStyleScore`, `traits`가 필수다. `lifeType`은 `STUDENT`, `WORKER`, `FLEXIBLE`을 사용하며 `UNEMPLOYED`는 호환용 deprecated 값이다.

`CharacterTraitSnapshot`의 최종 trait는 다음 10개다.

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

각 값은 필수 정수 0~10이다. `calculationVersion`은 선택 메타데이터이며 AI 서버가 계산이나 호환성 판정에 사용하지 않는다. 기본 trait, 키워드 조합, MBTI fallback은 없다.

`RelationshipSnapshot`은 `relationshipId`, `relationshipStage`, `relationshipTemperatureScore`, `trust`, `repairProgress`, `breakupRisk`, `daysTogether`, `strategy`를 전달한다. `closeness`와 `conflictLevel`은 선택값이며 BE의 affinity/floor score를 매핑하지 않는다. AI는 내부 `AgentSelfState`로 두 값을 계산하고, 상태가 없으면 중립값(50/0)을 사용한다. 관계 수치는 0~100, `daysTogether`는 0 이상이며 `strategy`가 없으면 `NORMAL`이다.

## 3. 응답 생성 파이프라인

일반 동기 채팅은 다음 순서로 처리한다.

```text
ChatController
→ ChatRequest endpoint validation
→ ChatService
→ AIProcessingService.prepare
→ EmotionUpdateService
→ ConversationEventService
→ RelationshipEngine / ContextUpdater
→ AgentWorldStateService / AgentGoalService
→ ContextLoader
→ MemoryRetrievalService / CharacterExampleService
→ PromptBuilder
→ GeminiService
→ ResponseStylePostProcessor
→ conditional ResponseQualityEvaluatorService
→ CharacterPreference / Memory / TurningPoint 파생 데이터 갱신
→ ChatResponse
```

streaming은 동일한 `prepare` 결과로 prompt를 만든 뒤 chunk를 전송한다. 누적 답변에 후처리·선택적 품질 평가·파생 데이터 갱신을 수행하고 `done` 이벤트로 끝낸다. proactive도 동일 pipeline을 사용하되 compact prompt와 내부 proactive 지시문을 사용한다.

## 4. 캐릭터 고정 성향 정책

캐릭터 정체성의 원본은 요청 snapshot이다. prompt에는 이름, `mind`, `responseStyle`, 직업과 생활 유형이 필요한 범위에서 사용된다. AI 서버는 이 값을 수정하거나 저장하지 않는다.

trait는 장기적 표현 성향이다. `TraitInstructionResolver`, `EmotionTraitModifier`, `CharacterExample` 선별, `Memory` 검색, `ResponseStylePostProcessor`가 동일 snapshot 값을 사용한다.

| Trait | 실제 영향 |
| --- | --- |
| humor | 높은 값에서 가벼운 농담 지시, playful 예시 가중치 |
| playfulness | 장난스러운 표현과 예시 선별; 심각한 고민에서는 empathy가 우선 |
| affection | 애정 표현, 긍정 감정 delta, 관련 기억·예시 가중치 |
| empathy | 고민 상황의 공감 우선, 관련 기억·예시 가중치 |
| attachment | 관계 위협 민감도와 연락·약속 관련 기억 가중치; 통제 표현은 금지 |
| jealousy | 실제 경쟁·질투 맥락이 있을 때만 감정과 예시에 영향 |
| dominance | 제안·리드 표현과 관련 예시 가중치 |
| confidence | 확신 있는 표현과 관련 예시 가중치 |
| expressiveness | 직접/간접 감정 표현; 매우 낮으면 반복 느낌표를 축소 |
| emotionalStability | 부정 반응 완화, 회복·시간 decay 조절, 갈등 예시 선별 |

높은 trait는 대체로 8 이상, 낮은 trait는 2 이하에서 명시적 prompt 규칙이 강해진다. trait는 사건을 만들어내지 않고 실제 메시지에서 분석된 사건의 반응 크기와 표현 방식만 조절한다.

## 5. 관계 정책

### RelationshipStage

현재 canonical stage는 다음 세 가지다.

| Stage | 표현 정책 |
| --- | --- |
| `CRUSH` | 관심과 약한 호감은 허용하되 확정 연인 표현, 강한 애칭과 소유 표현을 제한한다. |
| `DATING` | 자연스러운 애정, 플러팅, 애칭과 전화 제안 텍스트를 허용한다. |
| `DEEP_LOVE` | 편안한 일상 배려와 안정된 친밀감을 우선한다. |

입력 호환을 위해 `EARLY_DATING`은 `DATING`, `LONG_TERM`은 `DEEP_LOVE`로 역직렬화된다. 한국어 `썸`, `연애`, `깊은 사랑`도 허용된다. 그 외 값은 400이다.

### 관계 온도와 연애 표현 강도

`relationshipTemperatureScore`는 현재 관계의 표현 온도로 0~100이다. 0~20은 차분함, 21~40은 부드러운 애정, 41~60은 가벼운 플러팅, 61~80은 적극적 애정, 81~100은 강한 리드 표현 band로 사용된다. 질투·압박·강요는 높은 score만으로 허용되지 않는다.

`romanceStyleScore`는 캐릭터 자체의 연애 표현 성향이며 0~100이다. 두 score는 서로 다른 입력이고 서로를 덮어쓰지 않는다.

`strategy`는 `NORMAL` 또는 `CONFLICT_REPAIR`다. 후자는 온도 band가 아니라 갈등 회복 문구와 예시를 선택하는 상황 전략이다.

### 관계 수치 계산

`RelationshipEngine`은 요청의 `trust`, `repairProgress`, `breakupRisk`와 AI 내부 상태에서 해석한 `closeness`, `conflictLevel`에서 시작한다. BE가 선택 필드를 보내더라도 affinity/floor의 대체값으로 사용하지 않는다. `EventAnalysis`가 normal이 아니면 사건 종류·severity·sincerity·조작성 여부를 반영하고, 아니면 메시지 키워드 규칙을 적용한다. 결과는 0~100으로 clamp한다.

AI 서버는 관계 원본을 저장하지 않는다. 변화량은 `relationshipDelta`, 계산 후 snapshot은 `nextRelationship`으로 반환하며 백엔드가 자기 DB에 원자적으로 반영한다. `relationshipStage`, `relationshipTemperatureScore`, `daysTogether`, `strategy`는 자동 진급·증가시키지 않고 입력값을 유지한다.

## 6. 사건과 감정 정책

`EventAnalyzer`는 최근 history와 현재 `AgentSelfState`를 참고해 `BREAKUP_DECLARATION`, `BREAKUP_RETRACTION`, `APOLOGY`, `AFFECTION`, `INSULT`, `IGNORE_OR_COLD`, `NORMAL` 중 하나와 severity, sincerity, joke/manipulation 여부, 대표 감정, 요약을 만든다. 분석 실패 시 `EventDetector` 규칙 결과로 fallback한다.

현재 감정 모델은 `AgentSelfState` 하나다. 별도의 대표 상태 엔티티나 별도 감정 엔진은 사용하지 않는다. 필드는 affection, trust, hurt, anger, insecurity, disappointment, emotionalDistance, lastEmotion, lastSignificantEvent이며 수치는 0~1로 clamp한다.

처리 순서는 시간 decay, 사건 delta, trait modifier, stage modifier, 메시지 signal transition, clamp, 로그 저장이다. 최초 상태는 affection 0.55, trust 0.60, insecurity 0.15, emotionalDistance 0.15, 나머지 부정 감정 0과 `calm`이다. 낙관적 잠금 충돌은 한 번 재시도한다.

응답은 `previousAgentSelfState`와 `nextAgentSelfState`를 모두 반환한다. 현재 구현은 이 파생 상태를 AI 서버에도 저장한다. 백엔드는 이를 관측·감사 또는 별도 동기화에 사용할 수 있지만, 관계 원본 업데이트와 혼동하면 안 된다.

## 7. AI 서버 소유 파생 데이터

| 데이터 | 생성·갱신 정책 | 사용처 |
| --- | --- | --- |
| `AgentSelfState` / log | 매 처리 전 감정 계산과 변경 근거 저장 | prompt 감정 전략, proactive 정책, 응답 snapshot |
| `AgentWorldState` | 시간대, 캐릭터 snapshot, 관계, self state로 갱신 | 조건부 life context |
| `AgentGoal` | 관계·상태에 따라 현재 목표 선택 | initiative와 proactive 정책 |
| `AgentLifeEvent` | 캐릭터 생활 유형에 맞춰 필요한 사건 생성·조회 | 자기 이야기 관련 prompt |
| `ConversationEvent` | 중요한 대화 signal이나 높은 severity 사건 저장 | 최근 shared-event context |
| `CharacterPreference` | 취향 질문에서 이미 알려진 값 사용 또는 생성 답변에서 파생 | 일관된 취향 답변 |
| `Memory` | 응답 후 중요한 사실·에피소드를 추출하고 embedding 저장 | 사실 RAG |
| `TurningPoint` | 고백·첫 데이트·갈등·화해·기념일·이별 위험 키워드로 파생 | 장기 사건 기록 |
| `CharacterExample` | 사전에 관리된 스타일 예시 | 스타일 RAG |
| `ResponseQualityEvaluation` | 갈등·안전 등 필요한 응답만 평가 | 선택적 재생성 |

이 파생 데이터에는 채팅 원문 전체를 메시지 행으로 저장하지 않는다. `Memory`, `TurningPoint`, 평가 레코드에 요약 또는 필요한 문맥 일부가 저장될 수 있다.

## 8. Memory와 CharacterExample

`Memory`는 과거 사실용 RAG다. 해당 `characterId`의 후보를 embedding 유사도, 중요도, 단어 겹침, 최근 사용 penalty, trait와 stage의 약한 bonus로 정렬해 최대 5개를 조회한다. prompt에는 그중 최대 1개만 140자로 넣는다. 취향을 새로 정하는 turn에는 memory retrieval을 생략한다.

`CharacterExample`은 말투 참고용이며 사실로 사용하면 안 된다. event type 후보를 조회한 뒤 stage, 관계 온도, 연애 표현 score, 관련 trait, tone tag, 우선순위, 중복과 다양성을 반영해 최대 5개를 고른다. 결과가 없으면 호환용 temperature 전략 검색을 사용한다.

두 데이터는 목적이 다르다. memory의 문체를 복제하거나 example의 내용을 실제 공유 기억으로 주장하지 않는다.

## 9. History와 prompt 정책

백엔드가 보낸 history는 `ContextLoader`가 그대로 사용하며 정렬하거나 뒤집지 않는다. `EventAnalyzer`는 앞에서 최대 10개, 일반 prompt는 앞에서 최대 6개, compact prompt(streaming/proactive)는 앞에서 최대 4개를 사용한다. prompt의 각 history content는 160자로 자른다.

따라서 백엔드는 사용할 최근 대화를 시간 오름차순(오래된 것→최신)으로 선별해 보내야 한다. 현재 요청 DTO에는 history 총개수·role enum·content 길이에 대한 상한 검증이 없다.

prompt는 안전 경계, 캐릭터 identity, 관계, trait, self state, topic/preference/initiative/life, conversation event, memory, example, history, 현재 메시지 순으로 조립된다. 답을 먼저 하고 follow-up 질문은 최대 하나로 제한하며 위협·강요를 금지한다.

## 10. 응답과 후처리 정책

`ChatResponse`는 `requestId`, `reply`, `relationshipDelta`, `nextRelationship`, `previousAgentSelfState`, `nextAgentSelfState`, `eventAnalysis`를 반환한다. 생성 답변에는 관계 단계, 온도, 연애 표현 score, trait와 self state를 반영한 후처리가 적용된다. 필요 시 품질 평가 후 한 번 재생성할 수 있다.

AI 서버는 사용자 메시지나 assistant 답변을 채팅 테이블에 저장하지 않는다. 백엔드는 성공 응답을 받은 후 assistant `reply`와 관계 결과를 자기 트랜잭션 경계에서 저장한다.

## 11. Proactive 정책

AI 서버에는 hourly/heartbeat 실행기, 사용자 연결 구독, SSE 구독 endpoint가 없다. 백엔드의 outbound scheduler가 전송 시점과 수신 가능 여부를 결정하고 `/api/chat/proactive/send`에 최신 snapshot과 history를 보낸다.

AI 서버는 새 사용자 메시지 대신 안전한 내부 check-in 지시문을 사용한다. `ProactiveContactPolicyService`는 hurt 0.65 이상(회복 목표 제외), anger 0.65 이상, 일부 고온도·고질투 조건, `CRUSH` 저온도에서의 고압 목표를 차단한다. `romanceStyleScore` 자체는 send/no-send 조건이 아니라 표현 강도에만 쓰인다.

## 12. 네트워크와 제외 범위

이 프로젝트에는 브라우저 직접 호출을 위한 CORS 설정이 없다. 브라우저 클라이언트는 백엔드를 호출하고, 백엔드가 AI 서버를 server-to-server로 호출한다.

현재 제공되는 endpoint는 동기 채팅, 두 개의 동일 streaming alias, proactive send뿐이다. 캐릭터·관계 CRUD, 채팅 조회·저장, call/STT/TTS, 구독, 스케줄 관리 endpoint는 이 AI 서버의 책임이 아니다.

## 13. 구현상 주의점과 미결정 사항

- 동일 `requestId`·동일 요청 body의 완료 요청은 ledger에 저장된 응답을 재사용한다. 처리 중인 ID 또는 동일 ID의 다른 body는 409를 반환하며, 실패한 실행의 ledger 항목은 삭제되어 재시도할 수 있다.
- streaming 이벤트에는 현재 `requestId`, 관계 변화, self-state 변화, event analysis 전체가 포함되지 않는다.
- proactive 정책 거절은 현재 전용 4xx 응답으로 매핑되지 않아 일반 서버 오류가 될 수 있다.
- history 최대 전송 개수와 role 값은 API 계약으로 고정되어 있지 않다.
- AI 파생 데이터의 보존 기간, 삭제, 백엔드와의 재동기화 정책은 별도 운영 합의가 필요하다.

## 14. 주요 코드 근거

- 요청·응답: `dto/ChatRequest.java`, `dto/CharacterSnapshot.java`, `dto/CharacterTraitSnapshot.java`, `dto/RelationshipSnapshot.java`, `dto/ChatResponse.java`
- endpoint와 오류: `controller/ChatController.java`, `controller/ProactiveChatController.java`, `controller/GlobalExceptionHandler.java`
- 조립: `service/AIProcessingService.java`, `context/ContextLoader.java`, `context/ContextUpdater.java`
- 감정·관계: `service/EmotionUpdateService.java`, `engine/RelationshipEngine.java`, `service/RelationshipStageEmotionPolicy.java`
- RAG: `context/MemoryRetrievalService.java`, `service/CharacterExampleService.java`, `service/CharacterExampleReranker.java`
- 생성·출력: `prompt/PromptBuilder.java`, `prompt/TraitInstructionResolver.java`, `service/ResponseStylePostProcessor.java`
- proactive: `service/ProactiveChatService.java`, `service/ProactiveContactPolicyService.java`
