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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 관리자 사이드바에서 "물품 관리" 메뉴 1개를 물품 목록 / 물품 등록 / 입출고 내역 3개로 교체합니다. 각 메뉴에는 현재 해당 페이지에 있을 때 메뉴를 강조 표시하기 위한 플래그(`isAdminItemList` 등)가 사용됩니다.
> - **왜 이렇게 썼는지**: 사이드바에서 현재 활성화된 페이지를 강조하려면 각 메뉴마다 boolean 플래그가 필요합니다. Mustache 템플릿에서 `{{#isAdminItemList}}active{{/isAdminItemList}}` 방식으로 조건부 스타일을 적용할 수 있습니다.
> - **쉽게 말하면**: 왼쪽 메뉴를 1개에서 3개로 늘리고, 지금 어느 메뉴에 있는지를 표시하기 위한 표시판(플래그)을 함께 추가하는 코드입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 관리자 물품 관리에 필요한 서비스 메서드들을 나열합니다. 카테고리별 조회, 폼 데이터 조회, 물품 저장/입고/삭제 기능을 각각 별도 메서드로 분리합니다.
> - **왜 이렇게 썼는지**: 서비스(Service) 계층은 비즈니스 로직을 담당하는 곳으로, 컨트롤러와 데이터베이스 사이에서 동작합니다. 각 기능을 별도 메서드로 나누면 코드의 재사용성이 높아지고 테스트하기도 쉬워집니다.
> - **쉽게 말하면**: 관리자가 물품을 조회하고, 등록하고, 수정하고, 삭제할 수 있도록 각각의 처리 기능을 만드는 목록입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 관리자 물품 관련 URL 요청들을 처리할 컨트롤러 메서드들을 정의합니다. `GET`은 페이지 조회, `POST`는 데이터 변경 요청입니다. `redirect`는 처리 후 다른 URL로 이동시키는 것입니다.
> - **왜 이렇게 썼는지**: POST 요청 후 바로 페이지를 반환하면 브라우저 새로고침 시 중복 제출이 발생할 수 있습니다. `redirect`를 통해 GET 페이지로 이동(PRG 패턴)하면 이 문제를 방지할 수 있습니다. `id` 파라미터 유무로 등록과 수정을 하나의 엔드포인트에서 처리합니다.
> - **쉽게 말하면**: 관리자가 물품 목록 보기, 등록하기, 수정하기, 입고 처리, 삭제, 내역 보기를 할 때 각각 어느 URL로 연결할지를 정의한 목록입니다.

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
