# W4-1 Report — 챗봇 UI 전 역할 확장

> **작성일**: 2026-03-19
> **브랜치**: `feature/reservation-Llm`
> **빌드**: BUILD SUCCESSFUL

---

## 작업 완료 목록


| #   | 항목                                                              | 상태  |
| --- | --------------------------------------------------------------- | --- |
| 1   | `doctor/chatbot.mustache` 버그 수정 (URL, body, 응답 처리)              | ✅   |
| 2   | `nurse/chatbot.mustache` 동일 버그 수정                               | ✅   |
| 3   | `sidebar-staff.mustache` AI 챗봇 메뉴 추가                            | ✅   |
| 4   | `sidebar-item-manager.mustache` AI 챗봇 메뉴 추가                     | ✅   |
| 5   | `sidebar-admin.mustache` AI 챗봇 메뉴 추가                            | ✅   |
| 6   | `templates/staff/chatbot.mustache` 신규                           | ✅   |
| 7   | `templates/item-manager/chatbot.mustache` 신규                    | ✅   |
| 8   | `templates/admin/chatbot.mustache` 신규                           | ✅   |
| 9   | `StaffChatbotController` 신규 (`GET /staff/chatbot`)              | ✅   |
| 10  | `ItemManagerChatbotController` 신규 (`GET /item-manager/chatbot`) | ✅   |
| 11  | `AdminChatbotController` 신규 (`GET /admin/chatbot`)              | ✅   |
| 12  | `./gradlew build -x test` 성공                                    | ✅   |


---

## 버그 수정 내용 (doctor/nurse 공통)


| 항목           | 수정 전                         | 수정 후                  |
| ------------ | ---------------------------- | --------------------- |
| API URL      | `/llm/chatbot/ask`           | `/llm/chatbot/query`  |
| Request body | `{ message: message }`       | `{ query: message }`  |
| 응답 파싱        | `res.json()` → `data.answer` | `res.text()` → `data` |


---

## 챗봇 현황


| 역할    | 사이드바 | 페이지                             | 컨트롤러                           |
| ----- | ---- | ------------------------------- | ------------------------------ |
| 의사    | ✅    | `doctor/chatbot.mustache`       | `DoctorChatbotController`      |
| 간호사   | ✅    | `nurse/chatbot.mustache`        | `NurseChatbotController`       |
| 원무과   | ✅    | `staff/chatbot.mustache`        | `StaffChatbotController`       |
| 물품담당자 | ✅    | `item-manager/chatbot.mustache` | `ItemManagerChatbotController` |
| 관리자   | ✅    | `admin/chatbot.mustache`        | `AdminChatbotController`       |


