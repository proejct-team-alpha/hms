# task-019 작업 로그

## 작업 전 준수 항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-019.md` 확인
- [x] report 폴더 기준 누적 요약 작성

## 작업 목표
- `workflow-019`의 직원·진료과 SSR 폼 유효성 검증 범위를 구현 가능한 작업 단위로 정리하고 완료한다.
- 관리자 입력 폼 전반에 `@Valid` 기반 기본 검증과 필드별 에러 표시 흐름을 적용한다.
- 검증 실패 시 같은 SSR 화면에서 입력값과 에러 메시지가 유지되는 UX를 공통 패턴으로 정리한다.
- 직원과 진료과 컨트롤러, 템플릿, 테스트를 함께 보강해 회귀를 방지한다.

## 보고서 소스
- `doc/dev-c/.person/reports/task-019/report-20260319-0922-task-19-1.md`
- `doc/dev-c/.person/reports/task-019/report-20260319-0930-task-19-2.md`
- `doc/dev-c/.person/reports/task-019/report-20260319-0952-task-19-3.md`
- `doc/dev-c/.person/reports/task-019/report-20260319-1005-task-19-4.md`
- `doc/dev-c/.person/reports/task-019/report-20260319-1012-task-19-5.md`
- `doc/dev-c/.person/reports/task-019/report-20260319-1025-task-19-6.md`

## 변경 파일
- `doc/dev-c/task/task-019.md`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/CreateAdminStaffRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/UpdateAdminStaffRequest.java`
- `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentController.java`
- `src/main/java/com/smartclinic/hms/admin/department/CreateAdminDepartmentRequest.java`
- `src/main/java/com/smartclinic/hms/admin/department/UpdateAdminDepartmentRequest.java`
- `src/main/java/com/smartclinic/hms/common/util/SsrValidationViewSupport.java`
- `src/main/resources/templates/admin/staff-form.mustache`
- `src/main/resources/templates/admin/department-list.mustache`
- `src/main/resources/templates/admin/department-detail.mustache`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/department/AdminDepartmentControllerTest.java`

## 구현 내용
1. 현재 SSR 폼 검증 구조를 먼저 점검해 직원과 진료과의 출발점을 분리해서 파악했다.
- 직원 쪽은 이미 `@Valid + BindingResult` same-view 재렌더링 골격이 있었고, 검증 규칙을 보강하는 방향이 적절했다.
- 진료과 쪽은 아직 `@RequestParam` 기반이라 등록용/수정용 입력 객체를 따로 두는 편이 자연스럽다는 결론을 냈다.
- 이 점검 결과를 `task-019` 메모에 남겨 이후 구현 순서를 흔들리지 않게 고정했다.

2. 직원 등록 폼에 기본 유효성 검증과 필드 에러 표시를 적용했다.
- `CreateAdminStaffRequest`에 부서 필수 검증을 추가하고, 기존 이름·로그인 아이디·비밀번호 검증과 함께 등록 기준을 맞췄다.
- `AdminStaffController`는 검증 실패 시 same-view로 `staff-form`을 다시 렌더링하고, 필드별 에러를 request attribute로 주입하도록 정리했다.
- `staff-form.mustache`는 에러 메시지와 기존 입력값이 그대로 보이도록 정리했고, 컨트롤러 테스트로 검증 실패 경로를 잠갔다.

3. 직원 수정 폼에도 같은 검증 패턴을 확장했다.
- `UpdateAdminStaffRequest`에 이름과 부서 필수 검증을 추가했다.
- 비밀번호는 선택 입력 정책을 유지하면서, 검증 실패 시 수정 화면에서 이름과 부서 선택값이 유지되도록 same-view 흐름을 정리했다.
- 읽기 전용 값과 수정 가능 값이 섞인 화면 특성을 고려해, 로그인 아이디와 사번은 그대로 보이고 오류가 난 필드만 바로 고칠 수 있게 했다.

4. 진료과 목록 인라인 등록 폼에 검증 DTO와 모달 재렌더링 흐름을 도입했다.
- `CreateAdminDepartmentRequest`를 새로 만들고 `name`에 `@NotBlank`를 적용해 빈 값과 공백 입력을 차단했다.
- 등록 컨트롤러는 `@Valid @ModelAttribute + BindingResult` 기반으로 바꾸고, 실패 시 목록 화면을 same-view로 다시 렌더링하도록 정리했다.
- `department-list.mustache`는 `createName`, `createActive`, `openCreateModal`, `nameError`를 사용해 모달을 다시 열고 입력값과 에러를 유지하도록 수정했다.

5. 진료과 상세/수정 화면에도 입력 DTO 검증과 same-view 재렌더링을 적용했다.
- `UpdateAdminDepartmentRequest`를 추가해 `departmentId`, `name` 필수 검증을 DTO 차원으로 올렸다.
- 수정 컨트롤러는 DTO 검증 실패와 서비스 검증 실패를 분리해 처리하도록 정리했다.
- 검증 실패 시 `department-detail.mustache`에서 `editName`과 `nameError`를 다시 표시해 사용자가 같은 상세 화면에서 바로 수정할 수 있게 했다.

6. SSR 검증 UX를 공통 유틸과 범위 테스트로 마무리했다.
- `SsrValidationViewSupport`를 추가해 페이지 공통 에러 메시지와 `fieldName + Error` 규칙을 한 곳에서 처리하도록 통일했다.
- 직원/진료과 컨트롤러는 검증 실패 시 이 유틸을 공통으로 사용하게 바꿨다.
- 공백 로그인 아이디, 빈 이름, 공백 이름, 서비스 미호출 보장 같은 경계 케이스를 테스트로 보강했고, 최종적으로 `admin.staff`, `admin.department` 범위 테스트를 함께 통과시켰다.

## 검증 결과
- 실행 명령어: `./gradlew test --tests 'com.smartclinic.hms.admin.staff.*'`
- 결과: `BUILD SUCCESSFUL`
- 실행 명령어: `./gradlew test --tests 'com.smartclinic.hms.admin.department.*'`
- 결과: `BUILD SUCCESSFUL`
- 실행 명령어: `./gradlew test --tests 'com.smartclinic.hms.admin.staff.*' --tests 'com.smartclinic.hms.admin.department.*'`
- 결과: `BUILD SUCCESSFUL`

## 참고 문서
- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-019.md`
- 로컬: `doc/dev-c/workflow/workflow-019.md`

## 남은 TODO / 리스크
- `task-019` 범위는 report 기준으로 완료 상태다.
- 현재 단계는 `@NotBlank`, `@NotNull` 중심의 기본 검증에 집중했기 때문에, 후속 확장 과제로는 길이 제한·패턴 검증 강화, API 요청 DTO 검증 통일, SSR 폼 공통 에러 파셜 분리가 자연스럽다.
- PR 직전에는 필요 시 전체 `./gradlew test`를 한 번 더 돌려 현재 브랜치의 전체 상태를 최종 확인하면 된다.
