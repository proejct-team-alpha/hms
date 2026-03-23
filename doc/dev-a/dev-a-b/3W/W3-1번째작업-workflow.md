# W3-1번째작업 Workflow — 접수 처리 후 의사 진료 목록 실시간 연동 (AJAX 폴링)

## 작업 개요

- **목표:** 원무과 접수 처리(`RESERVED → RECEIVED`) 시 의사 진료 목록에 5초 폴링으로 실시간 반영
- **담당:** dev-a-b
- **날짜:** 2026-03-16

---

## 현황 분석

| 항목 | 현재 상태 |
|------|-----------|
| `POST /staff/reception/receive` | ✅ 구현 완료 (RESERVED → RECEIVED 전환) |
| `GET /doctor/treatment-list` | ✅ SSR 페이지 구현 완료 |
| `DoctorTreatmentService.getTreatmentPage()` | ✅ 구현 완료 (CANCELLED 제외 전체 조회) |
| `GET /doctor/treatment-list/poll` (AJAX) | ❌ 미구현 |
| 진료 목록 JS 폴링 | ❌ 미구현 |

---

## 작업 목록

### 1. `DoctorTreatmentService` — `getTodayReceivedList()` 메서드 추가

**파일:** `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentService.java`

```java
// TODO: 오늘 날짜 기준 RECEIVED 상태 예약 목록 조회 (폴링용)
public List<DoctorReservationDto> getTodayReceivedList(String username) { ... }
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 오늘 날짜를 기준으로 접수 완료(`RECEIVED`) 상태인 예약 목록만 가져오는 서비스 메서드를 선언합니다. `username`으로 현재 로그인한 의사를 구분합니다.
> - **왜 이렇게 썼는지**: 폴링(주기적으로 서버에 데이터를 요청하는 방식)은 매 5초마다 실행되기 때문에 불필요한 데이터를 최소화해야 합니다. 오늘 날짜 + RECEIVED 상태만 조회해서 응답 크기를 줄입니다.
> - **쉽게 말하면**: 오늘 접수된 환자 목록만 가져오는 기능입니다. 매 5초마다 호출되기 때문에 딱 필요한 데이터만 조회합니다.

- 오늘 날짜 + `RECEIVED` 상태만 필터링
- `DoctorReservationDto` 리스트 반환

---

### 2. `DoctorTreatmentController` — 폴링 AJAX 엔드포인트 추가

**파일:** `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentController.java`

```java
// TODO: GET /doctor/treatment-list/poll
// @ResponseBody JSON 반환
// Authentication으로 현재 의사 식별
// { success: true, data: [...] } 형태
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 브라우저가 5초마다 호출하는 폴링 전용 API 엔드포인트(접속 주소)를 만듭니다. `@ResponseBody`는 결과를 JSON 형식으로 반환하라는 의미이고, `Authentication`은 현재 로그인한 사용자 정보를 자동으로 받아옵니다.
> - **왜 이렇게 썼는지**: 페이지 전체를 다시 불러오지 않고 데이터만 JSON으로 응답하여 화면 깜빡임 없이 목록을 갱신할 수 있습니다. `Resp.ok(data)` 표준 포맷을 써서 응답 구조를 일관되게 유지합니다.
> - **쉽게 말하면**: 브라우저가 5초마다 "새 환자 없어요?" 하고 물어보면 서버가 JSON 데이터로 대답해주는 창구입니다.

- `GET /doctor/treatment-list/poll`
- `@ResponseBody` + `Resp.ok(data)` 표준 포맷 반환
- `ROLE_DOCTOR` 인증 필요 (기존 Security 정책 적용)

---

### 3. `doctor/treatment-list.mustache` — 폴링 JS 추가

**파일:** `src/main/resources/templates/doctor/treatment-list.mustache`

```javascript
// TODO: 5초마다 /doctor/treatment-list/poll 호출
// 응답 data 배열로 카드 목록 재렌더링
// 목록 영역에 id="treatment-card-list" 부여
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 페이지가 열려 있는 동안 5초마다 자동으로 서버에 최신 진료 목록을 요청하고, 응답받은 데이터로 화면의 카드 목록을 다시 그립니다.
> - **왜 이렇게 썼는지**: `setInterval`은 지정한 시간(5000ms = 5초)마다 함수를 반복 실행하는 JavaScript 기능입니다. `id="treatment-card-list"`를 부여해야 JavaScript가 갱신할 위치를 정확히 찾을 수 있습니다. 에러 발생 시 silent fail(콘솔 로그만)로 처리해서 폴링 오류가 사용자 화면을 방해하지 않도록 합니다.
> - **쉽게 말하면**: 5초마다 "새 환자 목록 주세요"라고 서버에 요청하고, 받아온 데이터로 화면을 조용히 업데이트하는 자동 새로고침 기능입니다.

- 카드 목록 감싸는 `div`에 `id="treatment-card-list"` 추가
- `setInterval` 5000ms 폴링
- 응답 데이터로 카드 HTML 동적 재구성
- 에러 발생 시 silent fail (콘솔 로그만)

---

### 4. 테스트

- [ ] `DoctorTreatmentServiceTest` — `getTodayReceivedList()` 단위 테스트 (Mockito)
- [ ] `DoctorTreatmentControllerTest` — `GET /doctor/treatment-list/poll` MockMvc 테스트
  - ROLE_DOCTOR 접근 성공 (200)
  - 미인증 접근 차단 (302)
- [ ] 수동 검증: staff 접수 처리 후 5초 이내 doctor 화면 반영 확인

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `doctor/treatment/DoctorTreatmentService.java` | 수정 (메서드 추가) |
| `doctor/treatment/DoctorTreatmentController.java` | 수정 (엔드포인트 추가) |
| `templates/doctor/treatment-list.mustache` | 수정 (폴링 JS 추가) |
| `test/.../DoctorTreatmentServiceTest.java` | 수정 또는 신규 |
| `test/.../DoctorTreatmentControllerTest.java` | 수정 또는 신규 |

---

## 금지 사항 체크

- [x] `config/`, `domain/` 수정 없음
- [x] `admin/**`, `reservation/**` 수정 없음
- [x] URL prefix 임의 변경 없음
- [x] 민감정보 하드코딩 없음
