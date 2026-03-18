# W3-10번째작업 Report — 입출고 내역 SQL 연동

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. ItemStockType — enum 신규 생성

- `IN` (입고), `OUT` (출고)

### 2. ItemStockLog — 엔티티 신규 생성

- 테이블명: `item_stock_log`
- 필드: id, itemId, itemName, type(ItemStockType), amount, createdAt

### 3. ItemStockLogRepository — 신규 생성

- `findAllByOrderByCreatedAtDesc()`: 전체 내역 최신순 조회
- `sumAmountByType(ItemStockType type)`: 입고/출고 총량 DB SUM 집계

### 4. ItemStockLogDto — 신규 생성

- itemName, typeText(입고/출고), isIn(boolean), amount, createdAt(yyyy-MM-dd HH:mm)

### 5. ItemManagerService — 수정

- `ItemStockLogRepository` 주입
- `restockItem()`: `ItemStockLog(IN)` 저장
- `restockItemAndGetQuantity()`: `ItemStockLog(IN)` 저장
- `useItem()`: `ItemStockLog(OUT)` 저장
- `getStockHistory()` 추가
- `getTotalInAmount()` / `getTotalOutAmount()` 추가

### 6. ItemManagerController — 수정

- `/item-history`: `histories`, `hasHistories`, `totalIn`, `totalOut` 모델 추가

### 7. item-history.mustache — 전면 교체

- **총량 요약 카드:** 총 입고 수량(파란색) / 총 출고 수량(주황색) — DB SUM 집계
- **검색:** 물품명 실시간 필터
- **구분 필터:** 전체 / 입고 / 출고 버튼
- **테이블:** 구분(배지) | 물품명 | 수량 | 일시
- **페이징:** 15건씩, 이전/다음 버튼 + "N건 중 X-Y / Z페이지" 표시
- **버그 수정:** `{{#histories}}` 중첩으로 인한 이중 렌더링 → `hasHistories` boolean 플래그로 분리

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `item/log/ItemStockType.java` | 신규 생성 |
| `item/log/ItemStockLog.java` | 신규 생성 |
| `item/log/ItemStockLogRepository.java` | 신규 생성 |
| `item/log/ItemStockLogDto.java` | 신규 생성 |
| `item/ItemManagerService.java` | IN/OUT 로그 저장, 총량 조회 메서드 추가 |
| `item/ItemManagerController.java` | `/item-history` 모델 보강 |
| `templates/item-manager/item-history.mustache` | 전면 교체 (총량 카드, 검색, 필터, 페이징) |

---

## 결과

- 입고(`restockItem`) / 출고(`useItem`) 발생 시 `ItemStockLog` 자동 저장
- 총 입고·출고 수량 DB SUM으로 정확하게 집계
- 물품명 검색 + 입고/출고 구분 필터 + 15건 페이징 정상 작동
- 이중 렌더링 버그 수정 완료
