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
