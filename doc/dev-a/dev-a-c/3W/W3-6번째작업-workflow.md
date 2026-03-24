# W3-6번째작업 Workflow — 간호사 환자 정보관리 물품 사용(출고) 기능

> **작성일**: 3W
> **목표**: 간호사 patient-detail 페이지 하단에 물품 사용 섹션 추가 (RECEIVED 환자만 표시, 사용 내역은 항상 표시)

---

## 전체 흐름

```
NursePatientDto에 canUseItem 필드 추가
  → NurseReceptionController 수정 (items·usageLogs 모델, AJAX 엔드포인트)
  → patient-detail.mustache 하단에 사용 물품 내역 + 물품 사용 섹션 추가
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 간호사 환자 상세 페이지 하단에 물품 사용 기능 추가 |
| 물품 사용 표시 조건 | `canUseItem=true` (RECEIVED 상태 환자)일 때만 표시 |
| 사용 이력 표시 | 항상 표시 (상태 무관) |
| 카테고리 필터·검색 | W3-5(의사)와 동일 구조 재사용 |
| 서비스 재사용 | `ItemManagerService.useItem()`, `getItemList()`, `getUsageLogs()` 그대로 재사용 |

---

## 실행 흐름

```
GET /nurse/patient/{id}
  → NurseReceptionController: items, usageLogs 모델 추가
  → patient-detail.mustache 렌더링
    - 사용 물품 내역 (항상 표시)
    - {{#detail.canUseItem}} 물품 사용 섹션 표시

사용 버튼 클릭 (AJAX)
  → fetch POST /nurse/item/use (itemId, amount, reservationId)
  → ItemManagerService.useItem() → 재고 차감 + ItemUsageLog 저장
  → 성공: {"quantity": N} / 재고 부족: 400 + {"error": "..."}
```

---

## UI Mockup

```
┌────────────────────────────────────────────┐
│ [환자 정보 폼]                              │
├────────────────────────────────────────────┤
│ 사용 물품 내역 (항상 표시)                   │
│ ┌──────────┬──────┬──────┐                 │
│ │ 물품명    │ 수량  │ 시간  │                 │
│ │ 붕대      │  2개  │ 09:15 │                 │
│ └──────────┴──────┴──────┘                 │
├────────────────────────────────────────────┤
│ 물품 사용 (RECEIVED 상태만 표시)             │
│ [전체] [의료소모품] [의료기기] [사무비품]     │
│ [검색: 물품명 입력...]                      │
│ 물품 카드 그리드 (4열)                       │
└────────────────────────────────────────────┘
```

---

## 작업 목록

1. `NursePatientDto` — `canUseItem` 필드 추가 (`RECEIVED` 상태 여부)
2. `NurseReceptionController` — `ItemManagerService` 주입, items·usageLogs 모델 추가, `POST /nurse/item/use` 엔드포인트 추가
3. `patient-detail.mustache` — 사용 물품 내역 섹션 + 물품 사용 섹션 추가

---

## 작업 진행내용

- [x] NursePatientDto `canUseItem` 필드 추가
- [x] NurseReceptionController `ItemManagerService` 주입
- [x] `patientDetail()` items·usageLogs 모델 추가
- [x] `POST /nurse/item/use` AJAX 엔드포인트 추가
- [x] patient-detail.mustache 사용 물품 내역 섹션 추가 (항상 표시)
- [x] patient-detail.mustache 물품 사용 섹션 추가 (RECEIVED만 표시)

---

## 실행 흐름에 대한 코드

### 1. NursePatientDto — canUseItem 필드 추가

```java
// NursePatientDto 생성자 또는 팩토리 메서드에서
private final boolean canUseItem;

// 예약 상태가 RECEIVED일 때만 물품 사용 폼 표시
this.canUseItem = r.getStatus() == ReservationStatus.RECEIVED;
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 간호사 환자 정보 DTO에 `canUseItem`이라는 boolean 필드를 추가합니다. 예약 상태가 `RECEIVED`일 때만 `true`가 됩니다.
> - **왜 이렇게 썼는지**: Mustache는 Java 코드 표현식을 쓸 수 없으므로, 서비스나 DTO에서 미리 계산한 boolean을 `{{#detail.canUseItem}}`처럼 씁니다.
> - **쉽게 말하면**: "물품 사용 가능 여부"를 참/거짓으로 저장하는 필드입니다.

### 2. NurseReceptionController — AJAX 엔드포인트 추가

```java
// POST /nurse/item/use — 물품 사용 AJAX 엔드포인트
@PostMapping("/item/use")
@ResponseBody
public ResponseEntity<?> useItem(
        @RequestParam(name = "id") Long itemId,
        @RequestParam(name = "amount") String amountStr,
        @RequestParam(name = "reservationId", required = false) Long reservationId) {
    try {
        int amount = Integer.parseInt(amountStr.trim());
        if (amount < 1) throw new IllegalArgumentException("수량 오류");
        int newQty = itemManagerService.useItem(itemId, amount, reservationId);
        return ResponseEntity.ok(Map.of("quantity", newQty));
    } catch (IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "처리 중 오류가 발생했습니다."));
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 간호사 화면에서 물품 사용 버튼을 눌렀을 때 재고를 차감하는 AJAX 엔드포인트입니다. `required = false`는 `reservationId`가 없어도 오류 없이 동작한다는 의미입니다.
> - **왜 이렇게 썼는지**: 간호사와 의사 모두 동일한 `ItemManagerService.useItem()`을 재사용해 중복 코드를 줄입니다. 역할마다 URL(`/nurse/item/use`, `/doctor/item/use`)이 다르므로 별도 엔드포인트가 필요합니다.
> - **쉽게 말하면**: 간호사 화면에서도 물품을 사용할 수 있도록 서버에 창구를 추가한 코드입니다.

### 3. patient-detail.mustache — 두 섹션 추가

```html
<!-- 사용 물품 내역 (항상 표시) -->
<section class="mt-6">
  <h3 class="font-bold text-slate-700 mb-3">사용 물품 내역</h3>
  {{#usageLogs}}
  <table>...</table>
  {{/usageLogs}}
  {{^usageLogs}}
  <p class="text-slate-400 text-sm">사용된 물품이 없습니다.</p>
  {{/usageLogs}}
</section>

<!-- 물품 사용 섹션 (RECEIVED 상태만) -->
{{#detail.canUseItem}}
<section class="mt-6">
  <!-- 카테고리 필터, 검색, 물품 카드 그리드, AJAX JS -->
</section>
{{/detail.canUseItem}}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 환자 상세 페이지 하단에 두 섹션을 추가합니다. 첫 번째는 항상 표시되는 사용 이력, 두 번째는 RECEIVED 상태일 때만 보이는 물품 사용 폼입니다.
> - **왜 이렇게 썼는지**: `{{#usageLogs}}`는 목록이 있을 때, `{{^usageLogs}}`는 없을 때 렌더링하는 Mustache 문법입니다.
> - **쉽게 말하면**: "어떤 물품을 썼나요" 내역과 "물품 사용하기" 폼을 화면 아래에 추가합니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| RECEIVED 환자 | 상세 페이지 접속 | 물품 사용 섹션 + 사용 내역 모두 표시 |
| 다른 상태 환자 | 상세 페이지 접속 | 사용 물품 내역만 표시 |
| 물품 사용 성공 | 유효한 수량 입력 | 재고 즉시 갱신 |
| 재고 부족 | 재고 초과 수량 입력 | alert 팝업 |
| 카테고리 필터·검색 | 탭 버튼·검색 입력 | 필터링 정상 동작 |

---

## 완료 기준

- [x] RECEIVED 환자: 물품 사용 섹션 + 사용 내역 모두 표시
- [x] 다른 상태 환자: 사용 물품 내역만 표시
- [x] 카테고리 필터·초성 검색·AJAX 출고 정상 작동
- [x] `config/`, `domain/` 수정 없음
- [x] `doctor/**` 수정 없음
