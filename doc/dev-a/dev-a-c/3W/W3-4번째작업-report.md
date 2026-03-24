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

---

> **💡 입문자 설명**
>
> **"물품 관리" 1개 메뉴 → 3개 메뉴로 분리한 이유**
> - 하나의 메뉴에 목록·등록·내역을 모두 담으면 각 기능으로 직접 이동할 수 없고, 사이드바에서 현재 위치를 표시하기 어렵습니다. 기능별로 분리하면 URL도 명확해지고(`/admin/item/list`, `/admin/item/form`, `/admin/item/history`), 각 페이지에 독립적인 활성 플래그를 적용할 수 있습니다.
>
> **등록/수정 통합 폼 (`ItemFormDto`) — 왜 하나로 합쳤는지**
> - 물품 등록과 수정은 같은 입력 필드(이름, 카테고리, 최소 수량 등)를 씁니다. `id` 유무로 등록인지 수정인지 구분하면 코드와 화면을 하나로 관리할 수 있어 유지보수가 편합니다.
> - **다른 방법**: 등록용·수정용 폼을 따로 만들 수 있지만, 동일한 필드를 두 번 관리해야 하므로 변경 시 둘 다 수정해야 합니다.
>
> **카테고리 필터 (`findByCategoryOrderByNameAsc`) — 왜 Repository에 추가했는지**
> - 카테고리로 필터링하려면 DB에서 해당 카테고리 물품만 조회해야 합니다. JPA Repository에 메서드 이름 규칙(`findBy` + 조건 + `OrderBy` + 정렬)으로 선언하면 Spring이 자동으로 SQL 쿼리를 만들어줍니다. SQL을 직접 작성하지 않아도 됩니다.
>
> **입출고 내역 placeholder — 왜 먼저 만드는지**
> - 기능을 단계별로 구현할 때 UI 구조(메뉴, 페이지 틀)를 먼저 만들고 내용을 나중에 채우는 방식입니다. 팀원들이 전체 구조를 미리 확인하고, 병렬로 작업할 수 있는 장점이 있습니다.
>
> **쉽게 말하면**: 관리자용 물품 관리 메뉴를 "큰 방 하나"에서 "목록실·등록실·기록실" 세 방으로 나눈 작업입니다. 각 방에 들어가는 문(사이드바 메뉴)도 따로 달았습니다.
