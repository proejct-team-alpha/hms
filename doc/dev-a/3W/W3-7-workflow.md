# W3-7 Workflow — 코드 리뷰 반영 + 예약 불가 슬롯 비활성화

> **작성일**: 3W
> **목표**: 코드 리뷰 8건 반영 + 의사·날짜 선택 시 이미 예약된 시간 슬롯 `disabled` 처리

---

## 전체 흐름

```
파트 1: 코드 리뷰 8건 반영 (보안·DTO·인증·Lock 등)
파트 2: booked-slots API → 시간 슬롯 비활성화
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 코드 리뷰 항목 | 8건 (C-05, H-01, H-02, H-03, H-06, H-13, M-03, M-10, M-11) |
| 슬롯 비활성화 | 의사+날짜 선택 시 booked-slots API 호출 |
| 표시 방식 | 회색 + "(예약불가)" + disabled |
| 설계 문서 | `W3-7-spec.md` 참조 |

---

## 실행 흐름

```
파트 1 — 코드 리뷰 반영
  C-05: direct-reservation fetch async/await + response.ok 검사
  H-01: IDOR 방어 — 취소/변경 시 phone 소유권 검증
  H-02: ReservationNumberGenerator 통일
  H-03: TOCTOU 방어 — @Lock PESSIMISTIC_WRITE 적용
  H-06: Admin 전용 쿼리 ReservationRepository에서 제거
  H-13: DTO 이름 변경
  M-03: @Pattern 검증 추가 (phone, timeSlot)
  M-10: index.mustache 파셜 교체
  M-11: 진료과 드롭다운 서버 바인딩

파트 2 — 슬롯 비활성화
  날짜 변경(onChange) → fetch /api/reservation/booked-slots
    → 응답: 예약된 시간 목록
    → forEach: booked 슬롯 → disabled + "(예약불가)"
```

---

## UI Mockup

```
[예약 시간 선택 - booked 슬롯 포함]

┌──────────────────────────────┐
│ 선택해주세요              ▼  │
├──────────────────────────────┤
│        09:00                 │  ← 선택 가능
│ [회색] 09:30  (예약불가)     │  ← booked
│        10:00                 │  ← 선택 가능
└──────────────────────────────┘
```

---

## 작업 목록

1. `ReservationRepository` — `findBookedTimeSlots`, `findBookedTimeSlotsExcluding` 쿼리 추가
2. `ReservationService` — `getBookedTimeSlots` 오버로드 2개 추가
3. `ReservationApiController` — `GET /api/reservation/booked-slots` 엔드포인트 추가
4. `direct-reservation.mustache` — `data-original-text` + `resetSlots()` + Flatpickr `onChange` 추가
5. `reservation-modify.mustache` — `excludeId` 주입 + `minDate: 'today'` + 동일 슬롯 로직
6. `ReservationController` — H-01 phone 소유권 검증 추가
7. `ReservationNumberGenerator` — H-02 유틸 생성
8. `CreateReservationRequest`, `UpdateReservationRequest` — H-13 DTO 이름 변경
9. `ReservationServiceTest` — `getBookedTimeSlots` 테스트 2건 추가

---

## 작업 진행내용

- [x] ReservationRepository booked-slots 쿼리 추가
- [x] ReservationService getBookedTimeSlots 추가
- [x] ReservationApiController booked-slots 엔드포인트
- [x] direct-reservation.mustache onChange 슬롯 비활성화
- [x] reservation-modify.mustache excludeId + minDate
- [x] 코드 리뷰 8건 반영
- [x] ReservationServiceTest 테스트 추가

---

## 실행 흐름에 대한 코드

### ReservationApiController — booked-slots

```java
@GetMapping("/booked-slots")
public List<String> getBookedSlots(
        @RequestParam Long doctorId,
        @RequestParam String date,
        @RequestParam(required = false) Long excludeId) {
    LocalDate reservationDate = LocalDate.parse(date);
    return excludeId != null
        ? reservationService.getBookedTimeSlots(doctorId, reservationDate, excludeId)
        : reservationService.getBookedTimeSlots(doctorId, reservationDate);
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 특정 의사(`doctorId`)와 날짜(`date`)를 받아 이미 예약된 시간 슬롯 목록을 반환하는 REST API 엔드포인트입니다. `excludeId`가 있으면 해당 예약은 제외하고 조회합니다(예약 변경 페이지에서 사용).
> - **왜 이렇게 썼는지**: `@GetMapping`은 HTTP GET 방식의 요청을 처리합니다. `@RequestParam(required = false)`는 선택적 파라미터로, 없어도 오류가 나지 않습니다. `excludeId` 여부에 따라 다른 서비스 메서드를 호출하는 분기 처리를 삼항 연산자로 간결하게 표현했습니다.
> - **쉽게 말하면**: "이 의사의 이 날짜에 이미 예약된 시간이 뭐뭐야?" 라고 물으면 목록을 돌려주는 API입니다.

### direct-reservation.mustache — onChange 슬롯 비활성화

```javascript
// 슬롯 텍스트 원본 저장
document.querySelectorAll('#time option').forEach(opt => {
    opt.dataset.originalText = opt.textContent;
});

function resetSlots() {
    document.querySelectorAll('#time option').forEach(opt => {
        opt.disabled = false;
        opt.classList.remove('text-slate-400');
        opt.textContent = opt.dataset.originalText;
    });
}

// Flatpickr onChange: 예약 불가 슬롯 비활성화
onChange: async (selectedDates, dateStr) => {
    const booked = await fetch(`/api/reservation/booked-slots?...`).then(r => r.json());
    document.querySelectorAll('#time option').forEach(opt => {
        if (booked.includes(opt.dataset.originalText)) {
            opt.disabled = true;
            opt.classList.add('text-slate-400');
            opt.textContent = opt.dataset.originalText + ' (예약불가)';
        } else {
            opt.disabled = false;
            opt.classList.remove('text-slate-400');
            opt.textContent = opt.dataset.originalText;
        }
    });
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 페이지 로드 시 각 시간 option의 원본 텍스트를 `data-originalText` 속성에 저장해둡니다. 날짜를 선택하면 서버에서 예약된 시간 목록을 받아와, 해당 슬롯은 회색으로 비활성화하고 " (예약불가)" 텍스트를 붙입니다. 이미 비활성화됐던 슬롯도 원본 텍스트로 올바르게 복원할 수 있습니다.
> - **왜 이렇게 썼는지**: `data-originalText`를 미리 저장하지 않으면 날짜를 여러 번 바꿀 때 "(예약불가)(예약불가)" 같이 텍스트가 중복 추가되는 버그가 생깁니다. `dataset`은 HTML 요소에 커스텀 데이터를 저장하는 표준 방법입니다. `resetSlots()`은 슬롯 상태를 초기화하는 헬퍼 함수입니다.
> - **쉽게 말하면**: 시간표를 보여줄 때 이미 예약된 자리는 회색으로 "예약불가" 표시를 해주고, 날짜를 바꾸면 표시를 깨끗하게 리셋한 후 다시 새로 표시하는 기능입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 날짜 선택 | 예약된 슬롯 존재 | 해당 슬롯 회색 + disabled |
| 날짜 변경 | 다른 날짜 선택 | 슬롯 초기화 후 재조회 |
| booked-slots API | GET /api/reservation/booked-slots | 예약된 시간 목록 JSON |
| H-01 소유권 검증 | 다른 phone으로 취소 시도 | 403 또는 오류 |
| ReservationServiceTest | getBookedTimeSlots 2건 | GREEN |

---

## 완료 기준

- [x] 날짜 선택 시 예약 불가 슬롯 회색 + disabled
- [x] booked-slots API 정상 응답
- [x] 코드 리뷰 8건 반영
- [x] ReservationServiceTest 통과
