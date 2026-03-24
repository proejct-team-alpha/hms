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

> **💡 입문자 설명**
>
> **API URL 변경 (`/ask` → `/query`) — 왜 맞지 않았는지**
> - JavaScript에서 `fetch('/llm/chatbot/ask')`로 요청을 보냈지만, 서버(Spring) 컨트롤러는 `/llm/chatbot/query`에만 응답하도록 등록되어 있었습니다. 주소가 틀리면 404(찾을 수 없음) 오류가 발생합니다.
> - **왜 이런 불일치가 생겼는지**: 서버 API가 개발 중에 이름이 바뀌었는데 프론트엔드 코드가 업데이트되지 않았기 때문입니다. 팀 개발에서 흔히 발생하는 동기화 문제입니다.
>
> **Request body 변경 (`message` → `query`) — 왜 필드명이 중요한지**
> - `{ message: message }`를 보내면 서버는 `query`라는 이름의 필드를 기대하므로 값을 못 찾습니다. Spring의 `@RequestBody`는 JSON 필드명과 Java 클래스 필드명이 정확히 일치해야 자동으로 매핑됩니다.
> - **쉽게 말하면**: 택배 수령란에 "받는 사람" 대신 "수취인"으로 써서 못 받는 것과 같습니다. 이름이 다르면 연결이 안 됩니다.
>
> **응답 파싱 변경 (`res.json()` → `res.text()`) — 왜 바꿨는지**
> - `res.json()`은 응답이 JSON 형식(`{"answer": "..."}`)일 때 씁니다. 하지만 서버가 챗봇 응답을 순수 텍스트 문자열로 반환하도록 변경되어, `res.text()`로 그대로 읽어야 합니다. JSON을 기대하는데 텍스트가 오면 파싱 오류가 납니다.
>
> **챗봇을 전 역할로 확장한 이유**
> - 의사와 간호사만 AI 챗봇을 쓸 수 있다면 원무과·관리자·물품담당자는 이 기능을 활용하지 못합니다. 동일한 컨트롤러-서비스-템플릿 구조를 역할별로 복제하면, 역할마다 독립적인 라우팅(`/staff/chatbot`, `/admin/chatbot`)과 사이드바 활성화 처리가 가능합니다.


---

## 챗봇 현황


| 역할    | 사이드바 | 페이지                             | 컨트롤러                           |
| ----- | ---- | ------------------------------- | ------------------------------ |
| 의사    | ✅    | `doctor/chatbot.mustache`       | `DoctorChatbotController`      |
| 간호사   | ✅    | `nurse/chatbot.mustache`        | `NurseChatbotController`       |
| 원무과   | ✅    | `staff/chatbot.mustache`        | `StaffChatbotController`       |
| 물품담당자 | ✅    | `item-manager/chatbot.mustache` | `ItemManagerChatbotController` |
| 관리자   | ✅    | `admin/chatbot.mustache`        | `AdminChatbotController`       |


