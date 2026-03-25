# W3-9번째작업 Workflow — 물품 담당자 물품 출고 기능

> **작성일**: 3W
> **목표**: 물품 담당자 사이드바에 "물품 출고" 메뉴 추가 + 전용 페이지에서 카테고리 필터·초성 검색·AJAX 출고·ItemUsageLog 저장

---

## 전체 흐름

```
LayoutModelInterceptor isItemUse 플래그 추가
  → sidebar-item-manager.mustache "물품 출고" 메뉴 추가
  → ItemManagerController GET/POST /item-manager/item-use 추가
  → item-manager/item-use.mustache 신규 생성
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 물품 담당자 사이드바에 "물품 출고" 메뉴 추가 + 전용 출고 페이지 |
| 방식 | staff(W3-7)/admin(W3-8)과 동일 구조 |
| 예약 연결 | 물품 담당자 출고도 예약 없이 진행 → `reservationId=null` 저장 |
| 사이드바 활성화 | `LayoutModelInterceptor`에서 URL 기반으로 `isItemUse` 플래그 처리 |
| 서비스 재사용 | `ItemManagerService.useItem()`, `getTodayStaffUsageLogs()` 그대로 재사용 |

---

## 실행 흐름

```
물품 담당자 사이드바 "물품 출고" 클릭
  → GET /item-manager/item-use
  → ItemManagerController: items, todayLogs 모델 → item-manager/item-use 반환

출고 버튼 클릭 (AJAX)
  → fetch POST /item-manager/item-use (itemId, amount, CSRF)
  → ItemManagerController: useItem(id, amount, null) 호출
  → ItemManagerService: 재고 차감 + ItemUsageLog(reservationId=null) 저장
  → 성공: {"quantity": N} / 실패: 400 + {"error": "..."}
```

---

## UI Mockup

```
┌───────────────────────────────────────────────┐
│ [사이드바] 물품 출고 ←활성                     │
│                                               │
│ 오늘 출고 내역                                 │
│ ┌──────────┬──────┬──────────────┐            │
│ │ 물품명    │ 수량  │    일시      │            │
│ │ 마스크    │ 10개  │ 11:00       │            │
│ └──────────┴──────┴──────────────┘            │
│                                               │
│ [전체] [의료소모품] [의료기기] [사무비품]        │
│ [검색: 물품명 입력...]                         │
│ 물품 카드 그리드 (4열) — 버튼명: "출고"         │
└───────────────────────────────────────────────┘
```

---

## 작업 목록

1. `LayoutModelInterceptor.java` — `isItemUse` 플래그 추가 (`path.startsWith("/item-manager/item-use")`)
2. `sidebar-item-manager.mustache` — "물품 등록"과 "물품 입출고 내역" 사이에 "물품 출고" 메뉴 추가 (아이콘: `package`, 활성 플래그: `isItemUse`)
3. `ItemManagerController.java` — `GET /item-manager/item-use` (items·todayLogs 모델), `POST /item-manager/item-use` AJAX 엔드포인트 추가
4. `item-manager/item-use.mustache` — 신규 생성 (`{{> common/sidebar-item-manager}}` 사용)

---

## 작업 진행내용

- [x] LayoutModelInterceptor `isItemUse` 플래그 추가
- [x] sidebar-item-manager.mustache "물품 출고" 메뉴 추가
- [x] GET /item-manager/item-use 엔드포인트 추가
- [x] POST /item-manager/item-use AJAX 엔드포인트 추가
- [x] item-manager/item-use.mustache 신규 생성

---

## 실행 흐름에 대한 코드

### 1. LayoutModelInterceptor — isItemUse 플래그 추가

```java
// /item-manager/item-use로 시작하는 URL에서 사이드바 "물품 출고" 메뉴 활성화
mav.addObject("isItemUse", path.startsWith("/item-manager/item-use"));
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 현재 URL이 `/item-manager/item-use`로 시작하면 `isItemUse`를 `true`로 설정합니다.
> - **왜 이렇게 썼는지**: 인터셉터에서 처리하면 컨트롤러마다 `model.addAttribute`를 추가하지 않아도 됩니다. 관리자(W3-8)는 컨트롤러에서 직접 설정했지만, 물품 담당자는 기존 인터셉터 방식을 따릅니다.
> - **쉽게 말하면**: "물품 출고" 페이지에 있을 때 사이드바 메뉴가 자동으로 강조되도록 설정하는 코드입니다.

### 2. ItemManagerController — GET/POST 엔드포인트

```java
@GetMapping("/item-use")
public String itemUsePage(Model model) {
    model.addAttribute("items", itemManagerService.getItemList(null));
    model.addAttribute("todayLogs", itemManagerService.getTodayStaffUsageLogs());
    return "item-manager/item-use";
}

@PostMapping("/item-use")
@ResponseBody
public ResponseEntity<?> useItem(
        @RequestParam(name = "id") Long id,
        @RequestParam(name = "amount") String amountStr) {
    try {
        int amount = Integer.parseInt(amountStr.trim());
        int newQty = itemManagerService.useItem(id, amount, null);  // reservationId=null
        return ResponseEntity.ok(Map.of("quantity", newQty));
    } catch (IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 물품 담당자 물품 출고 페이지를 보여주고(GET), 출고 버튼 클릭 시 재고를 차감합니다(POST AJAX). `reservationId=null`로 예약 없는 출고임을 명시합니다.
> - **왜 이렇게 썼는지**: 물품 담당자는 재고 관리(입고·목록·내역)가 주업무입니다. 직접 출고도 할 수 있어야 하므로 동일한 `ItemManagerService.useItem()`을 재사용합니다. 역할마다 다른 URL이 필요한 이유는 Spring Security가 URL 기반으로 접근 권한을 제어하기 때문입니다.
> - **쉽게 말하면**: 원무과·관리자에 이어 물품 담당자에게도 출고 기능을 추가한 코드입니다. 같은 기능이지만 역할(권한)이 달라 별도 URL과 컨트롤러 메서드가 필요합니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 물품 출고 페이지 접속 | `/item-manager/item-use` | 오늘 출고 내역 + 물품 그리드 표시 |
| 사이드바 활성화 | 출고 페이지 접속 | "물품 출고" 메뉴 강조 |
| AJAX 출고 성공 | 유효한 수량 입력 | 재고 즉시 갱신 |
| AJAX 출고 실패 | 재고 초과 수량 | alert 팝업 |
| 출고 이력 저장 | 출고 성공 후 | ItemUsageLog(reservationId=null) 저장 확인 |

---

## 완료 기준

- [x] 물품 담당자 사이드바 "물품 출고" 메뉴 정상 표시 및 활성 상태 강조
- [x] 물품 출고 페이지: 카테고리 필터·초성 검색·AJAX 출고 정상 작동
- [x] 출고 시 `ItemUsageLog` 저장 (reservationId=null)
- [x] 오늘 출고 내역 실시간 반영 (페이지 재방문 시)
- [x] `config/`, `domain/` 수정 없음
