# W3-7 설계 - 예약 불가 시간 슬롯 비활성화

**날짜:** 2026-03-17
**담당:** 강태오 (개발자 A)
**적용 페이지:** `direct-reservation.mustache`, `reservation-modify.mustache`

---

## 요구사항

진료 예약/변경 페이지에서 이미 예약된(CANCELLED 제외) 시간 슬롯을 사용자가 선택할 수 없도록 한다.

- 이미 예약된 슬롯: 드롭다운에 표시하되 `disabled` 처리 + 회색 텍스트 + `" (예약불가)"` 접미사
- 가용 슬롯: 정상 선택 가능

---

## 아키텍처

### 백엔드

#### 1. ReservationRepository — 쿼리 메서드 2개 추가

Spring Data JPA / Hibernate는 nullable Long 파라미터를 JPQL `IS NULL` 조건으로 바인딩하면
타입 추론 실패로 런타임 예외가 발생한다. 따라서 두 개의 별도 메서드로 분리한다.

```java
// 직접 예약 페이지용 (excludeId 없음)
@Query("SELECT r.timeSlot FROM Reservation r " +
       "WHERE r.doctor.id = :doctorId " +
       "AND r.reservationDate = :date " +
       "AND r.status <> :excluded")
List<String> findBookedTimeSlots(
    @Param("doctorId") Long doctorId,
    @Param("date") LocalDate date,
    @Param("excluded") ReservationStatus excluded);

// 예약 변경 페이지용 (현재 수정 중인 예약 제외)
@Query("SELECT r.timeSlot FROM Reservation r " +
       "WHERE r.doctor.id = :doctorId " +
       "AND r.reservationDate = :date " +
       "AND r.status <> :excluded " +
       "AND r.id <> :excludeId")
List<String> findBookedTimeSlotsExcluding(
    @Param("doctorId") Long doctorId,
    @Param("date") LocalDate date,
    @Param("excluded") ReservationStatus excluded,
    @Param("excludeId") Long excludeId);
```

#### 2. ReservationService — `getBookedTimeSlots` 오버로드 2개 추가

```java
public List<String> getBookedTimeSlots(Long doctorId, LocalDate date) {
    return reservationRepository.findBookedTimeSlots(doctorId, date, ReservationStatus.CANCELLED);
}

public List<String> getBookedTimeSlots(Long doctorId, LocalDate date, Long excludeId) {
    return reservationRepository.findBookedTimeSlotsExcluding(
        doctorId, date, ReservationStatus.CANCELLED, excludeId);
}
```

#### 3. ReservationApiController — `/api/reservation/booked-slots` 추가

```
GET /api/reservation/booked-slots
  ?doctorId={Long}          (필수)
  &date={yyyy-MM-dd}        (필수)
  &excludeId={Long}         (선택, 변경 페이지에서만 전달)

→ 200 OK: ["09:00", "10:30", ...]
```

---

### 프론트엔드

#### 페이지 로드 시 초기화 — `data-original-text` 저장

```javascript
document.querySelectorAll('#time option').forEach(opt => {
  opt.dataset.originalText = opt.textContent;
});
```

#### `resetSlots()` 헬퍼 함수

가드 조건 미충족 시 또는 fetch 실패 시 호출:

```javascript
function resetSlots() {
  document.querySelectorAll('#time option').forEach(opt => {
    opt.disabled = false;
    opt.classList.remove('text-slate-400');
    opt.textContent = opt.dataset.originalText;
  });
}
```

#### Flatpickr `onChange` 공통 로직

```
1. doctorId, dateStr 유효성 확인 → 미충족 시 resetSlots() 후 return
2. fetch /api/reservation/booked-slots?doctorId=&date=[&excludeId=]
3. booked 슬롯 목록 수신
4. #time 옵션 순회: 포함 시 disabled + "(예약불가)", 미포함 시 originalText 복원
5. catch → console.error + resetSlots()
```

#### reservation-modify.mustache 추가사항

- `const excludeId = {{id}};` — Mustache로 예약 ID 주입
- Flatpickr에 `minDate: 'today'` 추가 (과거 날짜 선택 방지)

---

## 데이터 흐름

```
[의사 변경] → datePicker.clear() → onChange([], "") → 가드 → resetSlots()
[날짜 선택] → fetch booked-slots → 슬롯 비활성화/복원 처리
```

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|---------|
| `ReservationRepository.java` | `findBookedTimeSlots`, `findBookedTimeSlotsExcluding` 추가 |
| `ReservationService.java` | `getBookedTimeSlots` 오버로드 2개 추가 |
| `ReservationApiController.java` | `GET /api/reservation/booked-slots` 엔드포인트 추가 |
| `direct-reservation.mustache` | 초기화 + Flatpickr `onChange` + `resetSlots()` 추가 |
| `reservation-modify.mustache` | 동일 + `excludeId` 전달 + `minDate: 'today'` |
