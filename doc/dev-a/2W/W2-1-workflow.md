# W2-1. 예약 폼 화면 구현 (S03 직접 예약)

## 개요

`direct-reservation.mustache`의 `<form>`을 Spring MVC 방식으로 교체한다.
현재 폼은 JS로만 동작하며 서버로 데이터를 전송하지 않는다.
이번 작업에서 `POST /reservation/create`로 실제 제출되도록 연결한다.
(진료과/의사 동적 로딩은 W2-2, 시간 슬롯은 W2-3에서 진행)

---

## 현재 문제점

| 항목 | 현재 (문제) | 변경 후 |
|------|------------|--------|
| form action | JS `e.preventDefault()` 후 `.html` 파일로 이동 | `method="POST" action="/reservation/create"` |
| input name 속성 | `id`만 있고 `name` 없음 → 서버 전송 불가 | `name` 속성 추가 |
| CSRF 토큰 | 없음 | hidden input 추가 |
| 컨트롤러 파라미터 | `Model model` | `HttpServletRequest request` (프로젝트 규칙) |

---

## 실행 흐름

```
브라우저: GET /reservation/direct-reservation
    ↓
ReservationController.directReservation(HttpServletRequest)
  request.setAttribute("pageTitle", "직접 선택 예약")
    ↓
direct-reservation.mustache 렌더링
  ├── 환자 성함 input[name="name"]
  ├── 연락처   input[name="phone"]
  ├── 진료과   select[name="departmentId"] ← 하드코딩 유지 (W2-2에서 동적 교체)
  ├── 전문의   select[name="doctorId"]     ← 하드코딩 유지 (W2-2에서 동적 교체)
  ├── 예약날짜 input[name="reservationDate"]
  ├── 예약시간 select[name="timeSlot"]     ← 하드코딩 유지 (W2-3에서 동적 교체)
  └── CSRF hidden input
    ↓ 제출
POST /reservation/create  ← W2-4에서 구현 예정
```

---

## 작업 목록

| 순서 | 파일 | 작업 내용 |
|------|------|----------|
| 1 | `ReservationController.java` | `directReservation()` 파라미터 `Model` → `HttpServletRequest` 변경 |
| 2 | `direct-reservation.mustache` | `<form>` 태그에 `method="POST" action="/reservation/create"` 추가 |
| 3 | `direct-reservation.mustache` | 모든 input/select에 `name` 속성 추가 |
| 4 | `direct-reservation.mustache` | CSRF hidden input 추가 |
| 5 | `direct-reservation.mustache` | JS 폼 submit 핸들러 제거 |

---

## 실행 흐름에 대한 코드

### 1. ReservationController.java — directReservation 수정

```java
@GetMapping("/direct-reservation")
public String directReservation(HttpServletRequest request) {
    request.setAttribute("pageTitle", "직접 선택 예약");
    return "reservation/direct-reservation";
}
```

### 2. direct-reservation.mustache — form 태그

```html
<form method="POST" action="/reservation/create" class="space-y-6">
  {{! CSRF 토큰 }}
  <input type="hidden" name="{{_csrf.parameterName}}" value="{{_csrf.token}}">
```

### 3. direct-reservation.mustache — input name 속성

```html
{{! 환자 성함 }}
<input type="text" id="name" name="name" required ... />

{{! 연락처 }}
<input type="tel" id="phone" name="phone" required ... />

{{! 진료과 }}
<select id="department" name="departmentId" required ...>
  <option value="">선택해주세요</option>
  <option value="1">내과</option>
  <option value="2">외과</option>
  <option value="3">소아과</option>
  <option value="4">이비인후과</option>
</select>

{{! 전문의 }}
<select id="doctor" name="doctorId" required ...>
  <option value="">선택해주세요</option>
</select>

{{! 예약 날짜 }}
<input type="date" id="date" name="reservationDate" required ... />

{{! 예약 시간 }}
<select id="time" name="timeSlot" required ...>
  <option value="">선택해주세요</option>
  <option value="09:00">09:00</option>
  <option value="09:30">09:30</option>
  ...
</select>
```

### 4. direct-reservation.mustache — JS 정리

```javascript
// 제거: form submit 핸들러 (서버 POST로 대체됨)
// 유지: AI 추천 정보 URL 파라미터 처리
feather.replace();

const urlParams = new URLSearchParams(window.location.search);
const recDept = urlParams.get('dept');
const recDoctor = urlParams.get('doctor');

if (recDept && recDoctor) {
  const infoBox = document.getElementById('ai-recommendation-info');
  infoBox.classList.remove('hidden');
  infoBox.classList.add('flex');
  document.getElementById('info-dept').innerText = recDept;
  document.getElementById('info-doctor').innerText = recDoctor;
}
```

---

## 수용 기준

- [ ] `GET /reservation/direct-reservation` 접속 시 폼 화면 정상 렌더링
- [ ] 모든 입력 필드에 `name` 속성 존재
- [ ] `<form>` 태그에 `method="POST" action="/reservation/create"` 적용
- [ ] CSRF hidden input 존재
- [ ] 기존 JS submit 핸들러 제거됨
- [ ] AI 추천 정보 URL 파라미터 표시 유지
