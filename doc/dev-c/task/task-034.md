# Task 034 - 관리자 대시보드 그래프 리팩토링

## Task 34-1. 현재 관리자/원무과/물품 그래프 구조 점검
- [x] 관리자 대시보드의 현재 그래프 구성과 데이터 연결 위치를 점검한다.
- [x] 원무과 대시보드 `시간대별 예약 및 접수 현황` 그래프의 스타일 기준을 확인한다.
- [x] 물품 담당자 대시보드 `최근 7일 입고/출고 추이` 그래프의 데이터 구조를 확인한다.
- [x] 관련 템플릿, 스크립트, 테스트 위치를 정리한다.

## Task 34-2. 관리자 환자 그래프 스타일 적용 설계
- [x] `일일 환자수 추이`의 기존 데이터 계약 유지 범위를 고정한다.
- [x] 원무과 그래프에서 차용할 시각 요소를 정리한다.
- [x] 관리자 그래프에 필요한 마크업/스크립트 변경 범위를 정의한다.
- [x] 테스트를 어디까지 보강할지 기준을 정한다.

## Task 34-3. 관리자 환자 그래프 시각 스타일 리팩토링
- [x] `일일 환자수 추이` 그래프를 원무과 그래프 톤으로 정리한다.
- [x] 카드 레이아웃, 색감, 축 스타일, 최근 7일 표시를 맞춘다.
- [x] 데이터 계산 방식은 유지한다.
- [x] 관련 템플릿과 스크립트 변경을 반영한다.

## Task 34-4. 관리자 물품 그래프 데이터 구조 교체
- [x] 기존 `카테고리별 물품 재고 현황` 데이터 연결을 제거한다.
- [x] 최근 7일 `입고 / 출고` 그래프에 필요한 DTO/API/서비스 계약을 연결한다.
- [x] 물품 담당자 대시보드와 같은 기간/축/데이터 계산 방식으로 맞춘다.
- [x] 관리자 대시보드에서 새 데이터를 렌더링할 수 있도록 준비한다.

## Task 34-5. 관리자 물품 그래프 UI 리팩토링
- [x] 기존 물품 그래프 영역을 최근 7일 입고/출고 그래프로 교체한다.
- [x] 카드 제목, 범례, 보조 문구를 물품 담당자 그래프 톤에 맞춘다.
- [x] 관리자 대시보드 전체 톤과 어색하지 않게 시각 요소를 조정한다.

## Task 34-6. 테스트 보강
- [x] 관리자 대시보드 렌더링 테스트를 현재 그래프 구조 기준으로 갱신한다.
- [x] 환자 그래프가 기존 데이터 계약을 유지하는지 확인한다.
- [x] 물품 그래프가 최근 7일 입고/출고 데이터로 바뀌었는지 검증한다.
- [x] 관련 범위 테스트를 다시 통과시킨다.

## Task 34-7. 문서 및 최종 검증 마무리
- [x] workflow-034를 현재 구현 상태 기준으로 갱신한다.
- [x] task-034의 전체 완료 기준과 리뷰 포인트를 정리한다.
- [x] 최종 테스트 결과를 문서에 반영한다.

## 전체 완료 기준
- [x] 관리자 대시보드 `일일 환자수 추이` 그래프가 원무과 그래프와 같은 톤으로 렌더링된다.
- [x] 해당 그래프는 기존 데이터 계산 방식을 유지한다.
- [x] 기존 `카테고리별 물품 재고 현황` 그래프가 제거된다.
- [x] 대신 최근 7일 기준 `입고 / 출고` 2시리즈 그래프가 렌더링된다.
- [x] 물품 담당자 화면과 기간/축/데이터 계산 방식이 일치한다.
- [x] 관리자 대시보드 범위 테스트가 통과한다.

## Task 34-1 점검 메모
- 관리자 대시보드 환자 그래프는 `AdminDashboardApiController -> AdminDashboardStatsService -> AdminDashboardChartResponse.dailyPatients` 경로로 데이터를 받고, 프론트에서는 `/js/pages/admin-dashboard.js`가 `daily-patient-canvas`를 렌더링하는 구조다.
- 관리자 대시보드 물품 그래프는 당시 `categoryCounts`를 받아 `admin-dashboard.js` 안에서 카테고리별 재고 막대를 그리는 구조였다.
- 원무과 대시보드 `시간대별 예약 및 접수 현황`은 `hourlyStats` 기반 line chart 스타일이다.
- 물품 담당자 대시보드의 최근 7일 그래프는 `ItemDashboardDto.chartDays`를 통해 최근 7일 입고/출고 값을 계산하고 렌더링한다.

## Task 34-2 설계 메모
- 환자 그래프는 `dailyPatients` 계약을 그대로 유지한다.
- 원무과 그래프에서 차용할 요소는 카드 레이아웃, 인디고 중심 색감, 슬레이트 계열 축/격자선, 최근 7일 배지다.
- 구현 범위는 `admin/dashboard.mustache`의 카드 마크업과 `admin-dashboard.js`의 `renderDailyPatientChart(...)` 스타일 조정으로 한정한다.

## Task 34-3 구현 메모
- `admin/dashboard.mustache`에 Chart.js CDN을 추가하고, 환자 그래프 카드를 원무과 카드 톤으로 재배치했다.
- `admin-dashboard.js`의 환자 그래프는 기존 custom canvas 구현을 Chart.js line chart로 바꾸되, 입력 데이터는 여전히 `dailyPatients`만 사용한다.
- 인디고 라인, 연한 영역 채우기, hover point, 슬레이트 축/격자선을 적용했다.

## Task 34-4 구현 메모
- `AdminDashboardChartResponse`에서 `categoryCounts`를 제거하고 `itemFlowDays`를 추가했다.
- `AdminDashboardStatsService`는 `ItemStockLogRepository`를 사용해 최근 7일 입고/출고 흐름을 계산하도록 바뀌었다.
- 계산 방식은 물품 담당자 대시보드와 동일하게 일자별 IN/OUT 합산 + 최대값 기준 높이 퍼센트 산출이다.

## Task 34-5 구현 메모
- 관리자 물품 그래프는 Chart.js canvas가 아니라 DOM 막대 레이아웃으로 바꿨다.
- `item-flow-chart` 컨테이너 안에 날짜별 입고/출고 쌍막대를 직접 렌더링한다.
- 점선 가이드 라인, 범례, 날짜 라벨을 물품 담당자 대시보드 톤에 맞췄다.

## Task 34-6 구현 메모
- `AdminDashboardControllerTest`는 새 그래프 카드 제목과 주요 DOM id(`daily-patient-canvas`, `item-flow-chart`)를 기준으로 SSR 렌더링을 확인하도록 보강했다.
- `AdminDashboardApiControllerTest`는 `itemFlowDays`의 높이 필드(`inHeight`, `outHeight`)와 `categoryCounts` 제거를 함께 검증하도록 정리했다.
- `AdminDashboardStatsServiceTest`는 입출고 로그가 하나도 없을 때도 최근 7일 `itemFlowDays`가 0값으로 채워지는 시나리오를 추가했다.
- 범위 회귀는 `./gradlew cleanTest test --tests 'com.smartclinic.hms.admin.dashboard.*'` 기준으로 다시 확인했다.

## Task 34-7 최종 검증 메모
- 범위 테스트: `./gradlew cleanTest test --tests 'com.smartclinic.hms.admin.dashboard.*'` 통과
- 전체 테스트: `./gradlew test` 실패
- 남은 실패: `AdminItemControllerTest > history uses today as default date range`
- 위 실패는 현재 관리자 대시보드 그래프 리팩토링 범위 밖의 `admin.item` 테스트 기대값 문제로 확인했다.