# 01. 예약방법 선택 화면 URL 연결

## 개요

`GET /reservation` 요청 시 `patient-choice.mustache`가 렌더링되도록 컨트롤러 매핑을 추가하고,
화면 내 버튼의 `.html` 직접 참조를 Spring MVC URL로 교체한다.

---

## 실행 흐름

```
브라우저: GET /reservation
    ↓
ReservationController
  @GetMapping ("")  ← 추가
    ↓
return "reservation/patient-choice"
    ↓
patient-choice.mustache 렌더링
  ├── "AI 증상 추천 예약" 버튼 → /reservation/symptom-reservation
  ├── "직접 선택 예약" 버튼   → /reservation/direct-reservation
  └── "처음으로" 버튼        → /
```

---

## 작업 목록

| 순서 | 파일 | 작업 내용 |
|------|------|----------|
| 1 | `ReservationController.java` | `@GetMapping("")` 추가 → `reservation/patient-choice` 반환 |
| 2 | `patient-choice.mustache` | 버튼 href `.html` → Spring MVC URL 교체 |

---

## 실행 흐름에 대한 코드

### 1. ReservationController.java — GET /reservation 매핑 추가

```java
@GetMapping("")   // GET /reservation
public String patientChoice(HttpServletRequest request) {
    request.setAttribute("pageTitle", "진료 예약");
    return "reservation/patient-choice";
}
```

### 2. patient-choice.mustache — 버튼 href 수정

```html
<!-- 처음으로 버튼 -->
<button onclick="location.href='/'">처음으로</button>

<!-- AI 증상 추천 예약 버튼 -->
<button onclick="location.href='/reservation/symptom-reservation'">AI 증상 추천 예약</button>

<!-- 직접 선택 예약 버튼 -->
<button onclick="location.href='/reservation/direct-reservation'">직접 선택 예약</button>
```

---

## 수용 기준

- [ ] `GET /reservation` 접속 시 patient-choice 화면 렌더링
- [ ] "AI 증상 추천 예약" 버튼 클릭 → `/reservation/symptom-reservation` 이동
- [ ] "직접 선택 예약" 버튼 클릭 → `/reservation/direct-reservation` 이동
- [ ] "처음으로" 버튼 클릭 → `/` 이동
