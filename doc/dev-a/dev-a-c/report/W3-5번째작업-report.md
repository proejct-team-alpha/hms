# W3-5번째작업 Report — 의사 진료실 물품 사용(출고) 기능

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

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

### 3. ItemListDto — category 필드 추가

- `category`: enum name (예: `"MEDICAL_SUPPLIES"`) → 카테고리 JS 필터용 `data-category` 속성에 활용

### 4. DoctorTreatmentController — 수정

- `treatmentDetail()`: `items`, `usageLogs` 모델 추가
- `POST /doctor/item/use`: 물품 사용 AJAX 엔드포인트
  - amount 파싱 및 범위 검증 (음수·소수·문자 차단)
  - 성공: `{"quantity": N}` 반환
  - 재고 부족·오류: 400 + `{"error": "..."}`
- `GET /doctor/completed-detail`: 진료 완료 목록 전용 조회 엔드포인트
  - 사이드바 "진료 완료 목록" 활성화 (`isDoctorCompleted=true`)

### 5. DoctorTreatmentService — 수정

- `getTreatmentPage()`: `status <> CANCELLED` → `status = RECEIVED` 변경
  - 오늘의 진료 목록에 진료 대기 환자만 표시

### 6. treatment-detail.mustache — 물품 사용 섹션 + 완료 뷰 개선

**물품 사용 섹션 (`{{#detail.canComplete}}`)**
- 카테고리 필터 버튼 4종 (전체 / 의료소모품 / 의료기기 / 사무비품)
- 검색 입력: 한글 초성 + 영어 대소문자 구분 없이 검색 가능
- 물품 카드 그리드 (4열): 물품명, 카테고리, 재고, 수량 입력 + 사용 버튼
- AJAX: 사용 성공 시 카드 재고 수량 즉시 갱신, 재고 부족 시 `alert()` 팝업

**완료 진료 뷰 (`{{^detail.canComplete}}`)**
- 진단 내용 / 처방 내역 / 특이사항 읽기 전용 표시
- 사용 물품 내역 섹션: 사용 이력 있으면 테이블, 없으면 "사용된 물품이 없습니다." 표시
- "목록으로 돌아가기" 링크: 완료 환자는 `/doctor/completed-list`로 이동

### 7. completed-list.mustache — 링크 변경

- 환자 카드 클릭 URL: `/doctor/treatment-detail` → `/doctor/completed-detail`
- 사이드바 "진료 완료 목록" 활성 상태 반영

### 8. sql_test.sql — 오늘 날짜 테스트 데이터 추가

- `RES-20260317-000`: 박지호, doctor01, 08:00, COMPLETED (진료 완료 목록 즉시 테스트용)
- `RES-20260317-001`: 김명준, doctor01, 09:00, RECEIVED (오늘의 진료 목록 즉시 테스트용)
- 치료 기록: 각 완료 예약에 `treatment_record` 추가

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `item/log/ItemUsageLog.java` | 신규 생성 |
| `item/log/ItemUsageLogRepository.java` | 신규 생성 |
| `item/log/ItemUsageLogDto.java` | 신규 생성 |
| `item/ItemManagerService.java` | `useItem()` 수정, `getUsageLogs()` 추가 |
| `item/dto/ItemListDto.java` | `category` 필드 추가 |
| `doctor/treatment/DoctorTreatmentController.java` | `treatmentDetail()` 수정, `useItem()` 추가, `completedDetail()` 추가 |
| `doctor/treatment/DoctorTreatmentService.java` | `getTreatmentPage()` 쿼리 변경 |
| `templates/doctor/treatment-detail.mustache` | 물품 사용 섹션 추가, 완료 뷰 개선 |
| `templates/doctor/completed-list.mustache` | 링크 `/completed-detail`로 변경 |
| `resources/sql_test.sql` | 오늘 날짜 테스트 데이터 추가 |

---

## 결과

- 진료 중(`canComplete=true`): 물품 사용 섹션 표시, 카테고리 필터 + 초성 검색 + AJAX 출고 정상 작동
- 진료 완료(`canComplete=false`): 완료 뷰에서 사용 물품 내역 조회 가능
- 오늘의 진료 목록: 진료 대기(RECEIVED) 환자만 표시
- 진료 완료 목록 → 환자 클릭 시 사이드바 "진료 완료 목록" 활성화 및 완료 차트 뷰 표시
