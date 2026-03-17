# W3-8번째작업 Report — 관리자 물품 출고 기능

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. sidebar-admin.mustache — "물품 출고" 메뉴 추가

- "물품 등록"과 "입출고 내역" 사이에 추가
- 아이콘: `package`, 활성 플래그: `isAdminItemUse`

### 2. AdminItemController — 출고 엔드포인트 추가

- `ItemManagerService` 필드 주입 추가
- `GET /admin/item/use`: `items`, `todayLogs` 모델 → `admin/item-use` 반환
  - `isAdminItemUse = true` 모델에 추가
- `POST /admin/item/use` (`@ResponseBody`): amount 검증 → `useItem(id, amount, null)` 호출
  - 성공: `{"quantity": N}` / 재고 부족·오류: 400 + `{"error": "..."}`

### 3. admin/item-use.mustache — 신규 생성

- **오늘 출고 내역** (상단): 테이블 또는 "없습니다." 표시
- **물품 선택** (하단): 카테고리 필터 + 초성·영문 검색 + 4열 카드 그리드
- 버튼명 "출고"
- AJAX: 출고 성공 시 재고 즉시 갱신, 재고 부족 시 `alert()`
- `{{> common/sidebar-admin}}`, `{{> common/header-staff}}`, `{{> common/footer-staff}}` 사용

### 4. AdminReservationController — @RequestParam name 명시 (버그 수정)

- `-parameters` 컴파일러 플래그 없는 환경에서 `int` 파라미터 이름 추론 실패 오류 수정
- `@RequestParam(defaultValue = "1") int page` → `@RequestParam(name = "page", defaultValue = "1") int page` (size, status 동일)

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `templates/common/sidebar-admin.mustache` | "물품 출고" 메뉴 추가 |
| `admin/item/AdminItemController.java` | `ItemManagerService` 주입, `GET/POST /admin/item/use` 추가 |
| `templates/admin/item-use.mustache` | 신규 생성 |
| `admin/reservation/AdminReservationController.java` | `@RequestParam` name 명시 (버그 수정) |

---

## 결과

- 관리자 사이드바 "물품 출고" 메뉴 정상 표시 및 활성 상태 강조
- 물품 출고 페이지: 카테고리 필터·초성 검색·AJAX 출고 정상 작동
- 출고 시 `ItemUsageLog` 저장 (reservationId=null, staff와 동일)
- 오늘 출고 내역 실시간 반영 (페이지 재방문 시)
