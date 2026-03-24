# W3-7번째작업 리포트 - 원무과 물품 출고 기능

## 작업 개요
- **작업명**: 원무과 사이드바에 "물품 출고" 메뉴 추가, 전용 출고 페이지 구성 (카테고리 필터·초성 검색·AJAX 출고·오늘 출고 내역)
- **수정 파일**: `item/log/ItemUsageLog.java`, `item/log/ItemUsageLogRepository.java`, `item/ItemManagerService.java`, `common/interceptor/LayoutModelInterceptor.java`, `templates/common/sidebar-staff.mustache`, `staff/item/StaffItemController.java`(신규), `templates/staff/item-use.mustache`(신규)

## 작업 내용

### 1. ItemUsageLog — reservationId nullable 변경

`@Column(name = "reservation_id", nullable = false)` → `nullable = true`. 예약(환자)에 연결되지 않는 출고(원무과 등)에서도 로그 저장 가능.

### 2. ItemUsageLogRepository — 스태프 출고 이력 조회 메서드 추가

```java
List<ItemUsageLog> findByReservationIdIsNullAndUsedAtBetweenOrderByUsedAtDesc(
    LocalDateTime start, LocalDateTime end);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 ID가 없는 오늘 날짜의 물품 사용 이력을 최신순으로 조회합니다.
> - **왜 이렇게 썼는지**: Spring Data JPA 메서드 이름 규칙으로 `WHERE reservation_id IS NULL AND used_at BETWEEN ... ORDER BY used_at DESC` SQL을 자동 생성합니다.
> - **쉽게 말하면**: "오늘 예약 없이 출고된 기록을 최신 순으로 가져와주세요"를 메서드 이름으로 표현한 것입니다.

### 3. ItemManagerService — 수정

- `useItem()`: `if (reservationId != null)` 조건 제거 → 항상 `ItemUsageLog` 저장
- `getTodayStaffUsageLogs()`: 오늘 00:00~24:00, reservationId=null 로그 반환

### 4. LayoutModelInterceptor — isStaffItemUse 플래그 추가

`mav.addObject("isStaffItemUse", path.startsWith("/staff/item"))`.

### 5. sidebar-staff.mustache — "물품 출고" 메뉴 추가

내 정보 관리 위에 추가. 아이콘: `package`, 활성 플래그: `isStaffItemUse`.

### 6. StaffItemController — 신규 생성

- `GET /staff/item/use`: `items`, `todayLogs` 모델 → `staff/item-use` 반환
- `POST /staff/item/use` (`@ResponseBody`): amount 검증 → `useItem(id, amount, null)` 호출
  - 성공: `{"quantity": N}` / 재고 부족·오류: 400 + `{"error": "..."}`

### 7. staff/item-use.mustache — 신규 생성

- **오늘 출고 내역** (상단): 테이블 또는 "없습니다." 표시
- **물품 선택** (하단): 카테고리 필터 + 초성·영문 검색 + 4열 카드 그리드
- 버튼명 "출고" (의사·간호사는 "사용")
- AJAX: 출고 성공 시 재고 즉시 갱신, 재고 부족 시 `alert()`

## 테스트 결과

| 항목 | 결과 |
|------|------|
| 사이드바 "물품 출고" 메뉴 표시 및 활성화 | ✅ |
| 물품 출고 페이지 접속 | ✅ |
| 카테고리 필터·초성 검색 | ✅ |
| AJAX 출고 성공 — 재고 즉시 갱신 | ✅ |
| 출고 시 ItemUsageLog 저장 (reservationId=null) | ✅ |
| 오늘 출고 내역 표시 | ✅ |

## 특이사항
- `reservationId` nullable 변경: 예약 없는 출고도 동일 테이블에서 null로 구분 저장
- `useItem()` 수정: 기존 `if (reservationId != null)` 조건 제거 → 항상 로그 저장
- 원무과 출고 버튼명 "출고", 의사·간호사는 "사용" — 역할에 따른 UX 차별화
