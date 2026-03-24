# W3-2번째작업 리포트 - 입고 버튼 AJAX 처리

## 작업 개요
- **작업명**: 입고 버튼 클릭 시 페이지 새로고침 없이 해당 행 재고 수량만 갱신 (AJAX 처리)
- **수정 파일**: `item/ItemManagerService.java`, `item/ItemManagerController.java`, `templates/item-manager/item-list.mustache`

## 작업 내용

### 1. ItemManagerService — restockItemAndGetQuantity() 추가

기존 `restockItem()`과 동일하게 `item.addStock(amount)` 호출. 트랜잭션 내에서 업데이트된 수량(`item.getQuantity()`)을 반환.

```java
@Transactional
public int restockItemAndGetQuantity(Long id, int amount) {
    Item item = itemRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("물품 없음"));
    item.addStock(amount);
    return item.getQuantity();
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 입고 처리 후 업데이트된 재고 수량을 반환합니다.
> - **왜 이렇게 썼는지**: 기존 `restockItem()`은 반환값이 없었습니다. AJAX 응답으로 새 수량을 화면에 반영하려면 업데이트된 수량이 필요해 새 메서드를 추가했습니다.
> - **쉽게 말하면**: 입고 처리 후 "이제 몇 개야?"를 알려주는 기능입니다.

### 2. ItemManagerController — /item/restock/ajax 엔드포인트 추가

`@PostMapping("/item/restock/ajax")` + `@ResponseBody`. 기존 `parseQuantity()` 재사용. 성공: `{"quantity": N}` JSON 반환. 실패: 400 + `{"error": 메시지}` JSON 반환.

### 3. item-list.mustache — JS AJAX 핸들러 추가

`form[action$="/item/restock"]` 셀렉터로 입고 폼 선택. `e.preventDefault()`로 기본 submit 차단. 검증: 빈값·문자·소수·음수 → `alert` 후 return. `URLSearchParams`로 id + amount + CSRF 토큰 조립. `fetch` POST → `/item-manager/item/restock/ajax`. 성공 시: 해당 행 3번째 td span 수량 갱신 + 입력 필드 초기화.

## 테스트 결과

| 항목 | 결과 |
|------|------|
| 정상 입고 — 페이지 이동 없이 수량 갱신 | ✅ |
| 음수·문자·소수·빈값 — alert 차단 | ✅ |
| 입고 성공 후 입력 필드 초기화 | ✅ |
| 기존 `/item/restock` 엔드포인트 동작 유지 | ✅ |

## 특이사항
- `restockItemAndGetQuantity()`를 별도로 만든 이유: 기존 `restockItem()`을 수정하면 다른 호출 지점에 영향을 줄 수 있어 새 메서드를 추가함
- CSRF 토큰을 `URLSearchParams`에 포함하지 않으면 Spring Security가 403을 반환하므로 반드시 포함
