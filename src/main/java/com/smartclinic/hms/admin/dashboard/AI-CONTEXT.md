<!-- Parent: ../AI-CONTEXT.md -->

# admin/dashboard

## 목적

관리자 대시보드 통계 및 차트 데이터 제공. 오늘 예약 수, 총 예약 수, 활성 직원 수, 재고 부족 물품 수 등의 핵심 지표를 집계한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| AdminDashboardController.java | 관리자 대시보드 메인 페이지 (`/admin/dashboard`) 컨트롤러 |
| AdminDashboardApiController.java | 대시보드 차트 데이터 등을 제공하는 REST API 컨트롤러 |
| AdminDashboardStatsService.java | 대시보드에 표시될 4대 지표 및 차트용 통계 데이터 계산 서비스 |
| dto/AdminDashboardStatsResponse.java | 4대 지표 통계 응답 DTO |
| dto/AdminDashboardChartResponse.java | 차트 데이터(카테고리별 물품 수, 일별 환자 수) 응답 DTO |

## 하위 디렉토리

- `dto/` - 대시보드 관련 데이터 전송 객체(Response Records)

## AI 작업 지침

- 대시보드 통계는 `AdminDashboardStatsService`에서 집중 관리한다.
- 새로운 통계 지표 추가 시 `AdminDashboardStatsResponse` 레코드에 필드를 추가하고 서비스에서 로직을 구현한다.
- 차트 데이터는 `AdminDashboardChartResponse`를 통해 JSON으로 반환하며, `Chart.js` 등 프론트엔드 라이브러리와 규격을 맞춘다.

## 테스트

- `AdminDashboardStatsServiceTest.java`: 통계 계산 로직 검증
- `AdminDashboardControllerTest.java`: 대시보드 진입 및 모델 데이터 확인

## 의존성

- 내부: `AdminReservationRepository`, `AdminStaffRepository`, `ItemRepository`
- 외부: Spring MVC, Spring Data JPA
