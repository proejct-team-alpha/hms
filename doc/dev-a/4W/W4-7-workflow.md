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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `/llm/medical` 경로로 GET 요청이 오면 `templates/llm/medical.mustache` 파일을 HTML로 렌더링해서 응답하고, `/llm/chatbot` 경로로 오면 `templates/llm/chatbot.mustache`를 렌더링합니다.
> - **왜 이렇게 썼는지**: `@Controller`는 HTML 페이지를 반환하는 SSR(서버 사이드 렌더링) 컨트롤러임을 나타냅니다. `return "llm/medical"`은 Spring MVC에게 `templates/llm/medical.mustache` 파일을 찾아서 렌더링하라는 의미입니다. `@RestController`(JSON 반환)와 다르게, 화면을 직접 내려주는 역할입니다.
> - **쉽게 말하면**: 웹 브라우저에서 주소를 입력했을 때 보여줄 HTML 페이지를 결정하는 안내판 역할입니다.

### medical.mustache — API URL 변경

```javascript
var STREAM_URL  = "/llm/medical/query/stream";   // /api/medical/query/stream → 변경
var CONSULT_URL = "/llm/medical/query/consult";  // /api/medical/query/consult → 변경
var SLOTS_URL   = "/llm/reservation/slots";       // /api/reservation/slots → 변경
// confirmBooking() 제거 — 예약은 /reservation 플로우로 안내
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: HTML 파일 안의 JavaScript에서 서버 API를 호출할 때 사용하는 URL을 HMS 경로로 변경합니다. `STREAM_URL`, `CONSULT_URL`, `SLOTS_URL` 변수에 새 경로를 저장해 두고, 버튼 클릭 등의 이벤트 때 이 변수를 사용해 서버에 요청합니다.
> - **왜 이렇게 썼는지**: 원본 spring-llm 프로젝트의 API 경로(`/api/medical/*`)를 HMS의 경로(`/llm/medical/*`)로 일괄 변경합니다. URL을 변수로 선언해 두면 나중에 경로가 바뀌어도 변수값 하나만 수정하면 됩니다.
> - **쉽게 말하면**: 전화번호부에 적힌 연락처를 최신 번호로 업데이트하는 것처럼, JS 코드가 호출할 서버 주소를 새 주소로 바꿉니다.

### chatbot.mustache — STAFF_ID 제거

```javascript
// 제거: var STAFF_ID = 1;
// 제거: X-Staff-Id 헤더
var STREAM_URL   = "/llm/chatbot/query/stream";  // /api/chat/query/stream → 변경
var FALLBACK_URL = "/llm/chatbot/query";          // /api/chat/query → 변경
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 원본에서 하드코딩된 `var STAFF_ID = 1`과 요청 헤더의 `X-Staff-Id`를 완전히 제거하고, API 경로를 HMS용으로 변경합니다.
> - **왜 이렇게 썼는지**: `var STAFF_ID = 1`은 개발 편의를 위해 직원 ID를 1로 고정해 둔 임시 코드입니다. HMS는 Spring Security 세션으로 서버가 직접 로그인한 직원 ID를 파악하므로, JS 측에서 ID를 전달할 필요가 없습니다. 하드코딩된 ID를 그대로 두면 다른 직원이 로그인해도 항상 직원 1의 데이터가 저장되는 버그가 생깁니다.
> - **쉽게 말하면**: "나는 1번 직원이야"라고 JS가 직접 말하는 대신, 서버가 "지금 로그인한 사람이 누구인지" 알아서 처리하도록 책임을 서버로 넘기는 것입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Docker Compose 설정에서 Spring 앱 서비스의 포트와 환경변수를 HMS 기준으로 변경합니다. 포트 8080으로 외부에서 접근 가능하게 하고, DB 이름을 `hms_db`로, LLM 서버 주소를 Docker 네트워크 내부 이름(`python-llm`)으로 설정합니다.
> - **왜 이렇게 썼는지**: Docker Compose는 여러 컨테이너(서버들)를 한 번에 실행하고 관리하는 도구입니다. `0.0.0.0:8080:8080`은 외부의 8080 포트를 컨테이너 내부 8080 포트에 연결한다는 의미입니다. `LLM_SERVICE_URL=http://python-llm:8000`에서 `python-llm`은 같은 Docker 네트워크 안의 Python 서버 컨테이너 이름으로, Docker가 자동으로 IP를 해석합니다.
> - **쉽게 말하면**: 여러 서버(MySQL DB, Python AI, Spring 앱)를 한 번에 실행하는 레시피 파일로, 각 서버가 서로 어떻게 연결할지 주소와 설정을 정의합니다.

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
