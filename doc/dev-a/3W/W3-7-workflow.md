# W3-7 워크플로우 - 코드 리뷰 반영 + 예약 불가 슬롯 비활성화

## 작업 목표
코드 리뷰 8건 반영 + 의사·날짜 선택 시 이미 예약된 시간 슬롯을 `disabled` + "(예약불가)"로 표시해 선택을 막는다.

## 작업 목록

### 파트 1 — 코드 리뷰 반영
<!-- DONE C-05: direct-reservation fetch async/await 전환 + response.ok 검사 -->
<!-- DONE H-01: IDOR 방어 — 예약 취소/변경 시 phone 소유권 검증 -->
<!-- DONE H-02: 예약번호 생성 ReservationNumberGenerator 통일 -->
<!-- DONE H-03: TOCTOU 방어 — 비관적 락(@Lock PESSIMISTIC_WRITE) 적용 -->
<!-- DONE H-06: Admin 전용 쿼리 ReservationRepository에서 제거 -->
<!-- DONE H-13: DTO 이름 변경 (ReservationCreateForm → CreateReservationRequest 등) -->
<!-- DONE M-03: @Pattern 검증 추가 (phone, timeSlot) -->
<!-- DONE M-10: index.mustache 인라인 header/footer → 파셜 교체 -->
<!-- DONE M-11: 진료과 드롭다운 서버 바인딩 -->

### 파트 2 — 예약 불가 슬롯 비활성화
<!-- DONE 1. ReservationRepository — findBookedTimeSlots, findBookedTimeSlotsExcluding 쿼리 추가 -->
<!-- DONE 2. ReservationService — getBookedTimeSlots 오버로드 2개 추가 -->
<!-- DONE 3. ReservationApiController — GET /api/reservation/booked-slots 엔드포인트 추가 -->
<!-- DONE 4. direct-reservation.mustache — data-original-text + resetSlots() + Flatpickr onChange 추가 -->
<!-- DONE 5. reservation-modify.mustache — excludeId 주입 + minDate: 'today' + 동일 슬롯 로직 -->
<!-- DONE 6. ReservationServiceTest — getBookedTimeSlots 테스트 2건 추가 -->

## 수정 파일
- `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationRepository.java`
- `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationService.java`
- `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationApiController.java`
- `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationController.java`
- `src/main/java/com/smartclinic/hms/reservation/reservation/CreateReservationRequest.java` (신규)
- `src/main/java/com/smartclinic/hms/reservation/reservation/UpdateReservationRequest.java` (신규)
- `src/main/resources/templates/reservation/direct-reservation.mustache`
- `src/main/resources/templates/reservation/reservation-cancel.mustache`
- `src/main/resources/templates/reservation/reservation-modify.mustache`
- `src/main/resources/templates/home/index.mustache`
- `src/test/java/com/smartclinic/hms/reservation/reservation/ReservationServiceTest.java`

## 관련 문서
- 설계: `doc/dev-a/3W/W3-7-spec.md`
- 리포트: `doc/dev-a/3W/W3-7-report.md`
