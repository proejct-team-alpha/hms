# 개발자 A — 작업 리포트

---

## 01. 예약방법 선택 화면 URL 연결 (2026-03-09)

### 작업 내용

`GET /reservation` 접속 시 `patient-choice.mustache`가 렌더링되도록 컨트롤러 매핑을 추가하고,
화면 내 버튼의 `.html` 직접 참조를 Spring MVC URL로 교체했다.

### 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `ReservationController.java` | `@GetMapping("")` 추가 → `GET /reservation` 매핑 |
| `patient-choice.mustache` | 버튼 3개 href 교체 |

### 상세 변경 사항

**ReservationController.java**
- 삭제: `@GetMapping("/dashboard")`, `@GetMapping("/index")` (미사용 매핑 제거)
- 추가: `@GetMapping("")` → `reservation/patient-choice` 반환
- 추가: `jakarta.servlet.http.HttpServletRequest` import

**patient-choice.mustache**
```
변경 전                              변경 후
---------------------------------   ---------------------------------
'../index.html'               →     '/'
'symptom-reservation.html'    →     '/reservation/symptom-reservation'
'direct-reservation.html'     →     '/reservation/direct-reservation'
```

### 수용 기준 확인

- [x] `GET /reservation` 접속 시 patient-choice 화면 렌더링
- [x] "AI 증상 추천 예약" 버튼 → `/reservation/symptom-reservation` 이동
- [x] "직접 선택 예약" 버튼 → `/reservation/direct-reservation` 이동
- [x] "처음으로" 버튼 → `/` 이동

### 참조 문서

- `doc/dev-a/01-workflow-patient-choice-connection.md`
- `doc/dev-a/A-project.md`

---

## W2-1. 예약 폼 화면 구현 (S03 직접 예약) (2026-03-09)

### 작업 내용

`direct-reservation.mustache`의 `<form>`을 Spring MVC 방식으로 교체하고,
모든 입력 필드에 `name` 속성을 추가했다. JS submit 핸들러(`.html` 이동)를 제거하고 CSRF 토큰을 삽입했다.

### 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `ReservationController.java` | `directReservation()` 파라미터 `Model` → `HttpServletRequest` |
| `direct-reservation.mustache` | form 태그 method/action 추가, name 속성 추가, CSRF 삽입, JS submit 핸들러 제거 |

### 상세 변경 사항

**ReservationController.java**
```
변경 전: public String directReservation(Model model)
변경 후: public String directReservation(HttpServletRequest request)
         request.setAttribute("pageTitle", "직접 선택 예약");
```

**direct-reservation.mustache**

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

### 수용 기준 확인

- [x] `GET /reservation/direct-reservation` 접속 시 폼 화면 정상 렌더링
- [x] 모든 입력 필드에 `name` 속성 존재
- [x] `<form>` 태그에 `method="POST" action="/reservation/create"` 적용
- [x] CSRF hidden input 존재
- [x] 기존 JS submit 핸들러 제거됨
- [x] AI 추천 정보 URL 파라미터 표시 유지

### 참조 문서

- `doc/dev-a/W2-1-workflow.md`
