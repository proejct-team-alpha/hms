# W2-1. 예약 폼 화면 구현 (S03 직접 예약) — 작업 리포트

**작업일**: 2026-03-09

---

## 작업 내용

`direct-reservation.mustache`의 `<form>`을 Spring MVC 방식으로 교체하고,
모든 입력 필드에 `name` 속성을 추가했다. JS submit 핸들러(`.html` 이동)를 제거하고 CSRF 토큰을 삽입했다.

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `ReservationController.java` | `directReservation()` 파라미터 `Model` → `HttpServletRequest` |
| `direct-reservation.mustache` | form 태그 method/action 추가, name 속성 추가, CSRF 삽입, JS submit 핸들러 제거 |

---

## 상세 변경 사항

### ReservationController.java

```
변경 전: public String directReservation(Model model)
변경 후: public String directReservation(HttpServletRequest request)
         request.setAttribute("pageTitle", "직접 선택 예약");
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 사용자가 예약 폼 주소(`/reservation/direct-reservation`)에 접속했을 때 실행되는 함수입니다. 페이지 제목을 "직접 선택 예약"으로 설정하고 예약 폼 화면을 반환합니다.
> - **왜 `Model` 대신 `HttpServletRequest`를 쓰는지**: 둘 다 뷰(화면)에 데이터를 전달하는 역할을 하지만, 이 프로젝트는 `HttpServletRequest`를 GET 컨트롤러 표준 파라미터로 정했습니다. `setAttribute("pageTitle", "직접 선택 예약")`은 Mustache 템플릿에서 `{{pageTitle}}`로 꺼내 쓸 수 있는 값을 설정하는 것입니다.
> - **다른 방법은 없는지**: `Model model`을 쓰고 `model.addAttribute("pageTitle", "...")` 로 해도 결과는 동일합니다. 이 프로젝트에서는 일관성을 위해 `HttpServletRequest` 방식을 통일 규칙으로 정한 것입니다.
> - **쉽게 말하면**: 손님(브라우저)이 예약 페이지 주소를 입력하면, 서버가 "페이지 제목은 이거야"라는 정보를 담아 예약 폼 화면을 찾아서 보내주는 안내원 역할입니다.

---

### direct-reservation.mustache

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| form 태그 | `<form id="reservation-form">` | `method="POST" action="/reservation/create"` 추가 |
| CSRF | 없음 | `<input type="hidden" name="{{_csrf.parameterName}}" value="{{_csrf.token}}">` 추가 |
| name input | `name` 속성 없음 | `name="name"` 추가 |
| phone input | `name` 속성 없음 | `name="phone"` 추가 |
| department select | `name` 속성 없음 | `name="departmentId"` 추가 |
| doctor select | `name` 속성 없음 | `name="doctorId"` 추가 |
| date input | `name` 속성 없음 | `name="reservationDate"` 추가 |
| time select | `name` 속성 없음 | `name="timeSlot"` 추가 |
| JS submit 핸들러 | `.html` 이동 로직 존재 | 제거 (AI 추천 URL 파라미터 로직은 유지) |

> **💡 입문자 설명**
>
> **`method="POST" action="/reservation/create"` — 왜 추가했는지**
> - 이전에는 JavaScript가 폼 제출을 가로채서 직접 서버로 요청을 보냈습니다. 이 방식은 JS가 없으면 동작하지 않고 유지보수가 어렵습니다.
> - `method="POST"`는 "이 폼의 데이터를 서버에 전송하는 방식"을 정하는 것이고, `action="/reservation/create"`는 "어디로 보낼지" 주소를 지정합니다. 이제 서버(Spring MVC)가 폼 처리를 담당합니다.
> - **다른 방법**: `method="GET"`도 있지만, 개인 정보(이름, 전화번호)를 URL에 노출하지 않으려면 반드시 `POST`를 써야 합니다.
>
> **CSRF 토큰 — 왜 필요한지**
> - CSRF(Cross-Site Request Forgery)는 악의적인 사이트가 사용자 몰래 이 폼을 제출하는 공격입니다.
> - Spring Security는 모든 POST 요청에 이 토큰을 요구합니다. 토큰이 없으면 요청이 거부됩니다. `{{_csrf.parameterName}}`과 `{{_csrf.token}}`은 Mustache가 서버에서 발급한 실제 토큰 값으로 채워줍니다.
> - **다른 방법**: API 방식(REST + JSON)으로 전환하면 헤더(`X-CSRF-Token`)로 처리할 수도 있지만, 일반 HTML 폼에서는 hidden input이 표준입니다.
>
> **`name` 속성 — 왜 없으면 안 되는지**
> - 폼이 제출될 때 브라우저는 `name` 속성이 있는 필드만 서버로 전송합니다. `name`이 없으면 데이터가 아예 서버에 도달하지 않습니다.
> - `name="departmentId"`처럼 서버의 Java 폼 객체 필드명과 정확히 일치해야 Spring이 자동으로 값을 매핑해줍니다.
>
> **JS submit 핸들러 제거 — 왜 제거했는지**
> - 기존 JS 핸들러는 폼 제출을 가로채서 직접 서버에 요청하는 역할이었습니다. 이제 `<form method="POST">`가 그 역할을 대신하므로 핸들러가 남아 있으면 중복 제출이 발생합니다.
> - AI 추천 URL 파라미터 로직(진료과/의사 자동 선택)은 폼 제출과 무관한 기능이므로 유지했습니다.

---

## 수용 기준 확인

- [x] `GET /reservation/direct-reservation` 접속 시 폼 화면 정상 렌더링
- [x] 모든 입력 필드에 `name` 속성 존재
- [x] `<form>` 태그에 `method="POST" action="/reservation/create"` 적용
- [x] CSRF hidden input 존재
- [x] 기존 JS submit 핸들러 제거됨
- [x] AI 추천 정보 URL 파라미터 표시 유지

---

## 참조 문서

- `doc/dev-a/W2-1-workflow.md`
