# task-031 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-031.md` 확인
- [x] `doc/dev-c/workflow/workflow-031.md` 확인
- [x] `doc/dev-c/.person/reports/task-031/` 보고서 확인

## 작업 목표

- 관리자 예약 목록에 환자명/연락처 기준 단일 `keyword` 검색을 추가한다.
- 관리자 환자 목록 검색도 기존 `nameKeyword`, `contactKeyword`에서 단일 `keyword`로 통합한다.
- 예약 목록과 환자 목록의 검색 UI를 병원 규칙 관리 화면 패턴과 유사한 검색형 목록 톤으로 정리한다.
- 검색/필터 조건을 유지한 페이지네이션과 범위 테스트까지 확보해 `admin.reservation`, `admin.patient` 회귀를 막는다.

## 보고서 소스

- `report-20260323-0001-task-31-1.md`
- `report-20260323-0002-task-31-2.md`
- `report-20260323-1248-task-31-3.md`
- `report-20260323-1255-task-31-4.md`
- `report-20260323-1410-task-31-5.md`
- `report-20260323-1430-task-31-6.md`
- `report-20260323-1432-task-31-7.md`
- `report-20260323-1437-task-31-8.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationController.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationService.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationRepository.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationApiController.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/dto/AdminReservationListResponse.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientController.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientService.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientRepository.java`
- `src/main/java/com/smartclinic/hms/admin/patient/dto/AdminPatientListResponse.java`
- `src/main/resources/templates/admin/reservation-list.mustache`
- `src/main/resources/templates/admin/patient-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationServiceTest.java`
- `src/test/java/com/smartclinic/hms/admin/patient/AdminPatientControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/patient/AdminPatientServiceTest.java`
- `doc/dev-c/task/task-031.md`
- `doc/dev-c/workflow/workflow-031.md`

## 구현 내용

### 1. 예약/환자 목록 검색 구조 점검과 계약 고정

- 예약 목록은 기존에 `page`, `size`, `status`만 받고 있어 `keyword` 검색을 추가하려면 컨트롤러, 서비스, 리포지토리, 템플릿을 함께 확장해야 한다는 점을 먼저 정리했다.
- 환자 목록은 `nameKeyword`, `contactKeyword`를 분리해 받고 있어 관리자 검색형 목록 UX가 병원 규칙 관리 화면과 다르다는 점을 확인했다.
- 병원 규칙 관리 검색 바를 기준 패턴으로 삼아, 예약 목록은 `status + keyword`, 환자 목록은 단일 `keyword` 중심 화면으로 맞추는 방향을 문서로 고정했다.

### 2. 예약 목록 `keyword` 검색 백엔드와 UI 구현

- `AdminReservationController`는 `GET /admin/reservation/list`와 취소 후 redirect 모두에서 `keyword`를 함께 받도록 확장했다.
- `AdminReservationService`는 `getReservationList(page, size, status, keyword)` 시그니처로 바꾸고, 이름 검색용 `trim()` 값과 연락처 비교용 하이픈 제거 값을 분리해 정규화했다.
- `AdminReservationRepository`는 `patient.name LIKE`와 `REPLACE(patient.phone, '-', '') LIKE`를 상태 필터와 함께 조합하는 조회를 지원하도록 바뀌었다.
- `AdminReservationListResponse`에는 `keyword`를 포함시켜 템플릿과 페이지 링크 생성에서 그대로 사용할 수 있게 했다.
- `reservation-list.mustache`는 검색 입력 1개 + 상태 select + 조회/초기화 버튼 구조로 정리하고, hidden field와 redirect 파라미터로 검색 상태가 유지되도록 맞췄다.

### 3. 환자 목록 검색 파라미터를 단일 `keyword`로 통합

- `AdminPatientController`는 더 이상 `nameKeyword`, `contactKeyword`를 따로 받지 않고 `keyword` 하나만 받도록 단순화했다.
- `AdminPatientService`는 `getPatientList(page, size, keyword)`로 정리하고, 내부에서 이름 검색용 값과 연락처 비교용 값을 각각 정규화하도록 바꿨다.
- `AdminPatientRepository`는 `이름 LIKE OR 정규화된 연락처 LIKE` 구조로 바뀌어 단일 입력으로 이름/연락처 검색을 모두 처리하게 됐다.
- `AdminPatientListResponse`는 `nameKeyword`, `contactKeyword` 대신 `keyword` 하나만 유지하도록 정리됐다.

### 4. 환자 목록 UI 패턴 통일

- `patient-list.mustache`는 예약 목록과 같은 검색형 목록 패턴으로 정리해 검색 입력 1개 + 조회 버튼 + 초기화 버튼 구조를 갖추게 됐다.
- 상단 설명 문구, 총 건수/현재 페이지 영역, 레이아웃 구조를 예약 목록과 비슷한 톤으로 맞춰 관리자 화면 일관성을 높였다.
- 검색 요약 줄은 제거하고 입력창 value만 유지해 화면 무게를 줄였다.
- `AdminPatientController`의 페이지 타이틀 한글 상수도 정상화해 화면 제목이 깨지지 않도록 함께 보강했다.

### 5. 테스트 보강과 선행 결함 정리

- `AdminReservationControllerTest`는 `keyword` 파라미터 전달과 keyword 유지 렌더링, 취소 후 keyword 유지 redirect를 검증하도록 확장됐다.
- `AdminReservationServiceTest`에는 환자명 검색, 연락처 정규화 검색, 상태+검색어 조합, 페이지 링크/상태 옵션 URL의 `status`와 `keyword` 유지 시나리오가 추가됐다.
- `AdminPatientControllerTest`는 단일 `keyword` 파라미터 전달, 입력창 value 유지, 새 설명 문구와 placeholder, 조회/초기화 버튼, 빈 결과 메시지 기준으로 정리됐다.
- `AdminPatientServiceTest`에는 이름 검색, 연락처 검색, 빈 결과, 페이지 링크의 `keyword` 유지 검증이 추가됐다.
- 범위 테스트 중 검색 기능과 무관하게 실패하던 `admin.reservation` API 테스트를 복구하기 위해, 주석 처리돼 있던 `AdminReservationApiController`의 취소 엔드포인트를 다시 살렸다.

### 6. 문서와 최종 검증 마무리

- `workflow-031`과 `task-031`을 현재 구현 상태 기준으로 완료 처리했다.
- 예약 목록은 `status + keyword`, 환자 목록은 단일 `keyword` 검색이라는 차이와 공통 UI 패턴을 문서에 함께 정리해 이후 작업자가 바로 이해할 수 있게 맞췄다.
- `Task 31-7`과 `Task 31-8` 구현 메모, 리뷰 포인트, 완료 기준을 현재 코드/테스트 상태와 맞춰 동기화했다.
- 범위 테스트 뒤 전체 `./gradlew test`까지 다시 실행해 프로젝트 전체 기준으로도 회귀가 없음을 확인했다.

## 검증 결과

- 구조 점검/설계 단계(`Task 31-1`, `Task 31-2`)는 문서 정리 중심이라 별도 테스트 실행 없음
- `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.*'` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.admin.patient.*'` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.*' --tests 'com.smartclinic.hms.admin.patient.*'` : `BUILD SUCCESSFUL`
- `./gradlew test` : `BUILD SUCCESSFUL`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-031.md`
- 로컬: `doc/dev-c/workflow/workflow-031.md`

## 남은 TODO / 리스크

- 현재 `workflow-031` / `task-031` 범위는 문서와 테스트까지 포함해 완료 상태다.
- 이번 범위 기준 known issue는 없다.
- 이후 후속 확장 후보는 예약번호/진료과/의사까지 넓히는 검색 범위 확장, 관리자 검색형 목록 공통 컴포넌트화 정도다.
