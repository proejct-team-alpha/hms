# W3-8 워크플로우 - 물품 담당자 페이지 페이징 + 검색

## 작업 목표
물품 담당자의 3개 페이지(물품 목록, 물품 출고, 입출고 내역)에 클라이언트 사이드 JS 페이징(10건/페이지, 번호 버튼)을 추가하고, 물품 목록에 초성·영문 검색창을 추가한다.

## 작업 목록

### Task 1 — 물품 목록 (`item-list.mustache`) 검색 + 페이징
<!-- DONE 1. <tbody>에 id="item-list-tbody" 추가, 빈 상태 <tr>에 id="empty-row" 추가 -->
<!-- DONE 2. 카테고리 필터 아래 검색창 HTML 삽입 -->
<!-- DONE 3. 테이블 아래 페이징 바 HTML 삽입 -->
<!-- DONE 4. JS: getChosung + itemListMatches + renderItemList 추가 -->

### Task 2 — 물품 출고 오늘 출고 내역 테이블 페이징 (`item-use.mustache`)
<!-- DONE 1. <tbody>에 id="today-log-tbody" 추가 -->
<!-- DONE 2. {{#todayLogs}} 블록 아래 페이징 바 HTML 삽입 -->
<!-- DONE 3. JS: renderTodayLog 추가 -->

### Task 3 — 물품 출고 카드 그리드 페이징 (`item-use.mustache`)
<!-- DONE 1. #item-use-grid 아래 페이징 바 HTML 삽입 -->
<!-- DONE 2. 기존 applyFilter()를 페이징 통합 버전으로 교체 -->
<!-- DONE 3. 카테고리/검색 변경 시 cardPage = 1 리셋 추가 -->

### Task 4 — 입출고 내역 페이징 교체 (`item-history.mustache`)
<!-- DONE 1. 페이징 바 HTML에 번호 버튼 div 추가 -->
<!-- DONE 2. 기존 <script> 전체를 PAGE_SIZE=10 + 번호 버튼 로직으로 교체 -->

## 수정 파일
- `src/main/resources/templates/item-manager/item-list.mustache`
- `src/main/resources/templates/item-manager/item-use.mustache`
- `src/main/resources/templates/item-manager/item-history.mustache`

## 관련 문서
- 설계: `doc/dev-a/3W/W3-8-spec.md`
