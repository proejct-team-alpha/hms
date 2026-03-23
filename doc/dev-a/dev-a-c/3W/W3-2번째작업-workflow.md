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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 페이지 새로고침 없이 입고 처리를 할 수 있도록 AJAX 전용 POST 엔드포인트를 추가합니다. `@ResponseBody`는 이 메서드가 HTML 페이지가 아닌 JSON 데이터를 응답한다는 의미이고, `ResponseEntity<?>`는 HTTP 상태 코드(200, 400 등)와 함께 응답을 전송할 수 있는 객체입니다.
> - **왜 이렇게 썼는지**: 기존 `/item/restock` 엔드포인트는 폼 제출 후 페이지를 리다이렉트(redirect)합니다. AJAX용 엔드포인트를 별도로 만들어서 JSON 응답을 주면, 화면 갱신 없이 자바스크립트로 결과를 처리할 수 있습니다.
> - **쉽게 말하면**: 입고 버튼을 눌렀을 때 페이지가 새로고침되지 않도록, 서버가 JSON으로만 결과를 알려주는 전용 창구를 만드는 코드입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 페이지의 모든 입고 폼에 JavaScript 이벤트 리스너를 등록해서, 폼 제출 시 기본 동작(페이지 이동)을 막고 `fetch`로 서버에 AJAX 요청을 보냅니다. 성공하면 해당 행의 수량 표시를 갱신하고 입력 필드를 비웁니다.
> - **왜 이렇게 썼는지**: `fetch`는 최신 JavaScript에서 서버와 비동기로 통신하는 방법입니다. CSRF 토큰은 악의적인 요청을 방어하기 위해 Spring Security가 요구하는 보안 값으로, 폼의 hidden input에서 읽어서 함께 전송해야 합니다.
> - **쉽게 말하면**: 입고 버튼을 눌렀을 때 화면이 이동하지 않고, 서버에 몰래 데이터를 보내고 결과만 받아서 숫자만 바꿔주는 JavaScript 코드입니다.

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
