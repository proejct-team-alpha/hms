# W2-#3 Workflow: 의사 진료 가능 요일 기반 날짜 제한

## 개요

의사 선택 시 해당 의사의 `available_days`(MON,WED,FRI 형식)를 API 응답에 포함시키고,
Flatpickr 캘린더 라이브러리를 통해 가능 요일이 아닌 날은 회색 처리 + 클릭 자체를 막는다.

---

## 실행 흐름

```
[1] DoctorDto.java 수정
    └─ availableDays 필드 추가 (Doctor 엔티티의 availableDays 그대로 반환)

[2] direct-reservation.mustache 수정
    └─ <head>에 Flatpickr CDN 추가 (CSS + JS)
    └─ <input type="date"> → <input type="text" readonly> 로 변경 (Flatpickr가 제어)
    └─ 의사 선택 이벤트 → availableDays 파싱 → Flatpickr 재초기화
       (불가 요일: 회색 + 클릭 불가, 애니메이션 비활성화)
```

---

## 실행 흐름에 대한 코드

### [1] DoctorDto.java — availableDays 필드 추가

```java
// 기존
@Getter
public class DoctorDto {
    private final Long id;
    private final String name;

    public DoctorDto(Doctor doctor) {
        this.id = doctor.getId();
        this.name = doctor.getStaff().getName();
    }
}

// 변경 후
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

---

### [2] direct-reservation.mustache 수정

#### HEAD — Flatpickr CDN 추가

```html
<!-- Flatpickr CSS -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css">
<!-- Flatpickr JS -->
<script src="https://cdn.jsdelivr.net/npm/flatpickr"></script>
```

#### BODY — input type="date" → Flatpickr용 input으로 교체

```html
<!-- 기존 -->
<input type="date" id="date" name="reservationDate" required ... />

<!-- 변경 후: readonly로 직접 입력 막고 Flatpickr가 제어 -->
<input type="text" id="date" name="reservationDate" required readonly
  class="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-xl
         focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
  placeholder="날짜를 선택해주세요" />
```

#### JS — 요일 매핑 + Flatpickr 초기화 + 의사 change 이벤트

```javascript
// 요일 매핑: Flatpickr disable 함수는 JS getDay() 기준 (0=일, 1=월 ... 6=토)
const DAY_MAP = { SUN: 0, MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6 };

let datePicker = null; // Flatpickr 인스턴스

// Flatpickr 초기화 함수
function initDatePicker(availableDayNums) {
  if (datePicker) datePicker.destroy(); // 기존 인스턴스 제거

  datePicker = flatpickr('#date', {
    dateFormat: 'Y-m-d',
    animate: false, // 애니메이션 비활성화
    disable: [
      function (date) {
        // 가능 요일 목록에 없으면 비활성화 (회색 + 클릭 불가)
        return !availableDayNums.includes(date.getDay());
      }
    ]
  });
}

// 의사 change 이벤트: availableDays 파싱 → Flatpickr 재초기화
document.getElementById('doctor').addEventListener('change', function () {
  const selectedOption = this.options[this.selectedIndex];
  const availableDaysStr = selectedOption.dataset.availableDays || '';

  const availableDayNums = availableDaysStr
    .split(',')
    .map(d => DAY_MAP[d.trim()])
    .filter(n => n !== undefined);

  // 날짜 초기화 후 Flatpickr 재설정
  document.getElementById('date').value = '';
  initDatePicker(availableDayNums);
});

// 의사 선택 이전에는 Flatpickr 비활성 상태로 초기화 (전체 비활성)
initDatePicker([]);
```

---

## 작업 파일

| 파일 | 작업 |
|------|------|
| `src/main/java/com/smartclinic/hms/doctor/DoctorDto.java` | `availableDays` 필드 추가 |
| `src/main/resources/templates/reservation/direct-reservation.mustache` | Flatpickr CDN 추가, input 교체, JS 로직 교체 |
