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

---

> **💡 입문자 설명**
>
> **Mustache partial (`{{> common/sidebar-item-manager}}`) — 어떻게 작동하는지**
> - `{{> 파일경로}}`는 다른 Mustache 파일의 내용을 이 위치에 삽입하는 문법입니다. 사이드바를 모든 페이지마다 복사하는 대신, 하나의 파일(`sidebar-item-manager.mustache`)을 공유해 수정이 한 곳에서 반영됩니다.
>
> **왜 `POST /item-manager/item-use`를 AJAX로 처리하는지**
> - 출고 버튼을 누를 때 페이지 전체를 새로 로드하면 사용자가 스크롤 위치를 잃고 느립니다. AJAX로 처리하면 출고 후 재고 수량만 업데이트되고 나머지 화면은 그대로입니다.
> - `action="/item-manager/item-use"`는 JavaScript가 없을 때 폴백으로 일반 폼 제출을 사용하기 위한 설정입니다.
>
> **쉽게 말하면**: 물품 담당자 전용 출고 화면을 만들되, 사이드바는 공통 파일을 재사용하고, 출고 처리는 화면 깜빡임 없이 처리합니다.
