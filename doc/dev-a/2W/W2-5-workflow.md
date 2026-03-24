# W2-5 Workflow — ReservationService 비즈니스 로직 (취소·변경·중복 체크)

> **작성일**: 2W
> **목표**: 예약 중복 방지, 조회, 취소(상태 변경), 변경(cancel+create) 구현

---

## 전체 흐름

```
중복 체크 → 예약 생성
취소: cancel() 상태 변경
변경: 기존 취소 + 신규 생성 (domain 금지 영역으로 update 불가)
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 변경 방식 | `domain/Reservation.java` 금지 영역 → cancel+create 조합 |
| 중복 체크 | `CANCELLED` 제외한 동일 의사+날짜+시간 존재 여부 |
| 취소/변경 화면 | 예약번호로 조회 후 처리 |
| 컨트롤러 패턴 | GET은 HttpServletRequest, POST는 PRG 패턴 |

---

## 실행 흐름

```
[1] ReservationRepository — 쿼리 메서드 3개 추가
[2] ReservationUpdateForm — 변경 폼 DTO 신규
[3] ReservationService — createReservation() 중복 체크 + 4개 메서드 추가
[4] ReservationController — 취소·변경 GET/POST 엔드포인트 추가
[5] reservation-cancel.mustache — 취소 확인 화면 신규
[6] reservation-modify.mustache — 변경 폼 화면 신규
```

---

## UI Mockup

```
[예약 취소 화면]
┌─────────────────────────────────┐
│ 예약번호 [________________] [조회] │
├─────────────────────────────────┤
│ 예약번호: RES-20260310-001       │
│ 환자명:   홍길동                 │
│ 진료과:   내과                   │
│ 전문의:   의사이영희             │
│ 일시:     2026-03-20 09:00       │
│              [예약 취소]         │
└─────────────────────────────────┘

[예약 변경 화면]
┌─────────────────────────────────┐
│ 예약번호 [________________] [조회] │
├─────────────────────────────────┤
│ 현재 예약: 내과 / 이영희 / ...   │
│ 진료과  [내과 ▼            ]    │
│ 전문의  [선택해주세요 ▼    ]    │
│ 날짜    [__________________]    │
│ 시간    [09:00 ▼           ]    │
│              [예약 변경]         │
└─────────────────────────────────┘
```

---

## 작업 목록

1. `ReservationRepository.java` — `findByReservationNumber`, `findByPatient_PhoneAndPatient_Name`, `existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot` 추가
2. `ReservationUpdateForm.java` — 변경 폼 DTO 신규 생성
3. `ReservationService.java` — `createReservation()` 중복 체크 추가 + 조회/취소/변경 메서드 추가
4. `ReservationController.java` — GET/POST 취소·변경 엔드포인트 추가
5. `reservation-cancel.mustache` — 취소 확인 화면 신규 생성
6. `reservation-modify.mustache` — 변경 폼 화면 신규 생성

---

## 작업 진행내용

- [x] ReservationRepository 쿼리 메서드 3개 추가
- [x] ReservationUpdateForm 신규 생성
- [x] ReservationService 중복 체크 + 4개 메서드 추가
- [x] ReservationController 취소·변경 엔드포인트 추가
- [x] reservation-cancel.mustache 신규 생성
- [x] reservation-modify.mustache 신규 생성

---

## 실행 흐름에 대한 코드

### ReservationRepository — 추가 메서드

```java
Optional<Reservation> findByReservationNumber(String reservationNumber);
List<Reservation> findByPatient_PhoneAndPatient_Name(String phone, String name);
boolean existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
    Long doctorId, LocalDate date, String timeSlot, ReservationStatus status);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약번호로 단건 조회, 환자 전화+이름으로 목록 조회, 특정 의사/날짜/시간에 이미 예약이 있는지(취소 제외) 확인하는 3가지 쿼리 메서드입니다.
> - **왜 이렇게 썼는지**: Spring Data JPA는 메서드 이름 규칙에 따라 자동으로 SQL을 생성합니다. `existsBy...StatusNot`은 "해당 조건을 만족하면서 status가 특정 값이 아닌 것이 존재하는가?"를 묻는 메서드입니다. `CANCELLED`된 예약은 빈 슬롯으로 봐야 하므로 제외합니다.
> - **쉽게 말하면**: 예약 검색 창구 3개 — 예약번호 창구, 이름+전화번호 창구, "이 시간 비어있나요?" 확인 창구입니다.

### ReservationService — 중복 체크

```java
@Transactional
public ReservationCompleteInfo createReservation(ReservationCreateForm form) {
    if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
            form.getDoctorId(), form.getReservationDate(), form.getTimeSlot(), ReservationStatus.CANCELLED)) {
        throw new IllegalStateException("이미 예약된 시간대입니다.");
    }
    // 이하 기존 로직 동일
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 생성 전에 같은 의사, 날짜, 시간에 이미 유효한 예약이 있으면 예외를 던져 중복 예약을 막습니다.
> - **왜 이렇게 썼는지**: `IllegalStateException`은 "현재 상태에서 이 작업은 불가능하다"는 Java 표준 예외입니다. 취소된(`CANCELLED`) 예약은 무효이므로 제외하여 중복 체크합니다. 예외를 던지면 `@Transactional`에 의해 이전 DB 변경이 모두 롤백됩니다.
> - **쉽게 말하면**: 병원 예약 담당자가 "이 날 이 시간에 이미 예약이 있습니다"라고 거절하는 상황입니다.

### ReservationService — 예약 변경 (cancel + create)

```java
@Transactional
public ReservationCompleteInfo updateReservation(Long id, ReservationUpdateForm form) {
    // 1. 기존 예약 취소
    Reservation old = reservationRepository.findById(id).orElseThrow();
    Patient patient = old.getPatient();
    old.cancel();
    // 2. 새 슬롯 중복 체크
    if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
            form.getDoctorId(), form.getReservationDate(), form.getTimeSlot(), ReservationStatus.CANCELLED)) {
        throw new IllegalStateException("이미 예약된 시간대입니다.");
    }
    // 3. 신규 예약 생성
    Doctor doctor = doctorRepository.findById(form.getDoctorId()).orElseThrow();
    Department dept = departmentRepository.findById(form.getDepartmentId()).orElseThrow();
    Reservation newR = Reservation.create("R" + System.currentTimeMillis(),
        patient, doctor, dept, form.getReservationDate(), form.getTimeSlot(), ReservationSource.ONLINE);
    reservationRepository.save(newR);
    return new ReservationCompleteInfo(...);
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 변경은 "기존 예약 취소 + 새 예약 생성"의 조합으로 처리합니다. 1단계에서 기존 예약을 취소 처리하고, 2단계에서 새 시간대 중복 확인 후, 3단계에서 새 예약을 생성합니다.
> - **왜 이렇게 썼는지**: `domain/Reservation.java`는 수정이 금지된 영역이라 `update` 메서드를 추가할 수 없습니다. 대신 기존 예약을 취소(`cancel()`)하고 새 예약을 생성하는 방식으로 구현했습니다. 이 방법은 이력 추적도 가능한 장점이 있습니다.
> - **쉽게 말하면**: 예약 수정이 불가능한 시스템에서, 기존 예약을 취소표로 만들고 새 예약표를 발행하는 방식입니다.

### ReservationController — 취소·변경 엔드포인트

```java
// 취소 화면 (GET)
@GetMapping("/cancel")
public String cancelPage(HttpServletRequest request) { ... }

// 취소 처리 (POST)
@PostMapping("/cancel/{id}")
public String cancelReservation(@PathVariable Long id) {
    reservationService.cancelReservation(id);
    return "redirect:/reservation";
}

// 변경 화면 (GET)
@GetMapping("/modify")
public String modifyPage(HttpServletRequest request) { ... }

// 변경 처리 (POST)
@PostMapping("/modify/{id}")
public String modifyReservation(@PathVariable Long id,
        @ModelAttribute ReservationUpdateForm form, RedirectAttributes ra) {
    ReservationCompleteInfo info = reservationService.updateReservation(id, form);
    // redirect attributes 추가
    return "redirect:/reservation/complete";
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 취소 화면 보여주기(GET), 실제 취소 처리(POST), 변경 화면 보여주기(GET), 실제 변경 처리(POST) 4개의 엔드포인트를 정의합니다.
> - **왜 이렇게 썼는지**: GET은 화면 조회용, POST는 데이터 처리용으로 역할을 분리합니다. `@PathVariable`은 URL 경로의 `{id}` 값을 파라미터로 받아옵니다(예: `/cancel/5` → id=5). `@ModelAttribute`는 폼 데이터를 자동으로 Form 객체에 바인딩합니다.
> - **쉽게 말하면**: 병원 예약 취소/변경 창구에서 "서류 확인(GET)"과 "실제 처리(POST)"를 별도 창구로 운영하는 것입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 중복 예약 | 동일 의사+날짜+시간 | IllegalStateException |
| 예약 취소 | 유효한 예약번호 | status CANCELLED 변경 |
| 예약 변경 | 유효한 예약번호 + 새 슬롯 | 기존 취소 + 신규 생성 |
| 변경 중복 | 새 슬롯이 이미 예약됨 | IllegalStateException |

---

## 완료 기준

- [x] 중복 예약 시 예외 처리
- [x] 예약 취소 상태 변경 동작
- [x] 예약 변경 (cancel + create) 동작
- [x] GET/POST 엔드포인트 정상 동작
