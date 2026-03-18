# 물품 담당자 페이징 + 검색 구현 계획

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 물품 담당자의 3개 페이지(물품 목록, 물품 출고, 입출고 내역)에 클라이언트 사이드 JS 페이징(10건/페이지, 번호 버튼)을 추가하고, 물품 목록에 초성·영문 검색창을 추가한다.

**Architecture:** 백엔드 변경 없음. Mustache 템플릿 3개에 순수 JS를 추가한다. 번호 버튼은 현재 페이지 중심 최대 5개 + `...` + 마지막 페이지 방식으로 생성한다. 필터/검색 변경 시 1페이지로 리셋.

**Tech Stack:** Mustache, Vanilla JS, Tailwind CSS (기존 스타일 클래스 재사용)

**Spec:** `docs/superpowers/specs/2026-03-18-item-manager-pagination-design.md`

---

## 공통 페이징 JS 로직 참고

아래 함수 패턴을 각 페이지에 맞게 독립 구현한다.

```javascript
var PAGE_SIZE = 10;
var currentPage = 1;

// 번호 버튼 생성 (공통 패턴)
function buildPageNumbers(totalPages, current, onPageChange) {
  var half = 2;
  var start = Math.max(1, current - half);
  var end = Math.min(totalPages, start + 4);
  start = Math.max(1, end - 4);

  var html = '';
  for (var p = start; p <= end; p++) {
    var active = p === current
      ? 'bg-indigo-600 text-white'
      : 'bg-white text-slate-600 hover:bg-slate-100';
    html += '<button type="button" data-page="' + p + '" class="page-num-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 ' + active + ' transition-colors">' + p + '</button>';
  }
  if (end < totalPages) {
    html += '<span class="px-1 text-xs text-slate-400">...</span>';
    html += '<button type="button" data-page="' + totalPages + '" class="page-num-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition-colors">' + totalPages + '</button>';
  }
  return html;
}

// 페이지 바 렌더 (공통 패턴)
function renderPaginationBar(barEl, numbersEl, infoEl, prevBtn, nextBtn, total, current, onPageChange) {
  var totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  if (totalPages <= 1) { barEl.style.display = 'none'; return; }
  barEl.style.display = '';
  numbersEl.innerHTML = buildPageNumbers(totalPages, current, onPageChange);
  numbersEl.querySelectorAll('.page-num-btn').forEach(function(btn) {
    btn.addEventListener('click', function() { onPageChange(parseInt(this.dataset.page)); });
  });
  if (infoEl) infoEl.textContent = total + '건 / ' + totalPages + '페이지';
  if (prevBtn) prevBtn.disabled = current <= 1;
  if (nextBtn) nextBtn.disabled = current >= totalPages;
}
```

---

## Task 1: 물품 목록 — 검색창 + 페이징

**Files:**
- Modify: `src/main/resources/templates/item-manager/item-list.mustache`

### 변경 내용 상세

**1-A. tbody 및 빈 상태 행 id 추가**

`<tbody class="divide-y divide-slate-200">` → `<tbody id="item-list-tbody" class="divide-y divide-slate-200">`

`{{^items}}` 안의 `<tr>` → `<tr id="empty-row">`

**1-B. 검색창 추가** — 카테고리 필터 div 아래, 테이블 위에 삽입:

```html
<!-- 검색창 -->
<div class="relative">
  <i data-feather="search" class="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"></i>
  <input type="text" id="item-list-search" placeholder="물품명 검색 (초성·영문 가능)"
         class="w-full pl-9 pr-4 py-2 text-sm border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-300">
</div>
```

**1-C. 페이징 바 추가** — 테이블 div 닫는 태그 아래에 삽입:

```html
<!-- 페이징 바 -->
<div id="item-list-pagination" class="flex items-center justify-between" style="display:none!important">
  <span id="item-list-page-info" class="text-xs text-slate-500"></span>
  <div class="flex items-center gap-2">
    <button id="item-list-prev" type="button"
            class="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
      이전
    </button>
    <div id="item-list-page-numbers" class="flex items-center gap-1"></div>
    <button id="item-list-next" type="button"
            class="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
      다음
    </button>
  </div>
</div>
```

**1-D. JS 추가** — 기존 restock AJAX 스크립트 앞에 삽입:

```javascript
// ===== 물품 목록 검색 + 페이징 =====
var ITEM_LIST_PAGE_SIZE = 10;
var itemListPage = 1;
var itemListQuery = '';

var CHOSUNG_LIST = ['ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'];
function getChosung(str) {
  return str.split('').map(function(ch) {
    var code = ch.charCodeAt(0);
    if (code >= 0xAC00 && code <= 0xD7A3) return CHOSUNG_LIST[Math.floor((code - 0xAC00) / 588)];
    return ch;
  }).join('');
}
function isChosungOnly(str) { return /^[ㄱ-ㅎ]+$/.test(str); }
function itemListMatches(name, query) {
  if (!query) return true;
  var q = query.toLowerCase();
  if (isChosungOnly(q)) return getChosung(name).includes(q);
  return name.toLowerCase().includes(q);
}

function getItemListRows() {
  return Array.from(document.querySelectorAll('#item-list-tbody tr:not(#empty-row)')).filter(function(row) {
    var name = row.querySelector('td:first-child') ? row.querySelector('td:first-child').textContent.trim() : '';
    return itemListMatches(name, itemListQuery);
  });
}

function buildItemListPageNumbers(totalPages) {
  var half = 2;
  var start = Math.max(1, itemListPage - half);
  var end = Math.min(totalPages, start + 4);
  start = Math.max(1, end - 4);
  var html = '';
  for (var p = start; p <= end; p++) {
    var active = p === itemListPage ? 'bg-indigo-600 text-white' : 'bg-white text-slate-600 hover:bg-slate-100';
    html += '<button type="button" data-page="' + p + '" class="il-page-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 ' + active + ' transition-colors">' + p + '</button>';
  }
  if (end < totalPages) {
    html += '<span class="px-1 text-xs text-slate-400">...</span>';
    html += '<button type="button" data-page="' + totalPages + '" class="il-page-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition-colors">' + totalPages + '</button>';
  }
  return html;
}

function renderItemList() {
  var rows = getItemListRows();
  var total = rows.length;
  var totalPages = Math.max(1, Math.ceil(total / ITEM_LIST_PAGE_SIZE));
  if (itemListPage > totalPages) itemListPage = totalPages;

  var start = (itemListPage - 1) * ITEM_LIST_PAGE_SIZE;
  var end = start + ITEM_LIST_PAGE_SIZE;

  document.querySelectorAll('#item-list-tbody tr:not(#empty-row)').forEach(function(r) { r.style.display = 'none'; });
  rows.forEach(function(r, i) { r.style.display = (i >= start && i < end) ? '' : 'none'; });

  var bar = document.getElementById('item-list-pagination');
  if (!bar) return;
  if (totalPages <= 1) { bar.style.removeProperty('display'); bar.style.display = 'none'; return; }
  bar.style.removeProperty('display');

  var numbersEl = document.getElementById('item-list-page-numbers');
  if (numbersEl) {
    numbersEl.innerHTML = buildItemListPageNumbers(totalPages);
    numbersEl.querySelectorAll('.il-page-btn').forEach(function(btn) {
      btn.addEventListener('click', function() { itemListPage = parseInt(this.dataset.page); renderItemList(); });
    });
  }

  var info = document.getElementById('item-list-page-info');
  if (info) info.textContent = total + '건 / ' + totalPages + '페이지';

  var prev = document.getElementById('item-list-prev');
  var next = document.getElementById('item-list-next');
  if (prev) prev.disabled = itemListPage <= 1;
  if (next) next.disabled = itemListPage >= totalPages;
}

var ilSearch = document.getElementById('item-list-search');
if (ilSearch) {
  ilSearch.addEventListener('input', function() {
    itemListQuery = this.value.trim();
    itemListPage = 1;
    renderItemList();
  });
}

var ilPrev = document.getElementById('item-list-prev');
var ilNext = document.getElementById('item-list-next');
if (ilPrev) ilPrev.addEventListener('click', function() { itemListPage--; renderItemList(); });
if (ilNext) ilNext.addEventListener('click', function() { itemListPage++; renderItemList(); });

renderItemList();
// ===== END 물품 목록 검색 + 페이징 =====
```

- [ ] **Step 1:** `item-list.mustache` 열기 — 현재 상태 확인
- [ ] **Step 2:** `<tbody>`에 `id="item-list-tbody"` 추가, 빈 상태 `<tr>`에 `id="empty-row"` 추가
- [ ] **Step 3:** 카테고리 필터 div와 테이블 div 사이에 검색창 HTML 삽입 (1-B)
- [ ] **Step 4:** 테이블 div 닫는 태그 아래에 페이징 바 HTML 삽입 (1-C)
- [ ] **Step 5:** 기존 `<script>` 태그 안 restock 코드 앞에 JS 삽입 (1-D)
- [ ] **Step 6:** 브라우저에서 `/item-manager/item-list` 접속 → 10건 이상 데이터 시 페이징 바 표시 확인, 검색어 입력 시 필터링 + 1페이지 리셋 확인
- [ ] **Step 7:** 커밋
  ```bash
  git add src/main/resources/templates/item-manager/item-list.mustache
  git commit -m "feat(item-manager): add search bar and pagination to item-list"
  ```

---

## Task 2: 물품 출고 — 오늘 출고 내역 테이블 페이징

**Files:**
- Modify: `src/main/resources/templates/item-manager/item-use.mustache`

### 변경 내용 상세

**2-A. tbody id 추가**

내부 `{{#todayLogs}}` 반복 안의 `<tbody>` → `<tbody id="today-log-tbody" class="divide-y divide-slate-100">`

**2-B. 페이징 바 추가** — 오늘 출고 내역 카드 div 닫는 태그(`</div>`) 바로 위, `{{#todayLogs}}` 조건 블록 안에 삽입:

```html
{{#todayLogs}}
<!-- 오늘 출고 페이징 바 -->
<div id="today-log-pagination" class="flex items-center justify-between pt-2" style="display:none!important">
  <span id="today-log-page-info" class="text-xs text-slate-500"></span>
  <div class="flex items-center gap-1">
    <button id="today-log-prev" type="button"
            class="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
      이전
    </button>
    <div id="today-log-page-numbers" class="flex items-center gap-1"></div>
    <button id="today-log-next" type="button"
            class="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
      다음
    </button>
  </div>
</div>
{{/todayLogs}}
```

**2-C. JS 추가** — 기존 `<script>` 안, chosung 변수 선언 위에 삽입:

```javascript
// ===== 오늘 출고 내역 페이징 =====
var TODAY_LOG_PAGE_SIZE = 10;
var todayLogPage = 1;

function buildTodayLogPageNumbers(totalPages) {
  var half = 2;
  var start = Math.max(1, todayLogPage - half);
  var end = Math.min(totalPages, start + 4);
  start = Math.max(1, end - 4);
  var html = '';
  for (var p = start; p <= end; p++) {
    var active = p === todayLogPage ? 'bg-indigo-600 text-white' : 'bg-white text-slate-600 hover:bg-slate-100';
    html += '<button type="button" data-page="' + p + '" class="tl-page-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 ' + active + ' transition-colors">' + p + '</button>';
  }
  if (end < totalPages) {
    html += '<span class="px-1 text-xs text-slate-400">...</span>';
    html += '<button type="button" data-page="' + totalPages + '" class="tl-page-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition-colors">' + totalPages + '</button>';
  }
  return html;
}

function renderTodayLog() {
  var tbody = document.getElementById('today-log-tbody');
  if (!tbody) return;
  var rows = Array.from(tbody.querySelectorAll('tr'));
  var total = rows.length;
  var totalPages = Math.max(1, Math.ceil(total / TODAY_LOG_PAGE_SIZE));
  if (todayLogPage > totalPages) todayLogPage = totalPages;

  var start = (todayLogPage - 1) * TODAY_LOG_PAGE_SIZE;
  var end = start + TODAY_LOG_PAGE_SIZE;
  rows.forEach(function(r, i) { r.style.display = (i >= start && i < end) ? '' : 'none'; });

  var bar = document.getElementById('today-log-pagination');
  if (!bar) return;
  if (totalPages <= 1) { bar.style.removeProperty('display'); bar.style.display = 'none'; return; }
  bar.style.removeProperty('display');

  var numbersEl = document.getElementById('today-log-page-numbers');
  if (numbersEl) {
    numbersEl.innerHTML = buildTodayLogPageNumbers(totalPages);
    numbersEl.querySelectorAll('.tl-page-btn').forEach(function(btn) {
      btn.addEventListener('click', function() { todayLogPage = parseInt(this.dataset.page); renderTodayLog(); });
    });
  }

  var info = document.getElementById('today-log-page-info');
  if (info) info.textContent = total + '건 / ' + totalPages + '페이지';

  var prev = document.getElementById('today-log-prev');
  var next = document.getElementById('today-log-next');
  if (prev) prev.disabled = todayLogPage <= 1;
  if (next) next.disabled = todayLogPage >= totalPages;
}

var tlPrev = document.getElementById('today-log-prev');
var tlNext = document.getElementById('today-log-next');
if (tlPrev) tlPrev.addEventListener('click', function() { todayLogPage--; renderTodayLog(); });
if (tlNext) tlNext.addEventListener('click', function() { todayLogPage++; renderTodayLog(); });

renderTodayLog();
// ===== END 오늘 출고 내역 페이징 =====
```

- [ ] **Step 1:** `item-use.mustache`의 오늘 출고 내역 섹션 내부 `<tbody>` 태그에 `id="today-log-tbody"` 추가
- [ ] **Step 2:** 기존 `{{#todayLogs}}...{{/todayLogs}}` 테이블 블록 바로 아래에 페이징 바를 새로운 `{{#todayLogs}}...{{/todayLogs}}` 블록으로 추가 (기존 블록 안에 넣지 않음, 2-B)
- [ ] **Step 3:** `<script>` 안 기존 코드 위에 JS 삽입 (2-C)
- [ ] **Step 4:** 브라우저에서 `/item-manager/item-use` 접속 → 오늘 출고 내역 10건 이상 시 페이징 바 표시 확인

---

## Task 3: 물품 출고 — 카드 그리드 페이징 (applyFilter 통합)

**Files:**
- Modify: `src/main/resources/templates/item-manager/item-use.mustache` (Task 2와 동일 파일)

### 변경 내용 상세

**3-A. 카드 그리드 아래 페이징 바 추가** — `#item-use-grid` div 닫는 태그 아래 삽입:

```html
<!-- 카드 그리드 페이징 바 -->
<div id="card-grid-pagination" class="flex items-center justify-between" style="display:none!important">
  <span id="card-grid-page-info" class="text-xs text-slate-500"></span>
  <div class="flex items-center gap-2">
    <button id="card-grid-prev" type="button"
            class="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
      이전
    </button>
    <div id="card-grid-page-numbers" class="flex items-center gap-1"></div>
    <button id="card-grid-next" type="button"
            class="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
      다음
    </button>
  </div>
</div>
```

**3-B. 기존 `applyFilter()` 함수를 페이징 통합 버전으로 교체**

기존:
```javascript
var currentCat = '';
function applyFilter() {
  var query = document.getElementById('item-search').value.trim();
  document.querySelectorAll('#item-use-grid .item-card').forEach(function(card) {
    var catOk = !currentCat || card.dataset.category === currentCat;
    var nameOk = itemMatches(card.querySelector('.item-name').textContent, query);
    card.style.display = (catOk && nameOk) ? '' : 'none';
  });
}
```

교체:
```javascript
var currentCat = '';
var CARD_PAGE_SIZE = 10;
var cardPage = 1;

function buildCardPageNumbers(totalPages) {
  var half = 2;
  var start = Math.max(1, cardPage - half);
  var end = Math.min(totalPages, start + 4);
  start = Math.max(1, end - 4);
  var html = '';
  for (var p = start; p <= end; p++) {
    var active = p === cardPage ? 'bg-indigo-600 text-white' : 'bg-white text-slate-600 hover:bg-slate-100';
    html += '<button type="button" data-page="' + p + '" class="cg-page-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 ' + active + ' transition-colors">' + p + '</button>';
  }
  if (end < totalPages) {
    html += '<span class="px-1 text-xs text-slate-400">...</span>';
    html += '<button type="button" data-page="' + totalPages + '" class="cg-page-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition-colors">' + totalPages + '</button>';
  }
  return html;
}

function applyFilter() {
  var query = document.getElementById('item-search').value.trim();
  var allCards = Array.from(document.querySelectorAll('#item-use-grid .item-card'));
  var filtered = allCards.filter(function(card) {
    var catOk = !currentCat || card.dataset.category === currentCat;
    var nameOk = itemMatches(card.querySelector('.item-name').textContent, query);
    return catOk && nameOk;
  });

  var total = filtered.length;
  var totalPages = Math.max(1, Math.ceil(total / CARD_PAGE_SIZE));
  if (cardPage > totalPages) cardPage = totalPages;
  var start = (cardPage - 1) * CARD_PAGE_SIZE;
  var end = start + CARD_PAGE_SIZE;

  allCards.forEach(function(card) { card.style.display = 'none'; });
  filtered.forEach(function(card, i) { card.style.display = (i >= start && i < end) ? '' : 'none'; });

  var bar = document.getElementById('card-grid-pagination');
  if (!bar) return;
  if (totalPages <= 1) { bar.style.removeProperty('display'); bar.style.display = 'none'; return; }
  bar.style.removeProperty('display');

  var numbersEl = document.getElementById('card-grid-page-numbers');
  if (numbersEl) {
    numbersEl.innerHTML = buildCardPageNumbers(totalPages);
    numbersEl.querySelectorAll('.cg-page-btn').forEach(function(btn) {
      btn.addEventListener('click', function() { cardPage = parseInt(this.dataset.page); applyFilter(); });
    });
  }

  var info = document.getElementById('card-grid-page-info');
  if (info) info.textContent = total + '건 / ' + totalPages + '페이지';

  var prev = document.getElementById('card-grid-prev');
  var next = document.getElementById('card-grid-next');
  if (prev) prev.disabled = cardPage <= 1;
  if (next) next.disabled = cardPage >= totalPages;
}
```

카테고리/검색 변경 시 `cardPage = 1` 리셋 추가:

```javascript
document.getElementById('item-search').addEventListener('input', function() {
  cardPage = 1; applyFilter();
});

document.querySelectorAll('.item-cat-btn').forEach(function(btn) {
  btn.addEventListener('click', function() {
    currentCat = this.dataset.cat;
    // ... 기존 active 클래스 토글 코드 ...
    cardPage = 1; applyFilter();
  });
});

var cgPrev = document.getElementById('card-grid-prev');
var cgNext = document.getElementById('card-grid-next');
if (cgPrev) cgPrev.addEventListener('click', function() { cardPage--; applyFilter(); });
if (cgNext) cgNext.addEventListener('click', function() { cardPage++; applyFilter(); });
```

- [ ] **Step 1:** `#item-use-grid` 닫는 div 아래 카드 그리드 페이징 바 HTML 삽입 (3-A)
- [ ] **Step 2:** 기존 `applyFilter()` 함수를 페이징 통합 버전으로 교체 (3-B)
- [ ] **Step 3:** 검색 `input` 이벤트 리스너에 `cardPage = 1;` 추가
- [ ] **Step 4:** 카테고리 버튼 클릭 이벤트 리스너에 `cardPage = 1;` 추가
- [ ] **Step 5:** 카드 그리드 이전/다음 버튼 이벤트 리스너 추가
- [ ] **Step 6:** 브라우저에서 `/item-manager/item-use` 접속 → 카드 10개 이상 시 페이징 바 표시 확인, 카테고리/검색 변경 시 1페이지 리셋 확인
- [ ] **Step 7:** 커밋
  ```bash
  git add src/main/resources/templates/item-manager/item-use.mustache
  git commit -m "feat(item-manager): add pagination to item-use (today-log table + card grid)"
  ```

---

## Task 4: 입출고 내역 — 기존 페이징 교체 (15→10건, 번호 버튼 추가)

**Files:**
- Modify: `src/main/resources/templates/item-manager/item-history.mustache`

### 변경 내용 상세

**4-A. 페이징 바 HTML 교체**

기존 (`line 107-120`):
```html
<div class="flex items-center justify-between" id="pagination-bar">
  <span class="text-xs text-slate-500" id="page-info"></span>
  <div class="flex gap-2">
    <button id="btn-prev" ...>이전</button>
    <button id="btn-next" ...>다음</button>
  </div>
</div>
```

교체:
```html
<div class="flex items-center justify-between" id="pagination-bar">
  <span class="text-xs text-slate-500" id="page-info"></span>
  <div class="flex items-center gap-2">
    <button id="btn-prev" type="button"
            class="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
      이전
    </button>
    <div id="page-numbers" class="flex items-center gap-1"></div>
    <button id="btn-next" type="button"
            class="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
      다음
    </button>
  </div>
</div>
```

**4-B. JS 전면 교체**

기존 `<script>` 블록 전체를 아래로 교체:

```javascript
var PAGE_SIZE = 10;  // 15 → 10
var currentPage = 1;
var currentType = '';
var currentQuery = '';

function getFilteredRows() {
  return Array.from(document.querySelectorAll('#history-tbody .history-row')).filter(function(row) {
    var nameOk = !currentQuery || row.dataset.name.includes(currentQuery);
    var typeOk = !currentType || row.dataset.type === currentType;
    return nameOk && typeOk;
  });
}

function buildPageNumbers(totalPages) {
  var half = 2;
  var start = Math.max(1, currentPage - half);
  var end = Math.min(totalPages, start + 4);
  start = Math.max(1, end - 4);
  var html = '';
  for (var p = start; p <= end; p++) {
    var active = p === currentPage ? 'bg-indigo-600 text-white' : 'bg-white text-slate-600 hover:bg-slate-100';
    html += '<button type="button" data-page="' + p + '" class="hist-page-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 ' + active + ' transition-colors">' + p + '</button>';
  }
  if (end < totalPages) {
    html += '<span class="px-1 text-xs text-slate-400">...</span>';
    html += '<button type="button" data-page="' + totalPages + '" class="hist-page-btn px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition-colors">' + totalPages + '</button>';
  }
  return html;
}

function render() {
  var rows = getFilteredRows();
  var total = rows.length;
  var totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  if (currentPage > totalPages) currentPage = totalPages;

  var start = (currentPage - 1) * PAGE_SIZE;
  var end = start + PAGE_SIZE;

  Array.from(document.querySelectorAll('#history-tbody .history-row')).forEach(function(row) {
    row.style.display = 'none';
  });
  rows.forEach(function(row, i) {
    row.style.display = (i >= start && i < end) ? '' : 'none';
  });

  var bar = document.getElementById('pagination-bar');
  var info = document.getElementById('page-info');
  var numbersEl = document.getElementById('page-numbers');

  if (totalPages <= 1) { if (bar) bar.style.display = 'none'; return; }
  if (bar) bar.style.display = '';

  if (info) info.textContent = total + '건 / ' + totalPages + '페이지';

  if (numbersEl) {
    numbersEl.innerHTML = buildPageNumbers(totalPages);
    numbersEl.querySelectorAll('.hist-page-btn').forEach(function(btn) {
      btn.addEventListener('click', function() { currentPage = parseInt(this.dataset.page); render(); });
    });
  }

  var btnPrev = document.getElementById('btn-prev');
  var btnNext = document.getElementById('btn-next');
  if (btnPrev) btnPrev.disabled = currentPage <= 1;
  if (btnNext) btnNext.disabled = currentPage >= totalPages;
}

var searchInput = document.getElementById('history-search');
if (searchInput) {
  searchInput.addEventListener('input', function() {
    currentQuery = this.value.trim();
    currentPage = 1;
    render();
  });
}

document.querySelectorAll('.type-filter-btn').forEach(function(btn) {
  btn.addEventListener('click', function() {
    currentType = this.dataset.type;
    currentPage = 1;
    document.querySelectorAll('.type-filter-btn').forEach(function(b) {
      b.className = 'type-filter-btn px-3 py-2 text-xs font-medium rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors';
    });
    this.className = 'type-filter-btn px-3 py-2 text-xs font-medium rounded-xl bg-indigo-600 text-white transition-colors';
    render();
  });
});

var btnPrev = document.getElementById('btn-prev');
var btnNext = document.getElementById('btn-next');
if (btnPrev) btnPrev.addEventListener('click', function() { currentPage--; render(); });
if (btnNext) btnNext.addEventListener('click', function() { currentPage++; render(); });

render();
```

- [ ] **Step 1:** `item-history.mustache`에서 `<div class="flex items-center justify-between" id="pagination-bar">` 블록만 교체. 바깥 `{{#hasHistories}}` / `{{/hasHistories}}` Mustache 조건 태그는 그대로 유지.
- [ ] **Step 2:** `<script>` 블록 전체를 4-B 코드로 교체 (`PAGE_SIZE = 10`, 번호 버튼 로직 추가)
- [ ] **Step 3:** 브라우저에서 `/item-manager/item-history` 접속 → 10건 기준 페이징 확인, 번호 버튼 표시 확인, 검색/필터 시 1페이지 리셋 확인
- [ ] **Step 4:** 커밋
  ```bash
  git add src/main/resources/templates/item-manager/item-history.mustache
  git commit -m "feat(item-manager): upgrade item-history pagination (10/page, numbered buttons)"
  ```

---

## 최종 확인

- [ ] `/item-manager/item-list` — 검색창 동작, 페이징 동작, 카테고리 필터 후 페이지 리셋
- [ ] `/item-manager/item-use` — 오늘 출고 내역 페이징, 카드 그리드 페이징, 카테고리+검색 필터 연동
- [ ] `/item-manager/item-history` — 10건/페이지, 번호 버튼, 검색+타입 필터 연동
