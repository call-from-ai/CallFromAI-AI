# AI 서버 백엔드 연동 가이드

> 기준: 2026-07-13의 `ChatController`, `ProactiveChatController`, DTO, `ContextLoader`, `GlobalExceptionHandler` 실제 코드.

## 캐릭터 snapshot 동기화

BE는 `X-Internal-Api-Key`를 포함해 `PUT /internal/characters/{characterId}/snapshot`을 호출한다. path와 body의 `characterId`는 같아야 하며 성공 응답은 `204 No Content`다. AI 서버는 `characterId`별 최신 snapshot 한 건만 유지하고, 기존 `calculationVersion`보다 낮은 요청은 무시한다. 사용자 식별자나 소유 관계는 저장하지 않는다. `DELETE /internal/characters/{characterId}/data`는 snapshot과 AI 파생 데이터를 함께 삭제하며 대상이 없어도 `204`를 반환한다.
/

## 1. 아키텍처 한 줄 요약

백엔드는 채팅·통화·사용자·캐릭터·관계 원본과 전송 일정을 소유하고, AI 서버는 매 요청 직전의 `character + relationship + history (+ message)` snapshot을 받아 답변과 계산 결과만 반환하는 request 단위 계산 서버다.

단, AI 서버는 행동 연속성을 위한 AI 전용 파생 데이터(`AgentSelfState`, Memory/RAG, world/goal/event/preference 등)를 `characterId` 기준으로 유지한다. 캐릭터·관계·채팅 원본을 AI DB에 복제하지 않는다는 의미의 snapshot 경계이며, 프로세스가 완전히 무상태라는 뜻은 아니다.

## 2. 엔드포인트

| Method | Path | 응답 | 용도 |
| --- | --- | --- | --- |
| `POST` | `/chat` | 동기 JSON `ChatResponse` | 일반 채팅 |
| `POST` | `/chat/stream` | SSE `text/event-stream` | 일반 채팅 streaming |
| `POST` | `/api/chat/stream` | SSE `text/event-stream` | 위 streaming endpoint의 동일 alias |
| `POST` | `/api/chat/proactive/send` | 동기 JSON `ChatResponse` | 백엔드 scheduler가 요청하는 proactive 답변 생성 |

모든 endpoint의 request body는 통합된 `ChatRequest`다. 캐릭터 CRUD, 관계 CRUD, 채팅 저장·조회, proactive 구독·스케줄 관리 endpoint는 없다.

## 3. 공통 요청 계약: ChatRequest

### 최상위 필드

| 필드 | Java 타입 | 일반 채팅/stream | proactive | 제약·동작 |
| --- | --- | --- | --- | --- |
| `requestId` | `String` | 필수 | 필수 | non-blank. 앞뒤 공백을 제거해 ledger key로 사용하며 DB 최대 길이는 100자다. 성공 응답에서 echo된다. |
| `character` | `CharacterSnapshot` | 필수 | 필수 | null이면 400. |
| `relationship` | `RelationshipSnapshot` | 필수 | 필수 | null이면 400. |
| `history` | `List<ChatHistoryItem>` | 선택 | 선택 | 생략 시 빈 목록. null도 처리 단계에서 빈 목록으로 취급한다. |
| `message` | `String` | 필수 | 선택/무시 | 일반 채팅은 null·빈 문자열·공백만 있으면 400. proactive는 없어도 되며 값이 있어도 내부 check-in seed로 대체한다. |

### CharacterSnapshot

| 필드 | Java 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| `characterId` | `Long` | 필수 | null 불가. AI 파생 데이터 조회 key다. |
| `name` | `String` | 필수 | non-blank |
| `mind` | `String` | 선택 | 길이 검증 없음 |
| `responseStyle` | `String` | 선택 | 길이 검증 없음 |
| `job` | `String` | 선택 | 길이 검증 없음 |
| `lifeType` | `AgentLifeType` | 선택 | `STUDENT`, `WORKER`, `FLEXIBLE`; `UNEMPLOYED`는 deprecated 호환값 |
| `preferTime` | `PreferTime` | 선택 | `MORNING`, `DAY`, `LATE_EVENING`, `ANYTIME`. 누락 시 `ANYTIME` |
| `romanceStyleScore` | `Integer` | 필수 | 0~100 |
| `traits` | `CharacterTraitSnapshot` | 필수 | null이면 400 |

### CharacterTraitSnapshot

| 필드 | Java 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| `humor` | `Integer` | 필수 | 0~10 |
| `playfulness` | `Integer` | 필수 | 0~10 |
| `affection` | `Integer` | 필수 | 0~10 |
| `empathy` | `Integer` | 필수 | 0~10 |
| `attachment` | `Integer` | 필수 | 0~10 |
| `jealousy` | `Integer` | 필수 | 0~10 |
| `dominance` | `Integer` | 필수 | 0~10 |
| `confidence` | `Integer` | 필수 | 0~10 |
| `expressiveness` | `Integer` | 필수 | 0~10 |
| `emotionalStability` | `Integer` | 필수 | 0~10 |
| `calculationVersion` | `Integer` | 선택 | 값 범위 검증과 런타임 분기 없음 |

AI 서버는 trait fallback을 계산하지 않는다. 10개 중 하나라도 누락되면 Jackson 역직렬화 과정에서 `IllegalArgumentException`이 발생해 400으로 반환된다.

### RelationshipSnapshot

| 필드 | Java 타입 | 필수 | 제약·동작 |
| --- | --- | --- | --- |
| `relationshipId` | `Long` | 선택 | null 허용. 백엔드 row 식별을 위해 제공 권장 |
| `relationshipStage` | `RelationshipStage` | 필수 | canonical: `CRUSH`, `DATING`, `DEEP_LOVE`. 호환: `EARLY_DATING`, `LONG_TERM`, `썸`, `연애`, `깊은 사랑` |
| `relationshipTemperatureScore` | `Integer` | 필수 | 0~100 |
| `trust` | `Integer` | 필수 | 0~100 |
| `closeness` | `Integer` | 선택 | 0~100. 생략 시 AI 내부 `AgentSelfState`, 상태가 없으면 중립값 50 사용 |
| `conflictLevel` | `Integer` | 선택 | 0~100. 생략 시 AI 내부 `AgentSelfState`, 상태가 없으면 중립값 0 사용 |
| `repairProgress` | `Integer` | 필수 | 0~100 |
| `breakupRisk` | `Integer` | 필수 | 0~100 |
| `daysTogether` | `Integer` | 필수 | 0 이상 |
| `strategy` | `RelationshipStrategy` | 선택 | `NORMAL`, `CONFLICT_REPAIR`; null/생략 시 `NORMAL` |

호환 stage는 내부 canonical 값으로 변환되므로 `nextRelationship.relationshipStage`에는 `DATING` 또는 `DEEP_LOVE`가 반환된다.

### ChatHistoryItem

| 필드 | Java 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| `role` | `String` | 필수 | non-blank. 허용값 enum 검증은 현재 없음 |
| `content` | `String` | 필수 | null 불가; 빈 문자열과 길이는 검증하지 않음 |

백엔드 구현에서는 `role`을 `user`와 `assistant`로 고정하는 것을 권장한다. AI 코드는 role 문자열을 그대로 prompt에 쓴다.

### 일반 채팅 요청 예시

```json
{
  "requestId": "chat-01J2Y8P0M6Q7YH2J9W8K5T3N1A",
  "character": {
    "characterId": 10,
    "name": "하나",
    "mind": "따뜻하지만 솔직하다",
    "responseStyle": "짧고 자연스러운 한국어 메신저 말투",
    "job": "개발자",
    "lifeType": "WORKER",
    "preferTime": "LATE_EVENING",
    "romanceStyleScore": 72,
    "traits": {
      "humor": 6,
      "playfulness": 7,
      "affection": 8,
      "empathy": 9,
      "attachment": 5,
      "jealousy": 2,
      "dominance": 4,
      "confidence": 7,
      "expressiveness": 8,
      "emotionalStability": 7,
      "calculationVersion": 3
    }
  },
  "relationship": {
    "relationshipId": 20,
    "relationshipStage": "DATING",
    "relationshipTemperatureScore": 64,
    "trust": 70,
    "closeness": 66,
    "conflictLevel": 10,
    "repairProgress": 30,
    "breakupRisk": 5,
    "daysTogether": 42,
    "strategy": "NORMAL"
  },
  "history": [
    { "role": "user", "content": "오늘 일이 많았어" },
    { "role": "assistant", "content": "고생했네. 지금은 좀 괜찮아?" }
  ],
  "message": "응, 이제 퇴근했어"
}
```

### proactive 요청 예시

`message`를 보내지 않는다. 나머지 snapshot 구조는 일반 채팅과 같다.

```json
{
  "requestId": "proactive-01J2Y9C3X7W6B4K2S8M0Q5R1TA",
  "character": {
    "characterId": 10,
    "name": "하나",
    "mind": "따뜻하지만 솔직하다",
    "responseStyle": "짧고 자연스러운 한국어 메신저 말투",
    "job": "개발자",
    "lifeType": "WORKER",
    "romanceStyleScore": 72,
    "traits": {
      "humor": 6,
      "playfulness": 7,
      "affection": 8,
      "empathy": 9,
      "attachment": 5,
      "jealousy": 2,
      "dominance": 4,
      "confidence": 7,
      "expressiveness": 8,
      "emotionalStability": 7,
      "calculationVersion": 3
    }
  },
  "relationship": {
    "relationshipId": 20,
    "relationshipStage": "DATING",
    "relationshipTemperatureScore": 64,
    "trust": 70,
    "closeness": 66,
    "conflictLevel": 10,
    "repairProgress": 30,
    "breakupRisk": 5,
    "daysTogether": 42,
    "strategy": "NORMAL"
  },
  "history": [
    { "role": "user", "content": "오늘 일이 많을 것 같아" },
    { "role": "assistant", "content": "끝나면 푹 쉬어. 너무 무리하지 말고" }
  ]
}
```

## 4. 동기 응답 계약: ChatResponse

| 필드 | Java 타입 | 의미 |
| --- | --- | --- |
| `requestId` | `String` | 요청의 필수 correlation/idempotency key를 echo |
| `reply` | `String` | 후처리와 필요 시 재생성을 마친 assistant 답변 |
| `relationshipDelta` | `RelationshipDelta` | 입력 관계 수치 대비 signed 변화량 |
| `nextRelationship` | `RelationshipSnapshot` | 계산 후 전체 관계 snapshot |
| `previousAgentSelfState` | `AgentSelfStateSnapshot` | 이번 처리 직전 AI 파생 감정 상태. 최초 생성 전이면 각 기본값이 들어간 snapshot |
| `nextAgentSelfState` | `AgentSelfStateSnapshot` | decay·사건·signal 반영 후 AI 파생 감정 상태 |
| `eventAnalysis` | `EventAnalysis` | 현재 입력에 대한 사건 분석 |

### RelationshipDelta

모든 필드는 `Integer`이며 `trust`, `closeness`, `conflictLevel`, `repairProgress`, `breakupRisk` 순으로 현재값 대비 증감량이다. 음수도 정상이다.

### AgentSelfStateSnapshot

| 필드 | Java 타입 | 제약·의미 |
| --- | --- | --- |
| `affection` | `Double` | 0.0~1.0 |
| `trust` | `Double` | 0.0~1.0 |
| `hurt` | `Double` | 0.0~1.0 |
| `anger` | `Double` | 0.0~1.0 |
| `insecurity` | `Double` | 0.0~1.0 |
| `disappointment` | `Double` | 0.0~1.0 |
| `emotionalDistance` | `Double` | 0.0~1.0 |
| `lastEmotion` | `String` | 대표 감정 label |
| `lastSignificantEvent` | `String` | 최근 중요 사건 label |
| `version` | `Long` | AI DB 낙관적 잠금 version |
| `updatedAt` | `LocalDateTime` | timezone 없는 ISO-8601 local datetime |

### EventAnalysis

| 필드 | Java 타입 | 의미 |
| --- | --- | --- |
| `eventType` | `AgentEventType` | `BREAKUP_DECLARATION`, `BREAKUP_RETRACTION`, `APOLOGY`, `AFFECTION`, `INSULT`, `IGNORE_OR_COLD`, `NORMAL` |
| `severity` | `Double` | 분석 강도, 일반적으로 0.0~1.0 |
| `sincerity` | `Double` | 진정성, 일반적으로 0.0~1.0 |
| `isJoke` | `Boolean` | 농담 가능성 판정 |
| `isManipulative` | `Boolean` | 조작성 판정 |
| `primaryEmotion` | `String` | 분석된 대표 감정 |
| `summary` | `String` | 분석 요약 |

### 동기 응답 예시

```json
{
  "requestId": "chat-01J2Y8P0M6Q7YH2J9W8K5T3N1A",
  "reply": "드디어 끝났네. 오늘 진짜 고생 많았어. 집 가서 뭐부터 하고 싶어?",
  "relationshipDelta": {
    "trust": 0,
    "closeness": 0,
    "conflictLevel": 0,
    "repairProgress": 0,
    "breakupRisk": 0
  },
  "nextRelationship": {
    "relationshipId": 20,
    "relationshipStage": "DATING",
    "relationshipTemperatureScore": 64,
    "trust": 70,
    "closeness": 66,
    "conflictLevel": 10,
    "repairProgress": 30,
    "breakupRisk": 5,
    "daysTogether": 42,
    "strategy": "NORMAL"
  },
  "previousAgentSelfState": {
    "affection": 0.55,
    "trust": 0.6,
    "hurt": 0.0,
    "anger": 0.0,
    "insecurity": 0.15,
    "disappointment": 0.0,
    "emotionalDistance": 0.15,
    "lastEmotion": "calm",
    "lastSignificantEvent": "none",
    "version": 0,
    "updatedAt": "2026-07-13T21:30:00"
  },
  "nextAgentSelfState": {
    "affection": 0.55,
    "trust": 0.6,
    "hurt": 0.0,
    "anger": 0.0,
    "insecurity": 0.15,
    "disappointment": 0.0,
    "emotionalDistance": 0.15,
    "lastEmotion": "interested",
    "lastSignificantEvent": "none",
    "version": 0,
    "updatedAt": "2026-07-13T21:30:01"
  },
  "eventAnalysis": {
    "eventType": "NORMAL",
    "severity": 0.1,
    "sincerity": 0.5,
    "isJoke": false,
    "isManipulative": false,
    "primaryEmotion": "neutral",
    "summary": "Analyzed by rule-based fallback detector."
  }
}
```

예시 값은 형식을 설명하기 위한 값이다. 실제 delta와 감정은 입력과 기존 AI 파생 상태에 따라 달라진다.

## 5. 요청 직전 백엔드 준비 체크리스트

AI 저장소에는 백엔드의 물리 테이블명이 없으므로 아래는 논리적 매핑이다. 실제 백엔드 schema의 대응 테이블·컬럼으로 구현한다.

1. 캐릭터 원본(통상 `character` 계열)에서 `characterId`, 이름, mind, response style, job, life type, romance style score를 읽는다.
2. 백엔드가 확정해 저장한 최종 trait 10개를 읽는다. tag·keyword·MBTI만 보내면 안 되며 AI 서버가 계산해 주지 않는다.
3. 관계 원본(통상 `relationship` 계열)에서 stage, temperature score, trust, closeness, conflict, repair, breakup risk, days together, strategy를 같은 row/version에서 읽는다.
4. 채팅 원본(통상 `chat_message`)에서 현재 room/relationship에 해당하는 최근 user/assistant 메시지를 고른다. 현재 신규 user message는 `history`에 중복하지 말고 `message`에만 둔다.
5. history를 오래된 것→최신 순으로 보낸다. AI 코드는 정렬·역순 변환을 하지 않고 목록 앞부분만 사용한다.
6. 일반 동기 prompt는 앞 6개, streaming/proactive compact prompt는 앞 4개, 사건 분석은 앞 10개를 사용한다. 서버 DTO에 hard maximum은 없지만, 불필요한 전송과 분석 불일치를 피하려면 최근 10개 이하를 권장한다.
7. 각 항목의 `role`은 백엔드에서 `user` 또는 `assistant`로 정규화한다.
8. 한 DB snapshot에서 조립한 뒤 유일한 `requestId`를 생성한다. 관계 row의 optimistic version을 별도 보관해 응답 반영 시 동시성 검사를 수행하는 것이 좋다.

## 6. 응답 후 백엔드 처리

동기 `/chat` 또는 proactive 성공 시 다음 순서로 한 트랜잭션에서 처리하는 것을 권장한다.

1. `requestId`가 pending 요청과 일치하는지 확인한다.
2. 요청 때 읽은 relationship row/version이 아직 유효한지 확인한다.
3. `nextRelationship`의 `trust`, `closeness`, `conflictLevel`, `repairProgress`, `breakupRisk`로 관계 row를 갱신한다. `relationshipDelta`를 현재 DB 값에 다시 더하는 방식보다 전체 next 값을 조건부 갱신하는 방식이 재시도에 안전하다.
4. `nextRelationship`의 stage, temperature, daysTogether, strategy는 AI가 입력을 유지해 반환한 값이다. 백엔드의 별도 비즈니스 규칙 없이 자동 진급·증가로 해석하지 않는다.
5. `reply`를 assistant 메시지로 `chat_message`에 저장하고 원래 user 메시지와 `requestId`로 연결한다.
6. `relationshipDelta`는 감사 로그, 이벤트 발행, UI 변화 표시가 필요할 때 사용한다.
7. `eventAnalysis`는 분석·모니터링 또는 백엔드 이벤트 처리에 사용할 수 있다. AI의 관계 계산에는 이미 반영되었으므로 같은 delta를 다시 적용하지 않는다.
8. `previousAgentSelfState`와 `nextAgentSelfState`는 AI 파생 상태의 관측값이다. 백엔드가 별도 감정 projection을 운영한다면 `requestId` 기준으로 한 번만 upsert하고, AI 서버의 관계 수치와 섞지 않는다.

관계 optimistic version이 바뀌었다면 무조건 overwrite하지 않는다. 최신 snapshot으로 새 `requestId`를 만들어 AI 계산을 다시 요청하거나, 백엔드 도메인 규칙에 따라 충돌을 해소한다.

## 7. 오류 계약과 처리

### ErrorResponse

`GlobalExceptionHandler`가 처리하는 오류는 다음 JSON shape다.

| 필드 | 타입 | 의미 |
| --- | --- | --- |
| `timestamp` | `Instant` | UTC ISO-8601 시각 |
| `status` | `int` | HTTP status code |
| `error` | `String` | HTTP reason phrase |
| `message` | `String` | 예외 메시지. 역직렬화 오류는 framework wrapper 문구를 포함할 수 있음 |
| `path` | `String` | 요청 URI |

```json
{
  "timestamp": "2026-07-13T12:40:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "message is required",
  "path": "/chat"
}
```

### 400이 되는 실제 조건

| 조건 | 대표 메시지 또는 원인 |
| --- | --- |
| body 누락·malformed JSON·타입 불일치 | `HttpMessageNotReadableException` 메시지 |
| 일반 채팅 `message` null/blank | `message is required` |
| `character` 누락 | `character snapshot is required` |
| `relationship` 누락 | `relationship snapshot is required` |
| character id/name/romance score/traits 오류 | `character.* is required` 또는 범위 메시지 |
| trait 10개 중 누락 또는 범위 이탈 | `character.traits.<name> must be between 0 and 10`이 원인에 포함 |
| 관계 수치 누락 또는 범위 이탈 | `relationship.<name> must be between 0 and 100`이 원인에 포함 |
| daysTogether 음수/누락 | `relationship.daysTogether must be non-negative` |
| stage 누락 | `relationship.relationshipStage is required` |
| 알 수 없는 stage | `Unsupported relationshipStage: <value>`가 원인에 포함 |
| 잘못된 enum 값(strategy, lifeType 등) | JSON 역직렬화 실패 |
| history role/content 오류 | `history.role is required`, `history.content is required`가 원인에 포함 |

`ResponseStatusException`도 status code와 reason을 같은 `ErrorResponse`로 변환하도록 handler가 준비되어 있지만 현재 controller/service 코드에는 이 예외를 직접 던지는 경로가 없다.

### 처리되지 않는 서버 오류

`IllegalStateException`과 일반 런타임 예외는 현재 `GlobalExceptionHandler`의 대상이 아니다. Gemini 설정/호출 문제, event 분석 parse 실패, proactive 정책 거절(`Proactive contact policy rejected this request.`) 등은 5xx가 될 수 있으며 응답 shape가 위 `ErrorResponse`라고 보장되지 않는다.

백엔드 처리 원칙:

- 400: payload 생성 버그로 보고 자동 재시도하지 않는다. 원인 필드와 requestId를 로깅한다.
- 409: 같은 `requestId`가 처리 중이거나 다른 요청 body에 이미 연결된 경우다. 처리 중이면 짧은 backoff 후 같은 body로 재시도하고, body가 다르면 새 `requestId`를 발급한다.
- 5xx/timeout: 같은 논리 요청은 같은 `requestId`와 같은 body로 제한적으로 재시도한다. 완료된 요청이면 AI 서버가 ledger의 저장 응답을 반환하고, 실패 처리된 요청이면 다시 실행한다.
- proactive 정책 거절: 현재는 전용 status가 없으므로 메시지 기반 영구 분류에 의존하지 말고 5xx로 취급하되 알림을 보내지 않는다. 전용 4xx/204 계약은 미결정 사항이다.
- 응답 parse 실패: 관계와 assistant 메시지를 부분 저장하지 말고 전체 요청을 실패 처리한다.

## 8. Streaming 중계

`POST /chat/stream`과 `POST /api/chat/stream`은 동일한 SSE stream을 반환한다. timeout은 120초다.

### 이벤트 순서와 data

1. `meta` — prompt 준비 후 한 번

```text
event:meta
data:{"eventType":"NORMAL","compactPrompt":true}

```

2. `chunk` — 0회 이상

```text
event:chunk
data:{"text":"드디어 "}

```

3. 성공 시 `done` — 한 번

```text
event:done
data:{"firstTokenLatencyMs":420,"totalLatencyMs":1380,"llmCallCount":2}

```

4. 비동기 처리 실패 시 `error`

```text
event:error
data:{"message":"stream failed"}

```

Spring `SseEmitter`가 실제 wire data를 JSON으로 직렬화한다. 백엔드는 AI 응답의 `event`와 `data`를 파싱한 뒤 클라이언트 SSE에 같은 event name으로 중계하거나, 자기 public contract에 맞게 변환해야 한다. proxy buffering을 끄고 연결 종료와 timeout을 전파한다.

중요한 현재 제약:

- streaming은 chunk들을 이어 붙인 문자열이 최종 `reply`다.
- AI 서버는 누적 답변에 대한 후처리·평가·파생 저장을 끝낸 뒤 `done`을 보낸다.
- `meta`에는 event type만 있고 전체 `eventAnalysis`는 없다.
- `done`에는 `requestId`, `relationshipDelta`, `nextRelationship`, self-state snapshot이 없다.
- 따라서 현재 streaming만으로는 동기 endpoint와 동일한 관계 업데이트 계약을 수행할 수 없다. 백엔드는 이 제약을 받아들이거나 계약 확장 전까지 streaming에서 관계 반영을 별도로 하지 않아야 한다.
- request body 검증이 emitter 생성 전에 실패하면 SSE 이벤트가 아니라 HTTP 400 `ErrorResponse`가 반환될 수 있다.

## 9. Proactive 연동

AI 서버 내부에는 timer, hourly/heartbeat job, 사용자 연결 registry 또는 구독 endpoint가 없다. 호출 여부와 실제 발송은 백엔드가 책임진다.

권장 흐름:

```text
backend outbound scheduler
→ 사용자 수신 가능 여부·quiet hours·최근 대화·rate limit 확인
→ 최신 character/trait/relationship snapshot과 최근 history 조립
→ POST /api/chat/proactive/send (message 생략)
→ 성공하면 reply와 관계 결과를 일반 동기 응답과 같은 방식으로 저장
→ backend가 client push/SSE/WebSocket 채널로 전달
```

호출 주기는 AI 코드에 정의되어 있지 않다. 백엔드의 outbound 정책이 후보 시점을 결정해야 한다. 동일 대상에 중복 job이 실행되지 않도록 scheduler/ledger에서 잠금과 idempotency를 보장한다.

AI 서버도 생성 전에 `ProactiveContactPolicyService`로 상태를 검사한다. 높은 hurt·anger나 일부 고온도/고질투·고압 목표는 차단될 수 있다. 이 검사는 backend scheduler의 rate limit, quiet hours, opt-out을 대신하지 않는다.

## 10. requestId와 idempotency

권장 생성 방식은 백엔드가 소유한 UUIDv7/ULID 또는 전역 유일 문자열이다. 접두사(`chat-`, `proactive-`)를 붙여 운영 추적을 쉽게 할 수 있다.

현재 코드 동작:

- 모든 채팅·streaming·proactive 요청에서 non-blank `requestId`가 필수다. 없으면 400이다.
- AI 서버는 처리 전에 `ai_request_ledger`에 `requestId`, 요청 body의 SHA-256 hash, `PROCESSING` 상태를 기록한다.
- 같은 `requestId`와 같은 body의 완료 요청은 저장된 `ChatResponse`를 반환해 AI 처리와 파생 side effect를 다시 실행하지 않는다.
- 같은 `requestId`가 아직 처리 중이거나, 같은 ID에 다른 body를 보내면 409 Conflict다.
- 처리 중 예외가 발생하면 ledger 항목을 삭제하므로 같은 ID·body로 재시도할 수 있다.
- `/chat`과 `/api/chat/proactive/send`는 응답 body에 `requestId`를 echo한다.
- streaming 신규 처리의 SSE event에는 `requestId`가 포함되지 않지만 ledger에는 같은 규칙이 적용된다. 완료 결과 재요청 시 저장된 `reply`를 `chunk`로 재생하고 `done.cached=true`를 보낸다.

백엔드는 다음을 보장해야 한다.

1. user message 저장과 outbound AI 요청에 동일 requestId를 연결한다.
2. timeout 재시도 시 `requestId`뿐 아니라 요청 body도 동일하게 유지한다.
3. 성공 응답의 assistant message와 관계 반영에는 requestId unique constraint를 적용한다.
4. timeout 후 재시도할 때 이미 성공 응답을 처리했는지 먼저 확인한다.
5. 관계 업데이트는 요청 당시 row version을 조건으로 `nextRelationship`을 적용한다.

## 11. 아직 결정되지 않았거나 백엔드 팀과 논의할 부분

- history의 공식 최대 개수, token/문자 제한, role enum. 현재 코드는 앞 10/6/4개만 서로 다르게 사용한다.
- `requestId`의 UUIDv7/ULID 등 공식 형식과 100자 이내의 더 구체적인 길이 제한.
- streaming 완료 이벤트에 requestId, 관계 delta, next relationship, 전후 self state, 전체 event analysis를 추가할지 여부.
- proactive 정책 거절을 204, 409, 422 등 어떤 안정된 HTTP 계약으로 표현할지 여부.
- AI 파생 `AgentSelfState`를 백엔드에도 projection할지, 응답에서는 관측만 할지 여부.
- 관계 row가 AI 처리 중 변경됐을 때 재계산, merge, 폐기 중 어떤 정책을 사용할지 여부.
- AI 파생 데이터의 삭제·보존 기간과 캐릭터 삭제 시 cascade/삭제 API.
- 인증, service-to-service authorization, timeout, retry budget은 이 코드에 명시되어 있지 않다.
