# W3-2번째작업 Workflow — 입고 버튼 AJAX 처리

## 작업 개요

- **목표:** 입고 버튼 클릭 시 페이지 새로고침 없이 해당 행 재고 수량만 갱신
- **담당:** dev-a-c
- **날짜:** 2026-03-17

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `src/main/java/com/smartclinic/hms/item/ItemManagerController.java` | 수정 (AJAX 엔드포인트 추가) |
| `src/main/resources/templates/item-manager/item-list.mustache` | 수정 (JS AJAX 핸들러 추가) |

---

## 작업 목록

### 1. 컨트롤러 — AJAX 엔드포인트 추가

**파일:** `ItemManagerController.java`

```java
// TODO: POST /item/restock/ajax 엔드포인트 추가
// - @RequestParam id, amountStr 받기
// - 기존 parseQuantity() + itemService.restockItem() 재사용
// - 성공 시 {"quantity": 새재고수량} JSON 반환
// - 실패 시 400 + {"error": 메시지} JSON 반환
@PostMapping("/item/restock/ajax")
@ResponseBody
public ResponseEntity<?> restockItemAjax(...) { ... }
```

### 2. mustache — 입고 폼 AJAX 처리 JS 추가

**파일:** `item-list.mustache`

```html
<!-- TODO: <script> 블록 추가 (feather.replace() 위) -->
<!--
  - 모든 입고 form에 submit 이벤트 리스너 등록
  - amount 값 검증: 음수 또는 문자면 alert 후 return
  - fetch로 /item-manager/item/restock/ajax POST 전송 (CSRF 포함)
  - 성공 시: 해당 행의 수량 span 텍스트를 새 값으로 업데이트, 입력 필드 초기화
  - 실패 시: 별도 처리 없음
-->
```

---

## 상세 구현 계획

### 1단계: 컨트롤러 AJAX 엔드포인트

- 기존 `restockItem()` 메서드 바로 아래에 추가
- `parseQuantity()` 그대로 재사용
- `itemService.restockItem(id, amount)` 후 업데이트된 수량 조회하여 반환
- 반환 형식: `{"quantity": N}`

### 2단계: JS 핸들러

- `document.querySelectorAll('form[action*="restock"]')`로 입고 폼 선택
- `e.preventDefault()` 후 입력값 검증
  - `isNaN(amount)` 또는 `amount < 0` → `alert("올바른 수량을 입력해주세요.")` 후 return
- `fetch` POST: `Content-Type: application/x-www-form-urlencoded`
- CSRF 토큰: 폼 내 hidden input에서 읽기
- 응답 JSON의 `quantity`로 해당 행 `<span>` 텍스트 갱신 (`N개`)
- 입력 필드 값 초기화 (`input.value = ''`)

---

## 금지 사항 체크

- [x] `config/`, `domain/` 수정 없음
- [x] `admin/**` 수정 없음
- [x] 기존 `/item/restock` 엔드포인트 변경 없음
- [x] 서비스 로직 변경 없음 (재사용만)
