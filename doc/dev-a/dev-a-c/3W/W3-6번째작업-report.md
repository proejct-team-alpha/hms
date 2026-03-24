# W3-6번째작업 리포트 - 간호사 환자 정보관리 물품 사용(출고) 기능

## 작업 개요
- **작업명**: 간호사 patient-detail 페이지 하단에 물품 사용 섹션 추가 (RECEIVED 환자만 표시, 사용 내역은 항상 표시)
- **수정 파일**: `nurse/dto/NursePatientDto.java`, `nurse/NurseReceptionController.java`, `templates/nurse/patient-detail.mustache`

## 작업 내용

### 1. NursePatientDto — canUseItem 필드 추가

`canUseItem = r.getStatus() == ReservationStatus.RECEIVED`. RECEIVED 상태일 때만 물품 사용 폼 표시 조건으로 사용.

```java
private final boolean canUseItem;
// 생성 시 할당
this.canUseItem = r.getStatus() == ReservationStatus.RECEIVED;
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `RECEIVED` 상태일 때만 `true`가 되는 boolean 필드를 추가합니다.
> - **왜 이렇게 썼는지**: Mustache에서 Java enum을 직접 비교할 수 없어 DTO에서 미리 boolean으로 계산합니다.
> - **쉽게 말하면**: "물품 사용 폼을 보여줄지 말지"를 참/거짓으로 저장하는 필드입니다.

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

## 테스트 결과

| 항목 | 결과 |
|------|------|
| RECEIVED 환자 — 물품 사용 섹션 + 사용 내역 표시 | ✅ |
| 다른 상태 환자 — 사용 물품 내역만 표시 | ✅ |
| 카테고리 필터·초성 검색 | ✅ |
| AJAX 출고 성공 — 재고 즉시 갱신 | ✅ |
| 재고 부족 — alert 팝업 | ✅ |

## 특이사항
- `NurseReceptionController`에 `ItemManagerService` 주입: 간호사 컨트롤러가 물품 서비스도 사용하게 됨
- 사용 이력은 어떤 상태에서도 조회할 수 있어야 함 — 진료 완료 후에도 확인 필요
