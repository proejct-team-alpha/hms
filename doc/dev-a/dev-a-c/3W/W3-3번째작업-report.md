# W3-3번째작업 리포트 - 재고 부족 알림 입고 AJAX 처리

## 작업 개요
- **작업명**: 대시보드 재고 부족 알림 카드에서 입고 AJAX 처리, 재고 해소 시 카드 자동 제거
- **수정 파일**: `templates/item-manager/dashboard.mustache`

## 작업 내용

### 1. 알림 카드 div — data-min-quantity 속성 추가

`<div class="p-4 bg-red-50 ...">` 에 `data-min-quantity="{{minQuantity}}"` 추가. JS에서 `card.dataset.minQuantity`로 읽어 재고 부족 여부 판단.

```html
<div class="p-4 bg-red-50 rounded-xl border border-red-100"
     data-min-quantity="{{minQuantity}}">
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 알림 카드에 최소 재고 기준치를 HTML 속성으로 저장합니다.
> - **왜 이렇게 썼는지**: 서버가 렌더링할 때 기준치를 HTML에 심어두고, 나중에 JavaScript가 꺼내 써서 카드 제거 여부를 판단합니다.
> - **쉽게 말하면**: 카드 자체에 "몇 개 이상이면 사라져"라는 기준을 붙여둔 것입니다.

### 2. JS AJAX 핸들러 추가

- `form[action$="/item/restock"]`으로 알림 카드 내 입고 폼 선택
- `e.preventDefault()`로 기본 submit 차단
- 검증: 빈값·문자·소수·음수 → `alert` 후 return
- 기존 `/item-manager/item/restock/ajax` 엔드포인트 재사용
- 응답 처리:
  - `quantity >= minQuantity` → `card.remove()` (알림 제거)
  - `quantity < minQuantity` → "현재 N개" 텍스트 갱신 + 입력 필드 초기화 (카드 유지)

## 테스트 결과

| 항목 | 결과 |
|------|------|
| 재고 해소 입고 — 알림 카드 제거 | ✅ |
| 재고 미해소 입고 — 카드 유지 + 수량 갱신 | ✅ |
| 음수·문자·소수 입력 — alert 차단 | ✅ |
| 컨트롤러·서비스 추가 변경 없음 | ✅ |

## 특이사항
- 기존 W3-2에서 만든 `/item/restock/ajax` 엔드포인트를 그대로 재사용하므로 서버 변경 없음
- `card.remove()`는 DOM에서 해당 요소를 즉시 삭제함 — 페이지 새로고침 없이 알림이 사라짐
