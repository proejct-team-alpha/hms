# W3-8 Workflow — 물품 담당자 페이지 페이징 + 검색

> **작성일**: 3W
> **목표**: 물품 목록·출고·입출고 내역 3개 페이지에 JS 페이징(10건/페이지) + 물품 목록 검색 추가

---

## 전체 흐름

```
클라이언트 사이드 JS 페이징 (서버 페이징 X)
  - item-list: 검색 + 페이징
  - item-use: 오늘 출고 내역 + 카드 그리드 페이징
  - item-history: 번호 버튼 페이징 교체
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 방식 | 클라이언트 사이드 JS 페이징 |
| 페이지당 항목 | 10건 |
| 페이징 UI | 번호 버튼 |
| 검색 | 초성·영문 검색 (item-list만) |
| 설계 문서 | `W3-8-spec.md` 참조 |

---

## 실행 흐름

```
Task 1 — item-list.mustache
  검색어 입력 → getChosung() + itemListMatches() → 필터링
  renderItemList(page) → 10건씩 표시 + 번호 버튼 생성

Task 2 — item-use.mustache (오늘 출고 내역)
  renderTodayLog(page) → 10건씩 표시 + 번호 버튼 생성

Task 3 — item-use.mustache (카드 그리드)
  applyFilter() 페이징 통합 버전
  카테고리/검색 변경 시 cardPage = 1 리셋

Task 4 — item-history.mustache
  기존 스크립트 교체 → PAGE_SIZE=10 + 번호 버튼 로직
```

---

## UI Mockup

```
[item-list 검색 + 페이징]
┌────────────────────────────────────────┐
│ [검색: ____________________]            │
├────────────────────────────────────────┤
│ 항목1 | 항목2 | ...                    │  10건
│ 항목11| ...                            │
├────────────────────────────────────────┤
│  [1] [2] [3] [4] [5]                   │  ← 번호 버튼
└────────────────────────────────────────┘
```

---

## 작업 목록

1. `item-list.mustache` — `#item-list-tbody`, `#empty-row` id 추가 + 검색창 HTML + 페이징 바 + JS
2. `item-use.mustache` — `#today-log-tbody` id 추가 + 페이징 바 + `renderTodayLog` JS
3. `item-use.mustache` — `#item-use-grid` 페이징 바 + `applyFilter()` 페이징 통합
4. `item-history.mustache` — 페이징 바 번호 버튼 div + 기존 `<script>` 교체

---

## 작업 진행내용

- [x] item-list.mustache 검색 + 페이징
- [x] item-use.mustache 오늘 출고 내역 페이징
- [x] item-use.mustache 카드 그리드 페이징
- [x] item-history.mustache 번호 버튼 페이징 교체

---

## 실행 흐름에 대한 코드

### item-list — getChosung + renderItemList

```javascript
const PAGE_SIZE = 10;
let currentPage = 1;
let filteredItems = allItems;

function getChosung(str) { /* 초성 추출 */ }
function itemListMatches(item, query) { /* 초성·영문 매칭 */ }

function renderItemList(page) {
    const start = (page - 1) * PAGE_SIZE;
    const pageItems = filteredItems.slice(start, start + PAGE_SIZE);
    // tbody 업데이트
    // 번호 버튼 생성
}

document.getElementById('search-input').addEventListener('input', function () {
    filteredItems = allItems.filter(item => itemListMatches(item, this.value));
    currentPage = 1;
    renderItemList(1);
});
```

### item-history — PAGE_SIZE 번호 버튼

```javascript
const PAGE_SIZE = 10;
let currentPage = 1;

function renderPage(page) {
    const start = (page - 1) * PAGE_SIZE;
    // 테이블 행 표시/숨김
    // 번호 버튼 갱신
}
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| item-list 검색 | 초성 검색 "ㄱ" | 매칭 항목만 표시 |
| 페이징 클릭 | 2페이지 클릭 | 11~20번 항목 표시 |
| 카테고리 변경 | 필터 변경 | cardPage 1로 리셋 |
| item-history | 번호 버튼 클릭 | 해당 페이지 표시 |

---

## 완료 기준

- [x] item-list 검색 + 10건 페이징
- [x] item-use 오늘 출고 내역 페이징
- [x] item-use 카드 그리드 페이징
- [x] item-history 번호 버튼 페이징
