# W3-10번째작업 리포트 - 입출고 내역 SQL 연동

## 작업 개요
- **작업명**: 입고/출고 발생 시 ItemStockLog 자동 기록, 입출고 내역 페이지에 총량 카드·검색·구분 필터·15건 페이징 구현
- **수정 파일**: `item/log/ItemStockType.java`(신규), `item/log/ItemStockLog.java`(신규), `item/log/ItemStockLogRepository.java`(신규), `item/log/ItemStockLogDto.java`(신규), `item/ItemManagerService.java`, `item/ItemManagerController.java`, `templates/item-manager/item-history.mustache`

## 작업 내용

### 1. ItemStockType — enum 신규 생성

`IN` (입고), `OUT` (출고).

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 입고와 출고 두 값만 허용하는 열거형 타입을 정의합니다.
> - **왜 이렇게 썼는지**: enum으로 만들면 DB에 저장할 때 문자열 오타를 코드에서 원천 차단하고 `type == ItemStockType.IN`처럼 타입 안전한 비교가 가능합니다.
> - **쉽게 말하면**: "입고" / "출고" 두 가지만 쓸 수 있게 타입을 제한하는 열거형입니다.

### 2. ItemStockLog — 엔티티 신규 생성

테이블명: `item_stock_log`. 필드: id, itemId, itemName, type(ItemStockType), amount, createdAt.

### 3. ItemStockLogRepository — 신규 생성

- `findAllByOrderByCreatedAtDesc()`: 전체 내역 최신순 조회
- `sumAmountByType(ItemStockType type)`: 입고/출고 총량 DB SUM 집계

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 전체 이력을 최신순으로 가져오고, 입고 또는 출고 총량을 DB에서 합산합니다.
> - **왜 이렇게 썼는지**: DB `SUM()` 집계로 계산하면 Java에서 전체를 불러와 합산하는 것보다 훨씬 빠릅니다. "DB가 잘하는 일은 DB에게"가 기본 원칙입니다.
> - **쉽게 말하면**: "전체 기록 최신순 조회"와 "입고(또는 출고) 총량 합산"을 DB에 요청합니다.

### 4. ItemStockLogDto — 신규 생성

itemName, typeText(입고/출고), isIn(boolean), amount, createdAt(yyyy-MM-dd HH:mm).

`isIn` boolean 필드: Mustache에서 `{{#isIn}}`처럼 boolean만 조건으로 쓸 수 있어 DTO에서 미리 `isIn = type == IN`으로 변환.

### 5. ItemManagerService — 수정

- `ItemStockLogRepository` 주입
- `restockItem()`: `ItemStockLog(IN)` 저장 추가
- `restockItemAndGetQuantity()`: `ItemStockLog(IN)` 저장 추가
- `useItem()`: `ItemStockLog(OUT)` 저장 추가
- `getStockHistory()` 추가 — 전체 내역 최신순 반환
- `getTotalInAmount()` / `getTotalOutAmount()` 추가

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 입고·출고가 발생할 때마다 `ItemStockLog`를 자동으로 저장합니다.
> - **왜 이렇게 썼는지**: `ItemUsageLog`는 예약(환자)과 연결된 출고 이력이고, `ItemStockLog`는 집계·보고용 전체 입출고 이력입니다. 역할이 달라 두 테이블에 각각 저장합니다.
> - **쉽게 말하면**: 물품이 들어오거나 나갈 때마다 "집계 장부"(StockLog)에 자동으로 기록합니다.

### 6. ItemManagerController — 수정

`/item-history`: `histories`, `hasHistories`, `totalIn`, `totalOut` 모델 추가.

### 7. item-history.mustache — 전면 교체

- **총량 요약 카드**: 총 입고 수량(파란색) / 총 출고 수량(주황색) — DB SUM 집계
- **검색**: 물품명 실시간 필터
- **구분 필터**: 전체 / 입고 / 출고 버튼
- **테이블**: 구분(배지) | 물품명 | 수량 | 일시
- **페이징**: 15건씩, 이전/다음 버튼 + "N건 중 X-Y / Z페이지" 표시
- **버그 수정**: `{{#histories}}` 중첩으로 인한 이중 렌더링 → `hasHistories` boolean 플래그로 분리

## 테스트 결과

| 항목 | 결과 |
|------|------|
| 입고 발생 시 ItemStockLog(IN) 자동 저장 | ✅ |
| 출고 발생 시 ItemStockLog(OUT) 자동 저장 | ✅ |
| 총 입고·출고 수량 집계 표시 | ✅ |
| 물품명 검색 + 구분 필터 | ✅ |
| 15건 페이징 정상 동작 | ✅ |
| 이중 렌더링 버그 수정 | ✅ |

## 특이사항
- `ItemStockLog` vs `ItemUsageLog`: StockLog는 입출고 집계·보고용, UsageLog는 예약(환자) 연결 이력 — 역할이 달라 분리
- `isIn` boolean 필드: Mustache에서 Java enum을 직접 조건으로 쓸 수 없어 DTO에서 미리 변환
- `hasHistories` 플래그: `{{#histories}}` 중첩 이중 렌더링 버그 방지용 boolean 분리
- DB SUM 집계: Java에서 합산 대신 `SUM()` 집계 함수 사용 — 데이터 증가에도 성능 유지
