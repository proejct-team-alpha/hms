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
