# W3-1번째작업 Report — 접수 처리 후 의사 진료 목록 실시간 연동 (AJAX 폴링)

## 작업 개요

- **날짜:** 2026-03-16
- **담당:** dev-a-b

---

## 구현 내용

### 1. DoctorTreatmentService — `getTodayReceivedList()` 추가

- 오늘 날짜 기준 `RECEIVED` 상태 예약만 조회
- 기존 `findTodayByDoctorAndStatus()` 재활용

### 2. DoctorTreatmentController — `GET /doctor/treatment-list/poll` 추가

- `@ResponseBody` + `Resp.ok()` 표준 JSON 응답
- `Authentication`으로 현재 의사 식별
- 기존 `/doctor/**` 보안 정책(`ROLE_DOCTOR`) 그대로 적용

### 3. doctor/treatment-list.mustache — 5초 폴링 JS 추가

- 카드 목록 `div`에 `id="treatment-card-list"` 부여
- `setInterval(5000)` 폴링: `/doctor/treatment-list/poll` 호출
- 응답 데이터로 카드 HTML 동적 재구성 후 `feather.replace()` 재실행
- 오류 발생 시 silent fail (콘솔 로그만)

---

## 변경 파일

| 파일 | 변경 유형 |
|------|----------|
| `doctor/treatment/DoctorTreatmentService.java` | 수정 — `getTodayReceivedList()` 추가 |
| `doctor/treatment/DoctorTreatmentController.java` | 수정 — `/poll` 엔드포인트 추가 |
| `templates/doctor/treatment-list.mustache` | 수정 — id 추가 + 폴링 JS |
| `test/.../DoctorTreatmentServiceTest.java` | 신규 |
| `test/.../DoctorTreatmentControllerTest.java` | 신규 |

---

## 테스트 결과

```
./gradlew test --tests "com.smartclinic.hms.doctor.treatment.DoctorTreatmentServiceTest"
              --tests "com.smartclinic.hms.doctor.treatment.DoctorTreatmentControllerTest"

BUILD SUCCESSFUL
```

| 테스트 | 결과 |
|--------|------|
| `getTodayReceivedList — RECEIVED 목록 반환` | ✅ 통과 |
| `getTodayReceivedList — 빈 리스트 반환` | ✅ 통과 |
| `ROLE_DOCTOR — 폴링 200 + JSON 반환` | ✅ 통과 |
| `미인증 — 로그인 리다이렉트` | ✅ 통과 |
| `ROLE_STAFF — 403 반환` | ✅ 통과 |

---

## 참조 문서

- `doc/RULE.md`
- `doc/PROJECT_STRUCTURE.md`

---

## TODO / 리스크

- 현재 폴링은 `RECEIVED` 상태만 갱신함. 진료 완료(`COMPLETED`) 상태 변경은 새로고침 필요
- 폴링 중 세션 만료 시 JSON 응답 대신 로그인 리다이렉트 HTML이 올 수 있음 (추후 처리 고려)
