# W3-9 Workflow — 물품 담당자 물품 출고 기능

## 작업 개요

- **목표:** 물품 담당자 페이지에 물품 출고 기능 추가 (staff/admin과 동일한 방식)
- **위치:** 사이드바 "물품 등록"과 "물품 입출고 내역" 사이
- **메뉴명:** 물품 출고

---

## 작업 목록

### 1. `LayoutModelInterceptor.java`
- `isItemUse` 플래그 추가: `path.startsWith("/item-manager/item-use")`

### 2. `sidebar-item-manager.mustache`
- "물품 등록"과 "물품 입출고 내역" 사이에 "물품 출고" 메뉴 추가
- 아이콘: `package`, 활성 플래그: `isItemUse`

### 3. `ItemManagerController.java`
- `GET /item-manager/item-use`: `items`, `todayLogs` 모델 → `item-manager/item-use` 반환
- `POST /item-manager/item-use` (`@ResponseBody`): amount 검증 → `useItem(id, amount, null)` 호출

### 4. `item-manager/item-use.mustache` (신규)
- `{{> common/sidebar-item-manager}}` 사용
- POST 폼 action: `/item-manager/item-use`
- AJAX fetch URL: `/item-manager/item-use`
