# W2-3 Workflow — 의사 진료 가능 요일 기반 날짜 제한 (Flatpickr)

> **작성일**: 2W
> **목표**: 의사 선택 시 `available_days` 기반으로 Flatpickr 캘린더에서 불가 요일 비활성화

---

## 전체 흐름

```
DoctorDto에 availableDays 추가
  → 의사 선택 이벤트 → availableDays 파싱
  → Flatpickr 재초기화 (불가 요일 회색 + 클릭 불가)
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 의사 진료 가능 요일 외 날짜 선택 불가 |
| 캘린더 라이브러리 | Flatpickr 적용 |
| availableDays 형식 | `MON,WED,FRI` (쉼표 구분) |
| 불가 날짜 처리 | 회색 표시 + 클릭 불가 |
| 애니메이션 | 비활성화 |

---

## 실행 흐름

```
[1] DoctorDto.java — availableDays 필드 추가
    └─ Doctor 엔티티의 availableDays 그대로 반환

[2] direct-reservation.mustache 수정
    ├─ <head>에 Flatpickr CSS/JS 로드
    ├─ <input type="date"> → <input type="text" readonly> 교체
    └─ 의사 change 이벤트 → availableDays 파싱 → Flatpickr 재설정
```

---

## UI Mockup

```
[의사 선택 후 캘린더]

     2026년 03월
Mo Tu We Th Fr Sa Su
                 1
 2  3  4  5  6  7  8   ← MON,WED,FRI만 활성 시
 9 10 11 12 13 14 15      (화,목,토,일 = 회색 클릭불가)
16 17 18 19 20 21 22
23 24 25 26 27 28 29
30 31
```

---

## 작업 목록

1. `DoctorDto.java` — `availableDays` 필드 추가
2. `direct-reservation.mustache` — `<head>` Flatpickr CDN 추가
3. `direct-reservation.mustache` — `<input type="date">` → Flatpickr용 `<input type="text" readonly>` 교체
4. `direct-reservation.mustache` — JS 요일 매핑 + Flatpickr 초기화 + 의사 change 이벤트 추가

---

## 작업 진행내용

- [x] DoctorDto availableDays 필드 추가
- [x] Flatpickr CDN 추가
- [x] input 교체
- [x] JS 요일 매핑 + 이벤트 추가

---

## 실행 흐름에 대한 코드

### DoctorDto — availableDays 추가

```java
@Getter
public class DoctorDto {
    private final Long id;
    private final String name;
    private final String availableDays; // 추가

    public DoctorDto(Doctor doctor) {
        this.id = doctor.getId();
        this.name = doctor.getStaff().getName();
        this.availableDays = doctor.getAvailableDays(); // 추가
    }
}
```

### direct-reservation.mustache — Flatpickr CDN

```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css">
<script src="https://cdn.jsdelivr.net/npm/flatpickr"></script>
```

### direct-reservation.mustache — input 교체

```html
<!-- 변경 전 -->
<input type="date" id="date" name="reservationDate" required />

<!-- 변경 후 -->
<input type="text" id="date" name="reservationDate" required readonly
  placeholder="날짜를 선택해주세요" />
```

### direct-reservation.mustache — JS

```javascript
// 요일 매핑: JS getDay() 기준 (0=일 ~ 6=토)
const DAY_MAP = { SUN: 0, MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6 };

let datePicker = null;

function initDatePicker(availableDayNums) {
    if (datePicker) datePicker.destroy();
    datePicker = flatpickr('#date', {
        dateFormat: 'Y-m-d',
        animate: false,
        disable: [date => !availableDayNums.includes(date.getDay())]
    });
}

// 의사 선택 시 availableDays 파싱 → Flatpickr 재초기화
document.getElementById('doctor').addEventListener('change', function () {
    const availableDaysStr = this.options[this.selectedIndex].dataset.availableDays || '';
    const availableDayNums = availableDaysStr
        .split(',').map(d => DAY_MAP[d.trim()]).filter(n => n !== undefined);
    document.getElementById('date').value = '';
    initDatePicker(availableDayNums);
});

// 의사 선택 전: 전체 비활성
initDatePicker([]);
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 의사 선택 전 | 캘린더 열기 | 모든 날짜 비활성 |
| MON,WED,FRI 의사 선택 | 캘린더 확인 | 월/수/금만 활성 |
| 의사 변경 | 다른 의사 선택 | 날짜 초기화 + Flatpickr 재설정 |

---

## 완료 기준

- [x] DoctorDto에 availableDays 포함
- [x] 의사 선택 시 해당 진료 가능 요일만 캘린더에서 선택 가능
- [x] 불가 요일 회색 표시 + 클릭 불가
- [x] 의사 변경 시 날짜 초기화
