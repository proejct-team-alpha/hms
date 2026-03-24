# W3-8번째작업 Workflow — 관리자 물품 출고 기능

> **작성일**: 3W
> **목표**: 관리자 사이드바에 "물품 출고" 메뉴 추가 + 전용 페이지에서 카테고리 필터·초성 검색·AJAX 출고·ItemUsageLog 저장

---

## 전체 흐름

```
sidebar-admin.mustache "물품 출고" 메뉴 추가
  → AdminItemController GET/POST /admin/item/use 추가
  → admin/item-use.mustache 신규 생성
  → AdminReservationController @RequestParam name 명시 (버그 수정)
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 관리자 사이드바에 "물품 출고" 메뉴 추가 + 전용 출고 페이지 |
| 방식 | staff(W3-7)와 동일 구조 — URL과 사이드바만 다름 |
| 예약 연결 | 관리자 출고도 예약 없이 진행 → `reservationId=null` 저장 |
| 사이드바 활성화 | `isAdminItemUse` 플래그를 컨트롤러 모델에서 직접 설정 |
| 서비스 재사용 | `ItemManagerService.useItem()` 그대로 재사용 |
| 추가 버그 수정 | `AdminReservationController` `@RequestParam` name 미명시 → 컴파일 환경 따라 파라미터 추론 실패 |

---

## 실행 흐름

```
관리자 사이드바 "물품 출고" 클릭
  → GET /admin/item/use
  → AdminItemController: items, todayLogs, isAdminItemUse=true 모델 → admin/item-use 반환

출고 버튼 클릭 (AJAX)
  → fetch POST /admin/item/use (itemId, amount, CSRF)
  → AdminItemController: useItem(id, amount, null) 호출
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
│ │ 장갑      │  5개  │ 10:15       │            │
│ └──────────┴──────┴──────────────┘            │
│                                               │
│ [전체] [의료소모품] [의료기기] [사무비품]        │
│ [검색: 물품명 입력...]                         │
│ 물품 카드 그리드 (4열) — 버튼명: "출고"         │
└───────────────────────────────────────────────┘
```

---

## 작업 목록

1. `sidebar-admin.mustache` — "물품 등록"과 "입출고 내역" 사이에 "물품 출고" 메뉴 추가 (아이콘: `package`, 활성 플래그: `isAdminItemUse`)
2. `AdminItemController.java` — `ItemManagerService` 주입, `GET /admin/item/use` (items·todayLogs·isAdminItemUse 모델), `POST /admin/item/use` AJAX 엔드포인트 추가
3. `admin/item-use.mustache` — 신규 생성 (`staff/item-use.mustache` 구조 동일, sidebar-admin 사용)
4. `AdminReservationController.java` — `@RequestParam(name = "page")` 등 name 명시 (버그 수정)

---

## 작업 진행내용

- [x] sidebar-admin.mustache "물품 출고" 메뉴 추가
- [x] AdminItemController ItemManagerService 주입
- [x] GET /admin/item/use 엔드포인트 추가
- [x] POST /admin/item/use AJAX 엔드포인트 추가
- [x] admin/item-use.mustache 신규 생성
- [x] AdminReservationController @RequestParam name 명시 버그 수정

---

## 실행 흐름에 대한 코드

### 1. AdminItemController — GET/POST 엔드포인트

```java
@GetMapping("/use")
public String itemUsePage(Model model) {
    model.addAttribute("items", itemManagerService.getItemList(null));
    model.addAttribute("todayLogs", itemManagerService.getTodayStaffUsageLogs());
    model.addAttribute("isAdminItemUse", true);  // 사이드바 활성화
    return "admin/item-use";
}

@PostMapping("/use")
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
> - **이 코드가 하는 일**: 관리자 물품 출고 페이지를 보여주고(GET), 출고 버튼 클릭 시 재고를 차감합니다(POST). `isAdminItemUse=true`를 모델에 넣어 사이드바 "물품 출고" 메뉴가 강조되도록 합니다.
> - **왜 이렇게 썼는지**: 원무과(W3-7)는 `LayoutModelInterceptor`에서 URL 기반으로 플래그를 설정했지만, 관리자는 컨트롤러에서 직접 `model.addAttribute`로 설정합니다. 두 방식 모두 사이드바 활성화 목적은 동일합니다.
> - **쉽게 말하면**: 관리자용 출고 창구를 추가한 코드입니다. 출고 로직은 원무과와 동일하고, 접근 URL과 사이드바만 다릅니다.

### 2. AdminReservationController — @RequestParam name 명시 (버그 수정)

```java
// 수정 전
@RequestParam(defaultValue = "1") int page

// 수정 후
@RequestParam(name = "page", defaultValue = "1") int page
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `@RequestParam`에 `name`을 명시해서 어떤 컴파일 환경에서도 파라미터 이름을 정확히 인식하게 합니다.
> - **왜 이렇게 썼는지**: Java는 기본적으로 컴파일 시 파라미터 이름 정보를 포함하지 않습니다. `-parameters` 플래그가 없는 환경에서는 `int page` 같은 원시 타입 파라미터 이름을 Spring이 추론하지 못해 오류가 발생합니다. `name = "page"`를 명시하면 어떤 환경에서도 안정적으로 동작합니다.
> - **쉽게 말하면**: 개발 환경과 빌드 환경의 컴파일 설정 차이에서 발생하는 버그를 name 명시로 방지합니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 물품 출고 페이지 접속 | `/admin/item/use` | 오늘 출고 내역 + 물품 그리드 표시 |
| 사이드바 활성화 | 출고 페이지 접속 | "물품 출고" 메뉴 강조 |
| AJAX 출고 성공 | 유효한 수량 입력 | 재고 즉시 갱신 |
| AJAX 출고 실패 | 재고 초과 수량 | alert 팝업 |
| 출고 이력 저장 | 출고 성공 후 | ItemUsageLog(reservationId=null) 저장 확인 |
| 예약 목록 페이징 | 페이지 이동 | 정상 동작 (버그 수정 확인) |

---

## 완료 기준

- [x] 관리자 사이드바 "물품 출고" 메뉴 정상 표시 및 활성 상태 강조
- [x] 물품 출고 페이지: 카테고리 필터·초성 검색·AJAX 출고 정상 작동
- [x] 출고 시 `ItemUsageLog` 저장 (reservationId=null)
- [x] 오늘 출고 내역 실시간 반영 (페이지 재방문 시)
- [x] `AdminReservationController` @RequestParam name 명시 버그 수정
- [x] `config/`, `domain/` 수정 없음
