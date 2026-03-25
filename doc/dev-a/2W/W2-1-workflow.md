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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 사용자가 브라우저에서 `/reservation/direct-reservation` 주소로 접속하면 이 함수가 실행됩니다. 페이지 제목을 "직접 선택 예약"으로 설정하고, 예약 폼 화면(`direct-reservation.mustache`)을 보여줍니다.
> - **왜 이렇게 썼는지**: `@GetMapping`은 "이 URL로 GET 요청(단순 조회)이 오면 이 함수를 실행해"라는 표시입니다. `HttpServletRequest`는 이 프로젝트의 컨트롤러 규칙으로 사용하는 요청 정보 객체이며, `setAttribute`로 페이지에 데이터를 전달합니다.
> - **쉽게 말하면**: 손님(브라우저)이 특정 주소를 입력하면, 서버가 알맞은 예약 폼 페이지를 찾아서 보여주는 안내원 역할입니다.

### 2. direct-reservation.mustache — form 태그

```html
<form method="POST" action="/reservation/create" class="space-y-6">
  {{! CSRF 토큰 }}
  <input type="hidden" name="{{_csrf.parameterName}}" value="{{_csrf.token}}">
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 폼 제출 시 데이터를 서버의 `/reservation/create` 주소로 POST 방식으로 전송합니다. CSRF 토큰은 보안을 위한 숨겨진 인증 값입니다.
> - **왜 이렇게 썼는지**: `method="POST"`는 데이터를 서버에 전송하는 방식입니다. CSRF(Cross-Site Request Forgery) 토큰은 악의적인 사이트에서 사용자 몰래 요청을 보내는 공격을 막기 위해 Spring Security가 요구하는 보안 값입니다.
> - **쉽게 말하면**: 편지 봉투(form)에 받는 사람 주소(`/reservation/create`)를 적고, 위조 방지 스티커(CSRF 토큰)를 붙여서 보내는 것과 같습니다.

### 3. direct-reservation.mustache — name 속성

```html
<input type="text" id="name" name="name" required />
<input type="tel"  id="phone" name="phone" required />
<select id="department" name="departmentId" required>...</select>
<select id="doctor"     name="doctorId"     required>...</select>
<input type="text" id="date" name="reservationDate" required readonly />
<select id="time"  name="timeSlot" required>...</select>
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 폼의 각 입력 필드에 `name` 속성을 부여합니다. 폼이 제출될 때 서버는 이 `name` 값을 기준으로 어떤 데이터인지 구분합니다.
> - **왜 이렇게 썼는지**: `name` 속성이 없으면 서버에 데이터가 전달되지 않습니다. `name="departmentId"`처럼 서버의 Java 클래스 필드명과 일치시켜야 Spring이 자동으로 값을 바인딩(연결)해줍니다.
> - **쉽게 말하면**: 택배 박스 안의 물건마다 이름표(`name`)를 붙여두어야 서버가 "이건 환자 이름이고, 저건 전화번호구나"하고 구분할 수 있습니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 이전에 있던 JS 폼 제출 핸들러를 제거하고, AI 추천 기능(URL 파라미터로 진료과/의사 자동 선택)만 남깁니다. `feather.replace()`는 아이콘을 렌더링하는 함수입니다.
> - **왜 이렇게 썼는지**: 폼 제출을 JS로 처리하던 방식에서 서버(Spring MVC)가 직접 처리하는 방식으로 전환했기 때문에, 기존 JS submit 핸들러는 필요 없어졌습니다. URL에서 `dept`나 `doctor` 파라미터를 읽어 자동으로 선택해주는 AI 추천 기능은 별도로 유지합니다.
> - **쉽게 말하면**: 직접 배달하던 택배기사를 해고하고 운송 회사(서버)에 맡겼으니, 배달 차 운전만 제거하면 됩니다. AI 추천 표시판은 그대로 유지합니다.

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
