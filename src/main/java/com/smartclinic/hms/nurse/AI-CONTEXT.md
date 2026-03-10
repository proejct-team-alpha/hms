<!-- Parent: ../AI-CONTEXT.md -->

# nurse — 간호사

## 목적

NURSE 역할 직원의 업무 처리. 환자 정보 조회·수정, 일정 관리, 접수 목록 확인.
**개발자 B(조유지) 소유.**

## 주요 파일

| 파일 | 설명 |
|------|------|
| NurseDashboardController.java | GET /nurse/dashboard |
| patient/PatientUpdateController.java | POST /nurse/patient/update |
| patient/PatientUpdateService.java | 환자 정보 수정 |
| schedule/ScheduleController.java | GET /nurse/schedule/** |

## AI 작업 지침

- NURSE 접근 권한: `/nurse/**` → `hasRole("NURSE")` 이상
- 환자 정보 수정 시 PRG 패턴 적용

## 의존성

- 내부: `domain/Patient`, `domain/Reservation`
- 뷰: `templates/nurse/`
