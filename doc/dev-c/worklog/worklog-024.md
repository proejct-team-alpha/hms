# task-024 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-024.md` 확인
- [x] `doc/dev-c/workflow/workflow-024.md` 확인
- [x] `doc/dev-c/.person/reports/task-024/` 보고서 확인

## 작업 목표

- 이미 구현된 관리자 환자 수정 API를 환자 상세 화면에 실제로 연동한다.
- 수정 UI는 목록이 아니라 상세 화면에서만 제공해 화면 역할을 분리한다.
- 이름, 연락처, 메모만 인라인 수정 가능하게 하고 성공/실패 피드백을 같은 카드 안에서 처리한다.
- `admin.patient` 범위 렌더링 테스트와 전체 테스트를 통해 연동 이후 회귀를 확인한다.

## 보고서 소스

- `report-20260319-1746-task-24.md`

## 변경 파일

- `doc/dev-c/workflow/workflow-024.md`
- `doc/dev-c/task/task-024.md`
- `src/main/java/com/smartclinic/hms/admin/patient/dto/AdminPatientDetailResponse.java`
- `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientService.java`
- `src/main/resources/templates/admin/patient-detail.mustache`
- `src/test/java/com/smartclinic/hms/admin/patient/AdminPatientControllerTest.java`

## 구현 내용

### 1. 상세 화면 연동 범위와 수정 가능한 필드 정리

- `workflow-024.md`, `task-024.md`를 추가해 환자 수정 UI를 목록이 아니라 상세 화면에만 두는 방향을 먼저 고정했다.
- 수정 가능 필드는 `name`, `phone`, `note`로 제한하고, `patientId`, `email`, `address`는 읽기 전용으로 유지했다.
- 기존 `POST /admin/api/patients/{id}/update`를 그대로 재사용하고, 새 API를 만드는 대신 화면 연동에 집중했다.

### 2. 상세 응답 DTO 보완

- `AdminPatientDetailResponse`에 편집용 `noteValue`를 추가해 표시용 메모와 실제 수정 원본 값을 분리했다.
- 서비스에서는 읽기 화면에서는 빈 메모를 `-`로 보여주되, 수정 폼에는 원본 메모 값이 들어가도록 응답을 구성했다.
- 이 분리 덕분에 화면에는 보기 좋은 값이 보이고, 저장 시에는 잘못된 표시 문자열이 다시 저장되지 않게 했다.

### 3. 환자 상세 화면 인라인 수정 UI 구현

- `patient-detail.mustache` 기본 정보 카드 상단에 `정보 수정` 버튼을 추가했다.
- 같은 카드 안에서 읽기 모드와 수정 모드를 전환하는 인라인 편집 UI를 넣었다.
- 이름, 연락처, 메모 입력 폼과 성공/실패 메시지 영역을 함께 배치해 별도 수정 페이지나 모달 없이 같은 흐름 안에서 작업이 끝나도록 정리했다.

### 4. 수정 API 연동 JavaScript 구현

- 템플릿 안의 작은 스크립트로 `fetch` 기반 저장 처리를 구현했다.
- CSRF 토큰 헤더를 함께 보내고, 성공 시 화면의 이름/연락처/메모 표시값을 즉시 갱신한 뒤 수정 모드를 닫도록 만들었다.
- 실패 시 `VALIDATION_ERROR`, `DUPLICATE_PATIENT_PHONE`, `RESOURCE_NOT_FOUND`를 나눠 필드 아래 에러 또는 카드 상단 메시지로 보여주도록 처리했다.
- 현재 공통 API 에러 응답은 문자열 중심이라, 화면에서는 필요한 수준의 분기만 가볍게 적용했다.

### 5. 렌더링 테스트와 범위 검증

- `AdminPatientControllerTest`에 상세 화면에서 수정 버튼, 수정 폼, API URL이 실제로 렌더링되는지 검증을 추가했다.
- 기존 상세 렌더링과 404 시나리오가 유지되는지도 함께 확인했다.
- 마지막으로 `admin.patient` 범위 테스트와 전체 테스트를 순차로 실행해 이번 연동이 다른 기능을 깨뜨리지 않는지 점검했다.

## 검증 결과

- `./gradlew test --tests 'com.smartclinic.hms.admin.patient.*'` : `BUILD SUCCESSFUL`
- `./gradlew test` : `BUILD SUCCESSFUL`
- 확인 범위:
- 환자 상세 화면 렌더링
- 수정 API 연동 이후 `admin.patient` 범위 회귀
- 전체 브랜치 테스트 재확인

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-024.md`
- 로컬: `doc/dev-c/workflow/workflow-024.md`

## 남은 TODO / 리스크

- 현재 `task-024` 기준 필수 구현은 완료 상태다.
- 이후 확장 후보는 연락처 입력 포맷 자동 정리, 수정 중 로딩 상태 표시, 수정 성공 후 상단 플래시와의 통일이다.
- 공통 API 에러 body가 구조화되면 현재 문자열 분기 기반 화면 에러 처리도 더 깔끔하게 바꿀 수 있다.
