# W3-2번째작업 Workflow — 입고 버튼 AJAX 처리

> **작성일**: 3W
> **목표**: 입고 버튼 클릭 시 페이지 새로고침 없이 해당 행 재고 수량만 갱신

---

## 전체 흐름

```
입고 폼 submit → JS 가로채기 → fetch POST /item/restock/ajax → 재고 수량 JSON 응답 → 해당 행 span 갱신
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 입고 버튼 클릭 시 페이지 새로고침 없이 해당 행 재고 수량만 갱신 |
| 기존 방식 | 폼 제출 → 페이지 리다이렉트 (전체 새로고침) |
| 변경 방식 | AJAX fetch → JSON 응답 → 해당 행 span 수량 갱신 |
| 서비스 변경 | 기존 `restockItem()` 재사용 + `restockItemAndGetQuantity()` 신규 추가 |
| 입력 검증 | 음수·문자·소수 입력 시 alert 차단 |

---

## 실행 흐름

```
사용자: 입고 폼 amount 입력 → 입고 버튼 클릭
  → e.preventDefault() (기본 submit 차단)
  → 검증: 빈값·문자·소수·음수 → alert 후 return
  → fetch POST /item-manager/item/restock/ajax (URLSearchParams, CSRF 포함)
  → 서버: parseQuantity() → restockItemAndGetQuantity() → {"quantity": N}
  → JS: 해당 행 3번째 td span 텍스트 갱신 (N개) + 입력 필드 초기화
```

---

## UI Mockup

```
┌───────────────────────────────────────────────┐
│ 물품명  │ 현재 재고 │    입고                   │
├─────────┼───────────┼───────────────────────────┤
│ 붕대    │  25개     │ [5    ] [입고] ← AJAX 처리 │
│         │  ↑ 갱신됨  │                           │
└─────────┴───────────┴───────────────────────────┘
  입고 성공 → 페이지 이동 없이 "25개" → "30개" 즉시 변경
```

---

## 작업 목록

1. `ItemManagerService` — `restockItemAndGetQuantity()` 메서드 추가
2. `ItemManagerController` — `POST /item/restock/ajax` AJAX 엔드포인트 추가
3. `item-list.mustache` — 입고 폼 AJAX submit 이벤트 핸들러 추가

---

## 작업 진행내용

- [x] `restockItemAndGetQuantity()` 서비스 메서드 추가
- [x] `/item/restock/ajax` 컨트롤러 엔드포인트 추가
- [x] 입고 폼 submit 이벤트 리스너 등록
- [x] amount 값 검증 (빈값·문자·소수·음수)
- [x] fetch POST 전송 (CSRF 토큰 포함)
- [x] 성공 시 해당 행 span 갱신 + 입력 필드 초기화

---

## 실행 흐름에 대한 코드

### 1. ItemManagerService — restockItemAndGetQuantity()

```java
// 입고 처리 후 업데이트된 재고 수량 반환 (AJAX용)
@Transactional
public int restockItemAndGetQuantity(Long id, int amount) {
    Item item = itemRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("물품 없음"));
    item.addStock(amount);          // 재고 추가
    return item.getQuantity();      // 트랜잭션 내에서 새 수량 반환
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 입고 처리 후 업데이트된 재고 수량을 반환합니다. 기존 `restockItem()`은 반환값이 없었지만, AJAX 응답으로 새 수량을 화면에 즉시 반영하기 위해 `int`를 반환합니다.
> - **왜 이렇게 썼는지**: 기존 메서드를 수정하면 다른 호출 지점에 영향을 줄 수 있어 새 메서드를 추가했습니다. `@Transactional` 안에서 수량을 읽어야 업데이트된 값이 보장됩니다.
> - **쉽게 말하면**: 입고 처리 후 "이제 몇 개야?"를 바로 알려주는 기능을 추가한 것입니다.

### 2. ItemManagerController — /item/restock/ajax

```java
// POST /item/restock/ajax — AJAX 전용 입고 엔드포인트
@PostMapping("/item/restock/ajax")
@ResponseBody
public ResponseEntity<?> restockItemAjax(
        @RequestParam(name = "id") Long id,
        @RequestParam(name = "amount") String amountStr) {
    try {
        int amount = parseQuantity(amountStr);
        int newQty = itemService.restockItemAndGetQuantity(id, amount);
        return ResponseEntity.ok(Map.of("quantity", newQty));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 페이지 새로고침 없이 입고 처리를 할 수 있도록 AJAX 전용 POST 엔드포인트를 추가합니다. 성공 시 `{"quantity": N}` JSON, 실패 시 400 + `{"error": 메시지}`를 반환합니다.
> - **왜 이렇게 썼는지**: 기존 `/item/restock`은 폼 제출 후 페이지를 리다이렉트합니다. AJAX용 엔드포인트를 별도로 만들어 JSON 응답을 주면 화면 갱신 없이 처리할 수 있습니다.
> - **쉽게 말하면**: 입고 버튼을 눌렀을 때 페이지가 이동하지 않고, 서버가 JSON으로만 결과를 알려주는 전용 창구를 만든 코드입니다.

### 3. item-list.mustache — AJAX 핸들러

```javascript
// 모든 입고 폼에 submit 이벤트 리스너 등록
document.querySelectorAll('form[action$="/item/restock"]').forEach(form => {
    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        const input = form.querySelector('input[name="amount"]');
        const amount = Number(input.value);

        // 검증: 빈값·문자·소수·음수
        if (!input.value || isNaN(amount) || !Number.isInteger(amount) || amount < 1) {
            alert('올바른 수량을 입력해주세요.');
            return;
        }

        const params = new URLSearchParams({
            id: form.querySelector('input[name="id"]').value,
            amount: input.value,
            [form.querySelector('input[name="_csrf"]')?.name || '_csrf']:
                form.querySelector('input[name="_csrf"]')?.value || ''
        });

        const res = await fetch('/item-manager/item/restock/ajax', {
            method: 'POST',
            body: params
        });

        if (res.ok) {
            const data = await res.json();
            // 해당 행 3번째 td의 span 수량 갱신
            const row = form.closest('tr');
            row.querySelector('td:nth-child(3) span').textContent = data.quantity + '개';
            input.value = '';
        }
    });
});
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 페이지의 모든 입고 폼에 이벤트 리스너를 등록해서, 폼 제출 시 기본 동작(페이지 이동)을 막고 `fetch`로 서버에 AJAX 요청을 보냅니다. 성공하면 해당 행의 수량 표시를 갱신하고 입력 필드를 비웁니다.
> - **왜 이렇게 썼는지**: CSRF 토큰은 Spring Security가 POST 요청에 요구하는 보안 값으로, 폼의 hidden input에서 읽어서 함께 전송해야 합니다. `URLSearchParams`로 폼 데이터처럼 묶어서 보냅니다.
> - **쉽게 말하면**: 입고 버튼을 눌렀을 때 화면이 이동하지 않고, 서버에 데이터를 보내고 결과만 받아서 숫자만 바꿔주는 JavaScript 코드입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 정상 입고 | 유효한 수량 입력 | 해당 행 재고 수량 즉시 갱신, 페이지 이동 없음 |
| 음수 입력 | -1 입력 | alert 표시, 요청 차단 |
| 문자 입력 | "abc" 입력 | alert 표시, 요청 차단 |
| 소수 입력 | 1.5 입력 | alert 표시, 요청 차단 |
| 빈값 입력 | 빈 상태 제출 | alert 표시, 요청 차단 |
| 입력 필드 초기화 | 입고 성공 후 | 수량 입력 필드 빈값으로 초기화 |

---

## 완료 기준

- [x] 입고 버튼 클릭 시 페이지 새로고침 없음
- [x] 해당 행 재고 수량만 실시간 갱신
- [x] 입력 필드 자동 초기화
- [x] 음수·문자·소수 입력 시 alert 차단
- [x] 기존 `/item/restock` 엔드포인트 및 서비스 로직 변경 없음
