# W2-1 Workflow — 예약 폼 화면 Spring MVC 연결

> **작성일**: 2W
> **목표**: `direct-reservation.mustache` 폼을 Spring MVC `POST /reservation/create`로 교체

---

## 전체 흐름

```
JS only 폼 → Spring MVC POST 폼으로 전환
  - form action, name 속성, CSRF 추가
  - JS submit 핸들러 제거
  - 컨트롤러 파라미터 HttpServletRequest 적용
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | JS only 폼 → Spring MVC POST 폼 전환 |
| 컨트롤러 파라미터 | `Model` → `HttpServletRequest` (프로젝트 규칙) |
| CSRF | hidden input 추가 필요 |
| 진료과/의사 동적 로딩 | W2-2에서 처리 |
| 시간 슬롯 동적 처리 | W2-3에서 처리 |

---

## 실행 흐름

```
브라우저: GET /reservation/direct-reservation
    ↓
ReservationController.directReservation(HttpServletRequest)
  request.setAttribute("pageTitle", "직접 선택 예약")
    ↓
direct-reservation.mustache 렌더링
  ├── 환자 성함 input[name="name"]
  ├── 연락처   input[name="phone"]
  ├── 진료과   select[name="departmentId"]
  ├── 전문의   select[name="doctorId"]
  ├── 예약날짜 input[name="reservationDate"]
  ├── 예약시간 select[name="timeSlot"]
  └── CSRF hidden input
    ↓ 제출
POST /reservation/create  ← W2-4에서 구현
```

---

## UI Mockup

```
┌─────────────────────────────────┐
│  직접 선택 예약                  │
├─────────────────────────────────┤
│ 환자 성함   [__________________] │  name="name"
│ 연락처      [__________________] │  name="phone"
│ 진료과      [내과 ▼            ] │  name="departmentId"
│ 전문의      [선택해주세요 ▼    ] │  name="doctorId"
│ 예약 날짜   [__________________] │  name="reservationDate"
│ 예약 시간   [09:00 ▼           ] │  name="timeSlot"
│ [hidden: _csrf]                  │
│         [예약하기]               │  POST /reservation/create
└─────────────────────────────────┘
```

---

## 작업 목록

1. `ReservationController.java` — `directReservation()` 파라미터 `Model` → `HttpServletRequest` 변경
2. `direct-reservation.mustache` — `<form>` 태그 `method="POST" action="/reservation/create"` 추가
3. `direct-reservation.mustache` — 모든 input/select에 `name` 속성 추가
4. `direct-reservation.mustache` — CSRF hidden input 추가
5. `direct-reservation.mustache` — JS 폼 submit 핸들러 제거

---

## 작업 진행내용

- [x] `ReservationController.java` 파라미터 변경
- [x] `<form>` 태그 method/action 설정
- [x] input/select name 속성 추가
- [x] CSRF hidden input 추가
- [x] JS submit 핸들러 제거

---

## 실행 흐름에 대한 코드

### 1. ReservationController.java

```java
@GetMapping("/direct-reservation")
public String directReservation(HttpServletRequest request) {
    request.setAttribute("pageTitle", "직접 선택 예약");
    return "reservation/direct-reservation";
}
```

### 2. direct-reservation.mustache — form 태그

```html
<form method="POST" action="/reservation/create" class="space-y-6">
  {{! CSRF 토큰 }}
  <input type="hidden" name="{{_csrf.parameterName}}" value="{{_csrf.token}}">
```

### 3. direct-reservation.mustache — name 속성

```html
<input type="text" id="name" name="name" required />
<input type="tel"  id="phone" name="phone" required />
<select id="department" name="departmentId" required>...</select>
<select id="doctor"     name="doctorId"     required>...</select>
<input type="text" id="date" name="reservationDate" required readonly />
<select id="time"  name="timeSlot" required>...</select>
```

### 4. direct-reservation.mustache — JS 정리

```javascript
// 제거: form submit 핸들러 (서버 POST로 대체)
// 유지: AI 추천 URL 파라미터 처리
feather.replace();
const urlParams = new URLSearchParams(window.location.search);
const recDept   = urlParams.get('dept');
const recDoctor = urlParams.get('doctor');
if (recDept && recDoctor) { ... }
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 폼 렌더링 | GET /reservation/direct-reservation | 폼 화면 정상 표시 |
| name 속성 | 모든 input/select 확인 | name 속성 존재 |
| CSRF | form 내부 확인 | hidden input 존재 |
| JS 핸들러 제거 | submit 이벤트 | preventDefault 없음 |

---

## 완료 기준

- [x] `GET /reservation/direct-reservation` 폼 화면 정상 렌더링
- [x] 모든 입력 필드 `name` 속성 존재
- [x] `<form>` 태그 `method="POST" action="/reservation/create"` 적용
- [x] CSRF hidden input 존재
- [x] 기존 JS submit 핸들러 제거
