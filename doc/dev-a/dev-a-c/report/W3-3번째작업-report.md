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
