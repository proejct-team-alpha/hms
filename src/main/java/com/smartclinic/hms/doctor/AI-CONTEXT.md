<!-- Parent: ../AI-CONTEXT.md -->

# doctor — 의사

## 목적

DOCTOR 역할 직원의 업무 처리. 예약 목록 확인 및 진료 완료(RECEIVED→COMPLETED) 처리.
**개발자 B(조유지) 소유.**

## 주요 파일

| 파일 | 설명 |
|------|------|
| DoctorDashboardController.java | GET /doctor/dashboard |
| DoctorDto.java | 의사 응답 DTO (id, name, availableDays) — 예약 AJAX에도 사용 |
| DoctorRepository.java | Doctor JPA Repository (`findByDepartment_Id` with JOIN FETCH) |
| treatment/TreatmentController.java | GET/POST /doctor/treatment/** |
| treatment/TreatmentService.java | RECEIVED→COMPLETED 상태 전이 |
| treatment/TreatmentRecordRepository.java | TreatmentRecord CRUD |
| treatment/dto/TreatmentCompleteRequest.java | 진료 완료 요청 DTO |

## 핵심 주의사항

- `DoctorDto.java`와 `DoctorRepository.java`는 현재 **개발자 A가 임시 생성**한 파일
  - `availableDays` 포함 (`"MON,WED,FRI"` 형식)
  - `JOIN FETCH d.staff` 쿼리 (LazyInitializationException 방지)
  - B 작업자가 정식 구현 시 교체 예정

## AI 작업 지침

- `ReservationReadRepository.java`: 의사 대시보드용 읽기 전용 Repository (경력자 인터페이스)
- 진료 완료 처리 시 반드시 `TreatmentRecord` 함께 생성

## 의존성

- 내부: `domain/Doctor`, `domain/Reservation`, `domain/TreatmentRecord`, `domain/ReservationStatus`
- 뷰: `templates/doctor/`
