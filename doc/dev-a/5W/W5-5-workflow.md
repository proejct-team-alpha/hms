# W5-5 Workflow — 진료 취소 후 슬롯 재예약 가능하도록 수정

> **작성일**: 5W
> **브랜치**: `dev`
> **목표**: 진료 취소 시 내역(CANCELLED 레코드)은 DB에 유지하되, 해당 날짜/시간 슬롯이 재예약 가능하도록 수정

---

## 전체 흐름

```
[현재 문제]
취소 처리 → status = CANCELLED (레코드 유지)
               ↓
재예약 시도 → INSERT 시 unique constraint (doctor_id, reservation_date, time_slot) 위반
               ↓
"이미 예약된 시간대" 오류 발생 → 재예약 불가

[추가 문제]
원무과 취소 → RECEIVED 상태일 때 cancel() 호출
               ↓
RECEIVED → RESERVED (접수 롤백, 슬롯 유지됨) → 슬롯이 안 풀림

[해결 방향]
1. Reservation 엔티티에서 unique constraint 제거
   → 애플리케이션 레벨 중복 체크(CANCELLED 제외)만으로 충분
2. ReceptionService.cancel() → cancelFully() 로 변경
   → RECEIVED/RESERVED 모두 바로 CANCELLED 처리
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 문제 1 | 진료 예약/변경 취소 후 동일 슬롯 재예약 불가 (unique constraint 위반) |
| 문제 2 | 원무과에서 RECEIVED 상태 예약 취소 시 슬롯이 풀리지 않음 |
| 내역 유지 | CANCELLED 레코드는 DB에 남겨야 함 |
| 조회 위치 | 접수 목록 "취소" 탭, 관리자 페이지 양쪽에서 조회 가능해야 함 |
| 범위 외 | 관리자 취소 로직, 취소 화면 UI 변경 없음 |

---

## 실행 흐름

```
[수정 1] Reservation.java
  @Table uniqueConstraints 에서 uk_reservation_doctor_date_slot 제거
  → Hibernate ddl-auto=create-drop 이므로 재시작 시 자동 반영

[수정 2] ReceptionService.cancel()
  r.cancel(reason) → r.cancelFully(reason) 으로 교체
  → RECEIVED → CANCELLED (슬롯 즉시 해제)
  → RESERVED → CANCELLED (기존과 동일)

[검증] 중복 체크 로직 (변경 없음)
  - createReservation(): existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(CANCELLED) ✓
  - createPhoneReservation(): findTodayExcludingStatus(CANCELLED) ✓
  - createWalkin(): findTodayExcludingStatus(CANCELLED) ✓
  - 슬롯 조회 API: findBookedTimeSlots(CANCELLED 제외) ✓
```

---

## UI Mockup

변경 없음. 취소 완료 후 기존 예약 가능 슬롯 조회 시 해당 슬롯이 활성화되어 선택 가능해야 함.

---

## 작업 목록

- [x] `Reservation.java` — `@Table`의 `uniqueConstraints`에서 `uk_reservation_doctor_date_slot` 제거
- [x] `ReceptionService.java` — `cancel()` 메서드 내 `r.cancel(reason)` → `r.cancelFully(reason)` 변경
- [x] 테스트: 진료 취소 후 동일 슬롯 재예약 성공 확인
- [x] 테스트: 원무과에서 RECEIVED 상태 취소 후 전화예약/방문접수 가능 확인
- [x] 테스트: 취소 내역이 접수 목록 "취소" 탭에서 조회 확인

---

## 작업 진행 내용

> 작업 시작 전 공란. 작업 진행하며 기록 예정.

---

## 실행 흐름에 대한 코드

### 수정 1: `Reservation.java` — unique constraint 제거

```java
// 수정 전
@Table(name = "reservation",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reservation_doctor_date_slot",
        columnNames = {"doctor_id", "reservation_date", "time_slot"}
    ),
    indexes = { ... })

// 수정 후
@Table(name = "reservation",
    indexes = { ... })
// uniqueConstraints 완전 제거
// → 중복 체크는 애플리케이션 레벨(CANCELLED 제외)에서만 수행
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 데이터베이스 테이블의 unique 제약 조건(같은 의사·날짜·시간에 중복 예약을 DB 수준에서 막는 규칙)을 제거합니다. 대신 애플리케이션 코드에서 CANCELLED(취소된) 상태를 제외하고 중복을 검사합니다.
> - **왜 이렇게 썼는지**: 취소된 예약은 `CANCELLED` 상태로 DB에 남아 있는데, DB의 unique 제약은 상태에 관계없이 중복을 막습니다. 따라서 취소 후 같은 슬롯 재예약이 DB 오류로 실패했습니다. 제약을 제거하고 Java 코드에서 `CANCELLED` 제외 조건으로 검사하면, 취소된 슬롯을 다시 예약할 수 있게 됩니다. `ddl-auto=create-drop` 설정으로 앱 재시작 시 자동 반영됩니다.
> - **쉽게 말하면**: "이미 사용한 자리는 영구 금지" 규칙을 "실제 사용 중인 자리만 금지"로 바꾼 것입니다. 취소된 자리는 다시 예약할 수 있게 됩니다.

### 수정 2: `ReceptionService.java` — cancelFully 로 변경

```java
// 수정 전
@Transactional
public Reservation cancel(Long id, String reason) {
    Reservation r = reservationRepository.findById(id)
            .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
    try {
        r.cancel(reason); // RECEIVED→RESERVED, RESERVED→CANCELLED
    } catch (IllegalStateException ex) {
        throw CustomException.invalidStatusTransition(ex.getMessage());
    }
    return r;
}

// 수정 후
@Transactional
public Reservation cancel(Long id, String reason) {
    Reservation r = reservationRepository.findById(id)
            .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
    try {
        r.cancelFully(reason); // 상태 무관하게 바로 CANCELLED → 슬롯 즉시 해제
    } catch (IllegalStateException ex) {
        throw CustomException.invalidStatusTransition(ex.getMessage());
    }
    return r;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 취소 처리 메서드에서 `r.cancel(reason)` 대신 `r.cancelFully(reason)`을 호출하도록 변경합니다. 변경 전에는 `RECEIVED` 상태 예약을 취소하면 `RESERVED`로만 롤백되고, `RESERVED`를 한 번 더 취소해야 `CANCELLED`가 됐습니다. 변경 후에는 현재 상태에 관계없이 즉시 `CANCELLED`로 처리합니다.
> - **왜 이렇게 썼는지**: `@Transactional`은 이 메서드가 하나의 DB 트랜잭션(전부 성공하거나 전부 실패) 안에서 실행됨을 의미합니다. `orElseThrow`는 예약을 찾지 못하면 즉시 예외를 던집니다. `cancelFully()`는 엔티티 내부에 정의된 메서드로, 상태 전이 로직(RECEIVED나 RESERVED 모두 → CANCELLED)을 캡슐화합니다.
> - **쉽게 말하면**: 예전에는 "접수 중 → 예약 확정 → 취소" 두 단계를 거쳐야 했는데, 이제는 어느 단계에 있든 "취소 버튼 한 번"으로 슬롯이 즉시 풀리게 됩니다.

---

## 테스트 진행

| 시나리오 | 기대 결과 |
|----------|-----------|
| 온라인 예약 취소 후 동일 슬롯 재예약 | 재예약 성공 |
| RECEIVED 상태 원무과 취소 후 전화예약 | 전화예약 성공 |
| RECEIVED 상태 원무과 취소 후 방문접수 | 방문접수 성공 |
| RECEIVED 상태 원무과 취소 후 비회원 예약 | 비회원 예약 성공 |
| 취소된 예약 접수 목록 "취소" 탭 조회 | CANCELLED 레코드 표시 |
| 취소된 예약 관리자 페이지 조회 | CANCELLED 레코드 표시 |
| IN_TREATMENT 상태 원무과 취소 시도 | 예외 처리 (변경 없음) |

---

## 완료 기준

- [ ] 진료 예약/변경 취소 후 동일 의사/날짜/시간으로 재예약 성공
- [ ] 원무과에서 RECEIVED 상태 예약 취소 시 슬롯 해제 후 전화예약/방문접수/비회원예약 가능
- [ ] 취소된 예약이 접수 목록 "취소" 탭에서 조회 가능
- [ ] 취소된 예약이 관리자 페이지에서 조회 가능
- [ ] IN_TREATMENT 상태에서 취소 시 기존 예외 처리 유지
