# W3-9번째작업 리포트 - 물품 담당자 물품 출고 기능

## 작업 개요
- **작업명**: 물품 담당자 사이드바에 "물품 출고" 메뉴 추가, 전용 출고 페이지 구성 (카테고리 필터·초성 검색·AJAX 출고·오늘 출고 내역)
- **수정 파일**: `common/interceptor/LayoutModelInterceptor.java`, `templates/common/sidebar-item-manager.mustache`, `item/ItemManagerController.java`, `templates/item-manager/item-use.mustache`(신규)

## 작업 내용

### 1. LayoutModelInterceptor — isItemUse 플래그 추가

`mav.addObject("isItemUse", path.startsWith("/item-manager/item-use"))`.

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 현재 URL이 `/item-manager/item-use`로 시작하면 `isItemUse`를 `true`로 설정해 사이드바 메뉴 강조에 사용합니다.
> - **왜 이렇게 썼는지**: 인터셉터에서 처리하면 컨트롤러마다 `model.addAttribute`를 중복 작성하지 않아도 됩니다. 관리자(W3-8)는 컨트롤러에서 직접 설정했지만, 물품 담당자는 기존 인터셉터 방식을 따릅니다.
> - **쉽게 말하면**: "물품 출고" 페이지에 있을 때 사이드바 메뉴가 자동으로 강조되도록 설정합니다.

### 2. sidebar-item-manager.mustache — "물품 출고" 메뉴 추가

"물품 등록"과 "물품 입출고 내역" 사이에 추가. 아이콘: `package`, 활성 플래그: `isItemUse`.

### 3. ItemManagerController — 출고 엔드포인트 추가

- `GET /item-manager/item-use`: `items`, `todayLogs` 모델 → `item-manager/item-use` 반환
- `POST /item-manager/item-use` (`@ResponseBody`): amount 검증 → `useItem(id, amount, null)` 호출
  - 성공: `{"quantity": N}` / 재고 부족·오류: 400 + `{"error": "..."}`

### 4. item-manager/item-use.mustache — 신규 생성

- **오늘 출고 내역** (상단): 테이블 또는 "없습니다." 표시
- **물품 선택** (하단): 카테고리 필터 + 초성·영문 검색 + 4열 카드 그리드
- 버튼명 "출고"
- AJAX: 출고 성공 시 재고 즉시 갱신, 재고 부족 시 `alert()`
- `{{> common/sidebar-item-manager}}` 사용

## 테스트 결과

| 항목 | 결과 |
|------|------|
| 사이드바 "물품 출고" 메뉴 표시 및 활성화 | ✅ |
| 물품 출고 페이지 접속 | ✅ |
| 카테고리 필터·초성 검색 | ✅ |
| AJAX 출고 성공 — 재고 즉시 갱신 | ✅ |
| 출고 시 ItemUsageLog 저장 (reservationId=null) | ✅ |
| 오늘 출고 내역 표시 | ✅ |

## 특이사항
- `isItemUse` 플래그를 인터셉터에서 처리: 관리자(W3-8)가 컨트롤러에서 직접 설정한 것과 대조적 — 두 방식 모두 사이드바 활성화 목적은 동일
- 물품 담당자는 재고 관리가 주업무이므로 직접 출고 기능도 필요 — 원무과·관리자와 동일한 `useItem()` 재사용
- 역할마다 별도 URL(`/item-manager/item-use`): Spring Security URL 기반 접근 제어 때문
