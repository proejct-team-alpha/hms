# W4-7 리포트 - UI 이식 및 docker-compose 작성

## 작업 개요
- **작업명**: spring-python-llm-exam-mng의 `medical.html`, `chat.html`을 HMS Mustache 템플릿으로 변환,
  `LlmPageController`(SSR) 추가, `docker-compose.yml` 신규 작성
- **신규 템플릿**:
  - `templates/llm/medical.mustache`
  - `templates/llm/chatbot.mustache`
- **신규 Controller**:
  - `llm/controller/LlmPageController.java`
- **신규 설정**:
  - `docker-compose.yml` (프로젝트 루트)

## 작업 내용

### 1. medical.mustache

| 항목 | 원본 (medical.html) | HMS |
|---|---|---|
| 레이아웃 | 독립 HTML | `{{> common/header-public}}` 포함 |
| body 구조 | `height:100vh; flex-column` | `min-h-screen flex flex-col` + `.llm-body` (100vh - 64px) |
| STREAM_URL | `/api/medical/query/stream` | `/llm/medical/query/stream` |
| CONSULT_URL | `/api/medical/query/consult` | `/llm/medical/query/consult` |
| SLOTS_URL | `/api/reservation/slots` | `/llm/reservation/slots` |
| RESERVATION_URL | `/api/reservation` (POST) | 제거 |
| 예약 버튼 | `confirmBooking()` — POST 예약 생성 | `/reservation` 링크로 대체 (예약 생성 미이식) |

**예약 버튼 처리**: spring-llm의 `POST /api/reservation`은 Task 6에서 이식 제외 결정.
슬롯 조회 후 "예약 페이지로 이동" 링크(`<a href="/reservation">`)로 대체.

### 2. chatbot.mustache

| 항목 | 원본 (chat.html) | HMS |
|---|---|---|
| 레이아웃 | 독립 HTML | `{{> common/header-staff}}` 포함 |
| STAFF_ID | `var STAFF_ID = 1;` 하드코딩 | 제거 — 서버 측 `resolveStaffId()` 처리 |
| X-Staff-Id 헤더 | fetch 요청에 포함 | 제거 (세션 인증으로 서버 처리) |
| STREAM_URL | `/api/chat/query/stream` | `/llm/chatbot/query/stream` |
| FALLBACK_URL | `/api/chat/query` | `/llm/chatbot/query` |
| CSRF 토큰 | 없음 (X-Staff-Id만 사용) | 불필요 (CSRF ignore 설정) |

`/llm/chatbot/**`는 `authenticated` 정책 → 미인증 접근 시 Spring Security가 `/login`으로 리다이렉트.

### 3. LlmPageController

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

- GET `/llm/medical` → `templates/llm/medical.mustache` (permitAll)
- GET `/llm/chatbot` → `templates/llm/chatbot.mustache` (authenticated)

### 4. docker-compose.yml

| 항목 | spring-llm | HMS |
|---|---|---|
| DB 이름 | `llm_db` | `hms_db` |
| DB 사용자/PW | `llm_admin` / `llm_password` | `hms_admin` / `hms_password` |
| spring-app 포트 | `8081:8081` | `8080:8080` |
| container prefix | `llm-*` | `hms-*` |
| volume prefix | `llm_*` | `hms_*` |
| python-llm MYSQL_DB | `llm_db` | `hms_db` |

서비스 구성: `mysql` → `chromadb` → `python-llm` → `spring-app` (healthcheck depends_on 체인)

**수동 작업 (docker-compose 실행 전 필요)**
```bash
cp -r ../spring-python-llm-exam-mng/python-llm ./python-llm
```

## 빌드 결과
```
BUILD SUCCESSFUL in 3s
```

## 특이사항
- `medical.mustache`의 예약 버튼: `confirmBooking()` 제거 → `<a href="/reservation">` 링크로 대체.
  예약 생성 API(`POST /api/reservation`)는 Task 6에서 이식 제외 결정이므로 HMS 기존 예약 플로우로 안내.
- `chatbot.mustache`: `STAFF_ID` 하드코딩 및 `X-Staff-Id` 헤더 완전 제거.
  ChatController의 `resolveStaffId()`가 세션 기반으로 staffId를 처리하므로 JS 측 변경 불필요.
- `LlmPageController`의 `@RequestMapping("/llm")`은 `RestController`들과 경로 충돌 없음 —
  `ChatController`(`/llm/chatbot`), `MedicalController`(`/llm/medical`)는 `@RestController`이고
  `LlmPageController`는 `@Controller`로 분리.
- `docker-compose.yml`의 `python-llm` 서비스는 `./python-llm` 디렉토리 빌드 —
  해당 디렉토리를 수동으로 복사해야 `docker compose up` 가능.
