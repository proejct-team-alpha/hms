# W3-6번째작업 Workflow — 간호사 환자 정보관리 물품 사용(출고) 기능

## 작업 개요

- **목표:** 간호사 patient-detail 페이지 하단에 물품 사용 섹션 추가 (RECEIVED 환자만 표시, 사용 내역은 항상 표시)
- **담당:** dev-a-c
- **날짜:** 2026-03-17

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `nurse/dto/NursePatientDto.java` | 수정 (canUseItem 필드 추가) |
| `nurse/NurseReceptionController.java` | 수정 (items·usageLogs 모델 추가, POST /nurse/item/use 추가) |
| `templates/nurse/patient-detail.mustache` | 수정 (물품 사용 섹션 + 사용 내역 추가) |

---

## 작업 목록

### 1. NursePatientDto — canUseItem 필드 추가

```java
// TODO [W3-6]: canUseItem 필드 추가
// private final boolean canUseItem;
// this.canUseItem = r.getStatus() == ReservationStatus.RECEIVED;
```

### 2. NurseReceptionController — 수정

```java
// TODO [W3-6]: ItemManagerService 주입

// TODO [W3-6]: patientDetail()에 items, usageLogs 모델 추가
// model.addAttribute("items", itemManagerService.getItemList(null));
// model.addAttribute("usageLogs", itemManagerService.getUsageLogs(id));

// TODO [W3-6]: POST /nurse/item/use AJAX 엔드포인트 추가
// - @RequestParam("id") Long id (itemId)
// - @RequestParam("amount") String amountStr
// - @RequestParam(required = false) Long reservationId
// - 검증: 음수·소수·문자 차단
// - 성공: {"quantity": N}
// - 재고 부족·오류: 400 + {"error": "..."}
```

### 3. patient-detail.mustache — 물품 사용 섹션 + 사용 내역 추가

```html
<!-- TODO [W3-6]: 환자 정보 폼 닫는 </div> 아래, footer 위에 추가 -->

<!-- 사용 물품 내역 (항상 표시) -->
<!-- {{#usageLogs}} 있으면 테이블, {{^usageLogs}} 없으면 "없습니다." -->

<!-- 물품 사용 섹션 ({{#detail.canUseItem}}만 표시) -->
<!-- 카테고리 필터 버튼 (전체/의료소모품/의료기기/사무비품) -->
<!-- 초성·영문 검색 입력 -->
<!-- 물품 카드 그리드 4열: 물품명 | 카테고리 | 재고 | 수량입력 + 사용 버튼 -->
<!-- AJAX: 성공 → 재고 갱신 / 재고 부족 → alert -->
```

---

## 상세 구현 계획

### canUseItem 조건
- `RECEIVED` 상태일 때만 물품 사용 폼 표시
- 사용 내역(usageLogs)은 상태 관계없이 항상 표시

### 레이아웃
1. 기존 환자 정보 카드 (변경 없음)
2. **사용 물품 내역** — 항상 표시 (카드 바깥, 별도 섹션)
3. **물품 사용 섹션** — `{{#detail.canUseItem}}`만 표시

### 재사용
- `ItemManagerService.useItem()`, `getItemList()`, `getUsageLogs()` — 기존 그대로
- AJAX JS 패턴, 카테고리 필터, 초성 검색 — doctor treatment-detail과 동일 구조

### AJAX 엔드포인트
- URL: `POST /nurse/item/use`
- 파라미터: `id` (itemId), `amount`, `reservationId`
- 응답: `{"quantity": N}` or `{"error": "..."}`

---

## 금지 사항 체크

- [x] `config/`, `domain/` 수정 없음
- [x] `admin/**` 수정 없음
- [x] `doctor/**` 수정 없음
