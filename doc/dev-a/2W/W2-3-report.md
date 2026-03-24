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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `DoctorDto`에 진료 가능 요일(`availableDays`) 필드를 추가하여, API 응답 JSON에 `"availableDays": "MON,WED,FRI"` 형태로 포함시킵니다.
> - **왜 이렇게 썼는지**: 프론트엔드(브라우저)가 달력에서 특정 요일만 활성화하려면 서버로부터 어떤 요일이 진료 가능한지 알아야 합니다. DTO에 이 정보를 추가하면 의사 목록 API 한 번만 호출해도 요일 정보까지 한꺼번에 받을 수 있습니다.
> - **쉽게 말하면**: 의사 소개 카드에 "이 선생님은 월·수·금 진료"라는 항목을 새로 추가하는 것입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 서버 내부에 저장된 Flatpickr 라이브러리 파일(CSS, JS)을 브라우저가 로드하도록 합니다.
> - **왜 이렇게 썼는지**: 이 프로젝트는 CSP(Content Security Policy, 콘텐츠 보안 정책)가 외부 CDN 접근을 차단합니다. 따라서 외부 URL 대신 서버의 `static/` 폴더에 파일을 직접 두고 `/css/`, `/js/` 경로로 서빙합니다.
> - **쉽게 말하면**: 인터넷 서점(CDN)에서 책을 가져오는 게 막혀 있으니, 책을 미리 사서 자체 도서관(`static/`)에 보관하고 거기서 빌려 쓰는 것입니다.

#### HTML — input 교체

```html
<!-- 기존: 네이티브 날짜 입력 -->
<input type="date" id="date" name="reservationDate" required ... />

<!-- 변경: Flatpickr가 제어하는 readonly 텍스트 입력 -->
<input type="text" id="date" name="reservationDate" required readonly
  placeholder="날짜를 선택해주세요" ... />
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 브라우저 기본 날짜 입력 필드를 Flatpickr가 제어하는 텍스트 입력 필드로 교체합니다.
> - **왜 이렇게 썼는지**: 기본 `<input type="date">`는 특정 요일을 비활성화하는 기능이 없습니다. `type="text"`와 `readonly` 조합으로 Flatpickr 달력 팝업만으로 날짜를 입력받습니다.
> - **쉽게 말하면**: 아무 날짜나 적을 수 있는 빈칸 대신, Flatpickr 달력 앱이 대신 채워주는 칸으로 바꾸는 것입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Flatpickr 달력을 초기화하고, 진료과 변경 시 의사 목록을 새로 불러오며, 의사 선택 시 해당 의사의 진료 가능 요일에 맞게 달력의 비활성 요일을 갱신합니다.
> - **왜 이렇게 썼는지**: `flatpickr(document.getElementById('date'), ...)`처럼 DOM 엘리먼트를 직접 넘겨야 `datePicker.clear()`같은 메서드를 사용할 수 있습니다(문자열 선택자로 넘기면 배열이 반환되어 메서드가 없음). `option.dataset.availableDays`는 HTML의 `data-*` 속성에 값을 저장하는 방법으로, 의사 옵션 태그에 요일 정보를 숨겨두고 의사 선택 시 읽어 씁니다.
> - **쉽게 말하면**: 달력 앱을 처음엔 전체 잠금 상태로 켜두고, 의사를 선택하면 그 의사의 출근 요일만 풀어주는 동작을 하는 로직입니다.

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
