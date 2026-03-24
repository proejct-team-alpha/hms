# W3-6번째작업 Report — 간호사 환자 정보관리 물품 사용(출고) 기능

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. NursePatientDto — canUseItem 필드 추가

- `canUseItem = r.getStatus() == ReservationStatus.RECEIVED`
- RECEIVED 상태일 때만 물품 사용 폼 표시 조건으로 사용

### 2. NurseReceptionController — 수정

- `ItemManagerService` 주입
- `patientDetail()`: `items`, `usageLogs` 모델 추가
- `POST /nurse/item/use` AJAX 엔드포인트 추가
  - amount 파싱 및 범위 검증 (음수·소수·문자 차단)
  - 성공: `{"quantity": N}` 반환
  - 재고 부족·오류: 400 + `{"error": "..."}`

### 3. patient-detail.mustache — 하단 섹션 2개 추가

**사용 물품 내역 (항상 표시)**
- 사용 이력 있으면 테이블 (물품명 / 수량 / 시간)
- 사용 이력 없으면 "사용된 물품이 없습니다." 표시

**물품 사용 섹션 (`{{#detail.canUseItem}}` — RECEIVED만)**
- 카테고리 필터 버튼 4종 (전체 / 의료소모품 / 의료기기 / 사무비품)
- 초성·영문 대소문자 구분 없는 물품명 검색
- 물품 카드 그리드 (4열): 물품명, 카테고리, 재고, 수량 입력 + 사용 버튼
- AJAX: 사용 성공 시 카드 재고 즉시 갱신, 재고 부족 시 `alert()` 팝업

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `nurse/dto/NursePatientDto.java` | `canUseItem` 필드 추가 |
| `nurse/NurseReceptionController.java` | `ItemManagerService` 주입, `patientDetail()` 수정, `POST /nurse/item/use` 추가 |
| `templates/nurse/patient-detail.mustache` | 사용 물품 내역 + 물품 사용 섹션 추가 |

---

## 결과

- RECEIVED 환자: 물품 사용 섹션 + 사용 내역 모두 표시
- 다른 상태 환자: 사용 물품 내역만 표시
- 카테고리 필터·초성 검색·AJAX 출고 정상 작동

---

> **💡 입문자 설명**
>
> **`canUseItem = r.getStatus() == ReservationStatus.RECEIVED` — DTO에서 계산하는 이유**
> - 간호사 화면에서 물품 사용 폼을 보여줄지 말지를 상태값으로 판단합니다. `RECEIVED` 상태는 접수됐지만 아직 진료가 시작되지 않은 환자입니다. 진료 완료나 취소 상태에서는 물품을 더 사용할 수 없으므로 폼을 숨깁니다.
> - Mustache는 Java 코드 표현식을 쓸 수 없으므로, 서비스나 DTO에서 미리 계산한 boolean을 `{{#detail.canUseItem}}`처럼 씁니다.
>
> **`NurseReceptionController`에 `ItemManagerService`를 주입한 이유**
> - 간호사 컨트롤러는 원래 예약 접수 관련 기능만 담당했습니다. 물품 사용 기능을 추가하면서 물품 서비스(`ItemManagerService`)도 필요해졌습니다. Spring의 `@Autowired`(또는 생성자 주입)로 여러 서비스를 하나의 컨트롤러에서 사용할 수 있습니다.
>
> **사용 물품 내역을 항상 표시하는 이유**
> - 물품 사용 폼은 RECEIVED 상태에서만 표시하지만, 사용 이력은 어떤 상태에서도 조회할 수 있어야 합니다. 진료가 완료된 후에도 "이 환자에게 어떤 물품을 사용했는지" 확인이 필요하기 때문입니다.
>
> **쉽게 말하면**: 간호사가 담당 환자 상세 화면에서 직접 물품(붕대, 주사기 등)을 사용 처리할 수 있게 한 기능입니다. 환자가 접수 대기 중일 때만 사용 가능하고, 기록은 언제든 볼 수 있습니다.
