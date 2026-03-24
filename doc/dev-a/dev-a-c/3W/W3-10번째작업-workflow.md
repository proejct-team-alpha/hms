# W3-10 Workflow — 입출고 내역 SQL 연동

## 작업 개요

- **목표:** 입고/출고 발생 시 내역 자동 기록 + 입출고 내역 페이지 테이블 표시
- **컬럼:** 구분(입고/출고) | 물품명 | 수량 | 일시
- **필터:** 없음 (전체 내역 최신순)

---

## 작업 목록

### 1. `ItemStockType` enum 신규 생성
- `IN` (입고), `OUT` (출고)

### 2. `ItemStockLog` 엔티티 신규 생성
- 필드: id, itemId, itemName, type(ItemStockType), amount, createdAt

### 3. `ItemStockLogRepository` 신규 생성
- `findAllByOrderByCreatedAtDesc()`

### 4. `ItemStockLogDto` 신규 생성
- itemName, typeText(입고/출고), amount, createdAt(yyyy-MM-dd HH:mm)

### 5. `ItemManagerService` 수정
- `ItemStockLogRepository` 주입
- `restockItem()`: ItemStockLog(IN) 저장
- `restockItemAndGetQuantity()`: ItemStockLog(IN) 저장
- `useItem()`: ItemStockLog(OUT) 저장
- `getStockHistory()` 추가 → 전체 내역 최신순 반환

### 6. `ItemManagerController` 수정
- `/item-history`: `histories` 모델 추가

### 7. `item-history.mustache` 수정
- placeholder → 실제 테이블로 교체

---

> **💡 입문자 설명**
>
> **왜 `ItemStockType` enum을 별도 파일로 만드는지**
> - `IN`/`OUT` 두 값만 있지만 enum으로 만들면 DB에 저장할 때 문자열 오타(예: "in", "입고")를 코드에서 원천 차단합니다. 또한 `type == ItemStockType.IN`처럼 타입 안전한 비교가 가능합니다.
>
> **`findAllByOrderByCreatedAtDesc()` — JPA 메서드 이름 규칙**
> - Spring Data JPA는 메서드 이름을 보고 자동으로 SQL을 생성합니다. `findAll`(모두 조회) + `OrderBy`(정렬) + `CreatedAt`(필드명) + `Desc`(내림차순) → `SELECT * FROM item_stock_log ORDER BY created_at DESC`
> - SQL을 직접 쓰지 않아도 메서드 이름만으로 쿼리가 만들어지는 JPA의 핵심 기능입니다.
>
> **높이 정규화 (0~100%) — 왜 필요한지**
> - 바 차트에서 막대 높이를 픽셀로 고정하면 데이터가 커질수록 막대가 화면 밖으로 나갑니다. 최대값 기준으로 비율(%)을 계산하면 항상 화면 안에 들어오고, 상대적 비교가 쉬워집니다.
>
> **쉽게 말하면**: 입고·출고가 발생할 때마다 이력 테이블에 기록하고, 입출고 내역 페이지에서 전체 목록과 총량 요약을 보여주는 기능입니다.
