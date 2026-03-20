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
