# task-027 작업 로그

## 작업 전 기준 문서/항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-027.md` 확인
- [x] `doc/dev-c/workflow/workflow-027.md` 확인
- [x] `doc/dev-c/.person/reports/task-027/` 보고서 확인

## 작업 목표

- 관리자 규칙 수정 기능 `S32`를 상세 화면 기반 흐름으로 완성한다.
- 조회는 `GET /admin/rule/detail?ruleId={id}`, 저장은 `POST /admin/rule/update` 기준으로 고정한다.
- 상세 화면 안에서 규칙 상세 정보와 수정 폼을 함께 제공하고, `title`, `content`, `category`, `isActive` 전체 필드를 수정 가능하게 만든다.
- 저장 성공 시 같은 상세 화면으로 PRG 리다이렉트하고, 실패 시 same-view 재렌더링으로 입력값과 오류 메시지를 유지한다.
- 등록 화면과 수정 화면의 폼 구조를 공통 partial로 재사용해 관리자 SSR 패턴을 정리한다.

## 보고서/리포트 소스

- `doc/dev-c/.person/reports/task-027/report-20260321-1835-task-27.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleController.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleService.java`
- `src/main/java/com/smartclinic/hms/admin/rule/dto/AdminRuleDetailResponse.java`
- `src/main/java/com/smartclinic/hms/admin/rule/dto/UpdateAdminRuleRequest.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AI-CONTEXT.md`
- `src/main/java/com/smartclinic/hms/admin/rule/FILES.md`
- `src/main/resources/templates/admin/_rule-form.mustache`
- `src/main/resources/templates/admin/rule-detail.mustache`
- `src/main/resources/templates/admin/rule-new.mustache`
- `src/main/resources/templates/admin/rule-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleServiceTest.java`
- `doc/dev-c/task/task-027.md`
- `doc/dev-c/workflow/workflow-027.md`

## 구현 내용

### 1. 수정 계약을 `detail/update` 기준으로 고정했다

- 초기 요구의 `/admin/rule/edit/{id}` 대신 인터뷰 결과와 문서 합의 기준으로 `GET /admin/rule/detail?ruleId={id}`, `POST /admin/rule/update`를 최종 계약으로 채택했다.
- 수정 화면은 별도 edit 페이지를 만들지 않고 상세 화면 안에서 수정 폼을 함께 렌더링하는 구조로 정리했다.

### 2. 상세 조회 모델과 수정 요청 모델을 분리했다

- `AdminRuleDetailResponse`를 추가해 상세 화면에서 필요한 출력 전용 데이터와 표시용 문자열을 한곳에 모았다.
- `UpdateAdminRuleRequest`를 추가해 `ruleId`, `title`, `content`, `category`, `active` 수정 요청과 검증 책임을 분리했다.
- 등록 DTO와 수정 DTO를 분리해 등록/상세/수정 역할이 섞이지 않도록 정리했다.

### 3. 컨트롤러와 서비스에 상세 조회/수정 플로우를 연결했다

- `AdminRuleController`에 `GET /admin/rule/detail`을 추가해 상세 정보와 수정 폼 모델을 함께 렌더링하도록 구현했다.
- `POST /admin/rule/update`는 `@Valid` 검증 후 서비스에 위임하고, 성공 시 `redirect:/admin/rule/detail?ruleId={id}` + `successMessage` 플래시를 전달하도록 만들었다.
- 존재하지 않는 `ruleId`는 `CustomException.notFound(...)` 기반으로 404 또는 목록 리다이렉트로 처리했다.
- `AdminRuleService`에는 `getRuleDetail(...)`, `updateRule(...)`를 추가해 수정 가능한 모든 필드를 도메인 `HospitalRule.update(...)`로 반영하도록 연결했다.

### 4. same-view 실패 처리와 친절한 오류 메시지를 유지했다

- 수정 저장 실패 시 같은 상세 화면을 다시 렌더링하면서 사용자가 입력한 `title`, `content`, `category`, `active` 값을 유지하도록 정리했다.
- 등록 때 사용하던 `applyFormErrors(...)` 규칙을 재사용해 `category` enum 바인딩 실패를 `올바른 카테고리를 선택해 주세요.`로 보정했다.
- `ruleId`가 비정상이거나 대상 규칙이 없으면 상세 same-view 대신 목록 리다이렉트 + 에러 메시지로 안전하게 정리했다.

### 5. 등록/수정 폼을 공통 partial로 재사용했다

- `admin/_rule-form.mustache`를 추가해 카테고리, 제목, 내용, 활성 여부, 버튼 영역을 공통 폼 블록으로 추출했다.
- `rule-new.mustache`와 새 `rule-detail.mustache`가 같은 partial을 사용하도록 바꿔 등록/수정의 차이를 `formAction`, `submitLabel`, 초기값 바인딩 정도로 제한했다.
- `rule-list.mustache`의 제목 링크는 상세 화면 URL로 연결되도록 정리했다.

### 6. 테스트와 문서를 현재 구현 상태에 맞춰 마감했다

- `AdminRuleControllerTest`에 상세 조회 렌더링, 수정 성공 리다이렉트, 수정 검증 실패 same-view, invalid category, not found, detail 링크 검증을 추가했다.
- `AdminRuleServiceTest`에 상세 조회, 전체 필드 수정, `active` true/false/null 처리, not found 케이스를 추가했다.
- `task-027.md`, `workflow-027.md`는 실제 코드와 테스트 결과 기준으로 완료 상태로 동기화했다.

## 검증 결과

- `.\gradlew.bat compileJava --no-watch-fs` : `BUILD SUCCESSFUL`
- `.\gradlew.bat compileTestJava --no-watch-fs` : `BUILD SUCCESSFUL`
- `.\gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"` : `BUILD SUCCESSFUL`
- `.\gradlew.bat test --no-watch-fs` : `FAILED`

전체 테스트의 비관련 실패:
- `DoctorTreatmentServiceTest > getTodayReceivedList`
- `ReservationControllerTest > @Valid 검증 실패 - 전화번호 빈 값 시 폼 뷰 재표시 및 에러 메시지`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-027.md`
- 로컬: `doc/dev-c/workflow/workflow-027.md`

## 남은 TODO / 리스크

- 로컬 `API.md` 또는 관련 문서에 남아 있을 수 있는 규칙 수정 URL 계약을 실제 구현 기준인 `GET /admin/rule/detail?ruleId={id}`, `POST /admin/rule/update`로 정리할 필요가 있다.
- `admin.rule` 범위 테스트는 통과했지만 프로젝트 전체 테스트는 `doctor`, `reservation` 영역의 기존 실패 2건 때문에 아직 완전 녹색이 아니다.
- 규칙 수정 기능은 상세 화면 내 폼 구조로 완성됐으므로, 이후 관련 기능 확장 시에도 별도 edit 페이지보다 partial 재사용 패턴을 유지하는 편이 안전하다.
