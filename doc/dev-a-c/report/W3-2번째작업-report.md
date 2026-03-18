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
