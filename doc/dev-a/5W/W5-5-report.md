# W5-5 Report — 진료 취소 후 슬롯 재예약 가능하도록 수정

> **작성일**: 5W
> **브랜치**: `dev`

---

## 작업 목적

진료 취소 시 내역(CANCELLED 레코드)은 DB에 유지하되, 해당 날짜/시간 슬롯이 재예약 가능하도록 수정

---

## 원인 분석

### 문제 1: 진료 예약/변경 취소 후 재예약 불가

`reservation` 테이블에 `(doctor_id, reservation_date, time_slot)` unique constraint가 존재했다.
취소 처리 시 레코드를 삭제하지 않고 `status = CANCELLED`로만 변경하므로, 동일 슬롯으로 재예약 INSERT 시 unique constraint 위반 오류가 발생했다.

```
HHH000247: ErrorCode: 23505
Unique index or primary key violation: "UK_RESERVATION_DOCTOR_DATE_SLOT"
```

### 문제 2: 원무과 취소 시 슬롯 미해제

`ReceptionService.cancel()`이 `Reservation.cancel()`을 호출하는데, 이 메서드는 `RECEIVED → RESERVED`로 롤백만 하고 슬롯을 해제하지 않았다.

---

## 수정 내용

### 1. `Reservation.java` — unique constraint 제거

```java
// 수정 전
@Table(name = "reservation",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reservation_doctor_date_slot",
        columnNames = {"doctor_id", "reservation_date", "time_slot"}
    ), ...)

// 수정 후
@Table(name = "reservation", ...)
// uniqueConstraints 제거
```

중복 체크는 애플리케이션 레벨(`CANCELLED` 상태 제외)에서만 수행하므로 DB 제약 없이도 정상 동작한다.

### 2. `ReceptionService.java` — cancelFully 로 변경

```java
// 수정 전
r.cancel(reason); // RECEIVED→RESERVED 롤백, RESERVED→CANCELLED

// 수정 후
r.cancelFully(reason); // 상태 무관하게 즉시 CANCELLED → 슬롯 해제
```

---

## 테스트 결과

| 시나리오 | 결과 |
|----------|------|
| 온라인 예약 취소 후 동일 슬롯 재예약 | 성공 |
| 원무과에서 RECEIVED 상태 취소 후 전화예약/방문접수/비회원예약 | 성공 |
| 취소된 예약 접수 목록 "취소" 탭 조회 | CANCELLED 레코드 정상 표시 |

---

## 완료 기준 확인

- [x] 진료 예약/변경 취소 후 동일 의사/날짜/시간으로 재예약 성공
- [x] 원무과에서 RECEIVED 상태 예약 취소 시 슬롯 해제 후 전화예약/방문접수/비회원예약 가능
- [x] 취소된 예약이 접수 목록 "취소" 탭에서 조회 가능
- [x] IN_TREATMENT 상태에서 취소 시 기존 예외 처리 유지
