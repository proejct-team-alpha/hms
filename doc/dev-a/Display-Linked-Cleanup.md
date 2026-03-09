# 개발자 A (강태오) — 화면 동작 원리 & 구성 정리

> 담당 영역: 비회원 예약 흐름 (`/`, `/reservation/**`)
> 기준 문서: SKILL_DEV_A.md / PRD.md / PROJECT_STRUCTURE.md

---

## 1. 전체 화면 흐름

```
[홈 메인]
GET /
→ home/index.mustache
  ├── "환자용" 버튼 → GET /reservation/index   (S00 예약 방법 선택)
  └── "직원용" 버튼 → GET /login

[S00] 예약 방법 선택
GET /reservation/index
→ reservation/patient-choice.mustache
  ├── "AI 증상 추천 예약" 버튼 → GET /reservation/symptom-reservation   (S01)
  └── "직접 선택 예약" 버튼   → GET /reservation/direct-reservation    (S03)

[S01] 증상 입력 + AI 분석
GET /reservation/symptom-reservation
→ reservation/symptom-reservation.mustache
  ├── 증상 입력 textarea
  ├── "증상 분석하기" 버튼 → POST /llm/symptom/analyze (AJAX)  ← S02
  └── AI 추천 결과 div (숨김 → 분석 완료 후 표시)
        └── "이 정보로 예약 진행하기" → GET /reservation/direct-reservation?dept=&doctor=

[S02] LLM 추천 결과 (별도 페이지 없음, S01 내 AJAX 영역)
POST /llm/symptom/analyze
→ JSON 응답 { recommendedDept, recommendedDoctor, recommendedTime }
→ S01 화면 내 #recommendation-result div를 동적으로 표시

[S03] 직접 예약 폼
GET /reservation/direct-reservation
→ reservation/direct-reservation.mustache
  ├── URL 파라미터(?dept=&doctor=)로 AI 추천 정보 자동 입력 가능
  ├── 진료과 선택 → AJAX → GET /reservation/getDoctors?departmentId=
  ├── 의사 선택  → AJAX → GET /reservation/getSlots?doctorId=&date=
  └── "예약 완료하기" 버튼 → POST /reservation/create

[S04] 예약 완료
GET /reservation/complete
→ reservation/reservation-complete.mustache
  └── 예약번호(RES-YYYYMMDD-NNN), 의사, 날짜/시간 표시
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
│   │   └── HomeController.java                ← GET /
│   └── reservation/reservation/
│       ├── ReservationController.java          ← GET /reservation/**
│       ├── ReservationService.java             ← 예약 생성 비즈니스 로직
│       ├── ReservationRepository.java          ← 예약 DB 접근
│       ├── PatientRepository.java              ← 환자 find Or Create
│       └── dto/                               ← DTO (Java Record 방식)
│
└── resources/
    ├── templates/
    │   ├── home/
    │   │   └── index.mustache                 ← GET /  홈 메인
    │   └── reservation/
    │       ├── patient-choice.mustache         ← S00 예약 방법 선택
    │       ├── symptom-reservation.mustache    ← S01 증상 입력 + AI 분석
    │       ├── direct-reservation.mustache     ← S03 직접 예약 폼
    │       └── reservation-complete.mustache   ← S04 예약 완료
    └── static/
        └── js/reservation/                    ← 예약 관련 JS (담당 영역)
```

---

## 4. 컨트롤러 라우팅

### HomeController.java

| 메서드 | URL | 뷰 경로      | 설명    |
| ------ | --- | ------------ | ------- |
| GET    | `/` | `home/index` | 홈 메인 |

### ReservationController.java (`@RequestMapping("/reservation")`)

| 메서드 | URL                                | 뷰 경로                            | 화면               |
| ------ | ---------------------------------- | ---------------------------------- | ------------------ |
| GET    | `/reservation/index`               | `reservation/patient-choice`       | S00                |
| GET    | `/reservation/symptom-reservation` | `reservation/symptom-reservation`  | S01                |
| GET    | `/reservation/direct-reservation`  | `reservation/direct-reservation`   | S03                |
| GET    | `/reservation/complete`            | `reservation/reservation-complete` | S04                |
| POST   | `/reservation/create`              | `redirect:/reservation/complete`   | 예약 생성 (미구현) |

### AJAX 엔드포인트 (담당)

| 메서드 | URL                                     | 설명                                       |
| ------ | --------------------------------------- | ------------------------------------------ |
| GET    | `/reservation/getDoctors?departmentId=` | 진료과별 의사 목록                         |
| GET    | `/reservation/getSlots?doctorId=&date=` | 의사별 가용 시간 슬롯                      |
| POST   | `/llm/symptom/analyze`                  | 증상 분석 (UI 연동만, 서비스는 책임개발자) |

---

## 5. 화면별 동작 원리

### 홈 메인 (`home/index.mustache`)

- 정적 화면, 서버 데이터 없음
- "환자용" → `/reservation/index` 이동
- "직원용" → `/login` 이동

---

### S00 — 예약 방법 선택 (`patient-choice.mustache`)

- 정적 화면, 서버 데이터 없음
- "AI 증상 추천 예약" → `/reservation/symptom-reservation`
- "직접 선택 예약" → `/reservation/direct-reservation`
- 뒤로가기 → `/`

---

### S01 — 증상 입력 + AI 분석 (`symptom-reservation.mustache`)

**동작 순서:**

1. textarea에 증상 텍스트 입력 (입력값 없으면 버튼 비활성화)
2. "증상 분석하기" 버튼 클릭 → 로딩 스피너 표시
3. `POST /llm/symptom/analyze` AJAX 호출 (CSRF 토큰 필수)
4. 응답 수신 → `#recommendation-result` div 표시
5. "이 정보로 예약 진행하기" → `/reservation/direct-reservation?dept=X&doctor=Y`

**AJAX 구조 (구현 예정):**

```java script
const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

const res = await fetch('/llm/symptom/analyze', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    [csrfHeader]: csrfToken   // CSRF 필수
  },
  body: JSON.stringify({ symptomText: symptomInput.value })
});
const data = await res.json();
// data.recommendedDept, data.recommendedDoctor, data.recommendedTime
```

**폴백 처리:** 5초 타임아웃 또는 API 실패 시 toast 표시 후 `/reservation/direct-reservation` 자동 이동

**면책 조항:** 추천 결과 영역에 `"AI 참고용, 의학적 진단 아님"` 문구 필수

---

### S02 — LLM 추천 결과 (S01 내 인라인 AJAX 영역)

별도 페이지 없음. `symptom-reservation.mustache` 내 `#recommendation-result` div에 동적 렌더링.

**LlmService 응답 구조:**

```json
{
  "recommendedDept": "내과",
  "recommendedDoctor": "김의사",
  "recommendedTime": "오전"
}
```

---

### S03 — 직접 예약 폼 (`direct-reservation.mustache`)

**동적 연동 순서:**

1. 페이지 진입 시 URL 파라미터 확인 → AI 추천 정보 있으면 배너 표시 + 폼 자동 입력
2. 진료과 선택 → `GET /reservation/getDoctors?departmentId={id}` → 의사 `<select>` 동적 갱신
3. 의사 + 날짜 선택 → `GET /reservation/getSlots?doctorId={id}&date={date}` → 시간 슬롯 동적 갱신
4. 폼 제출 → `POST /reservation/create`

**비즈니스 규칙:**

- 시간 슬롯: **30분 단위** (09:00 ~ 17:30) — `SlotService` 연동
- 의사 진료 가능 요일(`available_days`) 기반 날짜 제한
- 중복 예약 방지: `SlotService.validateAndLock()` 호출 (책임개발자 제공)

**폼 구조 (구현 예정):**

```html
<form method="POST" action="/reservation/create">
  <input type="hidden" name="{{_csrf.parameterName}}" value="{{_csrf.token}}" />
  <input type="hidden" name="name" ... />
  <input type="hidden" name="phone" ... />
  <input type="hidden" name="departmentId" ... />
  <input type="hidden" name="doctorId" ... />
  <input type="hidden" name="reservationDate" ... />
  <input type="hidden" name="timeSlot" ... />
</form>
```

---

### S04 — 예약 완료 (`reservation-complete.mustache`)

- `POST /reservation/create` 성공 → PRG 패턴 → `redirect:/reservation/complete?reservationNumber=RES-...`
- 예약번호 형식: `RES-YYYYMMDD-NNN`
- 표시 항목: 예약번호, 환자명, 진료과/의사, 예약 날짜, 예약 시간
- "메인으로 가기" → `/`

---

## 6. 현재 구현 상태 (2026-03-09 기준)

### 완료

- [x] `home/index.mustache` — 홈 메인 UI
- [x] `reservation/patient-choice.mustache` — S00 UI
- [x] `reservation/symptom-reservation.mustache` — S01 UI (시뮬레이션 JS 포함)
- [x] `reservation/direct-reservation.mustache` — S03 UI (하드코딩 데이터)
- [x] `reservation/reservation-complete.mustache` — S04 UI
- [x] `HomeController` — GET /
- [x] `ReservationController` — GET 라우팅 기본 골격

### 미완성 (TODO)

- [ ] JS 내 HTML 파일 직접 참조 → Spring MVC URL로 교체
  - `'../index.html'` → `'/'`
  - `'direct-reservation.html?...'` → `'/reservation/direct-reservation?...'`
- [ ] CSRF 토큰 폼 및 AJAX에 추가
- [ ] 진료과/의사 목록 DB 동적 조회 연동 (하드코딩 제거)
- [ ] 시간 슬롯 30분 단위 구현 (`SlotService.getAvailableSlots()` 연동)
- [ ] `POST /reservation/create` 구현 (PRG 패턴)
- [ ] `PatientRepository` findOrCreate 구현
- [ ] `@Valid` 유효성 검증 DTO 작성
- [ ] `POST /llm/symptom/analyze` AJAX 실제 연동 (W4)
- [ ] 폴백 처리 (5초 타임아웃 → 직접 선택 전환)
- [ ] 면책 조항 문구 UI 추가

---

## 7. 절대 터치 금지 영역

| 파일/디렉터리                       | 소유자             | 접근 수준          |
| ----------------------------------- | ------------------ | ------------------ |
| `config/SecurityConfig.java`        | 책임개발자(김민구) | 읽기 전용          |
| `domain/*.java`                     | 책임개발자(김민구) | 수정 시 Issue 등록 |
| `common/service/SlotService.java`   | 책임개발자(김민구) | 인터페이스 호출만  |
| `llm/LlmService.java`               | 책임개발자(김민구) | 인터페이스 호출만  |
| `templates/common/**`               | 책임개발자(김민구) | 수정 금지          |
| `staff/**`, `doctor/**`, `nurse/**` | 개발자 B(조유지)   | 접근 금지          |
| `admin/**`                          | 개발자 C(강상민)   | 접근 금지          |

---

## 8. 코딩 규칙 요약

| 항목               | 규칙                                                |
| ------------------ | --------------------------------------------------- |
| 컨트롤러 GET       | `request.setAttribute()` 후 뷰 경로 반환            |
| 컨트롤러 POST 성공 | `redirect:/다음화면` (PRG 패턴)                     |
| 컨트롤러 POST 실패 | 폼 뷰 경로 재반환 (에러 메시지 포함)                |
| AJAX 응답          | `@ResponseBody` + `{ success, data, message }` JSON |
| DTO                | Java Record 우선, `@Valid` 검증 필수                |
| JS 변수            | `const` 우선, `var` 금지                            |
| JS 비동기          | `async/await` 중심                                  |
| CSRF               | 모든 POST AJAX에 CSRF 토큰 포함 필수                |
| 시간 슬롯          | 30분 단위 09:00 ~ 17:30                             |
