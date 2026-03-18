# 예약 불가 시간 슬롯 비활성화 설계

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

- CANCELLED 제외: `existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot` 기존 규칙과 동일

#### 2. ReservationService — `getBookedTimeSlots` 오버로드 2개 추가

```java
// 직접 예약
public List<String> getBookedTimeSlots(Long doctorId, LocalDate date) {
    return reservationRepository.findBookedTimeSlots(doctorId, date, ReservationStatus.CANCELLED);
}

// 예약 변경 (현재 예약 제외)
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

컨트롤러에서 `excludeId` null 여부에 따라 적절한 서비스 메서드 호출.

- 잘못된 `date` 형식: Spring이 400 반환 (별도 처리 불필요)
- 존재하지 않는 `doctorId`: 빈 리스트 반환 (정상 동작)

---

### 프론트엔드

#### 페이지 로드 시 초기화 — `data-original-text` 저장

페이지 로드 시 `<select id="time">` 의 모든 `<option>` 에 현재 텍스트를
`data-original-text` 속성으로 저장해 둔다. 이후 슬롯 업데이트 시 이 값을
기준으로 텍스트를 복원한다.

```javascript
document.querySelectorAll('#time option').forEach(opt => {
  opt.dataset.originalText = opt.textContent;
});
```

#### `resetSlots()` 헬퍼 함수

다음 두 경우에 호출:
1. **가드 조건 미충족 시** (dateStr 비어 있거나 doctorId 비어 있음)
2. **fetch 실패 시**

수행 동작:
```javascript
function resetSlots() {
  document.querySelectorAll('#time option').forEach(opt => {
    opt.disabled = false;
    opt.classList.remove('text-slate-400');
    opt.textContent = opt.dataset.originalText; // 원래 텍스트 복원
  });
}
```

#### Flatpickr `onChange(selectedDates, dateStr)` 공통 로직

```
1. doctorId = document.getElementById('doctor').value
2. 가드: dateStr 비어 있음 OR doctorId 비어 있음 → resetSlots() 후 return
3. fetch URL 구성 (direct: excludeId 없음 / modify: excludeId 포함)
4. await fetch → response.ok 확인
5. booked = await res.json()  // string[]
6. #time 옵션 전체 순회:
   - booked 포함: disabled=true, classList.add('text-slate-400'),
                  textContent = originalText + " (예약불가)"
   - booked 미포함: disabled=false, classList.remove('text-slate-400'),
                    textContent = originalText  (data-original-text 복원)
7. catch → console.error + resetSlots()
```

#### direct-reservation.mustache

- `excludeId` 파라미터 없음 (신규 예약)
- 추가 변경: Flatpickr `onChange` 콜백에 위 공통 로직 삽입

#### reservation-modify.mustache

- 페이지 로드 시 의사/날짜 모두 미선택 상태(pre-fill 없음) → 초기 로드 시 API 호출 불필요
- Mustache 템플릿에서 예약 ID를 JS 변수로 주입: `const excludeId = {{id}};`
- `excludeId`를 fetch URL에 포함: `&excludeId=${excludeId}`
- Flatpickr에 `minDate: 'today'` 추가 (기존 미설정 상태, 과거 날짜 선택 방지)

---

## 데이터 흐름

```
[의사 변경]
  → datePicker.clear() 발생
  → onChange([], "") → 가드(dateStr 비어 있음) → resetSlots()

[날짜 선택 (Flatpickr onChange)]
  → doctorId, dateStr 유효성 확인
  → fetch /api/reservation/booked-slots?doctorId=&date=[&excludeId=]
  → booked 슬롯 목록 수신
  → #time 옵션 비활성화/복원 처리
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
