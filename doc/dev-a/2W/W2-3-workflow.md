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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 의사 정보 DTO에 진료 가능 요일 정보(`availableDays`)를 추가합니다. API 응답 JSON에 `"availableDays": "MON,WED,FRI"` 형태로 포함됩니다.
> - **왜 이렇게 썼는지**: 브라우저가 캘린더에서 특정 요일을 비활성화하려면, 어떤 요일이 가능한지 알아야 합니다. 서버가 의사 목록 API 응답에 이 정보를 함께 담아 전달하면 JS가 이를 활용할 수 있습니다.
> - **쉽게 말하면**: 의사 소개 카드에 "진료 요일: 월, 수, 금"이라는 항목을 추가하는 것입니다.

### direct-reservation.mustache — Flatpickr CDN

```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css">
<script src="https://cdn.jsdelivr.net/npm/flatpickr"></script>
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Flatpickr 라이브러리(날짜 선택 달력)의 CSS(스타일)와 JS(동작)를 외부 CDN에서 불러옵니다.
> - **왜 이렇게 썼는지**: 네이티브 `<input type="date">`는 특정 요일만 비활성화하는 기능을 지원하지 않습니다. Flatpickr는 `disable` 옵션으로 특정 요일 클릭을 막고 회색 처리할 수 있어 선택했습니다.
> - **쉽게 말하면**: 기본 달력 대신 더 강력한 기능의 달력 앱을 인터넷에서 가져다 쓰는 것입니다.

### direct-reservation.mustache — input 교체

```html
<!-- 변경 전 -->
<input type="date" id="date" name="reservationDate" required />

<!-- 변경 후 -->
<input type="text" id="date" name="reservationDate" required readonly
  placeholder="날짜를 선택해주세요" />
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 브라우저 기본 날짜 입력(`type="date"`)을 일반 텍스트 입력으로 바꿉니다. `readonly` 속성으로 직접 입력을 막고, Flatpickr가 이 필드를 제어합니다.
> - **왜 이렇게 썼는지**: Flatpickr는 `<input type="text">`를 대상으로 달력 팝업을 연결합니다. `readonly`를 사용해 사용자가 직접 타이핑하지 못하고 달력에서만 날짜를 선택하도록 강제합니다.
> - **쉽게 말하면**: 기존 날짜 입력창을 떼어내고 Flatpickr 달력이 대신 날짜를 채워주는 입력창으로 교체하는 것입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 의사가 선택될 때마다 해당 의사의 진료 가능 요일을 파악해 Flatpickr 달력을 새로 초기화합니다. 진료 불가 요일은 회색 처리되어 클릭이 불가능해집니다.
> - **왜 이렇게 썼는지**: `DAY_MAP`은 "MON" 같은 문자열을 JS의 `getDay()`가 반환하는 숫자(0~6)로 변환하는 사전입니다. `disable: [date => !availableDayNums.includes(date.getDay())]`는 "가능 요일 목록에 없는 날은 비활성화해라"는 Flatpickr 설정입니다. 의사를 바꿀 때마다 `datePicker.destroy()`로 기존 달력을 제거하고 새로 만듭니다.
> - **쉽게 말하면**: 의사를 선택하면 그 의사의 출근 요일표를 달력에 반영해서, 쉬는 날엔 예약 버튼 자체를 회색으로 비활성화하는 로직입니다.

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
