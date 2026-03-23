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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 간호사 환자 정보 DTO(데이터 전달 객체)에 `canUseItem`이라는 boolean 필드를 추가합니다. 예약 상태가 `RECEIVED`(접수 완료)일 때만 `true`가 됩니다.
> - **왜 이렇게 썼는지**: 뷰 템플릿에서 물품 사용 폼 표시 여부를 조건부로 제어할 때, 비즈니스 로직(상태 비교)을 DTO 생성 시점에 처리하면 템플릿은 단순히 `canUseItem` 값만 참조하면 됩니다. 뷰에서 상태 비교 로직을 직접 쓰는 것보다 깔끔합니다.
> - **쉽게 말하면**: 환자가 접수된 상태일 때만 물품 사용 버튼이 보이도록, "물품 사용 가능 여부"를 참/거짓으로 저장하는 필드입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 간호사 환자 상세 페이지 컨트롤러에 물품 목록(`items`)과 해당 환자의 물품 사용 이력(`usageLogs`)을 전달하고, 물품 사용 AJAX 엔드포인트를 추가합니다. `required = false`는 해당 파라미터가 없어도 오류 없이 동작한다는 의미입니다.
> - **왜 이렇게 썼는지**: `reservationId`는 선택적 파라미터(`required = false`)로 지정해서, 예약과 연결할 수 있는 경우에만 이력에 기록되도록 합니다. 간호사와 의사 모두 동일한 `itemManagerService`를 재사용하여 중복 코드를 줄입니다.
> - **쉽게 말하면**: 간호사 화면에서도 물품을 사용할 수 있도록 서버 쪽에 기능을 추가하는 코드입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 간호사 환자 상세 페이지 하단에 두 가지 섹션을 추가합니다. 첫 번째는 어떤 상태의 환자든 항상 표시되는 "사용 물품 내역"이고, 두 번째는 접수 완료(`RECEIVED`) 상태일 때만 보이는 "물품 사용 섹션"입니다.
> - **왜 이렇게 썼는지**: `{{#usageLogs}}`는 목록이 있을 때, `{{^usageLogs}}`는 없을 때 렌더링하는 Mustache 문법입니다. 물품 사용 폼은 접수 완료 환자에게만 의미가 있으므로 `canUseItem` 조건으로 분리합니다.
> - **쉽게 말하면**: 항상 보이는 "어떤 물품을 썼나요" 내역과, 접수된 환자일 때만 보이는 "물품 사용하기" 폼을 화면 아래에 추가합니다.

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
