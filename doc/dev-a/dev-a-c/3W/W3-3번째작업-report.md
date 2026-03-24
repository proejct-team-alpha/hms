# W3-3번째작업 Report — 재고 부족 알림 입고 AJAX 처리

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. 알림 카드 div — data-min-quantity 속성 추가

- `<div class="p-4 bg-red-50 ...">` 에 `data-min-quantity="{{minQuantity}}"` 추가
- JS에서 `card.dataset.minQuantity`로 읽어 재고 부족 여부 판단

### 2. JS AJAX 핸들러 추가

- `form[action$="/item/restock"]` 으로 알림 카드 내 입고 폼 선택
- `e.preventDefault()`로 기본 submit 차단
- 검증: 빈값 · 문자 · 소수 · 음수 → `alert` 후 return
- 기존 `/item-manager/item/restock/ajax` 엔드포인트 재사용
- 응답 처리:
  - `quantity >= minQuantity` → 카드 `remove()` (알림 제거)
  - `quantity < minQuantity` → "현재 N개" 텍스트 갱신 + 입력 필드 초기화 (카드 유지)

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `templates/item-manager/dashboard.mustache` | 카드 div에 `data-min-quantity` 속성 추가, JS AJAX 핸들러 추가 |

---

## 결과

- 입고 버튼 클릭 시 페이지 새로고침 없음
- 재고 해소 시 해당 알림 카드만 DOM에서 제거
- 재고 여전히 부족 시 카드 유지 + 현재 수량 텍스트 실시간 갱신
- 컨트롤러/서비스 추가 변경 없음 (기존 AJAX 엔드포인트 재사용)

---

> **💡 입문자 설명**
>
> **`data-min-quantity` 속성 — HTML의 data 속성이란**
> - HTML 요소에 `data-`로 시작하는 속성을 붙이면 JavaScript에서 `element.dataset.minQuantity`로 읽을 수 있습니다. 서버가 렌더링할 때 `{{minQuantity}}`값을 HTML에 심어두고, 나중에 JavaScript가 꺼내 쓰는 방식입니다.
> - **왜 이렇게 쓰는지**: 서버에서 받은 데이터를 JavaScript 변수로 따로 선언하지 않고, HTML 요소 자체에 붙여두면 해당 요소와 데이터가 함께 관리됩니다. 여러 알림 카드가 있을 때 각 카드가 자신의 최소 수량을 독립적으로 기억합니다.
>
> **`card.remove()` — 왜 DOM에서 직접 제거하는지**
> - 재고가 해소되면 알림 카드를 숨기거나 지워야 합니다. 페이지를 새로고침하면 서버에서 재고 부족 목록을 다시 계산해 알림이 없어지지만, AJAX 방식에서는 직접 제거합니다.
> - `element.remove()`는 DOM(화면의 HTML 구조)에서 해당 요소를 즉시 삭제합니다. 사용자가 기다리지 않아도 됩니다.
>
> **기존 AJAX 엔드포인트 재사용 — 왜 새 엔드포인트를 만들지 않았는지**
> - W3-2에서 만든 `/item/restock/ajax`는 입고 후 새 수량을 반환합니다. 알림 카드의 입고 버튼도 같은 서버 로직을 필요로 하므로 동일한 엔드포인트를 재사용했습니다. 중복 코드 없이 여러 화면에서 같은 서버 기능을 공유합니다.
>
> **쉽게 말하면**: 재고 부족 경고 카드에서 입고 처리를 할 때, 재고가 충분해지면 그 경고 카드 자체를 화면에서 즉시 없애고, 아직 부족하면 카드를 유지하면서 현재 수량만 바꿉니다.
