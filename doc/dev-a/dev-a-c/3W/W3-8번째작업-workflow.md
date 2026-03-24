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

---

> **💡 입문자 설명**
>
> **`staff/item-use.mustache` 구조를 동일하게 쓰는 이유**
> - 원무과(staff)와 관리자(admin)의 출고 화면은 기능이 동일합니다. 코드를 복사해 URL과 사이드바만 바꾸면 됩니다. `{{> common/sidebar-admin}}`은 Mustache partial로, 관리자용 사이드바 HTML을 이 위치에 삽입합니다.
>
> **GET과 POST 엔드포인트를 둘 다 만드는 이유**
> - `GET /admin/item/use`: 출고 화면을 보여줍니다(물품 목록, 오늘 출고 내역 조회).
> - `POST /admin/item/use`: 실제 출고 처리를 합니다(재고 차감, 로그 저장). `@ResponseBody`로 JSON 응답을 반환해 AJAX가 처리합니다.
> - 조회(GET)와 처리(POST)를 분리하는 것은 REST 설계 원칙입니다. GET은 데이터를 변경하지 않고, POST만 변경합니다.
>
> **쉽게 말하면**: 관리자가 "물품 출고" 메뉴를 클릭하면 출고 화면이 열리고(GET), 출고 버튼을 누르면 서버에 요청이 가서 재고가 줄어듭니다(POST AJAX).
