# W3-1번째작업 Workflow — 물품 목록 현재 재고 / 입고 컬럼 분리

## 작업 개요

- **목표:** 물품 목록 테이블의 '현재 재고' 컬럼에 혼합된 재고 수량과 입고 폼을 별도 컬럼으로 분리
- **담당:** dev-a-c
- **날짜:** 2026-03-17

---

## 현황 분석

| 항목 | 현재 상태 |
|------|-----------|
| `item-list.mustache` '현재 재고' 컬럼 | ❌ 수량 표시 + 입고 폼이 같은 `<td>` 안에 혼합 |
| '입고' 전용 컬럼 | ❌ 없음 |

---

## 작업 목록

### 1. 테이블 헤더 — '입고' 컬럼 추가

**파일:** `src/main/resources/templates/item-manager/item-list.mustache`

```html
<!-- TODO: '현재 재고' th 다음에 '입고' th 추가 -->
<th class="p-4 font-medium text-center">입고</th>
```

### 2. 테이블 행 — `<td>` 분리

```html
<!-- TODO: 현재 재고 td — 수량만 표시 -->
<td class="p-4 text-sm text-right">
  <span class="font-bold ...">{{quantity}}개</span>
</td>

<!-- TODO: 입고 td — 폼만 표시, 가운데 정렬 -->
<td class="p-4 text-sm text-center">
  <form ...>...</form>
</td>
```

### 3. 빈 행 colspan 수정

```html
<!-- TODO: colspan 6 → 7 -->
<td colspan="7" ...>등록된 물품이 없습니다.</td>
```

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `templates/item-manager/item-list.mustache` | 수정 (컬럼 분리 + 정렬) |

---

## 금지 사항 체크

- [x] `config/`, `domain/` 수정 없음
- [x] `admin/**` 수정 없음
- [x] 컨트롤러/서비스/DTO 변경 없음
- [x] 입고 POST 엔드포인트 변경 없음
