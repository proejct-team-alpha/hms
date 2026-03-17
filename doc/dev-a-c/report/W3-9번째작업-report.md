# W3-9번째작업 Report — 물품 담당자 물품 출고 기능

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. LayoutModelInterceptor — isItemUse 플래그 추가

- `mav.addObject("isItemUse", path.startsWith("/item-manager/item-use"))`

### 2. sidebar-item-manager.mustache — "물품 출고" 메뉴 추가

- "물품 등록"과 "물품 입출고 내역" 사이에 추가
- 아이콘: `package`, 활성 플래그: `isItemUse`

### 3. ItemManagerController — 출고 엔드포인트 추가

- `GET /item-manager/item-use`: `items`, `todayLogs` 모델 → `item-manager/item-use` 반환
- `POST /item-manager/item-use` (`@ResponseBody`): amount 검증 → `useItem(id, amount, null)` 호출
  - 성공: `{"quantity": N}` / 재고 부족·오류: 400 + `{"error": "..."}`

### 4. item-manager/item-use.mustache — 신규 생성

- **오늘 출고 내역** (상단): 테이블 또는 "없습니다." 표시
- **물품 선택** (하단): 카테고리 필터 + 초성·영문 검색 + 4열 카드 그리드
- 버튼명 "출고"
- AJAX: 출고 성공 시 재고 즉시 갱신, 재고 부족 시 `alert()`
- `{{> common/sidebar-item-manager}}`, `{{> common/header-staff}}`, `{{> common/footer-staff}}` 사용

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `common/interceptor/LayoutModelInterceptor.java` | `isItemUse` 플래그 추가 |
| `templates/common/sidebar-item-manager.mustache` | "물품 출고" 메뉴 추가 |
| `item/ItemManagerController.java` | `GET/POST /item-manager/item-use` 추가 |
| `templates/item-manager/item-use.mustache` | 신규 생성 |

---

## 결과

- 물품 담당자 사이드바 "물품 출고" 메뉴 정상 표시 및 활성 상태 강조
- 물품 출고 페이지: 카테고리 필터·초성 검색·AJAX 출고 정상 작동
- 출고 시 `ItemUsageLog` 저장 (reservationId=null, staff/admin과 동일)
- 오늘 출고 내역 실시간 반영 (페이지 재방문 시)
