# RomanticAgent

Gemini를 기반으로 캐릭터의 성격, 관계 단계, 감정 상태와 대화 기억을 반영해 응답을 생성하는 AI 연애 에이전트 백엔드입니다. 일반 채팅과 이미지 입력, SSE 스트리밍, 선제 메시지, 통화 주제 생성 및 대화 요약 API를 제공합니다.

## 주요 기능

- 캐릭터 페르소나와 관계 상태를 반영한 대화 생성
- 대화 이벤트 감지 및 관계·감정 상태 변화 계산
- 대화 기억 저장과 관련 기억 검색
- JPEG, PNG, WebP 이미지가 포함된 멀티모달 채팅
- Server-Sent Events(SSE) 기반 스트리밍 응답
- 관계 상태와 선호 시간을 고려한 선제 메시지 생성
- 통화 주제 생성 및 대화 요약
- `requestId` 기반 중복 요청 방지
- 내부 API 토큰 인증 및 헬스 체크

## AI/RAG 구현 방식

이 프로젝트의 RAG는 외부 문서를 검색하는 전통적인 지식 베이스 RAG보다 **캐릭터별 장기 기억을 검색해 대화에 주입하는 Memory RAG**에 가깝습니다. MySQL에 대화와 중요 에피소드를 저장하고, 현재 발화와 관련 있는 기억을 점수화해 Gemini 프롬프트에 추가합니다.

### 전체 처리 흐름

```text
사용자 요청
  → 요청 검증 및 requestId 중복 확인
  → 발화 이벤트 분석
  → 캐릭터 감정·관계 상태 갱신
  → 최근 대화 + 장기 기억 + 페르소나 검색
  → 프롬프트 조립
  → Gemini 응답 생성
  → 응답 정제 및 품질 평가
  → 현재 대화와 중요 에피소드 저장
```

구현의 중심은 다음 클래스입니다.

| 역할 | 구현 |
| --- | --- |
| 전체 AI 처리 오케스트레이션 | `AIProcessingService` |
| 컨텍스트 수집 | `ContextLoader` |
| 임베딩 생성과 유사도 계산 | `MemoryEmbeddingService` |
| 장기 기억 검색·랭킹 | `MemoryRetrievalService` |
| 기억 생성 여부 판단 | `MemoryEngine` |
| 프롬프트 구성 | `PromptBuilder` |
| 응답 이후 기억 저장 | `ContextUpdater`, `ConversationMemoryService` |

### 1. 기억의 종류와 저장

대화 기억은 `memories` 테이블에 캐릭터 단위로 저장합니다. 기억은 두 계층으로 나뉩니다.

- `CONVERSATION_TURN`: 매 요청의 사용자 메시지와 AI 답변 원문입니다. `requestId`, 채널(`CHAT`/`CALL`), 발생 시각과 함께 저장하며 최근 대화 구성에 사용합니다.
- `EPISODE`: 고백, 이별, 사과, 화해, 기념일처럼 장기적으로 기억할 가치가 있는 사건을 요약한 기억입니다. 검색용 임베딩과 중요도도 함께 저장합니다.

응답 생성이 끝나면 `MemoryEngine`이 아래 방식으로 에피소드 생성 여부를 결정합니다.

1. 현재 캐릭터 감정 강도를 `0~10`으로 정규화합니다.
2. 대화에 고백·이별·배신·데이트·기념일·사과·질투·약속 등의 중요 키워드가 있으면 `+2`를 부여합니다.
3. 감정이 `sadness`, `hurt`, `happy`, `jealousy`, `anxiety` 등 중요 감정이면 `+1`을 부여합니다.
4. 최종 중요도가 `7` 이상일 때만 `EPISODE`로 저장합니다.
5. 사용자 발화와 AI 답변을 최대 200자로 요약하고 당시 대표 감정을 함께 기록합니다.

별도로 고백, 첫 데이트, 갈등, 화해, 기념일, 이별 위험 키워드가 감지되면 `TurningPoint`도 저장하여 관계의 주요 전환점을 추적합니다.

### 2. 임베딩 방식

현재 구현은 별도의 임베딩 API나 벡터 데이터베이스를 사용하지 않습니다. 네트워크 호출 없이 동작하도록 `MemoryEmbeddingService`에서 **128차원 해시 기반 로컬 임베딩**을 생성합니다.

```text
문장 정규화
  → 영문·숫자·한글 토큰 분리
  → 각 토큰을 해시해 128개 버킷 중 하나에 누적
  → 의미 별칭이 감지되면 가중치 2.0 추가
  → L2 정규화
  → 쉼표로 직렬화해 MySQL TEXT 컬럼에 저장
```

의미 별칭은 단순 단어 일치의 한계를 줄이기 위한 도메인 사전입니다. 예를 들어 `선물`, `생일`, `기념일`, `축하`는 공통 `gift` 특징으로, `미안`, `사과`, `화해`, `용서`는 `apology` 특징으로 매핑됩니다. 현재 사전은 선물, 이별, 사과, 애정, 스트레스, 데이트, 신뢰, 질투의 8개 연애 대화 주제를 다룹니다.

이 방식은 가볍고 비용이 들지 않으며 재현 가능하지만, 문맥을 학습한 신경망 임베딩은 아닙니다. 따라서 대규모 문서 검색보다는 제한된 연애 대화 도메인의 에피소드 회상에 맞춘 구현입니다.

### 3. 검색 대상과 랭킹

장기 기억 검색 시 현재 캐릭터의 기억만 조회하고 `CONVERSATION_TURN`은 제외합니다. 최근 대화는 별도 경로로 불러오기 때문에 에피소드 검색 결과와 중복되지 않습니다.

각 기억의 최종 점수는 다음 요소를 조합합니다.

```text
score = 중요도 × 0.5
      + 코사인 유사도 × 70
      + 현재 감정 일치 보너스(8)
      + 동일 토큰 수
      + 캐릭터 특성 보너스(최대 6)
      + 관계 단계 보너스(최대 2)
      - 최근 사용 및 반복 노출 패널티
```

- **의미 유사도**: 현재 메시지와 기억의 128차원 벡터 간 코사인 유사도를 계산합니다.
- **감정 일치**: 기억 요약에 현재 대표 감정이 포함되면 보너스를 줍니다.
- **특성 기반 검색**: 애착이 높은 캐릭터는 약속·연락·화해 기억, 공감이 높은 캐릭터는 고민·피로·스트레스 기억, 애정이 높은 캐릭터는 데이트·선물·기념일 기억을 더 높게 평가합니다. 질투 특성은 현재 발화가 질투 사건으로 판단된 경우에만 반영합니다.
- **반복 방지**: 10분 이내 다시 검색된 기억에는 18점, 30분 이내에는 10점, 2시간 이내에는 4점의 패널티를 줍니다. 누적 검색 횟수에 따라서도 최대 8점을 감점합니다.
- **검색 기록**: 선택된 기억의 `lastRetrievedAt`과 `retrievalCount`를 즉시 갱신합니다.

랭킹 상위 5개를 검색하지만, 현재 프롬프트에는 가장 높은 기억 1개만 최대 140자로 삽입합니다. 이는 관련 없는 과거 기억이 답변을 지배하거나 프롬프트가 불필요하게 길어지는 것을 막기 위한 제한입니다.

### 4. 최근 대화와 장기 기억의 분리

RAG 검색과 대화 문맥은 서로 다른 방식으로 구성합니다.

- 최근 대화는 캐릭터별 `CONVERSATION_TURN` 최신 5개를 시간순으로 복원합니다.
- DB에 저장된 최근 대화가 없을 때만 API 요청의 `history`를 대체 입력으로 사용합니다.
- 일반 응답 프롬프트에는 최근 메시지 최대 6개, SSE의 compact 프롬프트에는 최대 4개를 넣습니다.
- 장기 기억은 유사도 검색을 거친 `EPISODE` 중 최상위 1개만 `[Optional Memory]` 영역에 넣습니다.

즉, 최근 대화는 단기 문맥 유지에 사용하고, 에피소드 RAG는 오래된 중요한 사건을 다시 떠올리는 데 사용합니다.

### 5. 이벤트 분석과 상태 기반 검색

검색 전에 사용자 발화를 `BREAKUP_DECLARATION`, `BREAKUP_RETRACTION`, `APOLOGY`, `AFFECTION`, `INSULT`, `IGNORE_OR_COLD`, `NORMAL` 중 하나로 분석합니다.

먼저 규칙 기반 탐지기로 판단하고, 신뢰도가 `0.8` 이상이면 추가 LLM 호출을 생략합니다. 애매한 표현이거나 캐릭터가 이미 상처·분노·불안 상태라 세밀한 판단이 필요하면 Gemini가 최근 대화와 현재 감정 상태를 함께 보고 구조화된 JSON으로 사건을 분석합니다. LLM 분석이 실패하면 규칙 기반 결과로 폴백합니다.

이 분석 결과는 다음 단계에 함께 사용됩니다.

- 캐릭터의 애정, 신뢰, 상처, 분노, 불안, 감정적 거리 갱신
- 관계의 친밀도, 갈등도, 회복도, 이별 위험 계산
- 질투와 같은 상황별 기억 검색 보너스 계산
- 현재 대화 사건 및 관계 전환점 저장
- 응답 품질 평가 필요 여부 결정

### 6. 프롬프트 조립

`ContextLoader`는 단순히 검색된 기억만 전달하지 않고, 다음 정보를 하나의 `Context`로 조합합니다.

- 캐릭터 기본 정보, 성격 특성, 말투 예시
- 관계 단계·온도·전략과 갱신된 관계 수치
- 캐릭터의 현재 감정과 내부 상태
- 현재 활동, 기분, 에너지 등 월드 상태
- 캐릭터의 현재 목표와 이번 턴의 대화 의도
- 최근 공유 사건과 캐릭터 선호 정보
- 검색된 장기 기억과 최근 대화
- 사용자 이름, 나이, 성별, 시간대

`PromptBuilder`는 현재 메시지에 필요한 영역만 선택적으로 포함합니다. 검색 기억은 `[Optional Memory]`로 표시하며, **현재 주제와 직접 관련 있을 때만 사용하라**는 지시를 함께 넣습니다. 캐릭터 말투 예시는 사실 정보가 아닌 스타일 참고 자료로 명시하여, 예시 속 사건을 현재 사실처럼 답하는 문제를 방지합니다.

사용자 선호를 새로 묻는 턴에는 기존 기억을 프롬프트에서 제외합니다. 새로운 선호 질문과 과거 기억이 충돌해 답변의 초점이 흐려지는 것을 막기 위한 분기입니다.

### 7. 생성 이후 처리

Gemini가 답변을 생성하면 다음 후처리를 수행합니다.

1. 채널에 맞게 응답을 정제합니다.
2. 중요한 감정 사건에서는 응답 품질을 평가합니다.
3. 품질 기준을 통과하지 못하면 평가 결과를 포함한 재생성 프롬프트로 한 번 더 생성합니다. SSE 스트리밍에서는 이미 토큰을 전송했으므로 재생성 없이 평가만 수행합니다.
4. 새로 파악한 사용자 선호, 중요 에피소드, 관계 전환점을 저장합니다.
5. 최종 사용자 메시지와 AI 답변을 `CONVERSATION_TURN`으로 저장해 다음 요청의 최근 문맥으로 사용합니다.

### 현재 구현의 한계와 확장 방향

- 해시 임베딩은 동의어와 문맥 이해가 제한적입니다. 규모가 커지면 Gemini Embeddings/OpenAI Embeddings 같은 모델 기반 벡터로 교체할 수 있습니다.
- 임베딩을 MySQL `TEXT`로 저장하고 애플리케이션 메모리에서 전수 점수화하므로 기억 수가 많아질수록 느려집니다. 운영 규모에서는 pgvector, OpenSearch, Pinecone 등의 ANN 검색으로 이전할 수 있습니다.
- 현재 검색 후보는 상위 5개지만 프롬프트에는 1개만 들어갑니다. 데이터가 충분해지면 유사도 하한선, 다양성 기반 재정렬(MMR), 토큰 예산 기반 동적 Top-K를 적용할 수 있습니다.
- 키워드와 의미 별칭이 코드에 고정되어 있습니다. 운영 데이터 기반 평가셋을 만들고 별칭·가중치·임계값을 튜닝하는 것이 다음 개선 지점입니다.

## 기술 스택

- Java 17
- Spring Boot 3.3.5
- Spring Web / Spring Data JPA / Actuator
- MySQL (운영), H2 (테스트)
- Google Gemini API
- Gradle
- Docker

## 시작하기

### 사전 요구 사항

- JDK 17
- MySQL 8.x
- Gemini API 키

### 환경 변수

다음 환경 변수를 설정해야 합니다.

| 변수 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `DB_URL` | 예 | - | JDBC 연결 URL (예: `jdbc:mysql://localhost:3306/romantic_agent`) |
| `DB_USER` | 예 | - | MySQL 사용자 이름 |
| `DB_PW` | 예 | - | MySQL 비밀번호 |
| `GEMINI_API_KEY` | 예 | - | Gemini API 키 |
| `GEMINI_MODEL` | 아니요 | `gemini-3.6-flash` | 사용할 Gemini 모델 |
| `AI_INTERNAL_TOKEN` | 예 | - | API 요청 인증에 사용할 내부 토큰 |
| `SERVER_PORT` | 아니요 | `8081` | 애플리케이션 포트 |
| `AWS_REGION` | 아니요 | `ap-northeast-2` | AWS 리전 |

PowerShell에서는 아래와 같이 현재 세션에 환경 변수를 설정할 수 있습니다.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/romantic_agent?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
$env:DB_USER="root"
$env:DB_PW="your-password"
$env:GEMINI_API_KEY="your-gemini-api-key"
$env:AI_INTERNAL_TOKEN="your-internal-token"
```

> 루트의 `.env` 파일은 Git에서 제외되지만 Spring Boot가 자동으로 읽지는 않습니다. 로컬 셸에 직접 주입하거나 IDE의 실행 구성에서 환경 변수로 등록하세요.

### 로컬 실행

```powershell
.\gradlew.bat bootRun
```

서버는 기본적으로 `http://localhost:8081`에서 실행됩니다. 데이터베이스 테이블은 JPA의 `ddl-auto: update` 설정에 따라 갱신됩니다.

헬스 체크:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

### 테스트

```powershell
.\gradlew.bat test
```

### Docker 실행

```powershell
docker build -t romantic-agent .
docker run --rm -p 8081:8081 --env-file .env romantic-agent
```

MySQL이 호스트에서 실행 중이라면 컨테이너의 `DB_URL`에서 `localhost` 대신 `host.docker.internal`을 사용해야 합니다.

## API 인증

헬스 체크와 `OPTIONS` 요청을 제외한 모든 API는 인증 토큰이 필요합니다. 다음 헤더 중 하나를 사용하세요.

```http
Authorization: Bearer <AI_INTERNAL_TOKEN>
```

또는:

```http
X-Internal-Api-Key: <AI_INTERNAL_TOKEN>
```

## API

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/chat` | JSON 또는 이미지가 포함된 채팅 |
| `POST` | `/chat/stream` | SSE 스트리밍 채팅 |
| `POST` | `/api/chat/stream` | SSE 스트리밍 채팅 별칭 |
| `POST` | `/api/chat/proactive/send` | 선제 메시지 즉시 생성 |
| `PUT` | `/internal/characters/{characterId}/snapshot` | 캐릭터 스냅샷 저장 또는 갱신 |
| `DELETE` | `/internal/characters/{characterId}/data` | 캐릭터 관련 파생 데이터 삭제 |
| `POST` | `/internal/calls/topic` | 통화 내용을 바탕으로 주제 생성 |
| `POST` | `/internal/conversations/summary` | 대화 요약 생성 |
| `GET` | `/actuator/health` | 서버 상태 확인 (인증 불필요) |

### 채팅 요청 예시

`character`, `relationship`, `channel`, `message`는 일반 채팅의 필수 값입니다. 특성 값은 `0~10`, 관계 지표는 `0~100` 범위를 사용합니다.

```bash
curl -X POST http://localhost:8081/chat \
  -H "Authorization: Bearer your-internal-token" \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "chat-20260814-001",
    "channel": "CHAT",
    "userName": "민수",
    "user": {
      "birth": "1998-05-12",
      "gender": "MALE",
      "job": "개발자",
      "mbti": "INTJ"
    },
    "character": {
      "characterId": 1,
      "name": "하린",
      "mind": "다정하고 솔직하다",
      "responseStyle": "짧고 자연스러운 반말",
      "job": "디자이너",
      "lifeType": "WORKER",
      "preferTime": "ANYTIME",
      "romanceStyleScore": 70,
      "keywords": ["다정함", "장난기"],
      "age": 27,
      "gender": "FEMALE",
      "traits": {
        "humor": 7,
        "playfulness": 6,
        "affection": 8,
        "empathy": 8,
        "attachment": 5,
        "jealousy": 3,
        "dominance": 4,
        "confidence": 7,
        "expressiveness": 8,
        "emotionalStability": 7,
        "calculationVersion": 1
      }
    },
    "relationship": {
      "relationshipId": 10,
      "relationshipStage": "DATING",
      "relationshipTemperatureScore": 65,
      "trust": 70,
      "closeness": 68,
      "conflictLevel": 10,
      "repairProgress": 0,
      "breakupRisk": 5,
      "daysTogether": 30,
      "strategy": "NORMAL"
    },
    "history": [],
    "message": "오늘 하루 어땠어?"
  }'
```

관계 단계는 `CRUSH`, `DATING`, `DEEP_LOVE`를 지원하며, 채널은 `CHAT` 또는 `CALL`입니다. 이미지 채팅은 `multipart/form-data`로 JSON 요청을 `request` 파트에, 10MB 이하의 JPEG·PNG·WebP 파일을 `image` 파트에 전달합니다.

## 프로젝트 구조

```text
src/
├─ main/
│  ├─ java/com/example/aidatingagentbackend/
│  │  ├─ config/       # 인증, Gemini 설정
│  │  ├─ context/      # 대화 컨텍스트와 기억 검색
│  │  ├─ controller/   # HTTP API
│  │  ├─ dto/          # 요청·응답 모델
│  │  ├─ engine/       # 이벤트, 기억, 관계 처리 엔진
│  │  ├─ entity/       # JPA 엔티티와 도메인 열거형
│  │  ├─ prompt/       # 페르소나 및 프롬프트 구성
│  │  ├─ repository/   # 데이터 접근 계층
│  │  └─ service/      # 애플리케이션 비즈니스 로직
│  └─ resources/application.yml
└─ test/               # 단위·통합·회귀 테스트
```

## 보안 주의 사항

- `.env`, API 키, 데이터베이스 비밀번호를 커밋하지 마세요.
- 운영 환경에서는 충분히 긴 `AI_INTERNAL_TOKEN`을 사용하고 HTTPS 뒤에서 서비스를 노출하세요.
- 현재 JPA 스키마 설정은 `update`입니다. 운영 배포에서는 Flyway/Liquibase 같은 명시적 마이그레이션 도구 사용을 권장합니다.
