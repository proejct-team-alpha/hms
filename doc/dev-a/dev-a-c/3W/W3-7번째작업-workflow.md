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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 물품 사용 이력 테이블에서 `reservation_id` 컬럼이 null(비어있음)을 허용하도록 변경합니다. 기존에는 반드시 예약 ID가 있어야 로그를 저장할 수 있었지만, 원무과처럼 예약과 무관한 출고도 이력으로 남길 수 있게 됩니다.
> - **왜 이렇게 썼는지**: `nullable = true`로 설정하면 데이터베이스 컬럼이 NULL 값을 허용합니다. 예약이 없는 출고(`원무과`, `관리자` 등)는 `reservationId`가 null로 저장되어 나중에 구분 조회할 수 있습니다.
> - **쉽게 말하면**: 이제 예약 번호가 없어도 물품 사용 기록을 남길 수 있습니다. "어느 환자에 사용했는지 모를 때" 빈칸으로 두는 것을 허용하는 설정입니다.

### 2. ItemUsageLogRepository — 스태프 로그 조회

```java
// TODO [W3-7]: reservationId가 null인 오늘 날짜 로그 조회
// List<ItemUsageLog> findByReservationIdIsNullAndUsedAtBetweenOrderByUsedAtDesc(
//     LocalDateTime start, LocalDateTime end);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 ID가 없는(`null`) 오늘 날짜의 물품 사용 이력을 최신순으로 조회하는 JPA 메서드를 추가합니다. 원무과나 관리자가 예약 없이 출고한 이력만 가져옵니다.
> - **왜 이렇게 썼는지**: Spring Data JPA는 메서드 이름을 분석해서 자동으로 SQL 쿼리를 생성합니다. `findBy...IsNull...Between...OrderBy...Desc`처럼 규칙에 맞게 이름을 작성하면 SQL 없이도 원하는 조회가 가능합니다.
> - **쉽게 말하면**: "오늘 예약 없이 출고된 물품 기록을 최신 것부터 가져와주세요"라는 요청을 메서드 이름만으로 표현한 것입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 첫 번째는 `useItem()` 메서드에서 기존의 "reservationId가 있을 때만 로그 저장" 조건을 제거해서 항상 로그를 저장하도록 변경합니다. 두 번째는 오늘 날짜의 원무과 출고 이력을 DTO 목록으로 반환하는 메서드를 추가합니다.
> - **왜 이렇게 썼는지**: `atStartOfDay()`는 오늘 00:00:00을, `plusDays(1)`은 내일 00:00:00을 의미해서 오늘 하루 범위를 지정합니다. `stream().map().toList()`는 엔티티 목록을 DTO 목록으로 변환하는 Java Stream API입니다.
> - **쉽게 말하면**: 이제 누가 출고하든 항상 기록이 남고, 오늘 예약 없이 출고한 이력 목록을 화면에 보여줄 수 있습니다.

### 4. LayoutModelInterceptor — 플래그 추가

```java
// TODO [W3-7]: isStaffItemUse 플래그
// mav.addObject("isStaffItemUse", path.startsWith("/staff/item"));
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 인터셉터(모든 요청을 가로채는 공통 처리기)에서 현재 URL이 `/staff/item`으로 시작하면 `isStaffItemUse`를 `true`로 설정합니다. 이 값이 뷰에 전달되어 사이드바 메뉴 강조에 사용됩니다.
> - **왜 이렇게 썼는지**: 인터셉터에서 페이지 활성 플래그를 관리하면 각 컨트롤러마다 `model.addAttribute`를 중복 작성하지 않아도 됩니다. `path.startsWith()`로 URL 패턴 매칭을 해서 하위 경로도 자동으로 포함됩니다.
> - **쉽게 말하면**: 원무과의 "물품 출고" 페이지에 있을 때 사이드바 메뉴가 자동으로 강조(활성화)되도록 설정하는 코드입니다.

### 5. sidebar-staff.mustache — 메뉴 추가

```html
<!-- TODO [W3-7]: 내 정보 관리 위에 추가 -->
<!-- <a href="/staff/item/use" ... {{#isStaffItemUse}} 활성 {{/isStaffItemUse}}> -->
<!--   <i data-feather="package"> 물품 출고 </a> -->
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 원무과 사이드바에 "물품 출고" 메뉴 링크를 추가합니다. `data-feather="package"`는 Feather Icons 라이브러리의 "패키지" 아이콘을 표시하는 속성이고, `{{#isStaffItemUse}}`는 현재 물품 출고 페이지에 있을 때 메뉴를 강조하는 조건입니다.
> - **왜 이렇게 썼는지**: Mustache의 `{{#플래그}}...{{/플래그}}` 문법으로 서버에서 전달한 boolean 값에 따라 활성 CSS 클래스를 조건부로 적용합니다. 이 방식으로 현재 위치를 시각적으로 알려줄 수 있습니다.
> - **쉽게 말하면**: 왼쪽 메뉴에 "물품 출고" 버튼을 추가하고, 이 페이지에 있을 때 버튼이 강조되어 표시되도록 합니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 원무과 물품 출고 페이지용 컨트롤러를 새로 만듭니다. GET 요청 시 물품 목록과 오늘 출고 이력을 화면에 전달하고, POST AJAX 요청 시 재고를 차감합니다. `reservationId=null`로 전달해 예약 없는 출고임을 명시합니다.
> - **왜 이렇게 썼는지**: 원무과 출고는 특정 환자 예약과 연결되지 않으므로 `reservationId`를 null로 전달합니다. `@ResponseBody` 어노테이션으로 JSON 응답을 반환하여 화면 이동 없는 AJAX 처리를 구현합니다.
> - **쉽게 말하면**: 원무과 물품 출고 페이지를 보여주고, 출고 버튼을 눌렀을 때 재고를 줄이는 서버 기능을 새로 만드는 코드입니다.

### 7. staff/item-use.mustache — 신규

```html
<!-- TODO [W3-7]: 전체 레이아웃 -->
<!-- 상단: 오늘 출고 내역 테이블 (todayLogs) -->
<!-- 하단: 카테고리 필터 + 초성 검색 + 물품 카드 4열 그리드 -->
<!-- AJAX: form[action$="/staff/item/use"] submit 가로채기 -->
<!-- reservationId hidden input 없음 (null 전달) -->
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 원무과 물품 출고 전용 페이지 HTML 레이아웃을 정의합니다. 상단에는 오늘 출고한 이력 테이블, 하단에는 카테고리 필터와 검색이 있는 물품 선택 그리드가 있습니다. AJAX로 출고 처리하며, `reservationId` hidden 필드는 없으므로 null이 전달됩니다.
> - **왜 이렇게 썼는지**: `hidden input`으로 `reservationId`를 전달하지 않으면 서버에 null이 넘어가서 예약 없는 출고로 처리됩니다. 의사/간호사 템플릿과 동일한 AJAX 패턴을 재사용해서 코드 일관성을 유지합니다.
> - **쉽게 말하면**: 원무과 물품 출고 화면을 만드는 코드로, 위쪽에는 오늘 뭘 출고했는지 보여주고 아래쪽에는 물품을 골라서 출고할 수 있는 그리드가 있습니다.

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
