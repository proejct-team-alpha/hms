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
