# W4-1번째작업 리포트 - 챗봇 UI 전 역할 확장

## 작업 개요
- **작업명**: 의사·간호사 전용 AI 챗봇을 원무과·물품담당자·관리자에도 확장, 기존 API 버그 수정
- **수정 파일**: `templates/doctor/chatbot.mustache`, `templates/nurse/chatbot.mustache`, `templates/common/sidebar-staff.mustache`, `templates/common/sidebar-item-manager.mustache`, `templates/common/sidebar-admin.mustache`, `templates/staff/chatbot.mustache`(신규), `templates/item-manager/chatbot.mustache`(신규), `templates/admin/chatbot.mustache`(신규), `staff/StaffChatbotController.java`(신규), `item/ItemManagerChatbotController.java`(신규), `admin/AdminChatbotController.java`(신규)

## 작업 내용

### 1. doctor/nurse 챗봇 — API 버그 수정

| 항목 | 수정 전 | 수정 후 |
|------|---------|---------|
| API URL | `/llm/chatbot/ask` | `/llm/chatbot/query` |
| Request body | `{ message: message }` | `{ query: message }` |
| 응답 파싱 | `res.json()` → `data.answer` | `res.text()` → `data` |

```javascript
// 수정 후 fetch 요청
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
> - **이 코드가 하는 일**: 서버의 실제 API 스펙에 맞게 URL, 요청 필드명, 응답 파싱 방식을 수정합니다.
> - **왜 이렇게 썼는지**: 서버 API가 개발 중 변경되었는데 프론트엔드가 업데이트되지 않아 불일치가 발생했습니다. `/ask` → `/query`는 URL 불일치, `message` → `query`는 필드명 불일치, `res.json()` → `res.text()`는 응답 형식 불일치 수정입니다.
> - **쉽게 말하면**: 서버와 프론트엔드가 서로 다른 규칙으로 대화하고 있었는데 같은 규칙으로 맞춰준 것입니다.

### 2. 사이드바 3개 — AI 챗봇 메뉴 추가

`sidebar-staff.mustache`, `sidebar-item-manager.mustache`, `sidebar-admin.mustache`의 내 정보 관리 위에 AI 챗봇 메뉴 링크 추가. 각각 `isStaffChatbot`, `isItemChatbot`, `isAdminChatbot` 활성 플래그 적용.

### 3. 챗봇 페이지 3개 신규 생성

`staff/chatbot.mustache`, `item-manager/chatbot.mustache`, `admin/chatbot.mustache` 신규 생성. 각 역할에 맞는 사이드바 partial(`{{> common/sidebar-*}}`) 사용. `POST /llm/chatbot/query` 연동.

### 4. 컨트롤러 3개 신규 생성

```java
// StaffChatbotController — 원무과 챗봇 (ItemManagerChatbotController, AdminChatbotController도 동일 구조)
@GetMapping("/chatbot")
public String chatbot(Model model) {
    model.addAttribute("isStaffChatbot", true);
    return "staff/chatbot";
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 역할별 챗봇 URL(`/staff/chatbot`, `/item-manager/chatbot`, `/admin/chatbot`)에 대응하는 컨트롤러를 각각 만듭니다. 사이드바 활성 플래그도 함께 설정합니다.
> - **왜 이렇게 썼는지**: Spring Security는 URL 기반으로 접근 권한을 제어합니다. 같은 기능이라도 역할마다 독립적인 URL과 컨트롤러가 필요합니다.
> - **쉽게 말하면**: 같은 챗봇 기능이지만 원무과·관리자·물품담당자 각각 별도의 입장 문(컨트롤러)을 만든 것입니다.

## 테스트 결과

| 항목 | 상태 |
|------|------|
| `doctor/chatbot.mustache` 버그 수정 (URL, body, 응답 처리) | ✅ |
| `nurse/chatbot.mustache` 동일 버그 수정 | ✅ |
| `sidebar-staff.mustache` AI 챗봇 메뉴 추가 | ✅ |
| `sidebar-item-manager.mustache` AI 챗봇 메뉴 추가 | ✅ |
| `sidebar-admin.mustache` AI 챗봇 메뉴 추가 | ✅ |
| `templates/staff/chatbot.mustache` 신규 | ✅ |
| `templates/item-manager/chatbot.mustache` 신규 | ✅ |
| `templates/admin/chatbot.mustache` 신규 | ✅ |
| `StaffChatbotController` 신규 (`GET /staff/chatbot`) | ✅ |
| `ItemManagerChatbotController` 신규 (`GET /item-manager/chatbot`) | ✅ |
| `AdminChatbotController` 신규 (`GET /admin/chatbot`) | ✅ |
| `./gradlew build -x test` 성공 | ✅ |

## 특이사항
- doctor·nurse 챗봇 버그는 서버 API 스펙 변경 후 프론트엔드가 업데이트되지 않아 발생한 동기화 문제
- 챗봇 응답이 순수 텍스트 문자열이므로 `res.text()`로 파싱해야 함 (`res.json()` 사용 시 파싱 오류)
