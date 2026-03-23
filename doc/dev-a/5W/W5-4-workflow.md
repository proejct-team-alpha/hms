# W5-4 Workflow — 예약 변경 화면 시간 슬롯 비활성화

> **작성일**: 5W
> **브랜치**: `feature/reservation`
> **목표**: 예약 변경 화면에서 ① 현재 시각 이전 슬롯, ② 현재 예약과 동일한 날짜+시간 슬롯을 회색 글자로 표시하고 선택 불가 처리

---

## 전체 흐름

```
사용자가 날짜 선택
  → datePicker onChange 트리거
  → booked-slots API 호출 (현재 예약 excludeId 제외)
  → 응답 수신 후 슬롯 우선순위 순으로 렌더링
      ├─ 1순위: booked 슬롯           → 회색 + "(예약불가)" + disabled
      ├─ 2순위: 현재 예약 날짜+시간   → 회색 + disabled (텍스트 변경 없음)
      ├─ 3순위: 오늘 날짜 + 지난 시간 → 회색 + disabled (텍스트 변경 없음)
      └─ 4순위: 선택 가능 슬롯        → 정상 표시
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 1 | 오늘 날짜 선택 시 현재 시각 이전 슬롯 선택 불가 |
| 요청 2 | 현재 예약과 동일한 날짜+시간 슬롯 선택 불가 |
| 표시 방식 | 회색 글자만 (텍스트 변경 없음, "지난 시간" 문구 없음) |
| 현재 예약 슬롯 표시 범위 | 동일 날짜 선택 시에만 적용 (다른 날짜 선택 시 정상) |
| 참조 구현 | `direct-reservation.mustache` 동일 패턴 |

---

## 실행 흐름

```
[1] reservation-modify.mustache — 현재 예약 날짜·시간 변수 주입
    - {{#reservation}}{{#canModify}} 블록에 currentReservationDate, currentTimeSlot 추가
[2] reservation-modify.mustache — datePicker.onChange 핸들러 수정
    - const booked = await res.json(); 이후 isToday / nowMinutes 계산 추가
    - booked → 현재 예약 시간 → 지난 시간 → 정상 순서로 4단계 분기 처리
```

---

## UI Mockup

```
[현재 예약: 2026-03-20 / 10:00]
오늘 날짜(2026-03-20) 선택, 현재 시각 10:30 기준

┌──────────────────────────────┐
│ 선택해주세요              ▼  │
├──────────────────────────────┤
│ [회색] 09:00                 │  ← 지난 시간
│ [회색] 09:30                 │  ← 지난 시간
│ [회색] 10:00                 │  ← 현재 예약 시간 + 지난 시간
│ [회색] 10:30                 │  ← 지난 시간 (현재 포함)
│        11:00                 │  ← 선택 가능
│        11:30                 │  ← 선택 가능
│ [회색] 12:00  (예약불가)     │  ← booked
│        12:30                 │  ← 선택 가능
│        ...                   │
└──────────────────────────────┘

내일 이후 날짜 선택 시 (현재 예약 날짜 아님)
┌──────────────────────────────┐
│        09:00                 │  ← 선택 가능
│        09:30                 │  ← 선택 가능
│        10:00                 │  ← 선택 가능 (다른 날짜이므로 정상)
│        ...                   │
└──────────────────────────────┘
```

---

## 작업 목록

1. `reservation-modify.mustache` — 현재 예약 날짜·시간 변수 주입 (`currentReservationDate`, `currentTimeSlot`)
2. `reservation-modify.mustache` — `datePicker.onChange` 핸들러 수정 (4단계 슬롯 분기)

---

## 작업 진행내용

- [x] `reservation-modify.mustache` — 현재 예약 날짜·시간 변수 주입
- [x] `reservation-modify.mustache` — onChange 핸들러 4단계 슬롯 분기 처리

---

## 실행 흐름에 대한 코드

### 현재 예약 변수 주입 (Mustache)

```javascript
const currentReservationDate = '{{reservationDate}}';
const currentTimeSlot = '{{timeSlot}}';
```

### datePicker.onChange — 4단계 슬롯 분기

```javascript
const booked = await res.json();

// 오늘 날짜 여부 판단 (로컬 날짜 기준, UTC 오차 방지)
const today = new Date();
const todayStr = today.getFullYear() + '-'
  + String(today.getMonth() + 1).padStart(2, '0') + '-'
  + String(today.getDate()).padStart(2, '0');
const isToday = dateStr === todayStr;

// 현재 시각을 분 단위로 변환 (비교용)
const nowMinutes = today.getHours() * 60 + today.getMinutes();

document.querySelectorAll('#time option').forEach(opt => {
  const original = opt.dataset.originalText;
  if (!original) return;

  if (booked.includes(original)) {
    // 1순위: 이미 예약된 슬롯 — 회색 + "(예약불가)" + disabled
    opt.disabled = true;
    opt.classList.add('text-slate-400');
    opt.textContent = original + ' (예약불가)';
  } else if (dateStr === currentReservationDate && original === currentTimeSlot) {
    // 2순위: 현재 예약과 동일한 날짜+시간 — 회색 + disabled (텍스트 변경 없음)
    opt.disabled = true;
    opt.classList.add('text-slate-400');
    opt.textContent = original;
  } else if (isToday && opt.value) {
    // 3순위: 오늘 날짜 — 현재 시각 이전 슬롯 회색 + disabled
    const [h, m] = opt.value.split(':').map(Number);
    if (h * 60 + m <= nowMinutes) {
      opt.disabled = true;
      opt.classList.add('text-slate-400');
      opt.textContent = original;
    } else {
      opt.disabled = false;
      opt.classList.remove('text-slate-400');
      opt.textContent = original;
    }
  } else {
    // 4순위: 내일 이후 날짜 — 모든 슬롯 정상 표시
    opt.disabled = false;
    opt.classList.remove('text-slate-400');
    opt.textContent = original;
  }
});
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 오늘 날짜 선택 | 현재 시각 10:30 | 09:00~10:30 회색 + disabled |
| 오늘 날짜 선택 | 현재 예약 시간 = 10:00 | 10:00 회색 + disabled (중복 적용) |
| 오늘 날짜 선택 | booked 슬롯 포함 | "(예약불가)" 유지 |
| 내일 이후 날짜 선택 | 현재 예약 날짜 아님 | 전체 슬롯 정상 표시 |
| 현재 예약 날짜 재선택 | 현재 예약 시간 = 14:00 | 14:00 회색 + disabled |
| 날짜 변경 | 오늘→내일 재선택 | 회색 슬롯 초기화 |

---

## 완료 기준

- [x] 오늘 날짜 선택 시 현재 시각 이전 슬롯 → 회색 + 선택 불가
- [x] 현재 예약과 동일한 날짜+시간 슬롯 → 회색 + 선택 불가
- [x] 텍스트 변경 없음 (회색 글자만, 문구 없음)
- [x] 내일 이후 날짜 선택 시 전체 슬롯 정상 표시
- [x] 기존 예약불가(booked) 슬롯 동작 유지
