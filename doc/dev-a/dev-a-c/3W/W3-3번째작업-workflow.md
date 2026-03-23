# W3-3번째작업 Workflow — 재고 부족 알림 입고 AJAX 처리

## 작업 개요

- **목표:** 대시보드 재고 부족 알림에서 입고 버튼 클릭 시 페이지 새로고침 없이 DB 수량 갱신, 재고 해소 시 해당 알림 카드만 제거
- **담당:** dev-a-c
- **날짜:** 2026-03-17

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `src/main/resources/templates/item-manager/dashboard.mustache` | 수정 (data 속성 추가 + JS AJAX 핸들러 추가) |

---

## 작업 목록

### 1. 알림 카드 div — data 속성 추가

**파일:** `dashboard.mustache`

```html
<!-- TODO: 카드 div에 data-min-quantity 추가 -->
<div class="p-4 bg-red-50 ..."
     data-min-quantity="{{minQuantity}}">
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 재고 부족 알림 카드의 HTML 요소에 최소 재고 기준치(`minQuantity`)를 `data-min-quantity`라는 속성으로 저장합니다. `{{minQuantity}}`는 서버에서 전달한 값이 이 자리에 채워집니다.
> - **왜 이렇게 썼는지**: `data-*` 속성은 HTML 요소에 커스텀 데이터를 넣는 방법입니다. JavaScript에서 `element.dataset.minQuantity`로 읽어서, 입고 후 재고가 기준치 이상이 되었는지 판단하는 데 사용합니다.
> - **쉽게 말하면**: "이 알림 카드는 몇 개 이상이면 사라져야 해"라는 기준값을 카드 자체에 붙여두는 것입니다.

### 2. JS AJAX 핸들러 추가

**파일:** `dashboard.mustache`

```javascript
// TODO: <script> 블록 추가 (feather.replace() 위)
// - form[action$="/item/restock"] 선택
// - 음수 / 문자 / 소수 → alert 후 return
// - fetch POST → /item-manager/item/restock/ajax (CSRF 포함)
// - 응답 quantity >= data-min-quantity → 카드 div.remove()
// - 응답 quantity < data-min-quantity → 아무 처리 없음
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 대시보드의 재고 부족 알림 카드 내 입고 폼을 AJAX로 처리합니다. 입고 후 재고가 최소 기준치 이상이 되면 해당 알림 카드를 화면에서 제거하고, 아직 부족하면 카드를 그대로 유지합니다.
> - **왜 이렇게 썼는지**: `form[action$="/item/restock"]`은 CSS 선택자로 action 속성이 "/item/restock"으로 끝나는 모든 폼을 선택합니다. 재고가 충분해졌을 때만 카드를 제거함으로써, 아직 부족한 알림은 계속 표시되도록 합니다.
> - **쉽게 말하면**: 입고 버튼을 눌러서 재고가 기준 이상이 되면 "재고 부족" 알림 카드가 알아서 사라지고, 아직 부족하면 그대로 남아있는 기능입니다.

---

## 상세 구현 계획

### 1단계: data 속성 추가

- 알림 카드 `<div class="p-4 bg-red-50 ...">` 에 `data-min-quantity="{{minQuantity}}"` 추가
- JS에서 `card.dataset.minQuantity`로 읽어 비교

### 2단계: JS 핸들러

- W3-2(item-list)와 동일한 패턴
- `fetch` 후 `data.quantity >= minQuantity` 이면 `card.remove()`
- 부족 상태 유지 시 아무 처리 없음 (카드 유지)

---

## 금지 사항 체크

- [x] `config/`, `domain/` 수정 없음
- [x] `admin/**` 수정 없음
- [x] 컨트롤러/서비스 변경 없음 (기존 `/item/restock/ajax` 재사용)
