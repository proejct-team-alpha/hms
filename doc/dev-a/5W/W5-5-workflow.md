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
