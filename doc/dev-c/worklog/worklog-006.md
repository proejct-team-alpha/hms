# worklog-006

## 1) 작업 전 준수 항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] 구현 전 체크리스트 출력 후 작업 진행

## 2) 구현 범위 (workflow-006)
- `GET /admin/reservation/list` DB 연동 목록 조회 구현
- 상태 필터 지원: `ALL`, `RESERVED`, `RECEIVED`, `COMPLETED`, `CANCELLED`
- 상태 파라미터 invalid/missing 시 `ALL` fallback
- 페이징 기본값: `page=1`, `size=10`
- 기본 정렬: `reservationDate DESC`, `timeSlot DESC`
- Mustache 템플릿을 서버 렌더링 기반으로 전환

## 3) 변경 파일
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationController.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationRepository.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationService.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/dto/AdminReservationListItemView.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/dto/AdminReservationListView.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/dto/AdminReservationPageLink.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/dto/AdminReservationStatusOption.java`
- `src/main/resources/templates/admin/reservation-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationServiceTest.java`

## 4) 구현 상세
- Repository
  - `findReservationListPage(status, pageable)` 추가
  - 환자/진료과/의사 조인 기반 목록 Projection 조회
  - 상태 조건은 `:status is null` 패턴으로 `ALL` 처리
- Service
  - `@Transactional(readOnly = true)` 적용
  - `status` 파싱 로직에서 잘못된 값은 `null` 처리(=ALL)
  - 페이징/정렬 생성 및 View DTO 매핑
  - 필터 버튼 URL, 페이지 링크 URL 생성
- Controller
  - 요청 파라미터(`page`, `size`, `status`) 수신
  - 서비스 결과를 `model` 속성으로 템플릿 전달
- Template
  - JS mock 렌더링 제거
  - 서버 데이터(`model`)로 목록/필터/페이지네이션 렌더링
  - 빈 목록 상태 메시지 표시

## 5) 테스트 결과
- 실행 명령:
  - `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.*'`
- 결과:
  - `BUILD SUCCESSFUL`
  - Controller 테스트 2건 통과
  - Service 테스트 3건 통과

## 6) 참조 문서
- 로컬:
  - `AGENTS.md`
  - `.ai/memory.md`
  - `doc/PROJECT_STRUCTURE.md`
  - `doc/RULE.md`
- 워크플로우:
  - `doc/dev-c/workflow/workflow-006.md`

## 7) 남은 TODO / 리스크
- 현재 목록 화면은 조회 전용이며, 취소/상태 변경 POST 기능은 범위 밖
- 추가 필터(기간/진료과/검색어)는 다음 워크플로우에서 확장 가능
