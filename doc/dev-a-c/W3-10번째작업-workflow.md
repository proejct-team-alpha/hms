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
