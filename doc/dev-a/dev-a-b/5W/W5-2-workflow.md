# W5-2번째작업 Workflow — 강상민 🌟🌟🌟 버그 수정

> **작성일**: 5W
> **목표**: 물품 입고 즉시 상태 변경 + 비활성화 직원 수정 오류 방어

---

## 전체 흐름

```
물품 입고 AJAX 응답에 minQuantity 추가 → JS에서 lowStock 판단 → 상태 배지 즉시 교체
비활성화 직원 detail() try-catch 추가 → CustomException 시 목록 redirect
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 이슈 1 | 물품 입고 후 상태 배지(정상/재고부족)가 새로고침 없이는 바뀌지 않음 |
| 이슈 2 | 비활성화 직원 수정 버튼 클릭 시 특정 케이스에서 오류 발생 (간헐적) |
| lowStock 기준 | `quantity < minQuantity` |
| 진행 범위 | 🌟🌟🌟 항목 2개 |

---

## 실행 흐름

```
[물품 입고 즉시 상태 변경]
입고 버튼 클릭
  → fetch POST /item-manager/item/restock/ajax
  → 응답: { quantity, minQuantity }
  → lowStock = quantity < minQuantity
  → 수량 텍스트 + 색상 갱신
  → 상태 배지 교체 (재고부족 ↔ 정상)

[비활성화 직원 수정 오류 방어]
GET /admin/staff/detail?staffId=X
  → try: getEditForm() → staff-form 렌더링
  → catch CustomException: errorMessage 플래시 → redirect /admin/staff/list
```

---

## 작업 목록

1. `ItemManagerService.java` — `restockItemAndGetInfo()` 신규 메서드 추가
2. `ItemManagerController.java` — `restockItemAjax()` 새 메서드 사용으로 교체
3. `item-list.mustache` — 입고 성공 후 수량 색상 + 상태 배지 즉시 갱신 JS
4. `AdminStaffController.java` — `detail()` try-catch 추가

---

## 작업 진행내용

- [x] ItemManagerService — restockItemAndGetInfo() 추가 (quantity + minQuantity 반환)
- [x] ItemManagerController — restockItemAjax() 수정 (새 메서드 사용)
- [x] item-list.mustache — JS 즉시 갱신 로직 추가
- [x] AdminStaffController — detail() try-catch 추가
- [x] 남은 작업 리스트 체크

---

## 실행 흐름에 대한 코드

### 1. restockItemAndGetInfo() — ItemManagerService

```java
@Transactional
public Map<String, Object> restockItemAndGetInfo(Long id, int amount) {
    Item item = itemRepository.findById(id)
            .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + id));
    item.addStock(amount);
    stockLogRepository.save(ItemStockLog.of(id, item.getName(), ItemStockType.IN, amount, getCurrentActorName()));
    return Map.of("quantity", item.getQuantity(), "minQuantity", item.getMinQuantity());
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 입고 처리 후 갱신된 수량과 최소수량을 함께 반환합니다.
> - **왜 이렇게 썼는지**: 기존 `restockItemAndGetQuantity()`는 수량만 반환해서 프론트에서 lowStock 판단을 할 수 없었습니다. minQuantity까지 같이 보내야 배지를 즉시 바꿀 수 있습니다.
> - **쉽게 말하면**: 창고에서 물건을 채운 뒤 "지금 몇 개야?"와 "최소 몇 개 있어야 해?"를 동시에 알려주는 것입니다.

### 2. 상태 배지 즉시 갱신 — item-list.mustache JS

```javascript
.then(function(data) {
  if (data.quantity !== undefined) {
    const row = form.closest('tr');
    const qtySpan = row.querySelector('td:nth-child(3) span');
    const lowStock = data.quantity < data.minQuantity;

    // 수량 텍스트 + 색상 갱신
    qtySpan.textContent = data.quantity + '개';
    qtySpan.className = 'font-bold ' + (lowStock ? 'text-red-600' : 'text-slate-800');

    // 상태 배지 갱신
    const statusCell = row.querySelector('td:nth-child(6)');
    if (lowStock) {
      statusCell.innerHTML = '<span class="...bg-red-100 text-red-800">재고 부족</span>';
    } else {
      statusCell.innerHTML = '<span class="...bg-green-100 text-green-800">정상</span>';
    }
    feather.replace();
    input.value = '';
  }
})
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 입고 응답을 받으면 같은 행의 수량 셀과 상태 배지 셀을 즉시 DOM으로 교체합니다.
> - **왜 이렇게 썼는지**: 페이지를 새로고침하지 않고 특정 셀만 innerHTML로 교체하면 사용자가 체감하는 반응 속도가 훨씬 빠릅니다. feather.replace()를 다시 호출해 아이콘도 렌더링합니다.
> - **쉽게 말하면**: 마트 재고 화면에서 물건을 채우면 화면 숫자와 상태 표시가 즉시 바뀌는 것입니다.

### 3. detail() 오류 방어 — AdminStaffController

```java
@GetMapping("/detail")
public String detail(
        @RequestParam("staffId") Long staffId,
        Authentication authentication,
        HttpServletRequest req,
        RedirectAttributes redirectAttributes) {
    try {
        req.setAttribute("model", adminStaffService.getEditForm(staffId, authentication.getName()));
        return "admin/staff-form";
    } catch (CustomException ex) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/admin/staff/list";
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 직원 상세 페이지 로드 시 예외가 발생하면 500 에러 대신 목록 화면으로 이동하며 오류 메시지를 표시합니다.
> - **왜 이렇게 썼는지**: 기존 코드에는 try-catch가 없어서 `getDoctorIfNeeded()` 등 내부 조회에서 예외가 발생하면 Spring 기본 에러 페이지로 넘어갔습니다.
> - **쉽게 말하면**: 문을 열었는데 안에 문제가 있으면 "오류가 있습니다"라고 안내하고 입구로 되돌려 보내는 것입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 입고 후 재고 부족 → 정상 전환 | quantity가 minQuantity 이상이 되도록 입고 | 상태 배지 즉시 "정상"으로 변경 |
| 입고 후 여전히 재고 부족 | quantity가 minQuantity 미만 유지 | 상태 배지 "재고 부족" 유지 |
| 수량 색상 갱신 | 재고부족 → 정상 | text-red-600 → text-slate-800 즉시 변경 |
| 비활성화 직원 수정 버튼 | 정상 케이스 | staff-form readOnly 화면 표시 |
| 비활성화 직원 예외 케이스 | getEditForm 내부 오류 발생 | 목록으로 redirect + 오류 메시지 |

---

## 완료 기준

- [x] 물품 입고 후 상태 배지 새로고침 없이 즉시 갱신
- [x] 수량 색상 lowStock 여부에 따라 즉시 변경
- [x] 비활성화 직원 detail() 예외 방어 처리
