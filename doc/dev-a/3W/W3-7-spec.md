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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Spring Data JPA의 `@Query`로 직접 JPQL 쿼리를 작성한 Repository 메서드입니다. 첫 번째는 신규 예약 페이지용(이미 예약된 시간 전체 조회), 두 번째는 예약 변경 페이지용(수정 중인 예약 자신은 제외하고 조회)입니다.
> - **왜 이렇게 썼는지**: JPQL은 DB 테이블 대신 Java 클래스와 필드명으로 쿼리를 작성하는 언어입니다. `@Param`은 쿼리의 `:파라미터명`과 메서드 파라미터를 연결합니다. 예약 변경 시 자신의 기존 슬롯을 제외하지 않으면, 변경 중인 슬롯도 "(예약불가)"로 표시되어 자기 자신의 시간을 선택하지 못하는 문제가 생깁니다.
> - **쉽게 말하면**: "이 의사, 이 날짜, 취소 아닌 예약들의 시간 목록 주세요. 단 내가 지금 수정 중인 예약은 빼고요."

#### 2. ReservationService — `getBookedTimeSlots` 오버로드 2개 추가

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 비즈니스 로직 층(Service)에서 Repository 메서드를 호출합니다. `ReservationStatus.CANCELLED`를 항상 제외 상태로 전달해 취소된 예약 슬롯은 "예약불가"로 표시되지 않게 합니다.
> - **왜 이렇게 썼는지**: 서비스 층이 "CANCELLED 제외"라는 비즈니스 규칙을 담당합니다. 컨트롤러나 Repository가 이 규칙을 직접 알 필요가 없도록 서비스에서 처리합니다. 두 메서드의 이름이 같지만 파라미터가 달라 Java 오버로드를 활용합니다.
> - **쉽게 말하면**: 취소된 예약은 다시 예약할 수 있으므로, 취소 예약을 제외하고 진짜 사용 중인 시간만 골라오는 서비스 함수입니다.

#### 3. ReservationApiController — `/api/reservation/booked-slots` 추가

```
GET /api/reservation/booked-slots
  ?doctorId={Long}          (필수)
  &date={yyyy-MM-dd}        (필수)
  &excludeId={Long}         (선택, 변경 페이지에서만 전달)

→ 200 OK: ["09:00", "10:30", ...]
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: API의 요청 URL 형식과 응답 형식을 정의한 명세입니다. 의사 ID(`doctorId`)와 날짜(`date`)는 반드시 있어야 하고, 예약 변경 페이지에서는 현재 수정 중인 예약 ID(`excludeId`)도 함께 보냅니다. 응답으로 예약된 시간 문자열 배열을 받습니다.
> - **왜 이렇게 썼는지**: 날짜 형식을 `yyyy-MM-dd`(예: `2026-03-17`)로 고정해 서버에서 `LocalDate.parse()`로 파싱합니다. `excludeId`를 선택적으로 만들어 신규 예약·변경 예약 두 상황에서 하나의 API를 재사용합니다.
> - **쉽게 말하면**: 프론트엔드와 백엔드가 "어떤 형식으로 요청하고, 어떤 형식으로 응답받을지" 약속한 계약서입니다.

---

### 프론트엔드

#### 페이지 로드 시 초기화 — `data-original-text` 저장

```javascript
document.querySelectorAll('#time option').forEach(opt => {
  opt.dataset.originalText = opt.textContent;
});
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 페이지가 처음 로드될 때 시간 선택 드롭다운의 모든 option 텍스트를 `data-original-text` 속성에 백업해둡니다.
> - **왜 이렇게 썼는지**: 나중에 "(예약불가)"를 텍스트에 추가할 때, 날짜를 다시 선택하면 원래 텍스트로 되돌려야 합니다. 원본을 미리 저장해두지 않으면 "09:00 (예약불가) (예약불가)"처럼 중복 추가되는 버그가 생깁니다. `dataset`은 HTML 요소에 커스텀 데이터를 저장하는 표준 방법입니다.
> - **쉽게 말하면**: 나중에 원래대로 되돌릴 수 있도록 원본 시간표를 먼저 복사해두는 것입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 시간 슬롯 드롭다운의 모든 option을 초기 상태(선택 가능, 회색 아님, 원본 텍스트)로 되돌리는 함수입니다. 의사를 바꾸거나, API 호출이 실패했을 때 호출합니다.
> - **왜 이렇게 썼는지**: 공통 초기화 로직을 함수로 분리해 여러 곳에서 재사용할 수 있습니다. `disabled = false`로 선택 가능 상태로, `classList.remove('text-slate-400')`으로 회색 스타일 제거, `textContent`를 저장해둔 원본으로 복원합니다.
> - **쉽게 말하면**: 시간 선택창을 깨끗하게 초기화(리셋)하는 청소 함수입니다.

#### Flatpickr `onChange` 공통 로직

```
1. doctorId, dateStr 유효성 확인 → 미충족 시 resetSlots() 후 return
2. fetch /api/reservation/booked-slots?doctorId=&date=[&excludeId=]
3. booked 슬롯 목록 수신
4. #time 옵션 순회: 포함 시 disabled + "(예약불가)", 미포함 시 originalText 복원
5. catch → console.error + resetSlots()
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 달력에서 날짜를 선택할 때마다 실행되는 로직의 순서입니다. 의사와 날짜가 제대로 선택됐는지 확인하고, 서버에서 예약된 슬롯을 가져와 화면에 반영합니다. 오류가 나면 슬롯을 초기화합니다.
> - **왜 이렇게 썼는지**: 1번의 유효성 확인(가드 조건)은 의사가 선택되지 않은 상태에서 날짜를 선택하는 잘못된 상황을 차단합니다. 5번의 catch는 네트워크 오류 등 예외 상황에서도 슬롯이 잘못 표시되지 않도록 안전망 역할을 합니다.
> - **쉽게 말하면**: "의사도 선택하고 날짜도 골랐으면, 그 날의 예약된 시간을 서버에 물어보고 화면을 업데이트하는" 5단계 순서입니다.

#### reservation-modify.mustache 추가사항

- `const excludeId = {{id}};` — Mustache로 예약 ID 주입
- Flatpickr에 `minDate: 'today'` 추가 (과거 날짜 선택 방지)

---

## 데이터 흐름

```
[의사 변경] → datePicker.clear() → onChange([], "") → 가드 → resetSlots()
[날짜 선택] → fetch booked-slots → 슬롯 비활성화/복원 처리
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 의사 변경과 날짜 선택 두 가지 이벤트에 따른 전체 데이터 흐름을 나타냅니다. 의사를 바꾸면 달력이 초기화되고 슬롯도 리셋됩니다. 날짜를 선택하면 예약된 슬롯을 서버에서 받아와 표시합니다.
> - **왜 이렇게 썼는지**: 의사를 바꿀 때 기존에 선택됐던 날짜를 자동으로 지워줘야(`datePicker.clear()`) 이전 의사 기준의 슬롯 정보가 남아있는 혼란을 방지합니다. 이때 onChange가 빈 값으로 실행되고 가드를 통과하지 못해 `resetSlots()`이 호출됩니다.
> - **쉽게 말하면**: 의사를 바꾸면 날짜와 시간이 초기화되고, 날짜를 새로 고르면 그 날의 예약 현황이 시간표에 반영됩니다.

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|---------|
| `ReservationRepository.java` | `findBookedTimeSlots`, `findBookedTimeSlotsExcluding` 추가 |
| `ReservationService.java` | `getBookedTimeSlots` 오버로드 2개 추가 |
| `ReservationApiController.java` | `GET /api/reservation/booked-slots` 엔드포인트 추가 |
| `direct-reservation.mustache` | 초기화 + Flatpickr `onChange` + `resetSlots()` 추가 |
| `reservation-modify.mustache` | 동일 + `excludeId` 전달 + `minDate: 'today'` |
