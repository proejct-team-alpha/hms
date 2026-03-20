# task-023 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-023.md` 확인
- [x] `doc/dev-c/workflow/workflow-023.md` 확인
- [x] `doc/dev-c/.person/reports/task-023/` 보고서 확인

## 작업 목표

- 관리자 환자 수정 API를 `POST /admin/api/patients/{id}/update` 기준으로 구현한다.
- 수정 가능 필드를 `name`, `phone`, `note`로 제한하고, `name`/`phone` 공백 입력을 검증한다.
- 전화번호 기반 환자 동일성 규칙을 유지하기 위해 다른 환자와의 전화번호 중복 수정을 차단한다.
- `admin.patient` 범위 API 테스트와 문서를 함께 정리해 범위/전체 검증까지 마무리한다.

## 보고서 소스

- `report-20260319-1652-task-23-1.md`
- `report-20260319-1655-task-23-2.md`
- `report-20260319-1713-task-23-3.md`
- `report-20260319-1717-task-23-4.md`
- `report-20260319-1726-task-23-5.md`
- `report-20260319-1733-task-23-6.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/patient/dto/UpdateAdminPatientApiRequest.java`
- `src/main/java/com/smartclinic/hms/admin/patient/dto/UpdateAdminPatientApiResponse.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientRepository.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientService.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientApiController.java`
- `src/test/java/com/smartclinic/hms/admin/patient/AdminPatientServiceTest.java`
- `src/test/java/com/smartclinic/hms/admin/patient/AdminPatientApiControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/patient/AdminPatientControllerTest.java`
- `doc/dev-c/task/task-023.md`
- `doc/dev-c/workflow/workflow-023.md`

## 구현 내용

### 1. 환자 수정 API 구조와 제약 조건 점검

- `Patient.updateInfo(name, phone, email, address, note)`만 제공되는 현재 엔티티 구조를 확인하고, 부분 수정 API에서는 서비스가 기존 `email`, `address`를 보존해야 한다는 점을 먼저 정리했다.
- 현재 환자 동일성 판단과 재사용 기준이 전화번호라는 점을 다시 확인해, 수정 API에서도 다른 환자와 같은 전화번호로 바꾸는 경우를 반드시 막아야 한다는 방향을 고정했다.
- 관리자 API는 `/admin/api/**`, `@Valid @RequestBody`, `Resp.ok(...)`, `GlobalExceptionHandler` 공통 패턴을 따른다는 점을 기준으로 잡았다.

### 2. 수정 요청/응답 DTO 계약 설계

- `UpdateAdminPatientApiRequest`를 추가해 `name`, `phone`, `note`만 입력으로 받도록 제한했다.
- `name`, `phone`에는 `@NotBlank`를 적용해 공백 입력을 Bean Validation 단계에서 막도록 정리했다.
- `UpdateAdminPatientApiResponse`는 `patientId`, `name`, `phone`, `note`, `message`만 담는 작은 응답 구조로 분리해 상세 DTO와 책임을 나눴다.
- DTO 추가 후 `./gradlew compileJava`로 컴파일을 확인했다.

### 3. 서비스 수정 로직과 전화번호 중복 방지 구현

- `AdminPatientService.updatePatient(...)`를 추가해 없는 환자는 `CustomException.notFound(...)`로 처리하고, `name`, `phone`, `note`만 수정하도록 구현했다.
- `AdminPatientRepository.existsByNormalizedPhoneAndIdNot(...)`를 추가해 자기 자신을 제외한 다른 환자와의 전화번호 중복을 하이픈/공백 제거 기준으로 검사하도록 정리했다.
- 기존 전화번호와 동일한 값으로 다시 저장하는 경우는 허용하고, `note`는 `trim()` 후 빈 문자열이면 `null`로 정규화하도록 정책을 확정했다.
- 엔티티 수정은 기존 `updateInfo(...)`를 사용하되 `email`, `address`는 기존 값을 그대로 유지하도록 서비스가 중간 보호막 역할을 맡았다.

### 4. 관리자 환자 수정 API 엔드포인트 연결

- `AdminPatientApiController`를 추가하고 `POST /admin/api/patients/{id}/update`를 열었다.
- 컨트롤러는 `@Valid @RequestBody`로 입력을 받고, 서비스가 반환한 `UpdateAdminPatientApiResponse`를 `Resp.ok(...)`로 감싸 반환하도록 얇게 유지했다.
- 비즈니스 규칙과 예외 변환은 서비스와 공통 예외 처리기에 맡겨 SSR 환자 목록/상세 기능과 책임을 분리했다.

### 5. API/서비스 테스트 보강과 기존 테스트 정리

- `AdminPatientServiceTest`에 수정 성공, 없는 환자, 다른 환자와 전화번호 중복, 동일 전화번호 유지 허용, blank note 정규화 시나리오를 추가했다.
- `AdminPatientApiControllerTest`를 새로 추가해 성공, 권한, `name`/`phone` 공백 검증 실패, 중복 전화번호, 없는 환자 시나리오를 HTTP 계층에서 검증했다.
- 범위를 다시 점검하는 과정에서 `AdminPatientControllerTest`에 남아 있던 인코딩 손상 흔적을 현재 SSR 계약 기준으로 정리해 `admin.patient.*` 범위 테스트가 안정적으로 돌도록 맞췄다.

### 6. 문서 갱신과 최종 검증

- `task-023.md`는 완료 기준, 메모, PR 리뷰 포인트를 현재 구현 상태에 맞게 갱신했다.
- report 기준으로는 `workflow-023.md`도 함께 갱신된 것으로 기록되어 있으나, 현재 로컬 파일은 비어 있어 실제 내용 확인은 되지 않았다.
- `admin.patient` 범위 테스트와 전체 `./gradlew test`를 순차로 재확인해 최종 통과 상태를 확인했다.

## 검증 결과

- 구조 점검 단계: 설계 확인 중심이라 별도 테스트 실행 없음
- `./gradlew compileJava` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.admin.patient.AdminPatientServiceTest'` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.admin.patient.AdminPatientApiControllerTest'` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.admin.patient.*'` : `BUILD SUCCESSFUL`
- `./gradlew test` : `BUILD SUCCESSFUL`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-023.md`
- 로컬: `doc/dev-c/workflow/workflow-023.md`

## 남은 TODO / 리스크

- 현재 `task-023` 기준 필수 구현은 완료 상태다.
- 현재 로컬 [workflow-023.md](C:\workspace\Team\hms\doc\dev-c\workflow\workflow-023.md)는 비어 있어, report에 적힌 workflow 갱신 내용이 파일에 실제 반영됐는지 재확인이 필요하다.
- 후속 확장 후보는 `email/address` 수정 지원, 전화번호 형식 검증 고도화, 관리자 UI와 수정 API 연결, 수정 이력 관리다.
