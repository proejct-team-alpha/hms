# worklog-007

## 1) 사전 준수 항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] workflow-007 요구사항 확인 후 체크리스트 먼저 출력

## 2) workflow-007 구현 목표
관리자 예약 목록 페이지에서 `액션` 컬럼을 통해 예약 취소를 수행하고,
확인창 이후 취소가 확정되면 `CANCELLED` 상태로 변경한다.
처리 후 PRG(`redirect:/admin/reservation/list`) + Flash 메시지를 사용한다.

## 3) 구현 내용
- 컨트롤러
  - `POST /admin/reservation/cancel` 추가
  - 파라미터: `reservationId`, `page`, `size`, `status`
  - 성공 시 `successMessage` flash 설정
  - 실패(`CustomException`) 시 `errorMessage` flash 설정
  - 목록으로 리다이렉트하면서 `page/size/status` 유지
- 서비스
  - `cancelReservation(Long reservationId)` 추가
  - 예약 조회 실패 시 not found 예외
  - 도메인 `cancel()` 호출
  - 상태 전이 불가 예외를 비즈니스 예외로 변환
- DTO
  - 목록 행 DTO에 `cancellable` 필드 추가
  - `RESERVED`, `RECEIVED` 상태에서만 취소 버튼 활성화
- 템플릿
  - 예약 목록 테이블에 `액션` 컬럼 추가
  - 활성 행: 취소 버튼 + `confirm(...)`
  - 비활성 행: disabled 버튼
  - Flash 메시지 영역(`successMessage`, `errorMessage`) 추가

## 4) 변경 파일
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationController.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationService.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/dto/AdminReservationItemResponse.java`
- `src/main/resources/templates/admin/reservation-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationServiceTest.java`

## 5) 테스트/검증
- 실행 명령
  - `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.*'`
- 결과
  - `BUILD SUCCESSFUL`
  - 예약 관리 관련 Controller/Service 테스트 통과

## 6) 참조 문서
- 로컬
  - `AGENTS.md`
  - `.ai/memory.md`
  - `doc/PROJECT_STRUCTURE.md`
  - `doc/RULE.md`
- 작업 명세
  - `doc/dev-c/workflow/workflow-007.md`

## 7) 메모
- `액션`은 DB 컬럼이 아니라 화면(UI) 컬럼으로 처리했다.
- 실제 데이터 변경은 예약 엔티티 `status`(`CANCELLED`) 업데이트로 처리했다.