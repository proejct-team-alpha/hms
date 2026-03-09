# 개발자 A — 작업 리포트

---

## 01. 예약방법 선택 화면 URL 연결 (2026-03-09)

### 작업 내용

`GET /reservation` 접속 시 `patient-choice.mustache`가 렌더링되도록 컨트롤러 매핑을 추가하고,
화면 내 버튼의 `.html` 직접 참조를 Spring MVC URL로 교체했다.

### 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `ReservationController.java` | `@GetMapping("")` 추가 → `GET /reservation` 매핑 |
| `patient-choice.mustache` | 버튼 3개 href 교체 |

### 상세 변경 사항

**ReservationController.java**
- 삭제: `@GetMapping("/dashboard")`, `@GetMapping("/index")` (미사용 매핑 제거)
- 추가: `@GetMapping("")` → `reservation/patient-choice` 반환
- 추가: `jakarta.servlet.http.HttpServletRequest` import

**patient-choice.mustache**
```
변경 전                              변경 후
---------------------------------   ---------------------------------
'../index.html'               →     '/'
'symptom-reservation.html'    →     '/reservation/symptom-reservation'
'direct-reservation.html'     →     '/reservation/direct-reservation'
```

### 수용 기준 확인

- [x] `GET /reservation` 접속 시 patient-choice 화면 렌더링
- [x] "AI 증상 추천 예약" 버튼 → `/reservation/symptom-reservation` 이동
- [x] "직접 선택 예약" 버튼 → `/reservation/direct-reservation` 이동
- [x] "처음으로" 버튼 → `/` 이동

### 참조 문서

- `doc/dev-a/01-workflow-patient-choice-connection.md`
- `doc/dev-a/A-project.md`
