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

---

> **💡 입문자 설명**
>
> **`ItemStockLog` vs `ItemUsageLog` — 두 로그 테이블의 차이**
> - `ItemUsageLog`: 누가 어떤 예약에서 물품을 사용했는지 기록(의사·간호사·원무과 출고).
> - `ItemStockLog`: 입고(IN)와 출고(OUT) 전체 이력을 유형만 구분해서 기록. 총 입고량·총 출고량 집계에 사용합니다.
> - 두 테이블이 나뉜 이유: 사용 이력은 예약(환자)과 연결이 필요하고, 재고 변동 이력은 집계·보고 목적입니다. 역할이 다르므로 분리합니다.
>
> **DB SUM 집계 (`sumAmountByType`) — 왜 Java에서 계산하지 않는지**
> - 모든 로그를 Java로 가져온 후 합산하면 데이터가 많아질수록 느립니다. DB에서 `SUM()` 집계 함수로 계산하면 훨씬 빠릅니다. "DB가 잘하는 일은 DB에게"가 기본 원칙입니다.
>
> **`isIn` boolean 필드 — Mustache에서 왜 필요한지**
> - Mustache는 `{{#isIn}}`처럼 boolean만 조건으로 쓸 수 있습니다. Java enum(`ItemStockType.IN`)을 직접 Mustache 조건에 쓸 수 없어서, DTO에서 미리 `isIn = type == IN`으로 변환합니다.
>
> **`hasHistories` boolean 플래그 — 이중 렌더링 버그 원인**
> - Mustache에서 `{{#histories}}테이블{{/histories}}{{^histories}}없음{{/histories}}`는 정상입니다. 그런데 `{{#histories}}` 블록 안에 또 `{{#histories}}`가 중첩되면 데이터가 2번 반복됩니다. `hasHistories` boolean으로 분리하면 이 문제가 해결됩니다.
>
> **쉽게 말하면**: 물품이 언제 얼마나 들어오고 나갔는지 전체 기록을 DB에 남기고, "총 입고량 N개 / 총 출고량 M개" 요약 카드와 상세 목록을 보여주는 기능입니다.
