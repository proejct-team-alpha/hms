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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 서버에서 렌더링한 현재 예약의 날짜와 시간 값을 JavaScript 변수에 저장합니다. `{{reservationDate}}`와 `{{timeSlot}}`은 Mustache 문법으로, 서버가 HTML을 만들 때 실제 값(예: `2026-03-20`, `10:00`)으로 치환됩니다.
> - **왜 이렇게 썼는지**: 브라우저에서 실행되는 JavaScript는 서버 데이터에 직접 접근할 수 없습니다. Mustache 템플릿이 HTML을 생성할 때 서버 데이터를 JS 변수에 박아 넣는 방식으로 브라우저에 전달합니다. 이 값을 사용해 현재 예약 슬롯을 선택 불가 처리합니다.
> - **쉽게 말하면**: 서버가 "지금 예약된 날짜랑 시간은 이거야"라고 HTML 안에 적어두면, 브라우저의 JavaScript가 그 값을 읽어서 활용하는 구조입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 날짜 선택 시 예약 시간 슬롯(드롭다운 옵션)들을 4가지 우선순위 규칙에 따라 회색(선택 불가) 또는 정상(선택 가능)으로 렌더링합니다.
> - **왜 이렇게 썼는지**: `padStart(2, '0')`은 날짜를 `01`, `02`처럼 두 자리로 맞춰 서버 날짜 형식과 일치시킵니다. `getMonth() + 1`은 JS의 월이 0부터 시작하기 때문입니다. `h * 60 + m <= nowMinutes`는 슬롯 시각을 분으로 환산해 현재 시각과 비교합니다. `opt.disabled = true`는 드롭다운 옵션을 선택 불가 상태로 만들고, `classList.add('text-slate-400')`은 TailwindCSS 클래스로 회색 텍스트를 적용합니다. 우선순위 순서가 중요한데, `if-else if` 체인이기 때문에 더 높은 순위 조건이 먼저 적용됩니다.
> - **쉽게 말하면**: 시간표에서 "이미 예약됨", "현재 내 예약", "지난 시간", "선택 가능" 순서로 각 시간 칸의 색깔과 선택 가능 여부를 결정하는 로직입니다.

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
