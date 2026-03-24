# W6-2 Workflow — AI 증상 분석 예약 자동 입력 + 시간 슬롯 처리

> **작성일**: 2026-03-24
> **브랜치**: `feature/dev-a`
> **목표**: AI 추천 결과 후 첫 번째 의사 자동 선택 및 날짜/시간 슬롯 비활성화 처리

---

## 전체 흐름

```
증상 분석 완료
  → loadDoctors(departmentId) 호출
  → 의사 목록 렌더링 후 첫 번째 의사 자동 선택
  → doctor change 이벤트 dispatch → 날짜 picker 활성화 (availableDays 반영)
  → 사용자가 날짜 선택
  → Flatpickr onChange → /api/reservation/booked-slots 조회
  → 예약 불가 슬롯 + 오늘이면 지난 시간 비활성화
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 문제 1 | `loadDoctors()` 완료 후 의사가 자동 선택되지 않아 날짜 picker가 비활성 상태 유지 |
| 문제 2 | 시간 슬롯이 15:30까지만 있어 `direct-reservation.mustache`와 불일치 |
| 수정 방식 | `loadDoctors()` 내에서 첫 번째 의사 선택 + `change` 이벤트 dispatch |
| 슬롯 비활성화 | Flatpickr onChange 로직 이미 존재 — 의사 자동 선택 후 정상 동작 |
| 시간 슬롯 | 16:00 ~ 17:30 (4개) 추가 (`direct-reservation.mustache`와 동기화) |
| 참조 | `남은 작업 리스트.md` 강태오 작업 항목 |

---

## 실행 흐름

```
[변경 전]
loadDoctors(departmentId)
  → 의사 select 옵션 채움
  → 아무것도 선택되지 않은 상태로 종료
  → 날짜 picker: 비활성(disable all) 유지
  → 사용자가 수동으로 의사 선택해야 함

[변경 후]
loadDoctors(departmentId)
  → 의사 select 옵션 채움
  → doctors[0] 자동 선택 (doctorSelect.value = doctors[0].id)
  → doctorSelect.dispatchEvent(new Event('change'))
      ↓
  doctor change 이벤트 핸들러 실행
  → availableDays 파싱
  → Flatpickr disable 규칙 갱신 → 진료 가능 요일만 선택 가능
  → enableDatePicker() 호출
      ↓
  사용자가 날짜 선택
  → Flatpickr onChange
  → /api/reservation/booked-slots 조회
  → 예약된 슬롯 disabled + "(예약불가)" 표시
  → 오늘 선택 시 지난 시간 disabled
```

---

## UI Mockup

```
[변경 전]
증상 분석 완료 후
  전문의 선택  [선택해주세요 ▼]  ← 의사 목록은 채워지나 선택 없음
  예약 날짜    [전문의 선택 후...]  ← 비활성 유지
  예약 시간    [선택해주세요 ▼]
               09:00 ~ 15:30

[변경 후]
증상 분석 완료 후
  전문의 선택  [의사이영희 ▼]     ← 첫 번째 의사 자동 선택
  예약 날짜    [날짜를 선택해주세요] ← 즉시 활성화
  예약 시간    [선택해주세요 ▼]
               09:00 ~ 17:30     ← 16:00~17:30 추가
               (날짜 선택 시 예약불가/지난시간 비활성)
```

---

## 작업 목록

1. `symptom-reservation.mustache` — 시간 슬롯 16:00 ~ 17:30 옵션 4개 추가
2. `symptom-reservation.mustache` — `loadDoctors()` 내 첫 번째 의사 자동 선택 + `change` 이벤트 dispatch
3. `남은 작업 리스트.md` — 강태오 작업 항목 체크 처리

---

## 작업 진행내용

- [x] 시간 슬롯 16:00 ~ 17:30 추가
- [x] `loadDoctors()` 첫 번째 의사 자동 선택 + `change` dispatch
- [x] `남은 작업 리스트.md` 완료 체크

---

## 실행 흐름에 대한 코드

### loadDoctors() — 자동 선택 추가

```javascript
async function loadDoctors(departmentId) {
  const doctorSelect = document.getElementById('doctor');
  doctorSelect.innerHTML = '<option value="">선택해주세요</option>';

  const res = await fetch(`/api/reservation/doctors?departmentId=${departmentId}`);
  if (!res.ok) throw new Error(`의사 목록 조회 실패: ${res.status}`);
  const json = await res.json();
  const doctors = json.body || json;

  doctors.forEach(doctor => {
    const option = document.createElement('option');
    option.value = doctor.id;
    option.textContent = doctor.name;
    option.dataset.availableDays = doctor.availableDays;
    doctorSelect.appendChild(option);
  });

  // AI 추천 후 첫 번째 의사 자동 선택 → change 이벤트로 날짜 picker 활성화
  if (doctors.length > 0) {
    doctorSelect.value = String(doctors[0].id);
    doctorSelect.dispatchEvent(new Event('change'));
  }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 의사 목록을 불러온 후 첫 번째 의사를 자동으로 선택하고, `change` 이벤트를 강제로 발생시킵니다.
> - **왜 이렇게 썼는지**: `dispatchEvent(new Event('change'))`는 사용자가 드롭다운을 직접 바꾼 것처럼 이벤트를 프로그래밍으로 발생시킵니다. 이를 통해 기존 `change` 이벤트 핸들러(availableDays 파싱, Flatpickr 갱신)가 자동으로 실행됩니다.
> - **쉽게 말하면**: AI가 진료과를 추천하면 그 과의 첫 번째 의사를 자동으로 고르고, 날짜 선택창도 바로 활성화되도록 하는 것입니다.

### 시간 슬롯 확장

```html
<option value="16:00">16:00</option>
<option value="16:30">16:30</option>
<option value="17:00">17:00</option>
<option value="17:30">17:30</option>
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| AI 분석 후 자동 선택 | 증상 분석 완료 | 첫 번째 의사 자동 선택 + 날짜 picker 즉시 활성화 |
| 날짜 선택 후 슬롯 비활성 | 오늘 날짜 선택 | 지난 시간 비활성화 |
| 날짜 선택 후 슬롯 비활성 | 예약된 시간 존재 | 해당 슬롯 "(예약불가)" 표시 |
| 시간 슬롯 범위 | - | 09:00 ~ 17:30 (30분 단위) 표시 |

---

## 완료 기준

- [x] AI 분석 후 전문의 드롭다운에 첫 번째 의사 자동 선택
- [x] 자동 선택 후 날짜 picker 즉시 활성화 (availableDays 반영)
- [x] 날짜 선택 시 예약 불가 슬롯 비활성화 동작
- [x] 시간 슬롯 09:00 ~ 17:30 표시 (direct-reservation.mustache와 동기화)
