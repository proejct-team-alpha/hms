# W3-7번째작업 Workflow — 원무과 물품 출고 기능

## 작업 개요

- **목표:** 원무과 사이드바에 "물품 출고" 메뉴 추가 + 전용 페이지에서 카테고리 필터·초성 검색·AJAX 출고·ItemUsageLog 저장
- **담당:** dev-a-c
- **날짜:** 2026-03-17

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `item/log/ItemUsageLog.java` | 수정 (reservationId nullable 허용) |
| `item/log/ItemUsageLogRepository.java` | 수정 (오늘 날짜 스태프 로그 조회 메서드 추가) |
| `item/ItemManagerService.java` | 수정 (useItem null reservationId도 저장, 스태프 로그 조회 메서드 추가) |
| `common/interceptor/LayoutModelInterceptor.java` | 수정 (isStaffItemUse 플래그 추가) |
| `templates/common/sidebar-staff.mustache` | 수정 ("물품 출고" 메뉴 추가) |
| `staff/item/StaffItemController.java` | 신규 생성 (GET /staff/item/use, POST /staff/item/use) |
| `templates/staff/item-use.mustache` | 신규 생성 (물품 출고 전용 페이지) |

---

## 작업 목록

### 1. ItemUsageLog — reservationId nullable

```java
// TODO [W3-7]: reservationId 컬럼 nullable 허용
// @Column(name = "reservation_id", nullable = true)  ← nullable = true
// of() 메서드: reservationId Long 그대로 유지 (null 가능)
```

### 2. ItemUsageLogRepository — 스태프 로그 조회

```java
// TODO [W3-7]: reservationId가 null인 오늘 날짜 로그 조회
// List<ItemUsageLog> findByReservationIdIsNullAndUsedAtBetweenOrderByUsedAtDesc(
//     LocalDateTime start, LocalDateTime end);
```

### 3. ItemManagerService — 수정

```java
// TODO [W3-7]: useItem() — reservationId null이어도 로그 저장
// if (reservationId != null) → 조건 제거, 항상 save

// TODO [W3-7]: getTodayStaffUsageLogs() 메서드 추가
// start = LocalDate.now().atStartOfDay()
// end   = start.plusDays(1)
// return findByReservationIdIsNullAndUsedAtBetween(start, end)
//        .stream().map(ItemUsageLogDto::new).toList()
```

### 4. LayoutModelInterceptor — 플래그 추가

```java
// TODO [W3-7]: isStaffItemUse 플래그
// mav.addObject("isStaffItemUse", path.startsWith("/staff/item"));
```

### 5. sidebar-staff.mustache — 메뉴 추가

```html
<!-- TODO [W3-7]: 내 정보 관리 위에 추가 -->
<!-- <a href="/staff/item/use" ... {{#isStaffItemUse}} 활성 {{/isStaffItemUse}}> -->
<!--   <i data-feather="package"> 물품 출고 </a> -->
```

### 6. StaffItemController — 신규

```java
// TODO [W3-7]: GET /staff/item/use
// - model: items, todayLogs, pageTitle="물품 출고"
// - return "staff/item-use"

// TODO [W3-7]: POST /staff/item/use (AJAX @ResponseBody)
// - amount 검증
// - itemManagerService.useItem(id, amount, null)  ← reservationId=null
// - 성공: {"quantity": N} / 실패: 400 + {"error": "..."}
```

### 7. staff/item-use.mustache — 신규

```html
<!-- TODO [W3-7]: 전체 레이아웃 -->
<!-- 상단: 오늘 출고 내역 테이블 (todayLogs) -->
<!-- 하단: 카테고리 필터 + 초성 검색 + 물품 카드 4열 그리드 -->
<!-- AJAX: form[action$="/staff/item/use"] submit 가로채기 -->
<!-- reservationId hidden input 없음 (null 전달) -->
```

---

## 상세 구현 계획

### ItemUsageLog 스키마 변경
- `reservation_id` 컬럼: `nullable = false` → `nullable = true`
- `of()` 메서드 그대로 유지 (null 허용됨)

### useItem() 변경
- 기존: `if (reservationId != null) { save log }`
- 변경: 항상 save (reservationId null이면 스태프 출고 이력으로 저장)

### 오늘 출고 내역 표시
- `reservationId IS NULL AND usedAt BETWEEN 오늘 00:00 ~ 내일 00:00`
- 최신순 정렬 (DESC)

### 페이지 레이아웃
1. **오늘 출고 내역** (상단 전체 너비 카드)
2. **물품 출고 섹션** (카테고리 필터 + 검색 + 카드 그리드)

---

## 금지 사항 체크

- [x] `config/`, `domain/` 수정 없음
- [x] `admin/**` 수정 없음
- [x] `doctor/**`, `nurse/**` 수정 없음
