# W3-7번째작업 Workflow — 원무과 물품 출고 기능

> **작성일**: 3W
> **목표**: 원무과 사이드바에 "물품 출고" 메뉴 추가 + 전용 페이지에서 카테고리 필터·초성 검색·AJAX 출고·ItemUsageLog 저장

---

## 전체 흐름

```
ItemUsageLog.reservationId nullable 변경
  → LayoutModelInterceptor isStaffItemUse 플래그 추가
  → sidebar-staff.mustache "물품 출고" 메뉴 추가
  → StaffItemController 신규 생성
  → staff/item-use.mustache 신규 생성
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 원무과 사이드바에 "물품 출고" 메뉴 추가 + 전용 출고 페이지 |
| 예약 연결 | 원무과 출고는 예약 없이 진행 → `reservationId=null` 저장 |
| 오늘 출고 내역 | 페이지 상단에 오늘 날짜 기준 출고 이력 표시 |
| 사이드바 활성화 | `LayoutModelInterceptor`에서 URL 기반으로 `isStaffItemUse` 플래그 처리 |
| 서비스 재사용 | `ItemManagerService.useItem()` 그대로 재사용 (reservationId=null 허용) |

---

## 실행 흐름

```
원무과 사이드바 "물품 출고" 클릭
  → GET /staff/item/use
  → StaffItemController: items, todayLogs 모델 → staff/item-use 반환

출고 버튼 클릭 (AJAX)
  → fetch POST /staff/item/use (itemId, amount, CSRF)
  → StaffItemController: useItem(id, amount, null) 호출
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
│ │ 붕대      │  3개  │ 09:30       │            │
│ └──────────┴──────┴──────────────┘            │
│                                               │
│ [전체] [의료소모품] [의료기기] [사무비품]        │
│ [검색: 물품명 입력...]                         │
│ 물품 카드 그리드 (4열) — 버튼명: "출고"         │
└───────────────────────────────────────────────┘
```

---

## 작업 목록

1. `ItemUsageLog.java` — `reservation_id` 컬럼 `nullable=true` 변경
2. `ItemUsageLogRepository.java` — 스태프 출고 이력 조회 메서드 추가
3. `ItemManagerService` — `useItem()` 항상 로그 저장, `getTodayStaffUsageLogs()` 추가
4. `LayoutModelInterceptor` — `isStaffItemUse` 플래그 추가
5. `sidebar-staff.mustache` — "물품 출고" 메뉴 추가
6. `StaffItemController.java` — 신규 생성 (GET/POST /staff/item/use)
7. `staff/item-use.mustache` — 신규 생성 (오늘 출고 내역 + 물품 출고 섹션)

---

## 작업 진행내용

- [x] ItemUsageLog `reservation_id` nullable 변경
- [x] ItemUsageLogRepository 스태프 출고 이력 조회 메서드 추가
- [x] ItemManagerService `useItem()` 항상 로그 저장으로 수정
- [x] ItemManagerService `getTodayStaffUsageLogs()` 추가
- [x] LayoutModelInterceptor `isStaffItemUse` 플래그 추가
- [x] sidebar-staff.mustache "물품 출고" 메뉴 추가
- [x] StaffItemController 신규 생성
- [x] staff/item-use.mustache 신규 생성

---

## 실행 흐름에 대한 코드

### 1. ItemUsageLog — reservationId nullable 변경

```java
// reservation_id 컬럼 nullable=true로 변경
@Column(name = "reservation_id", nullable = true)
private Long reservationId;
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 물품 사용 이력 테이블에서 `reservation_id` 컬럼이 null을 허용하도록 변경합니다.
> - **왜 이렇게 썼는지**: 원무과처럼 예약과 무관한 출고도 이력으로 남길 수 있게 됩니다. `nullable=true`로 설정하면 DB 컬럼이 NULL 값을 허용합니다.
> - **쉽게 말하면**: 예약 번호가 없어도 물품 사용 기록을 남길 수 있게 됩니다.

### 2. ItemUsageLogRepository — 스태프 출고 이력 조회

```java
// reservationId가 null인 오늘 날짜 로그 조회
List<ItemUsageLog> findByReservationIdIsNullAndUsedAtBetweenOrderByUsedAtDesc(
    LocalDateTime start, LocalDateTime end);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 ID가 없는 오늘 날짜의 물품 사용 이력을 최신순으로 조회하는 JPA 메서드를 추가합니다.
> - **왜 이렇게 썼는지**: Spring Data JPA는 메서드 이름을 분석해서 자동으로 SQL 쿼리를 생성합니다. `findBy...IsNull...Between...OrderBy...Desc`처럼 규칙에 맞게 작성하면 됩니다.
> - **쉽게 말하면**: "오늘 예약 없이 출고된 물품 기록을 최신 것부터 가져와주세요"를 메서드 이름으로 표현한 것입니다.

### 3. LayoutModelInterceptor — isStaffItemUse 플래그

```java
// /staff/item으로 시작하는 URL에서 사이드바 "물품 출고" 메뉴 활성화
mav.addObject("isStaffItemUse", path.startsWith("/staff/item"));
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 현재 URL이 `/staff/item`으로 시작하면 `isStaffItemUse`를 `true`로 설정합니다. 이 값이 뷰에 전달되어 사이드바 메뉴 강조에 사용됩니다.
> - **왜 이렇게 썼는지**: 인터셉터에서 처리하면 각 컨트롤러마다 `model.addAttribute`를 중복 작성하지 않아도 됩니다.
> - **쉽게 말하면**: "물품 출고" 페이지에 있을 때 사이드바 메뉴가 자동으로 강조되도록 설정하는 코드입니다.

### 4. StaffItemController — 신규 생성

```java
@Controller
@RequestMapping("/staff/item")
public class StaffItemController {

    @GetMapping("/use")
    public String itemUsePage(Model model) {
        model.addAttribute("items", itemManagerService.getItemList(null));
        model.addAttribute("todayLogs", itemManagerService.getTodayStaffUsageLogs());
        return "staff/item-use";
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
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 원무과 물품 출고 페이지용 컨트롤러를 새로 만듭니다. GET 요청 시 물품 목록과 오늘 출고 이력을 화면에 전달하고, POST AJAX 요청 시 재고를 차감합니다. `reservationId=null`로 예약 없는 출고임을 명시합니다.
> - **왜 이렇게 썼는지**: 원무과 출고는 특정 환자 예약과 연결되지 않으므로 `reservationId`를 null로 전달합니다.
> - **쉽게 말하면**: 원무과 물품 출고 페이지를 보여주고, 출고 버튼을 눌렀을 때 재고를 줄이는 서버 기능을 새로 만든 코드입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 물품 출고 페이지 접속 | `/staff/item/use` | 오늘 출고 내역 + 물품 그리드 표시 |
| 사이드바 활성화 | 출고 페이지 접속 | "물품 출고" 메뉴 강조 |
| AJAX 출고 성공 | 유효한 수량 입력 | 재고 즉시 갱신 |
| AJAX 출고 실패 | 재고 초과 수량 | alert 팝업 |
| 출고 이력 저장 | 출고 성공 후 | ItemUsageLog(reservationId=null) 저장 확인 |

---

## 완료 기준

- [x] 원무과 사이드바 "물품 출고" 메뉴 정상 표시 및 활성 상태 강조
- [x] 물품 출고 페이지: 카테고리 필터·초성 검색·AJAX 출고 정상 작동
- [x] 출고 시 `ItemUsageLog` 저장 (reservationId=null)
- [x] 오늘 출고 내역 실시간 반영 (페이지 재방문 시)
- [x] `config/`, `domain/` 수정 없음
