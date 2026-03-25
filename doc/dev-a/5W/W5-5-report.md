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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 재예약 시도 시 발생하던 DB 오류 메시지입니다. `ErrorCode: 23505`는 H2 데이터베이스의 unique constraint 위반 오류 코드이며, `UK_RESERVATION_DOCTOR_DATE_SLOT`은 위반된 unique 제약의 이름입니다.
> - **왜 이렇게 썼는지**: 이 오류 메시지가 발생하는 원인을 명확히 보여주기 위해 실제 오류 로그를 기록했습니다. 같은 `(doctor_id, reservation_date, time_slot)` 조합으로 INSERT를 시도할 때 DB가 이 오류를 발생시킵니다. `CANCELLED` 상태인 레코드도 이 조합이 같으면 오류가 발생하는 것이 문제였습니다.
> - **쉽게 말하면**: DB가 "이미 이 의사 + 날짜 + 시간 조합의 데이터가 존재한다"고 거부하는 오류 메시지입니다. 취소된 것도 존재로 간주해서 문제가 됩니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: JPA 엔티티의 `@Table` 어노테이션에서 unique 제약 조건을 제거합니다. 이로 인해 DB 테이블을 재생성할 때 `UK_RESERVATION_DOCTOR_DATE_SLOT` 제약이 만들어지지 않습니다.
> - **왜 이렇게 썼는지**: `@UniqueConstraint`는 JPA가 테이블을 생성할 때 DB에 unique 제약을 추가하도록 지시합니다. 이를 제거하면 같은 (의사 + 날짜 + 시간) 조합의 데이터를 여러 개 저장할 수 있게 됩니다. 대신 Java 코드의 서비스 레이어에서 `CANCELLED` 상태를 제외한 중복 체크를 수행하므로 정상 예약의 중복은 여전히 방지됩니다.
> - **쉽게 말하면**: "같은 자리는 한 명만 앉을 수 있다"는 DB 규칙을 없애고, "취소한 사람 자리는 다시 앉을 수 있다"는 프로그램 규칙으로 대체한 것입니다.

중복 체크는 애플리케이션 레벨(`CANCELLED` 상태 제외)에서만 수행하므로 DB 제약 없이도 정상 동작한다.

### 2. `ReceptionService.java` — cancelFully 로 변경

```java
// 수정 전
r.cancel(reason); // RECEIVED→RESERVED 롤백, RESERVED→CANCELLED

// 수정 후
r.cancelFully(reason); // 상태 무관하게 즉시 CANCELLED → 슬롯 해제
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 취소 메서드를 `cancel(reason)`에서 `cancelFully(reason)`으로 교체합니다. `cancel()`은 `RECEIVED` 상태를 `RESERVED`로 되돌리기만 했는데, `cancelFully()`는 현재 상태와 관계없이 바로 `CANCELLED`로 처리합니다.
> - **왜 이렇게 썼는지**: 예약의 상태 흐름은 `RECEIVED(접수) → RESERVED(예약 확정) → CANCELLED(취소)`입니다. 원무과에서 `RECEIVED` 상태를 취소하면 `RESERVED`로만 돌아가고 슬롯이 해제되지 않는 버그가 있었습니다. `cancelFully()`는 이 중간 단계를 건너뛰고 즉시 `CANCELLED`로 전환해 슬롯을 바로 해제합니다.
> - **쉽게 말하면**: 예전에는 "접수 중 → 예약 확정"으로만 롤백되어 자리가 안 풀렸는데, 이제는 어느 단계이든 "취소" 한 번에 바로 자리가 풀립니다.

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
