# W3-5번째작업 Workflow — 의사 진료실 물품 사용(출고) 기능

## 작업 개요

- **목표:** 의사 진료 기록 작성 화면 좌측에 물품 사용 섹션 추가 (카테고리 필터 + 목록 + AJAX 출고)
- **담당:** dev-a-c
- **날짜:** 2026-03-17

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `doctor/treatment/DoctorTreatmentController.java` | 수정 (ItemManagerService 주입, items 모델 추가, AJAX 엔드포인트 추가) |
| `templates/doctor/treatment-detail.mustache` | 수정 (물품 사용 섹션 추가 + JS AJAX 핸들러) |

---

## 작업 목록

### 1. DoctorTreatmentController — 수정

```java
// TODO [W3-5]: ItemManagerService 주입
// TODO [W3-5]: treatmentDetail()에 items 모델 추가
//   model.addAttribute("items", itemManagerService.getItemList(null));
// TODO [W3-5]: POST /doctor/item/use AJAX 엔드포인트 추가
//   - amount 검증 (음수/문자)
//   - itemManagerService로 재고 차감
//   - 성공: {"quantity": N} JSON 반환
//   - 재고 부족: 400 + {"error": "재고가 N개 부족합니다."} 반환
```

### 2. ItemManagerService — 출고 메서드 추가

```java
// TODO [W3-5]: useItem(Long id, int amount) 메서드 추가
//   - item.getQuantity() - amount < 0 → 부족량 계산 후 예외
//   - item.updateQuantity(newQuantity) 호출
//   - return newQuantity
```

### 3. treatment-detail.mustache — 물품 사용 섹션 추가

```html
<!-- TODO [W3-5]: 환자 정보 카드 아래, canComplete 조건 안에 추가 -->
<!-- 카테고리 탭 버튼 (JS로 필터링, 페이지 이동 없음) -->
<!-- 물품 목록 테이블 (data-category 속성으로 JS 필터) -->
<!-- 각 행: 물품명 | 카테고리 | 현재재고 | 수량입력 | 사용 버튼 -->
<!-- JS: 카테고리 버튼 클릭 → 해당 카테고리 행만 표시 -->
<!-- JS: 사용 버튼 클릭 → AJAX POST /doctor/item/use -->
<!--   성공: 해당 행 수량 갱신 -->
<!--   재고 부족: alert("재고가 N개 부족합니다.") -->
```

---

## 상세 구현 계획

### 출고 서비스 로직
- `item.getQuantity() - amount`가 음수이면 `-(newQuantity)`가 부족량
- `item.updateQuantity(newQuantity)` 로 도메인 수정 없이 차감
- return newQuantity

### 카테고리 JS 필터
- 탭 버튼: 전체 / 의료소모품 / 의료기기 / 사무비품
- 각 물품 행: `data-category="MEDICAL_SUPPLIES"` 속성
- 버튼 클릭 시: 전체 행 숨김 → 해당 카테고리 행만 표시

### AJAX 핸들러
- `form[action$="/doctor/item/use"]` submit 이벤트 가로채기
- 음수/문자/소수 → alert 후 return
- 성공: 해당 행 수량 span 갱신
- 실패: `data.error` → alert

---

## 금지 사항 체크

- [x] `config/`, `domain/` 수정 없음 (`updateQuantity()` 기존 메서드 재사용)
- [x] `admin/**` 수정 없음
