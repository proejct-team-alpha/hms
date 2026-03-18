# W3-8 Workflow — 관리자 물품 출고 기능

## 작업 개요

- **목표:** 관리자 페이지에 물품 출고 기능 추가 (staff와 동일한 방식)
- **위치:** 사이드바 "물품 등록"과 "입출고 내역" 사이
- **메뉴명:** 물품 출고

---

## 작업 목록

### 1. `sidebar-admin.mustache`
- "물품 등록"(`/admin/item/form`)과 "입출고 내역"(`/admin/item/history`) 사이에 "물품 출고" 메뉴 추가
- 아이콘: `package`, 활성 플래그: `isAdminItemUse`

### 2. `AdminItemController.java`
- `ItemManagerService` 주입 추가
- `GET /admin/item/use`: `items`, `todayLogs` 모델 → `admin/item-use` 반환
  - `isAdminItemUse = true` 모델에 추가
- `POST /admin/item/use` (`@ResponseBody`): amount 검증 → `useItem(id, amount, null)` 호출
  - 성공: `{"quantity": N}` / 오류: 400 + `{"error": "..."}`

### 3. `admin/item-use.mustache` (신규)
- `staff/item-use.mustache` 구조 동일
- `{{> common/sidebar-admin}}` 사용
- POST 폼 action: `/admin/item/use`
- AJAX fetch URL: `/admin/item/use`
