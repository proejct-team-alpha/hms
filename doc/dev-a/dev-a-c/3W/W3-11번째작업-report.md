# W3-11번째작업 리포트 - 대시보드 그래프 입출고 내역 연동

## 작업 개요
- **작업명**: 대시보드 입출고 현황 바 차트를 하드코딩 → 실제 DB 데이터(최근 7일)로 교체
- **수정 파일**: `item/log/ItemStockLogRepository.java`, `item/dto/ItemChartDayDto.java`(신규), `item/dto/ItemDashboardDto.java`, `item/ItemManagerService.java`, `templates/item-manager/dashboard.mustache`

## 작업 내용

### 1. ItemStockLogRepository — 날짜 범위 조회 메서드 추가

```java
List<ItemStockLog> findByCreatedAtBetweenOrderByCreatedAtAsc(
    LocalDateTime start, LocalDateTime end);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 지정한 날짜 범위의 입출고 이력을 날짜 오름차순으로 조회합니다.
> - **왜 이렇게 썼는지**: 전체 이력을 가져오면 데이터가 많아질수록 느립니다. 최근 7일만 필요하므로 날짜 범위 조건을 줘서 필요한 데이터만 조회합니다. `Between`은 JPA 메서드 이름 규칙으로 `WHERE created_at BETWEEN start AND end` SQL을 자동 생성합니다.
> - **쉽게 말하면**: "이 기간 사이의 입출고 기록만 날짜 순으로 가져오기"를 메서드 이름으로 표현합니다.

### 2. ItemChartDayDto — 신규 생성

필드: label(MM/dd), inAmount, outAmount, inHeight(0~100%), outHeight(0~100%).

높이는 7일 중 입고/출고 합산 최대값 기준으로 정규화. 예: 최대 50개인 날 = 100%, 25개인 날 = 50%.

### 3. ItemDashboardDto — chartDays 필드 추가

`List<ItemChartDayDto> chartDays`.

### 4. ItemManagerService — buildChartDays() 추가

- 최근 7일(오늘 포함) 범위로 `ItemStockLog` 조회
- `LinkedHashMap`으로 날짜순 정렬 유지, 날짜별 IN/OUT 합산
- 최대값 기준 높이(%) 정규화
- `getDashboard()`에서 호출

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 최근 7일 입출고 이력을 날짜별로 집계하고 바 차트 높이(%)를 계산합니다.
> - **왜 이렇게 썼는지**: `LinkedHashMap`을 쓰는 이유는 일반 `HashMap`이 키 순서를 보장하지 않기 때문입니다. 날짜별 데이터를 날짜 순서대로 차트에 표시하려면 삽입 순서를 유지하는 `LinkedHashMap`이 필요합니다. 높이를 최대값 기준으로 정규화하면 막대가 항상 화면 안에 들어오고 상대적 비교가 쉬워집니다.
> - **쉽게 말하면**: 7일치 입출고 기록을 날짜별로 정리하고 막대 높이를 계산해 차트용 데이터를 만들어 줍니다.

### 5. dashboard.mustache — 하드코딩 바 차트 → 실데이터 교체

```html
{{#dashboard.chartDays}}
<div class="flex flex-col items-center gap-1">
  <div class="flex gap-1 items-end h-24">
    <div class="w-3 bg-blue-400" style="height:{{inHeight}}%" title="입고 {{inAmount}}개"></div>
    <div class="w-3 bg-orange-400" style="height:{{outHeight}}%" title="출고 {{outAmount}}개"></div>
  </div>
  <span class="text-xs text-slate-500">{{label}}</span>
</div>
{{/dashboard.chartDays}}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `{{#dashboard.chartDays}}` 반복으로 7개 막대를 동적으로 렌더링합니다. `style="height:{{inHeight}}%"`로 서버에서 계산한 높이를 HTML에 직접 삽입합니다.
> - **왜 이렇게 썼는지**: Mustache 서버 렌더링에서는 서버가 이미 계산한 % 값을 HTML에 넣으면 JavaScript 없이도 차트가 표시됩니다. `title` 속성으로 막대에 마우스를 올리면 수량을 볼 수 있어 추가 JavaScript 없이 툴팁 효과를 냅니다.
> - **쉽게 말하면**: 서버가 막대 높이를 계산해 HTML에 넣어주면, 브라우저는 그냥 그 높이대로 막대를 그립니다.

## 테스트 결과

| 항목 | 결과 |
|------|------|
| 대시보드 차트 실데이터 표시 (최근 7일) | ✅ |
| 데이터 없는 날 막대 없음(0%) | ✅ |
| 최대값 기준 높이 정규화 | ✅ |
| 막대 title 속성 툴팁 표시 | ✅ |

## 특이사항
- `LinkedHashMap` 사용: `HashMap`은 키 순서 미보장 → 날짜 순서 유지를 위해 필수
- 높이 정규화: 고정 최대값 대신 실데이터 최대값 기준 동적 계산 — 데이터 규모에 무관하게 시각적으로 정확
- 서버 사이드 CSS 계산: `style="height:{{inHeight}}%"` 방식으로 JavaScript 없이 차트 구현
- workflow 파일 없음: 작업 규모가 작아 report만 작성
