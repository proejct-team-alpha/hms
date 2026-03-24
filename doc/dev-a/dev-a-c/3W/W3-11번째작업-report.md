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

---

> **💡 입문자 설명**
>
> **날짜 범위 조회 (`findByCreatedAtBetween...`) — 왜 7일 범위로 조회하는지**
> - 전체 이력을 가져오면 데이터가 많아질수록 느립니다. "오늘 포함 최근 7일"만 필요하므로 날짜 범위 조건을 줘서 필요한 데이터만 조회합니다. `Between`은 JPA 메서드 이름 규칙으로 `WHERE created_at BETWEEN start AND end` SQL을 만듭니다.
>
> **`LinkedHashMap` — 왜 일반 `HashMap`이 아닌지**
> - `HashMap`은 키 순서를 보장하지 않습니다. 날짜별 데이터를 날짜 순서대로 차트에 표시하려면 삽입 순서를 유지하는 `LinkedHashMap`이 필요합니다. "월·화·수·목·금·토·일" 순서가 섞이면 차트가 의미 없어집니다.
>
> **높이 정규화 — 어떻게 계산하는지**
> - 7일 중 입고/출고 합산 최대값을 찾아 100%로 설정합니다. 각 날의 값 ÷ 최대값 × 100 = 높이(%)입니다. 예: 최대 50개인 날 = 100%, 25개인 날 = 50%로 표시됩니다.
> - **다른 방법**: 고정 최대값(예: 항상 100개 기준)을 설정할 수도 있지만, 실제 데이터 범위에 맞게 동적으로 조정하는 것이 시각적으로 더 정확합니다.
>
> **`style="height:{{inHeight}}%"` — 서버에서 CSS를 만드는 이유**
> - 막대 높이를 JavaScript로 계산해 동적으로 설정할 수도 있습니다. 하지만 Mustache 서버 렌더링에서는 서버가 이미 계산한 % 값을 HTML에 직접 넣으면 JavaScript가 없어도 차트가 표시됩니다. 더 단순하고 안정적입니다.
>
> **쉽게 말하면**: 대시보드의 "이번 주 입출고 현황" 막대 그래프가 하드코딩된 가짜 데이터에서 실제 DB 데이터로 바뀝니다. 서버가 최근 7일 기록을 읽어 각 날의 막대 높이를 계산해 HTML에 넣어줍니다.
