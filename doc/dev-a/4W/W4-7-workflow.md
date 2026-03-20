# W4-7 Workflow — UI 이식 및 docker-compose 작성

> **작성일**: 4W
> **브랜치**: `feature/Llm`
> **목표**: medical.html, chat.html을 HMS Mustache 템플릿으로 변환 + docker-compose.yml 작성

---

## 전체 흐름

```
spring-llm HTML → HMS Mustache 변환
  → LlmPageController SSR 추가
  → docker-compose.yml HMS 기준 작성
  → python-llm/ 수동 복사 안내
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| medical.html | → medical.mustache, header-public 포함, API URL /llm/medical/* |
| chat.html | → chatbot.mustache, header-staff 포함, STAFF_ID 하드코딩 제거 |
| docker-compose | mysql, chromadb, python-llm, spring-app 4개 서비스 |
| python-llm/ | 수동 복사 필요 (workflow 실행 대상 아님) |

---

## 실행 흐름

```
[1] templates/llm/medical.mustache 신규 — medical.html 변환
[2] templates/llm/chatbot.mustache 신규 — chat.html 변환
[3] LlmPageController.java 신규 — GET /llm/medical, GET /llm/chatbot
[4] docker-compose.yml 신규 — HMS 기준 4개 서비스
[5] ./gradlew build 검증
```

---

## UI Mockup

```
[AI 증상 상담 - /llm/medical]
┌─────────────────────────────────┐
│  HMS Header (비인증 접근 가능)   │
├─────────────────────────────────┤
│  chat-messages 영역              │
│  quick-chips                    │
│  [입력창] [전송]                 │
└─────────────────────────────────┘

[병원규칙 Q&A - /llm/chatbot]
┌─────────────────────────────────┐
│  HMS Staff Header (인증 필요)   │
├─────────────────────────────────┤
│  chat-messages 영역              │
│  [입력창] [전송]                 │
└─────────────────────────────────┘
```

---

## 작업 목록

1. `templates/llm/medical.mustache` 신규 — `{{> common/header-public}}`, API URL `/llm/medical/*` 수정
2. `templates/llm/chatbot.mustache` 신규 — `{{> common/header-staff}}`, `STAFF_ID` 하드코딩 제거
3. `LlmPageController.java` 신규 — `GET /llm/medical`, `GET /llm/chatbot`
4. `docker-compose.yml` 신규 — mysql, chromadb, python-llm, spring-app (포트 8080)
5. `./gradlew build` 검증

---

## 작업 진행내용

- [x] medical.mustache 신규
- [x] chatbot.mustache 신규
- [x] LlmPageController 신규
- [x] docker-compose.yml 신규
- [x] 빌드 확인 — BUILD SUCCESSFUL

---

## 실행 흐름에 대한 코드

### LlmPageController

```java
@Controller
@RequestMapping("/llm")
public class LlmPageController {

    @GetMapping("/medical")
    public String medicalPage() {
        return "llm/medical";
    }

    @GetMapping("/chatbot")
    public String chatbotPage() {
        return "llm/chatbot";
    }
}
```

### medical.mustache — API URL 변경

```javascript
var STREAM_URL  = "/llm/medical/query/stream";   // /api/medical/query/stream → 변경
var CONSULT_URL = "/llm/medical/query/consult";  // /api/medical/query/consult → 변경
var SLOTS_URL   = "/llm/reservation/slots";       // /api/reservation/slots → 변경
// confirmBooking() 제거 — 예약은 /reservation 플로우로 안내
```

### chatbot.mustache — STAFF_ID 제거

```javascript
// 제거: var STAFF_ID = 1;
// 제거: X-Staff-Id 헤더
var STREAM_URL   = "/llm/chatbot/query/stream";  // /api/chat/query/stream → 변경
var FALLBACK_URL = "/llm/chatbot/query";          // /api/chat/query → 변경
```

### docker-compose.yml (주요 변경사항)

```yaml
# DB: llm_db → hms_db, 포트 8081 → 8080
spring-app:
  ports:
    - "0.0.0.0:8080:8080"
  environment:
    - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/hms_db
    - LLM_SERVICE_URL=http://python-llm:8000
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| GET /llm/medical | 비인증 | 200 OK, medical.mustache 렌더링 |
| GET /llm/chatbot | 비인증 | 3xx → /login |
| GET /llm/chatbot | 인증 후 | 200 OK, chatbot.mustache 렌더링 |
| docker-compose | `docker compose config` | 문법 오류 없음 |

---

## 완료 기준

- [x] `./gradlew build` 오류 없음
- [x] GET /llm/medical 비인증 200
- [x] GET /llm/chatbot 비인증 리다이렉트 / 인증 200
- [x] docker-compose.yml 문법 오류 없음
