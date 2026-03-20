# W5-4 Report — 예약 변경 화면 지난 시간 슬롯 비활성화

> **작성일**: 2026-03-20
> **브랜치**: `feature/reservation`
> **빌드**: 브라우저 직접 확인

---

## 작업 완료 목록

| # | 항목 | 상태 |
|---|------|------|
| 1 | `reservation-modify.mustache` — 오늘 날짜 선택 시 현재 시각 이전 슬롯 회색 + 선택 불가 | ✅ |
| 2 | `reservation-modify.mustache` — 현재 예약과 동일한 날짜+시간 슬롯 회색 + 선택 불가 | ✅ |

---

## 변경 내용 상세

### reservation-modify.mustache

#### 1. 현재 예약 날짜·시간 변수 주입

```javascript
// 현재 예약의 날짜·시간 (서버에서 주입) — 동일 날짜 선택 시 해당 슬롯 선택 불가 처리
const currentReservationDate = '{{reservationDate}}';
const currentTimeSlot = '{{timeSlot}}';
```

#### 2. datePicker.onChange 슬롯 렌더링 로직 변경

| 조건 | 변경 전 | 변경 후 |
|------|---------|---------|
| 오늘 날짜 + 지난 시간 | 선택 가능 | 회색 글자 + 선택 불가 |
| 현재 예약 날짜 + 현재 예약 시간 | 선택 가능 | 회색 글자 + 선택 불가 |
| 내일 이후 날짜 | 선택 가능 | 정상 유지 |
| booked 슬롯 | "(예약불가)" | 유지 |

#### 슬롯 판단 우선순위

```
1. booked 슬롯          → 회색 + "(예약불가)" + disabled
2. 현재 예약 날짜+시간  → 회색 + disabled (텍스트 변경 없음)
3. 오늘 날짜 + 지난시간 → 회색 + disabled (텍스트 변경 없음)
4. 선택 가능 슬롯       → 정상 표시
```

---

## 참조

- `direct-reservation.mustache` — 오늘 날짜 지난 시간 비활성화 패턴 동일 적용
