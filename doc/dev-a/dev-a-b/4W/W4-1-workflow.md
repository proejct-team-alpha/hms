# W4-1번째작업 Workflow — 챗봇 UI 전 역할 확장

> **작성일**: 4W
> **목표**: 의사·간호사에만 있는 AI 챗봇을 원무과·물품담당자·관리자에도 추가, 기존 버그 수정

---

## 전체 흐름

```
기존 버그 수정 (doctor/nurse) → 사이드바 메뉴 추가 → 챗봇 페이지 신규 생성 → 컨트롤러 신규 생성 → 빌드 검증
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 의사·간호사 전용 AI 챗봇을 전 역할(원무과·물품담당자·관리자)로 확장 |
| 기존 버그 | `/llm/chatbot/ask` → `/llm/chatbot/query`, `{ message }` → `{ query }`, `res.json()` → `res.text()` |
| 확장 대상 | staff, item-manager, admin 3개 역할 |
| 사이드바 활성 플래그 | `isStaffChatbot`, `isItemChatbot`, `isAdminChatbot` |
| 빌드 검증 | `./gradlew build -x test` 성공 여부 확인 |

---

## 실행 흐름

```
사용자 메시지 입력
  → fetch POST /llm/chatbot/query (query 필드)
  → 서버: LlmService.chat(query) 호출
  → 응답: 순수 텍스트 문자열
  → res.text()로 파싱 → 채팅창에 출력
```

---

## UI Mockup

```
┌─────────────────────────────────────┐
│ [사이드바] AI 챗봇 메뉴 (신규 추가)   │
├─────────────────────────────────────┤
│ AI 챗봇                              │
│ ┌───────────────────────────────┐   │
│ │ AI: 안녕하세요. 무엇을 도와드릴까요? │
│ │ 나: 진료비 환급 규정이 어떻게 돼요?  │
│ │ AI: 진료비 환급은 ...              │
│ └───────────────────────────────┘   │
│ [질문 입력...          ] [전송]       │
└─────────────────────────────────────┘
```

---

## 작업 목록

1. `doctor/chatbot.mustache` 버그 수정 (URL, body, 응답 파싱)
2. `nurse/chatbot.mustache` 동일 버그 수정
3. `sidebar-staff.mustache` — 내 정보 관리 위에 AI 챗봇 메뉴 추가
4. `sidebar-item-manager.mustache` — AI 챗봇 메뉴 추가
5. `sidebar-admin.mustache` — AI 챗봇 메뉴 추가
6. `templates/staff/chatbot.mustache` 신규 생성
7. `templates/item-manager/chatbot.mustache` 신규 생성
8. `templates/admin/chatbot.mustache` 신규 생성
9. `StaffChatbotController` 신규 생성
10. `ItemManagerChatbotController` 신규 생성
11. `AdminChatbotController` 신규 생성
12. `./gradlew build -x test` 빌드 오류 없음 검증

---

## 작업 진행내용

- [x] doctor/nurse 챗봇 API 버그 수정
- [x] 원무과 사이드바 AI 챗봇 메뉴 추가
- [x] 물품담당자 사이드바 AI 챗봇 메뉴 추가
- [x] 관리자 사이드바 AI 챗봇 메뉴 추가
- [x] staff/chatbot.mustache 신규 생성
- [x] item-manager/chatbot.mustache 신규 생성
- [x] admin/chatbot.mustache 신규 생성
- [x] StaffChatbotController 신규 생성
- [x] ItemManagerChatbotController 신규 생성
- [x] AdminChatbotController 신규 생성
- [x] 빌드 성공 검증

---

## 실행 흐름에 대한 코드

### 1. 버그 수정 — fetch 요청 (doctor/nurse 공통)

```javascript
// 수정 전 (버그)
const res = await fetch('/llm/chatbot/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message: message })
});
const data = await res.json();
addMessage('ai', data.answer || data.message);

// 수정 후
const res = await fetch('/llm/chatbot/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json',
                'X-CSRF-TOKEN': csrfToken },
    body: JSON.stringify({ query: message })
});
const data = await res.text();
addMessage('ai', data);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: JavaScript에서 서버로 챗봇 질문을 보내는 fetch 요청을 수정합니다. URL, 요청 데이터 필드명, 응답 파싱 방식 세 가지를 모두 바꿉니다.
> - **왜 이렇게 썼는지**: 서버 API가 개발 중 이름이 바뀌었는데 프론트엔드가 업데이트되지 않아 불일치가 발생했습니다. `/ask` → `/query`, `message` → `query`, `res.json()` → `res.text()`로 서버 스펙에 맞게 수정했습니다.
> - **쉽게 말하면**: 서버와 프론트엔드가 서로 다른 언어를 쓰고 있었는데 같은 언어로 맞춰준 것입니다.

### 2. 신규 컨트롤러 — StaffChatbotController (예시)

```java
// GET /staff/chatbot — 원무과 AI 챗봇 페이지
@Controller
@RequestMapping("/staff")
public class StaffChatbotController {

    @GetMapping("/chatbot")
    public String chatbot(Model model) {
        model.addAttribute("isStaffChatbot", true);
        return "staff/chatbot";
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 원무과(`/staff/chatbot`) URL로 접근하면 챗봇 페이지를 보여주는 컨트롤러입니다. `isStaffChatbot: true`를 모델에 넣어 사이드바에서 현재 페이지 메뉴가 강조되도록 합니다.
> - **왜 이렇게 썼는지**: 역할별로 별도 컨트롤러를 만드는 이유는 Spring Security가 URL 기반으로 접근 권한을 제어하기 때문입니다. 같은 기능이라도 역할마다 독립적인 라우팅과 사이드바 활성화 처리가 필요합니다.
> - **쉽게 말하면**: 의사·간호사 전용이었던 AI 상담실을 원무과에도 복제하되, 각 층(역할)마다 별도 안내 표지판(컨트롤러)을 단 것입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| doctor 챗봇 버그 수정 | 질문 입력 후 전송 | AI 응답 정상 출력 |
| nurse 챗봇 버그 수정 | 질문 입력 후 전송 | AI 응답 정상 출력 |
| staff 챗봇 접근 | `/staff/chatbot` 접속 | 챗봇 페이지 + 사이드바 활성화 |
| item-manager 챗봇 접근 | `/item-manager/chatbot` 접속 | 챗봇 페이지 + 사이드바 활성화 |
| admin 챗봇 접근 | `/admin/chatbot` 접속 | 챗봇 페이지 + 사이드바 활성화 |
| 빌드 검증 | `./gradlew build -x test` | BUILD SUCCESSFUL |

---

## 완료 기준

- [x] doctor/nurse 챗봇 API 버그 수정 완료
- [x] 원무과·물품담당자·관리자 사이드바에 AI 챗봇 메뉴 추가
- [x] 3개 역할 챗봇 페이지 + 컨트롤러 신규 생성
- [x] `./gradlew build` 성공
