<!-- Parent: ../AI-CONTEXT.md -->

# admin/reservation

## 목적

관리자 전용 예약 관리 기능 제공. 예약 목록 조회, 필터링, 예약 취소 기능을 담당한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| AdminReservationController.java | 관리자 예약 목록 페이지 컨트롤러 (`/admin/reservation`) |
| AdminReservationService.java | 예약 목록 조회, 페이징 처리, 예약 취소 로직을 포함하는 서비스 |
| AdminReservationRepository.java | 예약 조회 쿼리(Querydsl/JPA) 및 통계 쿼리 담당 |
| dto/AdminReservationListResponse.java | 예약 목록 조회 응답 DTO |
| dto/AdminReservationItemResponse.java | 예약 목록 내 개별 항목 응답 DTO |

## 하위 디렉토리

- `dto/` - 예약 관리 기능에서 사용하는 데이터 전송 객체들

## AI 작업 지침

- 예약 목록 조회 시 페이징과 검색 필터가 포함될 수 있으므로 `AdminReservationService`의 로직을 참고한다.
- 예약 상태 변경(취소 등) 시 비즈니스 로직은 서비스 계층에서 처리한다.

## 테스트

- `AdminReservationServiceTest.java`: 예약 취소 로직 및 목록 필터링 검증
- `AdminReservationRepositoryTest.java`: 복잡한 조회 쿼리 성능 및 정확도 검증

## 의존성

- 내부: `Reservation`, `Patient`, `Department` 도메인 엔티티
- 외부: Spring MVC, Spring Data JPA
