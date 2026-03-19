# W4-7 UI 이식 및 docker-compose 작성

## 작업 목표
`spring-python-llm-exam-mng`의 `medical.html`, `chat.html`을 HMS Mustache 템플릿으로 변환하고,
`LlmPageController`(SSR)를 추가한다. `docker-compose.yml`도 HMS 기준으로 신규 작성한다.

## 작업 목록
<!-- TODO 1. templates/llm/medical.mustache 신규 (medical.html 변환, header-public 포함, API URL 수정) -->
<!-- TODO 2. templates/llm/chatbot.mustache 신규 (chat.html 변환, header-staff + sidebar-doctor/nurse 포함, STAFF_ID 제거) -->
<!-- TODO 3. llm/controller/LlmPageController.java 신규 (GET /llm/medical, GET /llm/chatbot) -->
<!-- TODO 4. docker-compose.yml 신규 작성 (mysql, chromadb, python-llm, spring-app) -->
<!-- TODO 5. 빌드 확인 -->

## 진행 현황
- [x] 1. medical.mustache 신규
- [x] 2. chatbot.mustache 신규
- [x] 3. LlmPageController 신규
- [x] 4. docker-compose.yml 신규
- [x] 5. 빌드 확인 — BUILD SUCCESSFUL

## 수정/추가 파일

**신규 템플릿**
- `templates/llm/medical.mustache`
- `templates/llm/chatbot.mustache`

**신규 Controller**
- `llm/controller/LlmPageController.java`

**신규 설정**
- `docker-compose.yml` (프로젝트 루트)

**수동 작업 안내** (workflow 실행 대상 아님)
- `python-llm/` 디렉토리: `spring-python-llm-exam-mng/python-llm/`에서 수동 복사

---

## 상세 내용

### 1. medical.mustache

원본: `spring-python-llm-exam-mng/src/main/resources/static/medical.html`

**변경 사항**

| 항목 | 원본 | HMS |
|---|---|---|
| 레이아웃 | 독립 HTML | `{{> common/header-public}}` 포함, `style.css` + `feather.min.js` 로드 |
| body 구조 | `height:100vh; flex-column` | `min-h-screen flex flex-col` (HMS 공통) |
| STREAM_URL | `/api/medical/query/stream` | `/llm/medical/query/stream` |
| CONSULT_URL | `/api/medical/query/consult` | `/llm/medical/query/consult` |
| SLOTS_URL | `/api/reservation/slots` | `/llm/reservation/slots` |
| RESERVATION_URL | `/api/reservation` (POST) | 제거 — 예약 생성 미이식, 슬롯 조회까지만 |
| 예약 버튼 | confirmBooking() 호출 | 버튼 제거 (선택만 가능, 실제 예약은 /reservation 플로우로 안내) |

**레이아웃 구조**
```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" ...>
  <title>AI 증상 상담 - MediCare+</title>
  <link rel="stylesheet" href="/css/style.css">
  <script src="/js/feather.min.js"></script>
  <style> /* medical.html의 CSS 변수 및 스타일 유지 */ </style>
</head>
<body class="min-h-screen flex flex-col" style="background: var(--bg);">
  {{> common/header-public}}
  <!-- chat-messages, quick-chips, chat-input-area -->
  <script>
    var STREAM_URL = "/llm/medical/query/stream";
    var CONSULT_URL = "/llm/medical/query/consult";
    var SLOTS_URL = "/llm/reservation/slots";
    /* confirmBooking 제거, loadSlots에서 예약 버튼 제거 */
  </script>
</body>
</html>
```

### 2. chatbot.mustache

원본: `spring-python-llm-exam-mng/src/main/resources/static/chat.html`

`/llm/chatbot/**`는 `authenticated` 정책이므로 staff 레이아웃 사용.
어떤 역할이든 접근 가능하도록 sidebar는 포함하지 않고 `header-staff`만 포함.

**변경 사항**

| 항목 | 원본 | HMS |
|---|---|---|
| 레이아웃 | 독립 HTML | `{{> common/header-staff}}` 포함, HMS 공통 CSS/JS |
| STAFF_ID | `var STAFF_ID = 1;` 하드코딩 | 제거 — 서버 측 `resolveStaffId()` 처리 |
| X-Staff-Id 헤더 | fetch 요청에 포함 | 제거 (세션 인증으로 서버 처리) |
| STREAM_URL | `/api/chat/query/stream` | `/llm/chatbot/query/stream` |
| FALLBACK_URL | `/api/chat/query` | `/llm/chatbot/query` |
| CSRF | X-Staff-Id 헤더 사용 | CSRF ignore 설정이므로 불필요, 제거 유지 |

**레이아웃 구조**
```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" ...>
  <title>병원규칙 Q&A - MediCare+</title>
  <link rel="stylesheet" href="/css/style.css">
  <script src="/js/feather.min.js"></script>
  <style> /* chat.html의 CSS 변수 및 스타일 유지 */ </style>
</head>
<body class="min-h-screen flex flex-col" style="background: var(--bg);">
  {{> common/header-staff}}
  <!-- chat-messages, quick-chips, chat-input-area -->
  <script>
    feather.replace();
    var STREAM_URL = "/llm/chatbot/query/stream";
    var FALLBACK_URL = "/llm/chatbot/query";
    /* STAFF_ID 제거, X-Staff-Id 헤더 제거 */
  </script>
</body>
</html>
```

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

- `/llm/medical` — `permitAll` (SecurityConfig 기존 설정)
- `/llm/chatbot` — `authenticated` (SecurityConfig 기존 설정)

### 4. docker-compose.yml

원본: `spring-python-llm-exam-mng/docker-compose.yml` 참조, HMS 기준으로 수정.

**변경 사항**

| 항목 | spring-llm | HMS |
|---|---|---|
| DB 이름 | `llm_db` | `hms_db` |
| DB 사용자 | `llm_admin` / `llm_password` | `hms_admin` / `hms_password` |
| spring-app 포트 | `8081:8081` | `8080:8080` |
| python-llm context | `./python-llm` | `./python-llm` (수동 복사 필요) |
| LLM_SERVICE_URL | `http://python-llm:8000` | `http://python-llm:8000` (동일) |

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: hms-db
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: hms_db
      MYSQL_USER: hms_admin
      MYSQL_PASSWORD: hms_password
    ports:
      - "0.0.0.0:3306:3306"
    volumes:
      - hms_mysql_data:/var/lib/mysql
    command: --default-authentication-plugin=mysql_native_password
    healthcheck: ...

  chromadb:
    image: chromadb/chroma:1.5.4
    container_name: hms-chromadb
    ports:
      - "0.0.0.0:8100:8000"
    volumes:
      - hms_chroma_data:/chroma/chroma

  python-llm:
    build:
      context: ./python-llm   # spring-python-llm-exam-mng/python-llm/ 수동 복사 필요
      dockerfile: Dockerfile
    container_name: hms-python
    ports:
      - "0.0.0.0:8000:8000"
    environment:
      - MYSQL_DB=hms_db
      - CHROMA_HOST=chromadb
      ... (나머지 spring-llm과 동일)

  spring-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: hms-spring
    ports:
      - "0.0.0.0:8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/hms_db
      - SPRING_DATASOURCE_USERNAME=hms_admin
      - SPRING_DATASOURCE_PASSWORD=hms_password
      - LLM_SERVICE_URL=http://python-llm:8000

volumes:
  hms_mysql_data:
  hms_chroma_data:
```

**수동 작업 안내 (실행 전 준비)**
```
# spring-python-llm-exam-mng/python-llm/ → hms/python-llm/ 복사
cp -r ../spring-python-llm-exam-mng/python-llm ./python-llm
```

## 수용 기준
- [ ] `./gradlew build` 오류 없음
- [ ] GET /llm/medical → medical.mustache 렌더링
- [ ] GET /llm/chatbot → chatbot.mustache 렌더링 (미인증 시 /login 리다이렉트)
- [ ] docker-compose.yml 문법 오류 없음 (`docker compose config` 확인)
