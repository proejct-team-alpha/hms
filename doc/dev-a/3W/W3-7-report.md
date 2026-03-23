# W3-7 리포트 - 코드 리뷰 반영 및 예약 불가 슬롯 비활성화

## 작업 개요
- **작업명**: 코드 리뷰 8건 해결 + 예약 불가 시간 슬롯 비활성화 기능 구현
- **날짜**: 2026-03-17

---

## 1. 코드 리뷰 반영 (`doc/CODE_REVIEW_2026-03-17.md`)

### C-05 — `direct-reservation.mustache` fetch async/await 전환
- AI 추천 의사 목록 조회, 진료과 변경 이벤트 핸들러를 `async/await` + `response.ok` 검사 + `catch` 구조로 전환
- 수정 파일: `direct-reservation.mustache`

### H-01 — IDOR 방어: 예약 소유권 검증
- 취소/변경 폼에 `<input type="hidden" name="phone">` 추가
- `cancelReservation`, `updateReservation` 서비스에서 전화번호 정규화(숫자만 추출) 후 일치 여부 확인
- 불일치 시 `CustomException.forbidden("예약 소유자가 아닙니다.")` 발생
- 수정 파일: `ReservationService.java`, `reservation-cancel.mustache`, `reservation-modify.mustache`

### H-02 — 예약번호 생성 통일 (`ReservationNumberGenerator`)
- `System.nanoTime()` 기반 임시 생성 로직 제거
- `ReservationNumberGenerator.generate(reservationDate, countSupplier)` 사용으로 통일
- 수정 파일: `ReservationService.java`

### H-03 — TOCTOU 방어: 비관적 락 적용
- `ReservationRepository`에 `@Lock(PESSIMISTIC_WRITE)` `findByIdForUpdate` 쿼리 추가
- `updateReservation` 서비스에서 기존 예약 조회 시 비관적 락 획득 후 소유권 검증 → 슬롯 중복 체크 순으로 처리
- 수정 파일: `ReservationRepository.java`, `ReservationService.java`

### H-06 — Admin 전용 쿼리 중복 제거
- `ReservationRepository`에서 Admin 전용 쿼리(`findDailyPatientCounts`, `findReservationListPage` 등) 제거
- 해당 기능은 `AdminReservationRepository`에서만 관리
- 수정 파일: `ReservationRepository.java`

### H-13 — DTO 이름 변경
- `ReservationCreateForm` → `CreateReservationRequest` (record)
- `ReservationUpdateForm` → `UpdateReservationRequest` (record)
- 기존 파일은 주석만 남겨 빈 상태로 유지 (패키지 충돌 방지)
- 수정 파일: `CreateReservationRequest.java` (신규), `UpdateReservationRequest.java` (신규), `ReservationController.java`, `ReservationService.java`, `ReservationServiceTest.java`

### M-03 — `@Pattern` 검증 추가
- `CreateReservationRequest.phone`: `^$|^01[0-9][- ]?(\d{3,4})[- ]?\d{4}$`
- `CreateReservationRequest.timeSlot`: `^$|^\d{2}:\d{2}$`
- 빈 값은 `@NotBlank`가 처리, 형식 오류는 `@Pattern`이 처리 (중복 메시지 방지를 위해 빈 문자열 허용 정규식 적용)
- 수정 파일: `CreateReservationRequest.java`

### M-10 — `index.mustache` 인라인 header/footer → 파셜 교체
- `{{> common/header-public}}`, `{{> common/footer-public}}` 파셜로 교체
- 수정 파일: `home/index.mustache`

### M-11 — 진료과 드롭다운 서버 바인딩
- `directReservation()` GET에서 `reservationService.getDepartments()` 결과를 모델에 바인딩
- 템플릿에서 `{{#departments}}<option value="{{id}}">{{name}}</option>{{/departments}}` 사용
- JS `DEPT_ID_MAP`을 렌더링된 select 옵션에서 동적 구성
- 수정 파일: `ReservationController.java`, `direct-reservation.mustache`

---

## 2. 예약 불가 시간 슬롯 비활성화 기능

### 요구사항
의사 + 날짜 선택 시 이미 예약된(CANCELLED 제외) 시간 슬롯을 `disabled` + 회색 + "(예약불가)" 텍스트로 표시

### 백엔드

#### `ReservationRepository.java`
```java
@Query("SELECT r.timeSlot FROM Reservation r WHERE r.doctor.id = :doctorId AND r.reservationDate = :date AND r.status <> :excluded")
List<String> findBookedTimeSlots(...);

@Query("... AND r.id <> :excludeId")
List<String> findBookedTimeSlotsExcluding(...);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 데이터베이스에서 특정 의사와 날짜의 예약된 시간 슬롯 목록을 조회하는 쿼리 메서드입니다. `findBookedTimeSlots`는 신규 예약용, `findBookedTimeSlotsExcluding`은 예약 변경 시 현재 수정 중인 예약은 제외하고 조회합니다.
> - **왜 이렇게 썼는지**: `@Query`는 직접 JPQL 쿼리를 작성하는 어노테이션입니다. `r.status <> :excluded`는 취소된 예약(`CANCELLED`)을 제외합니다. Hibernate가 nullable Long 파라미터를 하나의 메서드에서 처리하지 못하는 기술적 제한 때문에 메서드 2개로 분리했습니다.
> - **쉽게 말하면**: "이 의사의 이 날짜에 취소되지 않은 예약들의 시간 목록을 DB에서 뽑아오는" 쿼리입니다.

- Hibernate nullable Long JPQL 타입 추론 실패 문제로 메서드 2개로 분리

#### `ReservationService.java`
```java
public List<String> getBookedTimeSlots(Long doctorId, LocalDate date) { ... }
public List<String> getBookedTimeSlots(Long doctorId, LocalDate date, Long excludeId) { ... }
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Repository의 쿼리를 감싸는 서비스 메서드 2개입니다. 같은 이름(`getBookedTimeSlots`)이지만 파라미터 개수가 달라 자바가 알아서 구분합니다(메서드 오버로드).
> - **왜 이렇게 썼는지**: Java에서 같은 이름의 메서드를 파라미터 목록이 다르게 여러 개 정의할 수 있는 것을 "오버로드(overload)"라고 합니다. 직접 예약 페이지와 예약 변경 페이지에서 동일한 메서드 이름으로 사용할 수 있어 코드가 직관적입니다.
> - **쉽게 말하면**: 이름은 같지만 상황에 따라 "제외할 예약 ID 없이", 또는 "제외할 예약 ID 포함해서" 두 가지 방식으로 호출할 수 있는 함수입니다.

#### `ReservationApiController.java`
```
GET /api/reservation/booked-slots?doctorId=&date=[&excludeId=]
→ 200 OK: ["09:00", "10:30", ...]
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `GET /api/reservation/booked-slots` API의 요청 형식과 응답 형식을 나타냅니다. `doctorId`와 `date`는 필수, `excludeId`는 선택입니다. 응답은 예약된 시간 문자열 배열입니다.
> - **왜 이렇게 썼는지**: REST API 설계에서 조회는 GET 방식을 사용합니다. `?` 이후의 쿼리 파라미터로 필요한 조건을 전달합니다. 응답을 JSON 배열(`["09:00", ...]`)로 반환하면 프론트엔드 JS에서 `booked.includes(time)`으로 쉽게 비교할 수 있습니다.
> - **쉽게 말하면**: "이 의사, 이 날짜에 이미 찬 시간 목록 주세요" 요청에 대해 `["09:00", "10:30"]`처럼 답해주는 API입니다.

### 프론트엔드

#### `direct-reservation.mustache`
- 페이지 로드 시 `data-original-text` 속성으로 옵션 원본 텍스트 저장
- `resetSlots()` 헬퍼 함수 추가 (가드 미충족 or fetch 실패 시 호출)
- Flatpickr `onChange` 콜백에 booked-slots API 호출 + 슬롯 비활성화 로직 삽입

#### `reservation-modify.mustache`
- `const excludeId = {{id}};` — Mustache로 예약 ID 주입
- 동일한 `resetSlots()` + Flatpickr `onChange` + `excludeId` 포함 URL 구성
- `minDate: 'today'` 추가 (과거 날짜 선택 방지)

---

## 3. 부수 버그 수정

### 500 에러 → 인라인 에러 메시지 전환
- `createReservation`, `cancelReservation`, `modifyReservation` 컨트롤러에서 `CustomException`을 `try-catch`로 잡아 폼 뷰 재표시
- 기존: `SsrExceptionHandler`가 500 에러 페이지 렌더링
- 수정 후: 폼에 인라인 에러 메시지 표시

---

## 수정 파일 목록

| 파일 | 변경 유형 |
|------|---------|
| `ReservationRepository.java` | `findByIdForUpdate`, `findBookedTimeSlots`, `findBookedTimeSlotsExcluding` 추가 / Admin 쿼리 제거 |
| `ReservationService.java` | 소유권 검증, 비관적 락, `getBookedTimeSlots` 오버로드 추가 / 예약번호 생성 통일 |
| `ReservationApiController.java` | `GET /api/reservation/booked-slots` 추가 |
| `ReservationController.java` | `CreateReservationRequest` 적용, try-catch, phone 파라미터 추가 |
| `CreateReservationRequest.java` | 신규 (record, `@Pattern` 포함) |
| `UpdateReservationRequest.java` | 신규 (record) |
| `direct-reservation.mustache` | 서버 바인딩, async/await, 슬롯 비활성화 |
| `reservation-cancel.mustache` | hidden phone 추가 |
| `reservation-modify.mustache` | hidden phone, 슬롯 비활성화, excludeId, minDate |
| `home/index.mustache` | 파셜 교체 |

---

## 테스트 결과

| 테스트 | 결과 |
|--------|------|
| `ReservationServiceTest` (5건) | ✅ PASS |
| `ReservationControllerTest` (3건) | ✅ PASS |
| 기타 (55건) | ✅ PASS |
| `AdminStaffApiControllerTest` (2건) | ⚠️ 기존 실패 (담당 외) |
| `DoctorTreatmentControllerTest` (3건) | ⚠️ 기존 실패 (담당 외) |

---

## 특이사항
- `@Pattern` 정규식에 `^$|` 접두사 추가 — `@NotBlank`와 분리하여 단일 에러 메시지 보장
- Hibernate nullable Long JPQL 바인딩 이슈로 repository 메서드 2개 분리 (설계 문서 반영)
- `data-original-text` 패턴으로 슬롯 텍스트 반복 변형 방지 (날짜 재선택 시 "(예약불가)(예약불가)" 중복 방지)
