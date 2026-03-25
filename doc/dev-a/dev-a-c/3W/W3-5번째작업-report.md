# W3-5번째작업 리포트 - 의사 진료실 물품 사용(출고) 기능

## 작업 개요
- **작업명**: 의사 진료 기록 화면에 물품 사용 섹션 추가 (카테고리 필터·초성 검색·AJAX 출고·사용 이력 기록)
- **수정 파일**: `item/log/ItemUsageLog.java`(신규), `item/log/ItemUsageLogRepository.java`(신규), `item/log/ItemUsageLogDto.java`(신규), `item/ItemManagerService.java`, `item/dto/ItemListDto.java`, `doctor/treatment/DoctorTreatmentController.java`, `doctor/treatment/DoctorTreatmentService.java`, `templates/doctor/treatment-detail.mustache`, `templates/doctor/completed-list.mustache`, `resources/sql_test.sql`

## 작업 내용

### 1. ItemUsageLog — 신규 엔티티 & 관련 클래스

- `item/log/ItemUsageLog.java`: 물품 사용 이력 엔티티 (`item_usage_log` 테이블)
  - 필드: `reservationId`, `itemId`, `itemName`, `amount`, `usedAt`
  - 팩토리 메서드: `ItemUsageLog.of(reservationId, itemId, itemName, amount)`
- `item/log/ItemUsageLogRepository.java`: JPA 리포지토리
  - `findByReservationIdOrderByUsedAtAsc(Long reservationId)`
- `item/log/ItemUsageLogDto.java`: 뷰용 DTO
  - `itemName`, `amount`, `usedAt` (HH:mm 포맷)

### 2. ItemManagerService — 출고 메서드 추가

- `useItem(Long id, int amount, Long reservationId)`:
  - 재고 부족 시 `"재고가 N개 부족합니다."` 예외 발생
  - `item.updateQuantity(newQuantity)` 재고 차감
  - `reservationId` 있으면 `ItemUsageLog` 저장
  - return 새 재고 수량
- `getUsageLogs(Long reservationId)`: 예약별 물품 사용 이력 조회

```java
@Transactional
public int useItem(Long id, int amount, Long reservationId) {
    Item item = itemRepository.findById(id).orElseThrow();
    int newQty = item.getQuantity() - amount;
    if (newQty < 0) throw new IllegalStateException("재고가 " + (-newQty) + "개 부족합니다.");
    item.updateQuantity(newQty);
    itemUsageLogRepository.save(ItemUsageLog.of(reservationId, id, item.getName(), amount));
    return newQty;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 물품 사용(출고)을 처리하고 이력을 저장합니다. 재고가 부족하면 예외를 발생시킵니다.
> - **왜 이렇게 썼는지**: 서비스 계층에서 재고 부족 예외를 던지면, 컨트롤러가 이를 잡아 400 응답으로 변환해 프론트엔드에서 alert로 안내할 수 있습니다.
> - **쉽게 말하면**: 재고가 충분하면 줄이고, 부족하면 "N개 부족합니다"라고 알려주는 기능입니다.

### 3. ItemListDto — category 필드 추가

`category`: enum name(예: `"MEDICAL_SUPPLIES"`) → 카테고리 JS 필터용 `data-category` 속성에 활용.

### 4. DoctorTreatmentController — 수정

- `treatmentDetail()`: `items`, `usageLogs` 모델 추가
- `POST /doctor/item/use`: 물품 사용 AJAX 엔드포인트 (amount 검증 + `useItem()` 호출)
- `GET /doctor/completed-detail`: 진료 완료 목록 전용 엔드포인트 신규 추가

### 5. DoctorTreatmentService — 쿼리 변경

`getTreatmentPage()`: `status <> CANCELLED` → `status = RECEIVED` 변경. 오늘의 진료 목록에 진료 대기 환자만 표시.

### 6. treatment-detail.mustache — 물품 사용 섹션 + 완료 뷰 개선

**물품 사용 섹션 (`{{#detail.canComplete}}`)**
- 카테고리 필터 버튼 4종 (전체 / 의료소모품 / 의료기기 / 사무비품)
- 검색 입력: 한글 초성 + 영어 대소문자 구분 없이 검색 가능
- 물품 카드 그리드 (4열): 물품명, 카테고리, 재고, 수량 입력 + 사용 버튼
- AJAX: 사용 성공 시 카드 재고 수량 즉시 갱신, 재고 부족 시 `alert()` 팝업

**완료 진료 뷰 (`{{^detail.canComplete}}`)**
- 진단 내용·처방 내역·특이사항 읽기 전용 표시
- 사용 물품 내역: 이력 있으면 테이블, 없으면 "사용된 물품이 없습니다." 표시

## 테스트 결과

| 항목 | 결과 |
|------|------|
| 진료 중 — 물품 사용 섹션 표시 | ✅ |
| 진료 완료 — 사용 이력 읽기 전용 | ✅ |
| 카테고리 필터·초성 검색 | ✅ |
| AJAX 출고 성공 — 재고 즉시 갱신 | ✅ |
| 재고 부족 — alert 팝업 | ✅ |
| 오늘의 진료 목록 — RECEIVED만 표시 | ✅ |

## 특이사항
- `ItemUsageLog`와 `ItemStockLog`는 다른 목적: 사용 이력은 예약(환자)과 연결, 재고 변동 이력은 집계 목적
- `canComplete` boolean을 DTO에서 미리 계산: Mustache에서 Java enum 상태를 직접 비교할 수 없어 서비스에서 변환
- 오늘의 진료 목록 쿼리 변경(`<> CANCELLED` → `= RECEIVED`): 진료 대기 환자만 명확히 표시하기 위해 변경
