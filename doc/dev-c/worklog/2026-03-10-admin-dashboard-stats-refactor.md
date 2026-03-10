# 2026-03-10 AdminDashboardStatsService 리팩터링 Worklog

## 작업 목적
- `AdminDashboardStatsService`의 가독성을 개선하고 메서드 책임을 분리한다.
- 서비스에서 수행하던 재고 부족 계산을 DB 집계로 이전한다.

## 변경 파일
- `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsService.java`
- `src/main/java/com/smartclinic/hms/admin/item/ItemRepository.java`
- `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsServiceTest.java`
- `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsRepositoryTest.java`

## 주요 변경 사항
1. DB 집계로 이전
- `ItemRepository`에 `countLowStockItems()` 쿼리 추가
- 기존 `findAllProjectedBy()` 기반 스트림 필터 집계를 제거

2. 서비스 가독성 개선
- `getDashboardStats(LocalDate)`를 단계형 메서드로 분리
  - `countTodayReservations(...)`
  - `countTotalReservations()`
  - `countActiveStaff()`
  - `countLowStockItems()`
- `getDashboardChart(LocalDate)` 내부도 역할별 private 메서드로 분리
  - `buildCategoryCounts()`
  - `buildDailyPatients(...)`

3. 테스트 정합성 반영
- 서비스 테스트를 `countLowStockItems()` 호출 기준으로 수정
- 리포지토리 테스트도 DB 집계 메서드 사용으로 변경

## 검증 결과
- 실행 명령어:
  - `./gradlew test --tests "com.smartclinic.hms.admin.dashboard.AdminDashboardStatsServiceTest" --tests "com.smartclinic.hms.admin.dashboard.AdminDashboardStatsRepositoryTest"`
- 결과: 성공 (통과)

## 적용 규칙 체크
- 로컬 문서 우선순위 준수: `AGENTS.md`, `.ai/memory.md`, `doc/PROJECT_STRUCTURE.md`, `doc/RULE.md`
- 서비스 오케스트레이션/DB 집계 분리 원칙 적용
- 테스트 Given-When-Then 유지

## 리스크 및 후속 TODO
- 카테고리 집계(`findCategoryCounts`)의 데이터 증가 시 실행계획 점검 필요
- 대시보드 통계 API의 목표 응답시간(SLA) 기준 정의 필요
