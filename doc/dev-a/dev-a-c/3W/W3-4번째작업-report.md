# W3-4번째작업 Report — 관리자 물품 메뉴 3개 구성

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. sidebar-admin.mustache — 메뉴 교체

- 기존 "물품 관리" 1개 → 물품 목록 / 물품 등록 / 입출고 내역 3개로 교체
- 각 메뉴 활성 상태 플래그: `isAdminItemList`, `isAdminItemForm`, `isAdminItemHistory`

### 2. AdminItemService — 메서드 추가

- `getItemList(String category)`: 카테고리 필터 조회
- `getCategoryFilters(String selected)`: 카테고리 필터 목록
- `getItemForm(Long id)`: 등록/수정 폼 데이터 (`ItemFormDto` 재사용)
- `saveItem(...)`: 등록/수정 통합
- `restockItem(Long id, int amount)`: 입고
- `deleteItem(Long id)`: 삭제

### 3. ItemRepository — 메서드 추가

- `findByCategoryOrderByNameAsc(ItemCategory category)`: 카테고리 필터 조회

### 4. AdminItemController — 엔드포인트 확장

- `GET /admin/item/list`: 카테고리 필터 + `isAdminItemList=true`
- `GET /admin/item/form`: 등록/수정 통합 + `isAdminItemForm=true`
- `POST /admin/item/form/save`: 등록/수정 저장
- `POST /admin/item/restock`: 입고 처리
- `POST /admin/item/delete`: 삭제 처리
- `GET /admin/item/history`: 입출고 내역 + `isAdminItemHistory=true`

### 5. admin/item-list.mustache — 전면 개편

- 카테고리 필터 버튼, 현재 재고 / 입고 컬럼 분리, 수정/삭제 관리 컬럼 추가

### 6. admin/item-form.mustache — 등록/수정 통합

- `ItemFormDto` 기반으로 기존 값 pre-fill, 카테고리 selected 처리

### 7. admin/item-history.mustache — 신규 생성

- 준비 중 placeholder (SQL 연동은 별도 작업)

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `templates/common/sidebar-admin.mustache` | 메뉴 1개 → 3개 교체 |
| `admin/item/AdminItemController.java` | 엔드포인트 전면 확장 |
| `admin/item/AdminItemService.java` | 메서드 추가 |
| `admin/item/ItemRepository.java` | `findByCategoryOrderByNameAsc` 추가 |
| `templates/admin/item-list.mustache` | 전면 개편 |
| `templates/admin/item-form.mustache` | 등록/수정 통합 |
| `templates/admin/item-history.mustache` | 신규 생성 |

---

## 결과

- 관리자 사이드바 물품 메뉴 3개 정상 표시 및 활성 상태 강조
- 물품 목록: 카테고리 필터, 입고, 수정, 삭제 기능 정상 작동
- 물품 등록/수정: 통합 폼 정상 작동
- 입출고 내역: 준비 중 placeholder 표시
