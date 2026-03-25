# W6-1 Report — 예약 컨트롤러 DTO 전달 방식 개선 + 코드 주석 보완

> **작성일**: 2026-03-23
> **브랜치**: `feature/dev-a`

---

## 작업 개요

코드 리뷰를 통해 확인된 두 가지 문제를 수정함.
1. `ReservationCompleteInfo` DTO가 뷰 모델로 활용되지 않고 각 필드를 URL 파라미터로 개별 노출
2. a-skill.md 규칙 중 "코드에 상세 주석" 미준수

---

## 구현 내용

### 1. ReservationController — `addFlashAttribute`로 DTO 전달

- **변경 전**: `redirectAttributes.addAttribute("name", info.getPatientName())` 등 필드 6개를 URL 파라미터로 개별 전달
  - 완료 URL: `/reservation/complete?name=홍길동&department=내과&...`
- **변경 후**: `redirectAttributes.addFlashAttribute("info", info)` — DTO 통째로 세션 flash 저장
  - 완료 URL: `/reservation/complete` (개인정보 노출 없음)
  - Spring MVC가 redirect 후 flash attribute를 자동으로 모델에 병합 → 뷰에서 `{{#info}}`로 접근

적용 핸들러:
- `POST /reservation/create` → `redirect:/reservation/complete`
- `POST /reservation/cancel/{id}` → `redirect:/reservation/cancel-complete`
- `POST /reservation/modify/{id}` → `redirect:/reservation/modify-complete`

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 처리(생성/취소/변경) 완료 후 화면에 정보를 전달하는 방식을 바꿉니다. 이전에는 URL 주소창에 이름·진료과·의사명을 직접 붙였지만, 이제 서버 내부 임시 저장소(flash)를 통해 전달합니다.
> - **왜 `addFlashAttribute`를 선택했는지**: `addAttribute()`는 데이터를 URL 쿼리 파라미터(`?name=홍길동&...`)로 변환해 주소창에 노출합니다. 환자 이름·진료과 같은 개인정보가 브라우저 히스토리, 서버 로그, 공유 링크에 남을 수 있습니다. `addFlashAttribute()`는 데이터를 서버 세션에 딱 한 번만 저장했다가 다음 요청 후 자동 삭제합니다.
> - **PRG 패턴이란**: POST(폼 제출) → Redirect(주소 이동) → GET(완료 화면 조회) 흐름입니다. 새로고침 시 폼이 중복 제출되는 문제를 막는 표준 방식입니다. flash attribute는 이 패턴에서 Redirect와 GET 사이에 데이터를 안전하게 전달하는 역할입니다.
> - **다른 방법은 없는지**: 세션에 직접 저장하거나, 완료 페이지에서 DB를 다시 조회할 수도 있습니다. 하지만 flash attribute는 Spring MVC가 관리하므로 코드가 단순하고, 조회 1번 결과를 재활용할 수 있어 가장 적합합니다.
> - **쉽게 말하면**: 정보를 "주소창에 적어" 전달하던 것을 "봉투에 넣어 한 번만 건네는" 방식으로 바꿔, 개인정보가 URL에 노출되지 않게 됩니다.

### 2. ReservationController — 전 메서드 상세 주석 추가

각 메서드에 역할 설명 주석, 내부 단계별 주석 추가:
- `directReservation()`: 진료과 목록 DepartmentDto 전달 목적 주석
- `createReservation()`: 유효성 실패 분기 / 예약 생성 / flash 전달 각 단계 주석
- `lookupPage()`: 예약번호 단건 / 이름+전화 목록 분기 주석
- `cancelPage()`, `cancelReservation()`: 조회 및 소유권 검증 흐름 주석
- `modifyPage()`, `modifyReservation()`: 유효성 실패 / 변경 처리 / 예외 분기 주석

### 3. ReservationService — 전 메서드 상세 주석 추가

| 메서드 | 추가된 주석 내용 |
|--------|-----------------|
| `getDoctorsByDepartment()` | AJAX 드롭다운용, DTO 변환 목적 |
| `getDepartments()` | select 옵션용, DTO 변환 목적 |
| `findByReservationNumber()` | LazyLoad 방지를 위한 DTO 변환 |
| `findById()` | fetch join 포함, DTO 변환 |
| `getBookedTimeSlots()` | CANCELLED 제외 이유, Flatpickr 비활성화 용도 |
| `getBookedTimeSlots(excludeId)` | 변경 시 본인 슬롯 제외 이유 |
| `findByPhoneAndName()` | 전화번호 정규화(숫자만 추출) 주석 |
| `createReservation()` | H-03 중복 체크 / 신규 환자 등록 / flush 이유 / LazyLoad 방지 DTO 포장 |
| `cancelReservation()` | H-01 소유권 검증 / 취소 전 DTO 추출 이유 |
| `updateReservation()` | H-01+H-03 비관적 락 / 소유권 검증 / 기존 예약 취소 후 신규 생성 흐름 |

### 4. 완료 화면 mustache 3개 — Mustache 바인딩으로 교체

| 파일 | 변경 내용 |
|------|----------|
| `reservation-complete.mustache` | JS `urlParams.get()` + DOM 조작 제거 → `{{#info}}{{patientName}}...{{/info}}` |
| `reservation-cancel-complete.mustache` | 동일 |
| `reservation-modify-complete.mustache` | 동일 |

공통 변경 사항:
- `id="success-view"` / `id="error-view"` + JS hidden 토글 → `{{#info}}` / `{{^info}}` Mustache 조건으로 교체
- "예약 조회" `<button onclick=...>` → `<a href="/reservation/lookup?reservationNumber={{reservationNumber}}">` 로 교체 (JS 불필요)
- `<script>` 블록에서 URL 파라미터 파싱 로직 전체 제거, `feather.replace()` 만 유지

> **💡 입문자 설명**
>
> **`{{#info}}` / `{{^info}}` — Mustache 조건문**
> - `{{#info}}...{{/info}}`는 "info 객체가 존재할 때" 블록을 렌더링합니다. `{{^info}}...{{/info}}`는 반대로 "info가 없을 때"를 의미합니다.
> - 이전 방식은 JavaScript가 URL에서 데이터를 꺼내(`urlParams.get('name')`) DOM을 직접 조작했습니다. 서버 렌더링 방식은 서버가 HTML을 만들 때 값을 이미 채워 넣어 완성된 페이지를 보냅니다. JS 없이도 동작하고, 코드가 훨씬 단순해집니다.
> - **다른 방법**: JavaScript `fetch()`로 API를 호출해 동적으로 그릴 수도 있지만, 이 프로젝트는 Mustache 서버 렌더링을 기본으로 하므로 이 방식이 적합합니다.
>
> **`<button onclick=...>` → `<a href=...>` 교체**
> - 단순히 다른 페이지로 이동하는 기능이라면 `<button>` + JavaScript보다 `<a>` 태그가 적합합니다. 브라우저 기본 동작(우클릭 → 새 탭 열기, 링크 복사 등)을 지원하고, JavaScript가 없어도 동작합니다.
> - `{{reservationNumber}}`를 URL에 넣는 것은 개인정보가 아닌 식별자이므로 URL 노출이 적절합니다.
>
> **쉽게 말하면**: 이전에는 "주소창 정보를 JavaScript로 읽어서 화면을 꾸미는" 방식이었다면, 이제는 "서버가 완성된 HTML을 그대로 보내주는" 방식입니다. 더 안전하고 간단합니다.

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `reservation/reservation/ReservationController.java` | `addFlashAttribute` 변경 (POST 3개), 전 메서드 상세 주석 |
| `reservation/reservation/ReservationService.java` | 전 메서드 단계별 상세 주석 |
| `templates/reservation/reservation-complete.mustache` | JS 제거, Mustache 바인딩 |
| `templates/reservation/reservation-cancel-complete.mustache` | JS 제거, Mustache 바인딩 |
| `templates/reservation/reservation-modify-complete.mustache` | JS 제거, Mustache 바인딩 |

---

## 결과

- 완료 화면 URL에 개인정보(이름, 진료과, 의사명 등) 쿼리 파라미터 노출 없음
- `ReservationCompleteInfo` DTO가 flash attribute → 모델 → Mustache 뷰까지 일관된 흐름으로 전달됨
- 직접 URL 접근 시 `{{^info}}` 오류 뷰 정상 표시
- a-skill.md "코드에 상세 주석" 규칙 준수 완료
