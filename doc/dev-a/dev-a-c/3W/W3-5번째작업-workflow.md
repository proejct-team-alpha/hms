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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 의사 진료 상세 페이지 컨트롤러에 물품 목록(`items`)을 추가하고, 물품 사용(출고) AJAX 요청을 처리할 엔드포인트를 추가합니다. 성공 시 갱신된 재고 수량을 JSON으로 반환하고, 재고 부족 시 HTTP 400(잘못된 요청) 상태 코드와 오류 메시지를 반환합니다.
> - **왜 이렇게 썼는지**: `model.addAttribute`로 뷰 템플릿에 데이터를 전달합니다. 재고 부족은 정상적인 실패 케이스이므로 400 상태 코드와 오류 메시지를 함께 반환해서 프론트엔드에서 `alert`로 사용자에게 안내할 수 있게 합니다.
> - **쉽게 말하면**: 의사 진료 화면에 물품 목록을 보여주고, 물품 사용 버튼을 눌렀을 때 재고를 줄이는 기능을 서버에 추가하는 코드입니다.

### 2. ItemManagerService — 출고 메서드 추가

```java
// TODO [W3-5]: useItem(Long id, int amount) 메서드 추가
//   - item.getQuantity() - amount < 0 → 부족량 계산 후 예외
//   - item.updateQuantity(newQuantity) 호출
//   - return newQuantity
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 물품 사용(출고) 서비스 메서드를 추가합니다. 현재 재고에서 사용 수량을 뺀 결과가 음수이면 재고가 부족하다는 예외를 발생시키고, 그렇지 않으면 재고를 차감하고 새 재고 수량을 반환합니다.
> - **왜 이렇게 썼는지**: 서비스 계층에서 재고 부족을 검사해 예외를 던지면, 컨트롤러에서 해당 예외를 잡아서 400 응답으로 변환할 수 있습니다. `item.updateQuantity()`는 도메인 엔티티의 기존 메서드를 재사용해 도메인 규칙을 일관되게 유지합니다.
> - **쉽게 말하면**: 재고가 충분하면 줄이고, 부족하면 "재고가 N개 부족합니다"라고 알려주는 기능을 추가합니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 진료 중인 환자(`canComplete=true`) 화면에만 물품 사용 섹션을 추가합니다. 카테고리 탭 버튼을 누르면 JavaScript로 해당 카테고리 물품만 필터링해 보여주고, 사용 버튼을 누르면 AJAX로 재고를 차감합니다.
> - **왜 이렇게 썼는지**: `data-category` 속성을 이용한 JavaScript 필터링은 서버 요청 없이 클라이언트 측에서 즉시 동작하므로 사용자 경험이 빠릅니다. `{{#canComplete}}` 조건은 Mustache 문법으로 해당 값이 true일 때만 이 섹션을 렌더링합니다.
> - **쉽게 말하면**: 진료 중인 상태일 때만 물품 목록을 보여주고, 탭 버튼으로 카테고리를 골라 원하는 물품을 빠르게 찾아 사용할 수 있게 합니다.

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
