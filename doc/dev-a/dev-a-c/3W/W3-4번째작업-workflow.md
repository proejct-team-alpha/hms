# W3-4번째작업 Workflow — 관리자 물품 메뉴 3개 구성

> **작성일**: 3W
> **목표**: 관리자 사이드바 "물품 관리" 1개를 물품 목록 / 물품 등록 / 입출고 내역 3개로 교체, 각 페이지 admin 레이아웃으로 구성

---

## 전체 흐름

```
사이드바 메뉴 교체 → 서비스 메서드 추가 → 컨트롤러 엔드포인트 확장
  → item-list.mustache 전면 개편 → item-form.mustache 등록/수정 통합
  → item-history.mustache 신규 생성 (placeholder)
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | "물품 관리" 1개 메뉴 → 물품 목록 / 물품 등록 / 입출고 내역 3개로 분리 |
| 활성 플래그 | `isAdminItemList`, `isAdminItemForm`, `isAdminItemHistory` |
| 카테고리 필터 | 물품 목록 페이지에 카테고리별 필터 버튼 |
| 등록/수정 통합 | `id` 유무로 등록/수정 구분, 하나의 폼 페이지에서 처리 |
| 입출고 내역 | SQL 미연동 → "준비 중" placeholder 먼저 생성 |

---

## 실행 흐름

```
관리자 사이드바 → 물품 목록 클릭 → GET /admin/item/list?category=...
  → AdminItemService.getItemList(category) → 카테고리 필터 목록 + 물품 목록
  → admin/item-list.mustache 렌더링

물품 등록 클릭 → GET /admin/item/form?id=(없으면 신규)
  → AdminItemService.getItemForm(id) → ItemFormDto
  → admin/item-form.mustache 렌더링

물품 저장 → POST /admin/item/form/save → redirect /admin/item/list

입출고 내역 클릭 → GET /admin/item/history
  → admin/item-history.mustache ("준비 중" placeholder)
```

---

## UI Mockup

```
[사이드바]          [물품 목록 페이지]
물품 목록 ←활성     카테고리: [전체] [의료소모품] [의료기기] [사무비품]
물품 등록           ┌──────┬──────┬──────┬──────┬──────┬──────┬──────┐
입출고 내역          │물품명│카테고리│최소재고│현재재고│입고│수정│삭제│
                    ├──────┼──────┼──────┼──────┼──────┼──────┼──────┤
                    │붕대  │의료소모│  10  │  25  │[5][입고]│[수정]│[삭제]│
                    └──────┴──────┴──────┴──────┴──────┴──────┴──────┘
```

---

## 작업 목록

1. `sidebar-admin.mustache` — "물품 관리" 1개 → 물품 목록·물품 등록·입출고 내역 3개로 교체
2. `AdminItemService` — `getItemList()`, `getCategoryFilters()`, `getItemForm()`, `saveItem()`, `restockItem()`, `deleteItem()` 메서드 추가
3. `ItemRepository` — `findByCategoryOrderByNameAsc()` 메서드 추가
4. `AdminItemController` — GET/POST 엔드포인트 전면 확장
5. `admin/item-list.mustache` — 카테고리 필터 + 입고 + 수정/삭제 컬럼 추가
6. `admin/item-form.mustache` — 등록/수정 통합 폼
7. `admin/item-history.mustache` — 신규 생성 (준비 중 placeholder)

---

## 작업 진행내용

- [x] 사이드바 메뉴 3개로 교체, 각 활성 플래그 적용
- [x] AdminItemService 메서드 추가
- [x] ItemRepository `findByCategoryOrderByNameAsc()` 추가
- [x] AdminItemController 엔드포인트 전면 확장
- [x] admin/item-list.mustache 전면 개편
- [x] admin/item-form.mustache 등록/수정 통합
- [x] admin/item-history.mustache 신규 생성

---

## 실행 흐름에 대한 코드

### 1. sidebar-admin.mustache — 메뉴 교체

```html
<!-- 기존 "물품 관리" 1개 → 3개로 교체 -->
<a href="/admin/item/list"
   class="... {{#isAdminItemList}}bg-indigo-50 text-indigo-700{{/isAdminItemList}}{{^isAdminItemList}}text-slate-600 hover:bg-slate-100{{/isAdminItemList}}">
  <i data-feather="list" class="w-5 h-5"></i> 물품 목록
</a>
<a href="/admin/item/form"
   class="... {{#isAdminItemForm}}bg-indigo-50 text-indigo-700{{/isAdminItemForm}}{{^isAdminItemForm}}text-slate-600 hover:bg-slate-100{{/isAdminItemForm}}">
  <i data-feather="plus-circle" class="w-5 h-5"></i> 물품 등록
</a>
<a href="/admin/item/history"
   class="... {{#isAdminItemHistory}}bg-indigo-50 text-indigo-700{{/isAdminItemHistory}}{{^isAdminItemHistory}}text-slate-600 hover:bg-slate-100{{/isAdminItemHistory}}">
  <i data-feather="file-text" class="w-5 h-5"></i> 입출고 내역
</a>
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 관리자 사이드바에서 "물품 관리" 메뉴 1개를 물품 목록·물품 등록·입출고 내역 3개로 교체합니다. 각 메뉴에 현재 페이지 강조를 위한 플래그(`isAdminItemList` 등)를 사용합니다.
> - **왜 이렇게 썼는지**: `{{#isAdminItemList}}active{{/isAdminItemList}}`는 Mustache 조건 문법입니다. 서버에서 해당 플래그를 `true`로 전달하면 이 블록 안의 CSS 클래스가 적용되어 메뉴가 강조됩니다.
> - **쉽게 말하면**: 왼쪽 메뉴를 1개에서 3개로 늘리고, 현재 어느 메뉴에 있는지 표시하는 표지판을 함께 추가한 것입니다.

### 2. AdminItemController — 엔드포인트 구성

```java
// GET /admin/item/list — 물품 목록 (카테고리 필터)
@GetMapping("/list")
public String itemList(@RequestParam(name = "category", required = false) String category, Model model) {
    model.addAttribute("items", adminItemService.getItemList(category));
    model.addAttribute("categories", adminItemService.getCategoryFilters(category));
    model.addAttribute("isAdminItemList", true);
    return "admin/item-list";
}

// GET /admin/item/form — 등록/수정 통합 폼
@GetMapping("/form")
public String itemForm(@RequestParam(name = "id", required = false) Long id, Model model) {
    model.addAttribute("item", adminItemService.getItemForm(id));
    model.addAttribute("isAdminItemForm", true);
    return "admin/item-form";
}

// POST /admin/item/form/save — 저장 (등록/수정 통합)
@PostMapping("/form/save")
public String saveItem(/* @RequestParam 필드들 */) {
    adminItemService.saveItem(...);
    return "redirect:/admin/item/list";
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 관리자 물품 관련 URL 요청들을 처리할 컨트롤러 메서드들을 정의합니다. `GET`은 페이지 조회, `POST`는 데이터 변경 요청입니다.
> - **왜 이렇게 썼는지**: POST 요청 후 `redirect`를 통해 GET 페이지로 이동(PRG 패턴)하면 브라우저 새로고침 시 중복 제출 문제를 방지할 수 있습니다. `id` 파라미터 유무로 등록과 수정을 하나의 엔드포인트에서 처리합니다.
> - **쉽게 말하면**: 물품 목록 보기, 등록/수정하기, 삭제하기, 내역 보기를 각각 어느 URL로 연결할지 정의한 목록입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 물품 목록 접속 | `/admin/item/list` | 목록 표시 + 사이드바 "물품 목록" 활성화 |
| 카테고리 필터 | `?category=MEDICAL_SUPPLIES` | 해당 카테고리 물품만 표시 |
| 물품 등록 | `/admin/item/form` | 빈 폼 + 사이드바 "물품 등록" 활성화 |
| 물품 수정 | `/admin/item/form?id=1` | 기존 값 pre-fill + 카테고리 selected |
| 물품 저장 | POST 후 | 목록 페이지로 redirect |
| 입출고 내역 | `/admin/item/history` | "준비 중" placeholder + 활성화 |

---

## 완료 기준

- [x] 관리자 사이드바 물품 메뉴 3개 정상 표시 및 활성 상태 강조
- [x] 물품 목록: 카테고리 필터, 입고, 수정, 삭제 기능 정상 작동
- [x] 물품 등록/수정: 통합 폼 정상 작동
- [x] 입출고 내역: 준비 중 placeholder 표시
- [x] `config/`, `domain/` 수정 없음
