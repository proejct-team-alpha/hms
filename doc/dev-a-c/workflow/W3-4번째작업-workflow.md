# W3-4번째작업 Workflow — 관리자 물품 메뉴 3개 구성

## 작업 개요

- **목표:** 관리자 사이드바 "물품 관리" 1개를 물품 목록 / 물품 등록 / 입출고 내역 3개로 교체, 각 페이지 admin 레이아웃으로 구성
- **담당:** dev-a-c
- **날짜:** 2026-03-17

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `templates/common/sidebar-admin.mustache` | 수정 (메뉴 1개 → 3개 교체) |
| `admin/item/AdminItemController.java` | 수정 (엔드포인트 확장 + active 플래그 추가) |
| `admin/item/AdminItemService.java` | 수정 (메서드 추가) |
| `templates/admin/item-list.mustache` | 수정 (카테고리 필터 + 입고 + 수정/삭제) |
| `templates/admin/item-form.mustache` | 수정 (등록/수정 통합) |
| `templates/admin/item-history.mustache` | 신규 (입출고 내역 placeholder) |

---

## 작업 목록

### 1. sidebar-admin.mustache — 메뉴 교체

```html
<!-- TODO: 기존 "물품 관리" 1개 → 3개로 교체 -->
<!-- 물품 목록: isAdminItemList 플래그 -->
<!-- 물품 등록: isAdminItemForm 플래그 -->
<!-- 입출고 내역: isAdminItemHistory 플래그 -->
```

### 2. AdminItemService — 메서드 추가

```java
// TODO: 추가 메서드
// - getItemList(String category): 카테고리 필터 조회
// - getCategoryFilters(String selected): 카테고리 필터 목록
// - getItemForm(Long id): 등록/수정 폼 데이터
// - saveItem(Long id, String name, String category, int quantity, int minQuantity)
// - restockItem(Long id, int amount)
// - deleteItem(Long id)
```

### 3. AdminItemController — 엔드포인트 확장

```java
// TODO: 수정/추가
// GET  /list   → isAdminItemList=true, 카테고리 필터 지원
// GET  /form   → isAdminItemForm=true, id 파라미터(수정) 지원
// POST /form   → id 파라미터로 등록/수정 통합
// POST /restock → 입고 처리 후 redirect /admin/item/list
// POST /delete  → 삭제 후 redirect /admin/item/list
// GET  /history → isAdminItemHistory=true
```

### 4. admin/item-list.mustache — 전면 개편

- 카테고리 필터 버튼 추가
- 입고 컬럼 추가 (현재 재고 / 입고 분리)
- 수정(edit-2 아이콘) / 삭제(trash-2 아이콘) 관리 컬럼 추가
- 빈 행 colspan 7로 조정
- item-manager/item-list.mustache 스타일 기준

### 5. admin/item-form.mustache — 등록/수정 통합

- hidden id 필드 추가
- 카테고리 selected 상태 처리
- quantity / minQuantity 기존 값 표시

### 6. admin/item-history.mustache — 신규

- admin 사이드바 레이아웃
- isAdminItemHistory=true
- 입출고 내역 SQL 미연동 상태 → "준비 중" placeholder

---

## 금지 사항 체크

- [x] `config/`, `domain/` 수정 없음
- [x] item-manager/** 수정 없음
