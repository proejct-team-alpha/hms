# 개발자 A (강태오) — 화면 동작 원리 & 구성 정리

> 담당 영역: 비회원 예약 흐름 (`/`, `/reservation/**`)
> 최종 업데이트: 2026-03-24

---

## 1. 전체 화면 흐름

```
[홈 메인]
GET /
→ home/index.mustache
  ├── "환자용" 버튼 → GET /reservation          (S00 예약 방법 선택)
  └── "직원용" 버튼 → GET /login

[S00] 예약 방법 선택
GET /reservation
→ reservation/patient-choice.mustache
  ├── "AI 증상 추천 예약" 버튼 → GET /reservation/symptom-reservation  (S01)
  └── "직접 선택 예약" 버튼   → GET /reservation/direct-reservation   (S03)

[S01] 증상 입력 + AI 분석
GET /reservation/symptom-reservation
→ reservation/symptom-reservation.mustache
  ├── 증상 입력 textarea
  ├── "증상 분석하기" 버튼 → POST /llm/symptom/analyze (AJAX)  ← S02
  └── AI 추천 결과 div (숨김 → 분석 완료 후 표시)
        └── "이 정보로 예약 진행하기" → GET /reservation/direct-reservation?dept=&doctor=

[S02] LLM 추천 결과 (별도 페이지 없음, S01 내 AJAX 영역)
POST /llm/symptom/analyze
→ JSON 응답 { dept, doctor, time }
→ S01 화면 내 추천 결과 div를 동적으로 표시

[S03] 직접 예약 폼
GET /reservation/direct-reservation
→ reservation/direct-reservation.mustache
  ├── URL 파라미터(?dept=&doctor=)로 AI 추천 정보 자동 입력 가능
  ├── 진료과 선택 → AJAX → GET /api/reservation/doctors?departmentId=
  ├── 의사 + 날짜 선택 → AJAX → GET /api/reservation/booked-slots?doctorId=&date=
  └── "예약 완료하기" 버튼 → POST /reservation/create

[S04] 예약 완료
GET /reservation/complete
→ reservation/reservation-complete.mustache
  ├── flash attribute "info"(ReservationCompleteInfo) 로 데이터 수신
  ├── {{#info}} 예약번호·환자명·진료과/의사·날짜·시간 표시
  ├── {{^info}} 직접 URL 접근 시 오류 화면 표시
  └── "예약 조회" 링크 → GET /reservation/lookup?reservationNumber=

[S05] 예약 조회
GET /reservation/lookup
→ reservation/reservation-lookup.mustache
  ├── 예약번호 단건 조회 → reservation 단건 표시
  ├── 이름 + 전화번호 목록 조회 → reservations 목록 표시
  ├── "예약 취소" 링크 → GET /reservation/cancel?reservationNumber=
  └── "예약 변경" 링크 → GET /reservation/modify?reservationNumber=

[S06] 예약 취소 확인 화면
GET /reservation/cancel?reservationNumber=
→ reservation/reservation-cancel.mustache
  ├── 예약번호로 예약 정보 조회 → reservation 표시
  └── "취소 확인" 버튼 → POST /reservation/cancel/{id}

[S07] 예약 취소 처리
POST /reservation/cancel/{id}
→ 성공: flash attribute "info" → redirect:/reservation/cancel-complete
→ 실패: 소유권 검증 실패 시 취소 화면 재표시 (errorMessage)

[S08] 예약 취소 완료
GET /reservation/cancel-complete
→ reservation/reservation-cancel-complete.mustache
  ├── flash attribute "info"(ReservationCompleteInfo) 로 데이터 수신
  └── {{#info}} 취소된 예약 정보 표시 / {{^info}} 직접 접근 시 오류 화면

[S09] 예약 변경 폼 화면
GET /reservation/modify?reservationNumber=
→ reservation/reservation-modify.mustache
  ├── 예약번호로 예약 정보 조회 → 기존 값 pre-fill
  ├── 의사 + 날짜 선택 → AJAX → GET /api/reservation/booked-slots?doctorId=&date=&excludeId=
  └── "변경 완료" 버튼 → POST /reservation/modify/{id}

[S10] 예약 변경 처리
POST /reservation/modify/{id}
→ 성공: flash attribute "info" → redirect:/reservation/modify-complete
→ 실패(@Valid): 유효성 오류 시 변경 폼 재표시 (기존 예약 정보 + errorMessage)
→ 실패(Exception): 소유권 검증 실패·중복 시간대 시 변경 폼 재표시

[S11] 예약 변경 완료
GET /reservation/modify-complete
→ reservation/reservation-modify-complete.mustache
  ├── flash attribute "info"(ReservationCompleteInfo) 로 데이터 수신
  └── {{#info}} 새 예약 정보 표시 / {{^info}} 직접 접근 시 오류 화면
```

---

## 2. 레이아웃 구조 (L1)

비회원 화면은 모두 **L1 레이아웃**을 사용한다. 사이드바 없음.

```html
<body class="min-h-screen flex flex-col">
  {{> common/header-public}}
  <!--
    헤더: MediCare+ 로고(왼쪽) + 직원 로그인 링크(오른쪽)
    파일: templates/common/header-public.mustache
  -->

  <main class="flex-1 ...">
    <!-- 각 화면의 콘텐츠 -->
  </main>

  {{> common/footer-public}}
  <!--
    푸터: 병원 주소 / 연락처 / 저작권
    파일: templates/common/footer-public.mustache
  -->

  <script>
    feather.replace();
  </script>
  <!-- 아이콘 초기화 — 모든 페이지 필수 -->
</body>
```

> **주의**: 비회원 화면에서 `header-staff`, `sidebar-*` 파셜을 절대 사용하지 않는다.

---

## 3. 파일 구조

```
src/main/
├── java/com/smartclinic/hms/
│   ├── home/
│   │   └── HomeController.java                    ← GET /
│   └── reservation/reservation/
│       ├── ReservationController.java             ← GET/POST /reservation/**
│       ├── ReservationApiController.java          ← REST /api/reservation/**
│       ├── ReservationService.java                ← 예약 비즈니스 로직
│       ├── ReservationRepository.java             ← 예약 DB 접근
│       ├── PatientRepository.java                 ← 환자 findOrCreate
│       ├── DepartmentRepository.java              ← 진료과 DB 접근
│       ├── CreateReservationRequest.java          ← 예약 생성 요청 DTO (@Valid)
│       ├── UpdateReservationRequest.java          ← 예약 변경 요청 DTO (@Valid)
│       ├── ReservationCompleteInfo.java           ← 완료/취소/변경 완료 화면 DTO
│       ├── ReservationInfoDto.java                ← 예약 조회/취소/변경 폼 화면 DTO
│       └── DepartmentDto.java                     ← 진료과 드롭다운 DTO
│
└── resources/
    └── templates/reservation/
        ├── patient-choice.mustache                ← S00 예약 방법 선택
        ├── symptom-reservation.mustache           ← S01 증상 입력 + AI 분석
        ├── direct-reservation.mustache            ← S03 직접 예약 폼
        ├── reservation-complete.mustache          ← S04 예약 완료
        ├── reservation-lookup.mustache            ← S05 예약 조회
        ├── reservation-cancel.mustache            ← S06 예약 취소 확인
        ├── reservation-cancel-complete.mustache   ← S08 예약 취소 완료
        ├── reservation-modify.mustache            ← S09 예약 변경 폼
        └── reservation-modify-complete.mustache   ← S11 예약 변경 완료
```

---

## 4. 컨트롤러 라우팅

### HomeController.java

| 메서드 | URL | 뷰 경로      | 설명    |
| ------ | --- | ------------ | ------- |
| GET    | `/` | `home/index` | 홈 메인 |

### ReservationController.java (`@RequestMapping("/reservation")`)

| 메서드 | URL                        | 뷰 경로 / 리다이렉트                    | 화면               |
| ------ | -------------------------- | --------------------------------------- | ------------------ |
| GET    | `/reservation`             | `reservation/patient-choice`            | S00                |
| GET    | `/reservation/symptom-reservation` | `reservation/symptom-reservation` | S01              |
| GET    | `/reservation/direct-reservation`  | `reservation/direct-reservation`  | S03              |
| POST   | `/reservation/create`      | `redirect:/reservation/complete`        | S03 → S04          |
| GET    | `/reservation/complete`    | `reservation/reservation-complete`      | S04                |
| GET    | `/reservation/lookup`      | `reservation/reservation-lookup`        | S05                |
| GET    | `/reservation/cancel`      | `reservation/reservation-cancel`        | S06                |
| POST   | `/reservation/cancel/{id}` | `redirect:/reservation/cancel-complete` | S06 → S08          |
| GET    | `/reservation/cancel-complete` | `reservation/reservation-cancel-complete` | S08           |
| GET    | `/reservation/modify`      | `reservation/reservation-modify`        | S09                |
| POST   | `/reservation/modify/{id}` | `redirect:/reservation/modify-complete` | S09 → S11          |
| GET    | `/reservation/modify-complete` | `reservation/reservation-modify-complete` | S11           |

### ReservationApiController.java (`@RequestMapping("/api/reservation")`)

| 메서드 | URL                                                       | 설명                                          |
| ------ | --------------------------------------------------------- | --------------------------------------------- |
| GET    | `/api/reservation/departments`                            | 진료과 전체 목록 (예약 폼 드롭다운 초기 로드) |
| GET    | `/api/reservation/doctors?departmentId=`                  | 진료과별 의사 목록 (AJAX 드롭다운 갱신)       |
| GET    | `/api/reservation/booked-slots?doctorId=&date=`           | 예약된 시간 슬롯 목록 (Flatpickr 비활성화용)  |
| GET    | `/api/reservation/booked-slots?doctorId=&date=&excludeId=`| 위와 동일, 변경 중인 예약 자신 제외           |

### LLM 엔드포인트 (담당 UI 연동)

| 메서드 | URL                     | 설명                                       |
| ------ | ----------------------- | ------------------------------------------ |
| POST   | `/llm/symptom/analyze`  | 증상 텍스트 → 진료과·의사·시간 추천 반환   |

---

## 5. 화면별 동작 원리

### 홈 메인 (`home/index.mustache`)

- 정적 화면, 서버 데이터 없음
- "환자용" → `/reservation`
- "직원용" → `/login`

---

### S00 — 예약 방법 선택 (`patient-choice.mustache`)

- 정적 화면, 서버 데이터 없음
- "AI 증상 추천 예약" → `/reservation/symptom-reservation`
- "직접 선택 예약" → `/reservation/direct-reservation`
- 뒤로가기 → `/`

---

### S01 — 증상 입력 + AI 분석 (`symptom-reservation.mustache`)

**동작 순서:**

1. textarea에 증상 텍스트 입력
2. "증상 분석하기" 버튼 클릭 → 로딩 표시
3. `POST /llm/symptom/analyze` AJAX 호출 (CSRF 메타 태그 방식)
4. 응답 수신 → 추천 결과 div 표시 (진료과 / 의사 / 시간)
5. "이 정보로 예약 진행하기" → `/reservation/direct-reservation?dept=X&doctor=Y&time=Z`

**AJAX 구조:**

```javascript
const csrfToken  = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

const res = await fetch('/llm/symptom/analyze', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    [csrfHeader]: csrfToken
  },
  body: JSON.stringify({ symptomText: symptomInput.value })
});
const data = await res.json();
// data.dept, data.doctor, data.time
```

**LLM 응답 구조:**

```json
{ "dept": "내과", "doctor": "이영희", "time": "09:00" }
```

---

### S03 — 직접 예약 폼 (`direct-reservation.mustache`)

**서버 데이터:**
- `departments`: `DepartmentDto` 리스트 (진료과 드롭다운 초기값)

**동적 연동 순서:**

1. 페이지 진입 시 URL 파라미터(`?dept=&doctor=&time=`) 확인 → AI 추천 정보 있으면 자동 입력
2. 진료과 선택 → `GET /api/reservation/doctors?departmentId={id}` → 의사 `<select>` 동적 갱신
3. 의사 + 날짜 선택 → `GET /api/reservation/booked-slots?doctorId={id}&date={date}` → Flatpickr 비활성화 슬롯 갱신
4. 폼 제출 → `POST /reservation/create`

**유효성 검증 실패 시:**
- `bindingResult.hasErrors()` → 폼 재표시 + `errorMessage` + `departments` 재전달

**비즈니스 예외 시 (중복 예약 등):**
- `CustomException` catch → 폼 재표시 + `errorMessage` + `departments` 재전달

---

### S04 — 예약 완료 (`reservation-complete.mustache`)

**데이터 수신 방식: flash attribute**
- `POST /reservation/create` 성공 → `addFlashAttribute("info", ReservationCompleteInfo)` → `redirect:/reservation/complete`
- Spring MVC가 redirect 후 flash attribute를 자동으로 모델에 병합

**표시 항목:** 예약번호(RES-YYYYMMDD-NNN) · 환자명 · 진료과/의사 · 예약 날짜 · 예약 시간

**직접 URL 접근 시:** `{{^info}}` 오류 화면("예약 정보가 없습니다.")

---

### S05 — 예약 조회 (`reservation-lookup.mustache`)

**두 가지 조회 방식:**

| 조건 | 결과 |
|------|------|
| `reservationNumber` 파라미터 있음 | 단건 조회 → `reservation` 속성 |
| `name` + `phone` 파라미터 있음 | 목록 조회 → `reservations` 속성 |
| 조회 결과 없음 | `errorMessage` 표시 |

---

### S06 — 예약 취소 확인 (`reservation-cancel.mustache`)

**서버 데이터:**
- `reservationNumber` 파라미터 → `findByReservationNumber()` → `reservation` 속성

**취소 폼:**
- `POST /reservation/cancel/{id}` + `phone` 파라미터 (소유권 검증용)

---

### S08 — 예약 취소 완료 (`reservation-cancel-complete.mustache`)

- flash attribute `"info"` 수신 방식 (S04와 동일 구조)
- 취소된 예약 정보 표시

---

### S09 — 예약 변경 폼 (`reservation-modify.mustache`)

**서버 데이터:**
- `reservationNumber` 파라미터 → `findByReservationNumber()` → `reservation` 속성 (기존 값 pre-fill)

**동적 연동:**
- 의사 + 날짜 선택 → `GET /api/reservation/booked-slots?doctorId=&date=&excludeId={현재예약id}` → 자신 슬롯 제외하고 Flatpickr 갱신

**유효성/비즈니스 예외 시:**
- 폼 재표시 + `errorMessage` + `findById(id)` 로 기존 예약 정보 재전달

---

### S11 — 예약 변경 완료 (`reservation-modify-complete.mustache`)

- flash attribute `"info"` 수신 방식 (S04와 동일 구조)
- 변경된 새 예약 정보 표시

---

## 6. DTO 구조

| DTO | 용도 |
|-----|------|
| `CreateReservationRequest` | POST /reservation/create 요청 바인딩 + @Valid 검증 |
| `UpdateReservationRequest` | POST /reservation/modify/{id} 요청 바인딩 + @Valid 검증 |
| `ReservationCompleteInfo` | 완료/취소완료/변경완료 화면 flash attribute 전달용 |
| `ReservationInfoDto` | 예약 조회/취소확인/변경폼 화면 표시용 |
| `DepartmentDto` | 진료과 드롭다운 옵션용 |

---

## 7. 코딩 규칙 요약

| 항목 | 규칙 |
| ---- | ---- |
| 컨트롤러 GET | `HttpServletRequest.setAttribute()` 후 뷰 경로 반환 |
| 컨트롤러 POST 성공 | `addFlashAttribute("info", dto)` → `redirect:` (PRG 패턴) |
| 컨트롤러 POST 실패 | 폼 뷰 경로 재반환 (errorMessage + 필요한 데이터 재설정) |
| REST API 응답 | `Resp.ok(data)` — `ResponseEntity<Resp<T>>` 형태 |
| DTO | Java Record, `@Valid` 검증 필수 |
| JS 변수 | `const` 우선, `var` 금지 |
| JS 비동기 | `async/await` 중심 |
| CSRF (폼) | `<input type="hidden" name="{{_csrf.parameterName}}" value="{{_csrf.token}}">` |
| CSRF (AJAX) | `<meta name="_csrf">` + `<meta name="_csrf_header">` 메타 태그 방식 |

---

## 8. 절대 터치 금지 영역

| 파일/디렉터리                         | 접근 수준          |
| ------------------------------------- | ------------------ |
| `config/`                             | 수정 금지          |
| `domain/`                             | 수정 금지          |
| `common/service/SlotService.java`     | 수정 금지          |
| `llm/LlmService.java`                 | 수정 금지          |
| `templates/common/**`                 | 수정 금지          |
| `staff/**`, `doctor/**`, `nurse/**`   | 접근 금지          |
| `admin/**`                            | 접근 금지          |
