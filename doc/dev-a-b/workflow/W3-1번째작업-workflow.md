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
