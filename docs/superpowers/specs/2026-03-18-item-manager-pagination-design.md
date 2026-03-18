# 물품 담당자 페이징 기능 설계

**날짜**: 2026-03-18
**대상 역할**: 물품 담당자 (`/item-manager`)

---

## 개요

물품 담당자의 3개 페이지에 클라이언트 사이드 JS 페이징을 추가한다. 백엔드 변경 없음.

---

## 요구사항

- 페이지당 **10건**
- 번호 버튼 최대 5개 + `...` 생략 + 이전/다음 버튼
- 필터/검색 변경 시 **1페이지로 리셋**
- 총 항목 0건 또는 총 페이지 1개이면 **페이지 바 숨김**
- 페이지 상태는 메모리(JS 변수)에만 유지 — 새로고침 시 1페이지로 복귀 (URL 저장 안 함, 알려진 제약사항)

---

## 번호 버튼 생성 로직 (공통)

총 페이지 수에 따라 아래 규칙으로 번호 버튼을 생성한다:

```
totalPages <= 5: 모든 번호 표시
totalPages > 5:
  - 현재 페이지 중심으로 window = [cur-2, cur+2] (경계 조정)
  - window가 마지막 페이지를 포함하면 '...' 및 마지막 페이지 버튼 숨김
  - window가 마지막 페이지에 닿지 않으면 window 끝 + '...' + 마지막 페이지 표시
  - window 시작이 1이면 앞쪽 '...' 숨김
```

예시:
```
page 1/12:  이전  [1][2][3][4][5]...[12]  다음
page 6/12:  이전  [4][5][6][7][8]...[12]  다음
page 10/12: 이전  [8][9][10][11][12]      다음  (끝 window, ... 없음)
page 12/12: 이전  [8][9][10][11][12]      다음
```

---

## 대상 페이지 및 구현 세부사항

### 1. 물품 목록 (`item-manager/item-list.mustache`)

**대상**: 물품 테이블 tbody의 `<tr>` 행

**추가 기능: 검색창** (물품 출고와 동일한 방식)
- 카테고리 필터 우측에 검색 입력창 추가
- 초성·영문 지원 (item-use의 `getChosung`, `itemMatches` 로직 그대로 사용)
- 검색어 기준: `<td>` 첫 번째 셀(물품명) 텍스트
- 검색 변경 시 → 1페이지 리셋 후 재렌더

**구현**:
- `<tbody>`에 `id="item-list-tbody"` 추가
- 빈 상태 행에 `<tr id="empty-row">`로 템플릿에 직접 부여하여 구분
- JS에서 `#item-list-tbody tr:not(#empty-row)`를 대상으로 검색 필터링 + 페이징
- 카테고리 필터는 URL 기반(`?category=`) → 페이지 리로드로 자연스럽게 1페이지 리셋
- 검색과 페이징 연동: `getFilteredRows()` → 검색어 매칭 행 반환 → `renderPage()`로 페이지 범위 표시

### 2. 물품 출고 (`item-manager/item-use.mustache`)

#### ① 오늘 출고 내역 테이블

**대상**: `{{#todayLogs}}` 반복 내의 `<tr>` 행

**구현**:
- Mustache 이중 중첩 구조 (`{{#todayLogs}}` 조건부 + `{{#todayLogs}}` 반복) 그대로 유지
- 내부 `<tbody>`에 `id="today-log-tbody"` 추가
- JS에서 `#today-log-tbody tr`를 선택하여 페이징
- `todayLogs`가 비어 있으면 테이블 자체가 렌더링 안 되므로 페이징 바도 숨겨짐 (Mustache 조건 처리)

#### ② 카드 그리드 (물품 선택)

**대상**: `#item-use-grid .item-card` 카드 요소

**충돌 해결**: 기존 `applyFilter()` 함수와 페이징을 통합한다.
- 페이징은 **필터링된 카드 부분집합에만** 적용 (전체 카드 기준 아님)
- `applyFilter()` → 모든 카드 숨기고, 조건 맞는 카드 목록 추출 → `renderCardPage(filteredCards)` 호출하여 현재 페이지 범위만 표시
- 카테고리/검색 필터 변경 시 `currentCardPage = 1` 후 `applyFilter()` 호출

### 3. 입출고 내역 (`item-manager/item-history.mustache`)

**대상**: 기존 JS 페이징 전면 교체

**변경**:
- `PAGE_SIZE`: 15 → **10**
- 이전/다음 버튼은 유지, **번호 버튼 추가** (위 공통 로직 적용)
- `render()` 함수에 번호 버튼 생성 로직 통합
- 번호 버튼 삽입 위치: 이전 버튼과 다음 버튼 사이

---

## 변경 파일 (3개)

| 파일 | 변경 내용 |
|------|-----------|
| `templates/item-manager/item-list.mustache` | 검색창 추가, tbody id 추가, 빈 상태 행 id 추가, JS 페이징+검색 추가 |
| `templates/item-manager/item-use.mustache` | tbody id 추가, JS 페이징 2개 추가 (applyFilter 통합) |
| `templates/item-manager/item-history.mustache` | 기존 JS 교체 (10건, 번호 버튼 추가) |

---

## 비고

- `admin/`, `staff/` 영역은 범위 외
- 각 페이지 JS는 독립 구현 (공통 파일 없음) — 3개 파일이라 중복 감수
