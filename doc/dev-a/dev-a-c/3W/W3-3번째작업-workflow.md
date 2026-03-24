# W3-3번째작업 Workflow — 재고 부족 알림 입고 AJAX 처리

> **작성일**: 3W
> **목표**: 대시보드 재고 부족 알림에서 입고 버튼 클릭 시 페이지 새로고침 없이 DB 수량 갱신, 재고 해소 시 해당 알림 카드만 제거

---

## 전체 흐름

```
알림 카드 입고 폼 submit → JS 가로채기 → fetch POST /item/restock/ajax → 재고 수량 확인
  → quantity >= minQuantity → card.remove() (알림 제거)
  → quantity < minQuantity → 카드 수량 텍스트 갱신 (카드 유지)
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 대시보드 재고 부족 알림에서 입고 AJAX 처리 |
| 기존 방식 | W3-2와 동일한 AJAX 엔드포인트(`/item/restock/ajax`) 재사용 |
| 알림 제거 조건 | 입고 후 재고 >= 최소 기준치(`minQuantity`) |
| 데이터 전달 방식 | 카드 div에 `data-min-quantity` 속성으로 최소 기준치 저장 |
| 컨트롤러·서비스 변경 | 없음 (기존 AJAX 엔드포인트 재사용) |

---

## 실행 흐름

```
dashboard.mustache 로드
  → 알림 카드 div에 data-min-quantity="{{minQuantity}}" 저장
  → 사용자: 입고 폼 수량 입력 → 버튼 클릭
  → e.preventDefault() → 검증
  → fetch POST /item-manager/item/restock/ajax
  → 응답 quantity >= card.dataset.minQuantity → card.remove()
  → 응답 quantity < minQuantity → "현재 N개" 텍스트 갱신 + 입력 초기화
```

---

## UI Mockup

```
┌────────────────────────────────────────┐
│ 재고 부족 알림                          │
│ ┌──────────────────────────────────┐   │
│ │ 붕대 — 현재 3개 (기준: 10개)      │   │
│ │ [5   ] [입고]                    │   │
│ └──────────────────────────────────┘   │
│   ↑ 입고 후 재고 >= 10개 → 카드 사라짐  │
│   ↑ 입고 후 재고 < 10개 → 카드 유지    │
└────────────────────────────────────────┘
```

---

## 작업 목록

1. `dashboard.mustache` 알림 카드 div — `data-min-quantity` 속성 추가
2. `dashboard.mustache` — 입고 폼 AJAX submit 핸들러 추가
3. 재고 해소 시 카드 제거, 미해소 시 수량 텍스트 갱신 로직

---

## 작업 진행내용

- [x] 알림 카드 `div`에 `data-min-quantity="{{minQuantity}}"` 추가
- [x] `form[action$="/item/restock"]` 이벤트 리스너 등록
- [x] 검증 (빈값·문자·소수·음수) 처리
- [x] 기존 `/item/restock/ajax` 재사용 fetch 전송
- [x] `quantity >= minQuantity` → `card.remove()`
- [x] `quantity < minQuantity` → 수량 텍스트 갱신 + 입력 초기화

---

## 실행 흐름에 대한 코드

### 1. 알림 카드 div — data-min-quantity 속성 추가

```html
<!-- 재고 부족 알림 카드 div에 최소 기준치 저장 -->
<div class="p-4 bg-red-50 rounded-xl border border-red-100"
     data-min-quantity="{{minQuantity}}">
  <div class="flex items-center justify-between">
    <div>
      <p class="font-semibold text-red-800">{{itemName}}</p>
      <p class="text-sm text-red-600">현재 <span class="qty-text">{{quantity}}</span>개 (기준: {{minQuantity}}개)</p>
    </div>
    <form method="POST" action="/item-manager/item/restock">
      ...
    </form>
  </div>
</div>
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 재고 부족 알림 카드의 HTML 요소에 최소 재고 기준치(`minQuantity`)를 `data-min-quantity`라는 속성으로 저장합니다.
> - **왜 이렇게 썼는지**: `data-*` 속성은 HTML 요소에 커스텀 데이터를 넣는 방법입니다. JavaScript에서 `element.dataset.minQuantity`로 읽어서, 입고 후 재고가 기준치 이상이 되었는지 판단하는 데 사용합니다.
> - **쉽게 말하면**: "이 알림 카드는 몇 개 이상이면 사라져야 해"라는 기준값을 카드 자체에 붙여두는 것입니다.

### 2. JS AJAX 핸들러

```javascript
// 대시보드 재고 부족 알림 카드 입고 폼 AJAX 처리
document.querySelectorAll('form[action$="/item/restock"]').forEach(form => {
    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        const card = form.closest('[data-min-quantity]');
        const minQuantity = parseInt(card.dataset.minQuantity, 10);
        const input = form.querySelector('input[name="amount"]');
        const amount = Number(input.value);

        if (!input.value || isNaN(amount) || !Number.isInteger(amount) || amount < 1) {
            alert('올바른 수량을 입력해주세요.');
            return;
        }

        const params = new URLSearchParams({
            id: form.querySelector('input[name="id"]').value,
            amount: input.value,
            _csrf: form.querySelector('input[name="_csrf"]').value
        });

        const res = await fetch('/item-manager/item/restock/ajax', {
            method: 'POST', body: params
        });

        if (res.ok) {
            const data = await res.json();
            if (data.quantity >= minQuantity) {
                card.remove();  // 재고 해소 → 알림 카드 제거
            } else {
                card.querySelector('.qty-text').textContent = data.quantity;  // 수량 갱신
                input.value = '';
            }
        }
    });
});
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 대시보드의 재고 부족 알림 카드 내 입고 폼을 AJAX로 처리합니다. 입고 후 재고가 최소 기준치 이상이 되면 해당 알림 카드를 화면에서 제거하고, 아직 부족하면 카드를 그대로 유지합니다.
> - **왜 이렇게 썼는지**: `form[action$="/item/restock"]`은 action 속성이 "/item/restock"으로 끝나는 모든 폼을 선택합니다. 재고가 충분해졌을 때만 카드를 제거함으로써, 아직 부족한 알림은 계속 표시됩니다.
> - **쉽게 말하면**: 입고 버튼을 눌러서 재고가 기준 이상이 되면 경고 카드가 자동으로 사라지고, 아직 부족하면 그대로 남아있는 기능입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 재고 해소 입고 | 입고 후 quantity >= minQuantity | 알림 카드 즉시 제거 |
| 재고 미해소 입고 | 입고 후 quantity < minQuantity | 카드 유지, 수량 텍스트 갱신 |
| 음수·문자·소수 입력 | 유효하지 않은 값 | alert 표시, 요청 차단 |
| AJAX 엔드포인트 재사용 | 기존 `/item/restock/ajax` 호출 | 서버 변경 없이 정상 동작 |

---

## 완료 기준

- [x] 입고 버튼 클릭 시 페이지 새로고침 없음
- [x] 재고 해소 시 해당 알림 카드만 DOM에서 제거
- [x] 재고 여전히 부족 시 카드 유지 + 현재 수량 텍스트 실시간 갱신
- [x] 컨트롤러·서비스 추가 변경 없음 (기존 AJAX 엔드포인트 재사용)
