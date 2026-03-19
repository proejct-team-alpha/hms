# task-022 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-022.md` 확인
- [x] `doc/dev-c/workflow/workflow-022.md` 확인
- [x] `doc/dev-c/.person/reports/task-022/` 보고서 확인

## 작업 목표

- 관리자 환자 관리 기능을 `admin.patient` 범위에서 목록/상세 화면으로 구현한다.
- 환자 관리 메인은 검색 중심 인덱스 화면으로 유지하고, 상세 정보와 전체 예약 이력은 상세 화면으로 분리한다.
- 이름/연락처 검색, 기본 20건 페이징, 환자 1명당 1줄 목록 구조를 안정적으로 제공한다.
- 없는 `patientId` 요청, 빈 결과 화면, 관리자 사이드바 진입점까지 포함해 실제 운영 흐름을 닫는다.

## 보고서 소스

- `report-20260319-1527-task-22-1.md`
- `report-20260319-1534-task-22-2.md`
- `report-20260319-1543-task-22-3.md`
- `report-20260319-1556-task-22-4.md`
- `report-20260319-1603-task-22-5.md`
- `report-20260319-1612-task-22-6.md`
- `report-20260319-1618-admin-patient-sidebar-menu.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientController.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientService.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientRepository.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientSummary.java`
- `src/main/java/com/smartclinic/hms/admin/patient/dto/AdminPatientListResponse.java`
- `src/main/java/com/smartclinic/hms/admin/patient/dto/AdminPatientPageLinkResponse.java`
- `src/main/java/com/smartclinic/hms/admin/patient/dto/AdminPatientDetailResponse.java`
- `src/main/java/com/smartclinic/hms/admin/patient/dto/AdminPatientReservationHistoryItemResponse.java`
- `src/main/java/com/smartclinic/hms/common/interceptor/LayoutModelInterceptor.java`
- `src/main/resources/templates/admin/patient-list.mustache`
- `src/main/resources/templates/admin/patient-detail.mustache`
- `src/main/resources/templates/common/sidebar-admin.mustache`
- `src/test/java/com/smartclinic/hms/admin/patient/AdminPatientControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/patient/AdminPatientServiceTest.java`
- `src/test/java/com/smartclinic/hms/common/interceptor/LayoutModelInterceptorTest.java`
- `doc/dev-c/task/task-022.md`
- `doc/dev-c/workflow/workflow-022.md`

## 구현 내용

### 1. 환자 관리 범위와 조회 기준 점검

- 환자 관리 화면은 예약 목록의 변형이 아니라 `Patient` 기준 목록이어야 한다는 점을 먼저 정리했다.
- 현재 시스템은 전화번호로 기존 환자를 재사용하고 `Reservation -> Patient`로 연결되므로, 같은 환자가 여러 예약을 가져도 목록은 환자 1명당 1줄이어야 한다는 방향을 문서로 고정했다.
- 없는 `patientId` 상세 요청은 관리자 SSR 패턴에 맞춰 404 화면으로 처리하는 기준도 이 단계에서 잡았다.

### 2. `admin.patient` 기본 구조와 응답 DTO 설계

- `AdminPatientController`, `AdminPatientService`, `AdminPatientRepository` 스켈레톤을 추가해 환자 관리 모듈 뼈대를 만들었다.
- 목록 응답은 `AdminPatientListResponse`, `AdminPatientPageLinkResponse`, `AdminPatientSummary`로, 상세 응답은 `AdminPatientDetailResponse`, `AdminPatientReservationHistoryItemResponse`로 분리했다.
- 목록 화면은 검색 조건, 총 건수, page/size, pageLinks, 이전/다음 URL까지 포함하는 기존 관리자 목록 패턴을 그대로 따르도록 정리했다.
- 새 자바 파일 생성 과정에서 BOM 인코딩 이슈가 있었고, UTF-8 without BOM으로 다시 저장해 컴파일 문제를 정리했다.

### 3. 환자 목록 검색과 페이징 구현

- `GET /admin/patient/list`를 추가해 `page`, `size`, `nameKeyword`, `contactKeyword`를 받도록 연결했다.
- 이름은 부분 일치, 연락처는 하이픈 제거 후 비교하는 방식으로 검색을 구현했다.
- 기본 페이지 크기는 20건, 기본 정렬은 `createdAt desc, id desc`로 맞춰 최신 등록 환자가 먼저 보이도록 했다.
- `patient-list.mustache`에는 페이지 설명, 검색 폼, 결과 요약, 환자 목록 테이블, 빈 상태 메시지, 페이지네이션을 반영해 검색형 인덱스 화면 역할에 집중시켰다.

### 4. 환자 상세와 전체 예약 이력 구현

- `GET /admin/patient/detail?patientId={id}`를 추가해 환자 기본 정보와 전체 예약 이력을 함께 보여주도록 구현했다.
- 예약 이력은 `Reservation -> Doctor -> Staff`, `Reservation -> Department`를 함께 조회해 예약번호, 날짜, 시간, 진료과, 의사, 상태 문구를 상세 화면에서 읽을 수 있게 정리했다.
- 상세 응답에서는 이메일, 주소, 메모가 비어 있어도 `-` 기본값으로 보정해 템플릿이 흔들리지 않게 했다.
- 없는 `patientId` 요청은 404 상태와 `error/404` 화면으로 처리하도록 맞췄다.

### 5. 목록/상세 템플릿 역할 분리와 테스트 보강

- `patient-list.mustache`는 `환자 관리 > 환자 목록` 톤으로 정리하고, 목록은 가볍게 찾는 화면이라는 점이 드러나도록 문구를 다듬었다.
- `patient-detail.mustache`는 기본 정보 카드와 예약 이력 영역을 분리해 읽기 중심 상세 화면 역할을 더 분명하게 만들었다.
- `AdminPatientControllerTest`, `AdminPatientServiceTest`를 추가 및 보강해 목록 검색, 연락처 정규화, 최신 등록순 정렬, 빈 결과, 상세 조회, 없는 `patientId`, 예약 이력 매핑을 검증했다.
- `workflow-022.md`, `task-022.md`를 완료 상태로 갱신하고 PR 리뷰 포인트와 후속 UX 개선 메모를 문서에 반영했다.

### 6. 관리자 사이드바 환자 관리 진입점 연결

- 관리자 사이드바에 `환자 관리` 메뉴를 추가하고 `/admin/patient/list`로 연결했다.
- `LayoutModelInterceptor`에 `isAdminPatient` 플래그를 추가해 기존 관리자 메뉴와 같은 active 표시 패턴을 맞췄다.
- `LayoutModelInterceptorTest`로 `/admin/patient` 경로에서 active 플래그가 정상 주입되는지 검증해 실제 관리자 동선까지 마무리했다.

## 검증 결과

- 구조 점검 단계: 별도 테스트 실행 없이 엔티티/리포지토리/현재 구현 상태를 확인
- `./gradlew compileJava` : `BUILD SUCCESSFUL`
- 참고: 새 `admin.patient` 자바 파일에 BOM이 포함되어 한 번 실패했고, UTF-8 without BOM으로 재저장 후 정상 통과
- `./gradlew test --tests 'com.smartclinic.hms.admin.patient.*'` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.common.interceptor.LayoutModelInterceptorTest'` : 통과
- `./gradlew test --tests 'com.smartclinic.hms.admin.patient.AdminPatientControllerTest'` : 통과
- `./gradlew test` : `BUILD SUCCESSFUL`
- 참고: 중간 한 번의 전체 테스트 실패는 코드 이슈가 아니라 병렬 실행으로 인한 Gradle 결과 파일 충돌이었고, 순차 재실행에서는 정상 통과

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-022.md`
- 로컬: `doc/dev-c/workflow/workflow-022.md`

## 남은 TODO / 리스크

- 현재 `workflow-022` / `task-022` 기준 기본 환자 관리 범위는 완료 상태다.
- 예약 목록에서 환자명 클릭을 환자 상세로 바로 연결하는 UX 개선은 후속 작업으로 남아 있다.
- 연락처 검색 정규화 기준은 현재 전화번호 기반 환자 식별 규칙과 맞지만, 이후 동일 환자 판단 규칙이 바뀌면 함께 재검토가 필요하다.
