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
