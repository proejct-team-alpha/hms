# W3-11번째작업 Report — 대시보드 그래프 입출고 내역 연동

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. ItemStockLogRepository — 날짜 범위 조회 메서드 추가

- `findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end)`

### 2. ItemChartDayDto — 신규 생성

- 필드: label(MM/dd), inAmount, outAmount, inHeight(0~100%), outHeight(0~100%)
- 높이는 7일 중 최대값 기준으로 정규화

### 3. ItemDashboardDto — chartDays 필드 추가

- `List<ItemChartDayDto> chartDays`

### 4. ItemManagerService — buildChartDays() 추가

- 최근 7일(오늘 포함) 범위로 `ItemStockLog` 조회
- `LinkedHashMap`으로 날짜순 정렬 유지, 날짜별 IN/OUT 합산
- 최대값 기준 높이(%) 정규화
- `getDashboard()`에서 호출

### 5. dashboard.mustache — 하드코딩 바 차트 → 실데이터 교체

- `{{#dashboard.chartDays}}` 반복으로 7개 막대 동적 렌더링
- `style="height:{{inHeight}}%"` / `style="height:{{outHeight}}%"`
- 막대에 `title` 속성으로 수량 툴팁 표시

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `item/log/ItemStockLogRepository.java` | 날짜 범위 조회 메서드 추가 |
| `item/dto/ItemChartDayDto.java` | 신규 생성 |
| `item/dto/ItemDashboardDto.java` | `chartDays` 필드 추가 |
| `item/ItemManagerService.java` | `buildChartDays()` 추가, `getDashboard()` 수정 |
| `templates/item-manager/dashboard.mustache` | 하드코딩 차트 → 동적 렌더링 교체 |

---

## 결과

- 대시보드 입출고 현황 그래프가 실제 DB 데이터(최근 7일)로 표시
- 데이터 없는 날은 막대 없음(0%)
- 입고/출고 중 최대값 기준으로 높이 정규화되어 시각적 비교 가능
