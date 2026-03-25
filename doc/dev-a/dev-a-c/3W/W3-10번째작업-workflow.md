# W3-10번째작업 Workflow — 입출고 내역 SQL 연동

> **작성일**: 3W
> **목표**: 입고/출고 발생 시 ItemStockLog 자동 기록 + 입출고 내역 페이지에 총량 카드·검색·필터·페이징 표시

---

## 전체 흐름

```
ItemStockType enum 신규 생성
  → ItemStockLog 엔티티·Repository·Dto 신규 생성
  → ItemManagerService 수정 (IN/OUT 로그 저장, 총량 조회, getStockHistory())
  → ItemManagerController /item-history 모델 보강
  → item-history.mustache 전면 교체 (총량 카드, 검색, 필터, 페이징)
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 입고/출고 발생 시 내역 자동 기록 + 입출고 내역 페이지 실데이터 표시 |
| 구분 | `ItemStockType.IN` (입고) / `OUT` (출고) |
| 표시 컬럼 | 구분(입고/출고) \| 물품명 \| 수량 \| 일시 |
| 집계 | DB SUM으로 총 입고량 / 총 출고량 계산 |
| 필터 | 물품명 실시간 검색 + 입고/출고 구분 필터 |
| 페이징 | 15건씩, 이전/다음 + "N건 중 X-Y / Z페이지" 표시 |

---

## 실행 흐름

```
입고 발생 (restockItem / restockItemAndGetQuantity)
  → ItemManagerService: 재고 갱신 + ItemStockLog(IN) 저장

출고 발생 (useItem)
  → ItemManagerService: 재고 차감 + ItemStockLog(OUT) 저장

GET /item-manager/item-history
  → ItemManagerController: histories, hasHistories, totalIn, totalOut 모델
  → item-history.mustache 렌더링
    - 총량 요약 카드 (파란색: 총 입고 / 주황색: 총 출고)
    - 물품명 검색 + 구분 필터
    - 내역 테이블 (구분 배지 | 물품명 | 수량 | 일시)
    - 15건 페이징
```

---

## UI Mockup

```
┌───────────────────────────────────────────────────────┐
│ 입출고 현황                                             │
│ ┌─────────────────┐  ┌─────────────────┐              │
│ │  총 입고         │  │  총 출고         │              │
│ │  1,250개 (파란색) │  │  430개 (주황색)  │              │
│ └─────────────────┘  └─────────────────┘              │
│                                                       │
│ [검색: 물품명 입력...]  [전체] [입고] [출고]             │
│                                                       │
│ ┌──────┬──────────┬──────┬────────────────┐           │
│ │ 구분  │ 물품명    │ 수량  │ 일시            │           │
│ │ 입고  │ 붕대      │ 100개 │ 2026-03-17 09:00│           │
│ │ 출고  │ 장갑      │  5개  │ 2026-03-17 10:15│           │
│ └──────┴──────────┴──────┴────────────────┘           │
│ [이전] 50건 중 1-15 / 1페이지 [다음]                    │
└───────────────────────────────────────────────────────┘
```

---

## 작업 목록

1. `ItemStockType.java` — enum 신규 생성 (`IN`, `OUT`)
2. `ItemStockLog.java` — 엔티티 신규 생성 (id, itemId, itemName, type, amount, createdAt)
3. `ItemStockLogRepository.java` — 신규 생성 (`findAllByOrderByCreatedAtDesc()`, `sumAmountByType()`)
4. `ItemStockLogDto.java` — 신규 생성 (itemName, typeText, isIn, amount, createdAt)
5. `ItemManagerService.java` — `ItemStockLogRepository` 주입, `restockItem()`/`restockItemAndGetQuantity()`/`useItem()`에 로그 저장 추가, `getStockHistory()`/`getTotalInAmount()`/`getTotalOutAmount()` 추가
6. `ItemManagerController.java` — `/item-history`: histories, hasHistories, totalIn, totalOut 모델 추가
7. `item-history.mustache` — 전면 교체 (총량 카드, 검색, 구분 필터, 내역 테이블, 페이징)

---

## 작업 진행내용

- [x] ItemStockType enum 신규 생성
- [x] ItemStockLog 엔티티 신규 생성
- [x] ItemStockLogRepository 신규 생성
- [x] ItemStockLogDto 신규 생성
- [x] ItemManagerService ItemStockLogRepository 주입
- [x] restockItem() ItemStockLog(IN) 저장 추가
- [x] restockItemAndGetQuantity() ItemStockLog(IN) 저장 추가
- [x] useItem() ItemStockLog(OUT) 저장 추가
- [x] getStockHistory() / getTotalInAmount() / getTotalOutAmount() 추가
- [x] ItemManagerController /item-history 모델 보강
- [x] item-history.mustache 전면 교체

---

## 실행 흐름에 대한 코드

### 1. ItemStockType — enum

```java
public enum ItemStockType {
    IN,   // 입고
    OUT   // 출고
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 입고(IN)와 출고(OUT) 두 값만 허용하는 타입을 정의합니다.
> - **왜 이렇게 썼는지**: enum으로 만들면 DB에 저장할 때 문자열 오타("in", "입고" 등)를 코드에서 원천 차단합니다. `type == ItemStockType.IN`처럼 타입 안전한 비교도 가능합니다.
> - **쉽게 말하면**: "입고" / "출고" 두 가지만 쓸 수 있게 타입을 제한하는 열거형입니다.

### 2. ItemStockLogRepository — 조회 메서드

```java
// 전체 내역 최신순
List<ItemStockLog> findAllByOrderByCreatedAtDesc();

// 입고/출고 총량 DB SUM
@Query("SELECT COALESCE(SUM(l.amount), 0) FROM ItemStockLog l WHERE l.type = :type")
Long sumAmountByType(@Param("type") ItemStockType type);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 전체 이력을 최신순으로 가져오고, 입고 또는 출고 총량을 DB에서 합산합니다.
> - **왜 이렇게 썼는지**: `findAllByOrderByCreatedAtDesc()`는 JPA 메서드 이름 규칙으로 SQL을 자동 생성합니다. `sumAmountByType()`은 Java에서 합산하는 것보다 DB `SUM()` 집계가 훨씬 빠르므로 `@Query`로 직접 작성합니다. "DB가 잘하는 일은 DB에게"가 기본 원칙입니다.
> - **쉽게 말하면**: "전체 기록 최신 순으로 가져오기"와 "입고(또는 출고) 수량 합산"을 DB에 요청하는 메서드입니다.

### 3. ItemManagerService — useItem() 로그 저장 추가

```java
@Transactional
public int useItem(Long id, int amount, Long reservationId) {
    Item item = itemRepository.findById(id).orElseThrow();
    int newQty = item.getQuantity() - amount;
    if (newQty < 0) throw new IllegalStateException("재고가 " + (-newQty) + "개 부족합니다.");
    item.updateQuantity(newQty);
    if (reservationId != null) {
        itemUsageLogRepository.save(ItemUsageLog.of(reservationId, id, item.getName(), amount));
    }
    // ItemStockLog(OUT) 저장 추가
    itemStockLogRepository.save(ItemStockLog.of(id, item.getName(), ItemStockType.OUT, amount));
    return newQty;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 출고 시 `ItemUsageLog`(예약 연결 이력) 외에 `ItemStockLog(OUT)`도 함께 저장합니다.
> - **왜 이렇게 썼는지**: `ItemUsageLog`는 누가 어떤 예약에서 출고했는지 기록하고, `ItemStockLog`는 집계·보고용 입출고 이력입니다. 역할이 달라 두 테이블에 각각 저장합니다.
> - **쉽게 말하면**: 출고할 때 "누가 언제 얼마나 썼나" 기록(UsageLog)과 "전체 입출고 집계" 기록(StockLog)을 동시에 남깁니다.

### 4. item-history.mustache — hasHistories 플래그 패턴

```html
{{#hasHistories}}
<table>
  {{#histories}}
  <tr>
    <td>{{typeText}}</td>
    <td>{{itemName}}</td>
    <td>{{amount}}</td>
    <td>{{createdAt}}</td>
  </tr>
  {{/histories}}
</table>
{{/hasHistories}}
{{^hasHistories}}
<p>입출고 내역이 없습니다.</p>
{{/hasHistories}}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `hasHistories` boolean으로 테이블 표시 여부를 판단합니다.
> - **왜 이렇게 썼는지**: `{{#histories}}테이블{{/histories}}` 안에 `{{#histories}}`가 중첩되면 데이터가 2번 반복되는 이중 렌더링 버그가 발생합니다. `hasHistories` boolean으로 분리하면 이 문제가 해결됩니다.
> - **쉽게 말하면**: 목록이 있으면 테이블, 없으면 안내 문구를 보여주되 Mustache 중첩 문제를 피하는 패턴입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 입고 발생 | restockItem() 호출 | ItemStockLog(IN) 저장 |
| 출고 발생 | useItem() 호출 | ItemStockLog(OUT) 저장 |
| 내역 페이지 접속 | `/item-manager/item-history` | 총량 카드 + 내역 테이블 표시 |
| 물품명 검색 | 검색어 입력 | 실시간 필터링 |
| 구분 필터 | 입고/출고 버튼 클릭 | 해당 구분만 표시 |
| 페이징 | 다음 버튼 클릭 | 다음 15건 표시 |
| 이중 렌더링 버그 | 내역 페이지 접속 | 각 항목 1회만 표시 |

---

## 완료 기준

- [x] 입고(`restockItem`) / 출고(`useItem`) 발생 시 `ItemStockLog` 자동 저장
- [x] 총 입고·출고 수량 DB SUM으로 정확하게 집계
- [x] 물품명 검색 + 입고/출고 구분 필터 + 15건 페이징 정상 작동
- [x] 이중 렌더링 버그 수정 (`hasHistories` boolean 분리)
- [x] `config/`, `domain/` 수정 없음
