# W3-7번째작업 Report — 원무과 물품 출고 기능

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. ItemUsageLog — reservationId nullable 변경

- `@Column(name = "reservation_id", nullable = false)` → `nullable = true`
- 예약(환자)에 연결되지 않는 출고(원무과 등)에서도 로그 저장 가능

### 2. ItemUsageLogRepository — 스태프 출고 이력 조회 메서드 추가

- `findByReservationIdIsNullAndUsedAtBetweenOrderByUsedAtDesc(LocalDateTime start, LocalDateTime end)`
- 오늘 날짜 기준 reservationId=null 로그를 최신순으로 조회

### 3. ItemManagerService — 수정

- `useItem()`: `if (reservationId != null)` 조건 제거 → 항상 `ItemUsageLog` 저장
- `getTodayStaffUsageLogs()`: 오늘 00:00~24:00, reservationId=null 로그 반환

### 4. LayoutModelInterceptor — isStaffItemUse 플래그 추가

- `mav.addObject("isStaffItemUse", path.startsWith("/staff/item"))`

### 5. sidebar-staff.mustache — "물품 출고" 메뉴 추가

- 내 정보 관리 위에 추가
- 아이콘: `package`, 활성 플래그: `isStaffItemUse`

### 6. StaffItemController — 신규 생성

- `GET /staff/item/use`: `items`, `todayLogs` 모델 → `staff/item-use` 반환
- `POST /staff/item/use` (`@ResponseBody`): amount 검증 → `useItem(id, amount, null)` 호출
  - 성공: `{"quantity": N}` / 재고 부족·오류: 400 + `{"error": "..."}`

### 7. staff/item-use.mustache — 신규 생성

- **오늘 출고 내역** (상단): 테이블 또는 "없습니다." 표시
- **물품 선택** (하단): 카테고리 필터 + 초성·영문 검색 + 4열 카드 그리드
- 버튼명 "출고" (의사·간호사는 "사용")
- AJAX: 출고 성공 시 재고 즉시 갱신, 재고 부족 시 `alert()`

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `item/log/ItemUsageLog.java` | `reservationId` nullable 허용 |
| `item/log/ItemUsageLogRepository.java` | 스태프 출고 이력 조회 메서드 추가 |
| `item/ItemManagerService.java` | `useItem()` 수정, `getTodayStaffUsageLogs()` 추가 |
| `common/interceptor/LayoutModelInterceptor.java` | `isStaffItemUse` 플래그 추가 |
| `templates/common/sidebar-staff.mustache` | "물품 출고" 메뉴 추가 |
| `staff/item/StaffItemController.java` | 신규 생성 |
| `templates/staff/item-use.mustache` | 신규 생성 |

---

## 결과

- 원무과 사이드바 "물품 출고" 메뉴 정상 표시 및 활성 상태 강조
- 물품 출고 페이지: 카테고리 필터·초성 검색·AJAX 출고 정상 작동
- 출고 시 `ItemUsageLog` 저장 (reservationId=null)
- 오늘 출고 내역 실시간 반영 (페이지 재방문 시)

---

> **💡 입문자 설명**
>
> **`reservationId` nullable 변경 — 왜 null을 허용하는지**
> - 이전에는 물품 사용이 반드시 특정 예약(환자)과 연결되어야 했습니다(`nullable=false`). 원무과 출고는 환자 예약 없이 일반 비품 출고가 가능해야 합니다. `nullable=true`로 바꾸면 `reservationId=null`로 저장해 "예약 없는 출고"를 표현합니다.
> - **왜 별도 테이블을 안 만드는지**: 예약 연결 유무만 다를 뿐 나머지 로직(물품명, 수량, 시각)이 동일하므로 같은 테이블에서 null로 구분하는 것이 단순합니다.
>
> **`useItem()` 수정 — 왜 항상 로그를 저장하도록 바꿨는지**
> - 이전에는 `if (reservationId != null)`일 때만 로그를 저장했습니다. 원무과 출고도 기록이 필요하므로 조건을 제거해 항상 저장합니다. `reservationId`가 null이면 "예약 없는 출고"로 기록됩니다.
>
> **`LayoutModelInterceptor`에 플래그를 추가하는 이유**
> - 사이드바는 현재 페이지를 강조 표시합니다. `/staff/item` URL로 시작하는 페이지에 있으면 "물품 출고" 메뉴가 활성화됩니다. 이 판단을 컨트롤러마다 반복 작성하는 대신, 모든 요청을 가로채는 인터셉터에서 한 번만 처리합니다.
>
> **쉽게 말하면**: 원무과 직원이 환자 진료와 무관하게 사무용품 등을 직접 출고 처리하고 기록할 수 있는 기능을 추가했습니다. 의사·간호사의 "사용" 버튼과 같은 원리이지만, 예약 번호 없이도 출고할 수 있습니다.
