# W3-2번째작업 Report — 입고 버튼 AJAX 처리

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. 서비스 — `restockItemAndGetQuantity()` 추가

- 기존 `restockItem()`과 동일하게 `item.addStock(amount)` 호출
- 트랜잭션 내에서 업데이트된 수량(`item.getQuantity()`)을 반환

### 2. 컨트롤러 — `/item/restock/ajax` 엔드포인트 추가

- `@PostMapping("/item/restock/ajax")` + `@ResponseBody`
- 기존 `parseQuantity()` 재사용
- 성공: `{"quantity": N}` JSON 반환
- 실패: 400 + `{"error": 메시지}` JSON 반환

### 3. mustache — JS AJAX 핸들러 추가

- `form[action$="/item/restock"]` 셀렉터로 입고 폼 선택
- `e.preventDefault()`로 기본 submit 차단
- 검증: 빈값 · 문자 · 소수 · 음수 → `alert` 후 return
- `URLSearchParams`로 id + amount + CSRF 토큰 조립
- `fetch` POST → `/item-manager/item/restock/ajax`
- 성공 시: 해당 행 3번째 td span 수량 갱신 + 입력 필드 초기화

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `ItemManagerService.java` | `restockItemAndGetQuantity()` 메서드 추가 |
| `ItemManagerController.java` | `/item/restock/ajax` 엔드포인트 추가, `ResponseEntity`/`Map` import 추가 |
| `templates/item-manager/item-list.mustache` | JS AJAX 핸들러 추가 |

---

## 결과

- 입고 버튼 클릭 시 페이지 새로고침 없음
- 해당 행 재고 수량만 실시간 갱신
- 입력 필드 자동 초기화
- 음수 · 문자 · 소수 입력 시 alert 차단
- 기존 `/item/restock` 엔드포인트 및 서비스 로직 변경 없음

---

> **💡 입문자 설명**
>
> **AJAX란 무엇인지 — 왜 새로고침 없이 동작하는지**
> - 기존 폼 제출은 페이지 전체를 서버에서 다시 받아옵니다. AJAX(Asynchronous JavaScript And XML)는 JavaScript가 백그라운드에서 서버에 요청을 보내고 응답을 받아 필요한 부분만 화면을 업데이트합니다.
> - 입고 버튼을 누를 때 전체 물품 목록 페이지를 다시 로드하면 느리고 사용자 경험이 나쁩니다. AJAX로 재고 수량만 바꾸면 빠르고 자연스럽습니다.
>
> **`restockItemAndGetQuantity()` — 기존 `restockItem()`과 왜 다른 메서드를 만들었는지**
> - 기존 `restockItem()`은 입고 후 아무 값도 반환하지 않았습니다. AJAX 응답으로 새 재고 수량을 화면에 즉시 반영하려면 업데이트된 수량을 반환해야 합니다. 기존 메서드를 수정하면 다른 호출 지점에 영향을 줄 수 있어 새 메서드를 추가했습니다.
>
> **`@ResponseBody` — 왜 붙이는지**
> - Spring MVC에서 컨트롤러가 뷰 이름(mustache 파일 경로) 대신 JSON 데이터를 직접 반환할 때 `@ResponseBody`를 붙입니다. 없으면 Spring이 `{"quantity": N}`을 뷰 이름으로 해석해 오류가 납니다.
>
> **`URLSearchParams`로 CSRF 토큰을 함께 보내는 이유**
> - Spring Security는 POST 요청에 CSRF 토큰을 요구합니다. AJAX 요청도 예외가 없습니다. `URLSearchParams`로 폼 데이터처럼 `id`, `amount`, CSRF 토큰을 묶어 전송하면 서버가 보안 검증을 통과시킵니다.
>
> **쉽게 말하면**: 입고 버튼을 누르면 "페이지 전체를 새로 받아오는" 대신, JavaScript가 몰래 서버에 전화해서 "수량이 얼마야?"를 물어보고 그 숫자만 화면에서 바꿔주는 방식입니다.
