# W4-1 Workflow — 챗봇 UI 전 역할 확장

> **작성일**: 2026-03-19
> **브랜치**: `feature/reservation-Llm`
> **목표**: 의사/간호사에만 있는 AI 챗봇을 원무과·물품담당자·관리자에도 추가, 기존 버그 수정

---

## 현황

| 역할 | 챗봇 사이드바 | 챗봇 페이지 | 컨트롤러 |
|------|-------------|-----------|---------|
| 의사 | ✅ `/doctor/chatbot` | ✅ doctor/chatbot.mustache | ✅ DoctorChatbotController |
| 간호사 | ✅ `/nurse/chatbot` | ✅ nurse/chatbot.mustache | ✅ NurseChatbotController |
| 원무과 | ❌ | ❌ | ❌ |
| 물품담당자 | ❌ | ❌ | ❌ |
| 관리자 | ❌ | ❌ | ❌ |

## 기존 버그

`doctor/chatbot.mustache`, `nurse/chatbot.mustache` 공통 버그:
- API URL: `/llm/chatbot/ask` → `/llm/chatbot/query`
- Request body: `{ message }` → `{ query }`
- 응답 처리: `data.answer || data.message` → 응답 자체가 문자열

> **💡 입문자 설명**
>
> **왜 의사·간호사에만 있던 챗봇을 전 역할로 확장하는지**
> - AI 챗봇(병원 규칙 Q&A)은 의사와 간호사뿐만 아니라 원무과·물품담당자·관리자도 유용하게 쓸 수 있습니다. 기능이 이미 구현되어 있으므로 각 역할에 맞는 페이지와 경로만 추가하면 됩니다.
>
> **역할별 별도 컨트롤러를 만드는 이유**
> - `/staff/chatbot`, `/item-manager/chatbot`, `/admin/chatbot` 각각 별도 컨트롤러를 만드는 이유는 역할별로 접근 권한(Security 설정)과 사이드바 활성화 플래그(`isStaffChatbot` 등)가 달라야 하기 때문입니다.
> - 하나의 컨트롤러에 조건문으로 처리하면 복잡해지므로, 역할별로 단순한 컨트롤러를 분리하는 것이 유지보수에 유리합니다.
>
> **사이드바 활성화 플래그 (`isStaffChatbot: true`) — 어떻게 작동하는지**
> - Mustache 템플릿의 사이드바는 `{{#isStaffChatbot}}active{{/isStaffChatbot}}` 같은 조건으로 현재 페이지 메뉴를 강조합니다. 컨트롤러에서 이 플래그를 `true`로 설정해야 사이드바에서 현재 위치가 표시됩니다.
>
> **쉽게 말하면**: 의사·간호사 전용이었던 AI 상담실을 건물의 모든 층(역할)에 복제하는 작업입니다. 각 층마다 별도 안내 표지판(컨트롤러)과 엘리베이터 버튼(사이드바 메뉴)을 달아줍니다.

---

## 작업 목록

1. `doctor/chatbot.mustache` 버그 수정
   - fetch URL: `/llm/chatbot/ask` → `/llm/chatbot/query`
   - Request body: `{ message }` → `{ query }`
   - 응답 처리: `res.json()` → `res.text()`, `data.answer` → `data`
2. `nurse/chatbot.mustache` — 동일 버그 수정 (doctor와 동일)
3. `sidebar-staff.mustache` — 내 정보 관리 위에 AI 챗봇 메뉴 추가 (`/staff/chatbot`, `isStaffChatbot`)
4. `sidebar-item-manager.mustache` — 내 정보 관리 위에 AI 챗봇 메뉴 추가 (`/item-manager/chatbot`, `isItemChatbot`)
5. `sidebar-admin.mustache` — 내 정보 관리 위에 AI 챗봇 메뉴 추가 (`/admin/chatbot`, `isAdminChatbot`)
6. `templates/staff/chatbot.mustache` 신규 — `{{> common/sidebar-staff}}` 사용, `POST /llm/chatbot/query` 연동
7. `templates/item-manager/chatbot.mustache` 신규 — `{{> common/sidebar-item-manager}}` 사용
8. `templates/admin/chatbot.mustache` 신규 — `{{> common/sidebar-admin}}` 사용
9. `staff/StaffChatbotController` 신규 — `GET /staff/chatbot`, `isStaffChatbot: true` 모델 속성
10. `item/ItemManagerChatbotController` 신규 — `GET /item-manager/chatbot`, `isItemChatbot: true` 모델 속성
11. `admin/AdminChatbotController` 신규 — `GET /admin/chatbot`, `isAdminChatbot: true` 모델 속성
12. `./gradlew build -x test` — 빌드 오류 없음 검증

---

## 완료 기준

- [ ] doctor/nurse 챗봇 API 버그 수정
- [ ] 원무과/물품담당자/관리자 사이드바에 AI 챗봇 메뉴 추가
- [ ] 3개 역할 챗봇 페이지 + 컨트롤러 신규 생성
- [ ] `./gradlew build` 성공
