<!-- Parent: ../AI-CONTEXT.md -->

# reservation/reservation

## 목적

비회원 환자의 예약 프로세스 전반을 관리한다. 예약 등록, 조회, 수정, 취소 기능을 담당한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| ReservationController.java | 비회원 예약 관련 페이지 컨트롤러 (등록, 조회, 수정, 취소) |
| ReservationApiController.java | 예약 관련 REST API (예약 번호 확인 등) |
| ReservationService.java | 예약 비즈니스 로직 (등록 처리, 유효성 검사 등) |
| ReservationRepository.java | 예약 엔티티에 대한 JPA Repository |
| PatientRepository.java | 환자 정보 조회를 위한 Repository |
| DepartmentRepository.java | 진료과 조회를 위한 Repository |

## AI 작업 지침

- 예약 프로세스는 `ReservationService`에서 집중적으로 처리하며, 환자 정보가 없는 경우 신규 등록 로직이 포함될 수 있다.
- 예약 수정 및 취소 시 예약 번호와 환자 정보의 일치 여부를 반드시 확인한다.
