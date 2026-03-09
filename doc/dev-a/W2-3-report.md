# W2-#3 Report: 의사 진료 가능 요일 기반 날짜 제한

## 작업 개요

의사 선택 후 해당 의사의 진료 가능 요일(`available_days`)에 해당하지 않는 날짜는
Flatpickr 캘린더에서 회색 처리되어 클릭 자체가 불가능하도록 구현.

---

## 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `src/main/java/com/smartclinic/hms/doctor/DoctorDto.java` | `availableDays` 필드 추가 |
| `src/main/resources/static/js/flatpickr.min.js` | Flatpickr JS 로컬 추가 |
| `src/main/resources/static/css/flatpickr.min.css` | Flatpickr CSS 로컬 추가 |
| `src/main/resources/templates/reservation/direct-reservation.mustache` | Flatpickr 적용 및 JS 로직 구현 |

---

## 구현 상세

### 1. DoctorDto.java — availableDays 필드 추가

API 응답에 `availableDays`를 포함시켜 프론트가 요일 정보를 받을 수 있도록 함.

```java
private final String availableDays;

public DoctorDto(Doctor doctor) {
    this.id = doctor.getId();
    this.name = doctor.getStaff().getName();
    this.availableDays = doctor.getAvailableDays(); // "MON,WED,FRI" 형식
}
```

---

### 2. Flatpickr 로컬 서빙

**왜 CDN 대신 로컬?**
프로젝트의 CSP(Content Security Policy)가 외부 CDN을 차단하므로,
`feather.min.js`와 동일하게 `static/` 폴더에 직접 파일을 두고 서빙.

- `static/js/flatpickr.min.js` (v4.6.13)
- `static/css/flatpickr.min.css`

---

### 3. direct-reservation.mustache — Flatpickr 적용

**왜 Flatpickr?**
네이티브 `<input type="date">`는 특정 요일을 개별 비활성화할 수 없음.
Flatpickr는 `disable` 옵션으로 특정 요일 클릭 차단 + 회색 처리를 기본 제공.

#### HEAD — 로컬 Flatpickr 로드

```html
<link rel="stylesheet" href="/css/flatpickr.min.css">
<script src="/js/flatpickr.min.js"></script>
```

#### HTML — input 교체

```html
<!-- 기존: 네이티브 날짜 입력 -->
<input type="date" id="date" name="reservationDate" required ... />

<!-- 변경: Flatpickr가 제어하는 readonly 텍스트 입력 -->
<input type="text" id="date" name="reservationDate" required readonly
  placeholder="날짜를 선택해주세요" ... />
```

#### JS — Flatpickr 초기화 및 이벤트 처리

```javascript
const DAY_MAP = { SUN: 0, MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6 };

// DOM 엘리먼트 직접 전달 (문자열 '#date' 전달 시 배열 반환되어 .clear() 등 메서드 사용 불가)
const datePicker = flatpickr(document.getElementById('date'), {
  dateFormat: 'Y-m-d',
  animate: false,
  position: 'below',      // 캘린더가 위로 열려 전문의 드롭다운을 가리지 않도록 고정
  disable: [() => true]   // 초기: 전체 비활성 (의사 미선택 상태)
});

// 진료과 변경 시: 날짜 초기화 + 캘린더 닫기 + 전문의 목록 재조회
document.getElementById('department').addEventListener('change', function () {
  const departmentId = this.value;
  const doctorSelect = document.getElementById('doctor');

  doctorSelect.innerHTML = '<option value="">선택해주세요</option>';
  datePicker.clear();
  datePicker.close();

  if (!departmentId) return;

  fetch(`/api/reservation/doctors?departmentId=${departmentId}`)
    .then(res => res.json())
    .then(doctors => {
      doctors.forEach(doctor => {
        const option = document.createElement('option');
        option.value = doctor.id;
        option.textContent = doctor.name;
        option.dataset.availableDays = doctor.availableDays; // "MON,WED,FRI"
        doctorSelect.appendChild(option);
      });
    });
});

// 의사 변경 시: availableDays 파싱 → Flatpickr disable 갱신
document.getElementById('doctor').addEventListener('change', function () {
  const selectedOption = this.options[this.selectedIndex];
  const availableDaysStr = selectedOption.dataset.availableDays || '';

  const availableDayNums = availableDaysStr
    .split(',')
    .map(d => DAY_MAP[d.trim()])
    .filter(n => n !== undefined);

  datePicker.clear();
  datePicker.set('disable', [
    function (date) { return !availableDayNums.includes(date.getDay()); }
  ]);
});
```

**요일 매핑 (JS `getDay()` 기준):**

| 문자열 | 숫자 |
|--------|------|
| SUN | 0 |
| MON | 1 |
| TUE | 2 |
| WED | 3 |
| THU | 4 |
| FRI | 5 |
| SAT | 6 |

---

## 버그 수정 이력

| 순서 | 현상 | 원인 | 수정 |
|------|------|------|------|
| 1 | Flatpickr CSS/JS 로드 실패 | CSP가 외부 CDN 차단 | CDN → 로컬 파일(`static/`)로 교체 |
| 2 | 전문의 목록 미표시 | `flatpickr('#date')` 문자열 선택자가 배열을 반환 → `datePicker.clear()` TypeError → fetch 미실행 | `flatpickr(document.getElementById('date'))` 로 DOM 엘리먼트 직접 전달 |
| 3 | 캘린더가 전문의 드롭다운 가림 | 진료과 변경 시 Flatpickr 캘린더가 열린 채로 유지 | `datePicker.close()` 추가 |

---

## 동작 흐름

```
진료과 선택
  └─ /api/reservation/doctors?departmentId={id} 호출
  └─ option에 data-available-days 저장
  └─ 날짜 초기화 + 캘린더 닫기

의사 선택
  └─ option의 data-available-days 파싱 → 숫자 배열 변환
  └─ Flatpickr disable 함수 갱신 (가능 요일만 클릭 허용)

날짜 선택
  └─ 가능 요일: 정상 선택
  └─ 불가 요일: 회색 처리, 클릭 불가
```

---

## 수용 기준 확인

- [x] 의사 선택 시 API 응답에 `availableDays` 포함
- [x] 불가 요일 날짜 회색 처리 + 클릭 불가
- [x] 가능 요일 선택 정상 동작
- [x] 의사 선택 전 전체 날짜 비활성
- [x] 진료과 변경 시 날짜 초기화 + 캘린더 닫힘
- [x] CSP 환경에서 Flatpickr 정상 로드 (로컬 서빙)
