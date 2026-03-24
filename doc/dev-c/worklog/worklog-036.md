# task-036 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-036.md` 확인
- [x] `doc/dev-c/workflow/workflow-036.md` 확인
- [x] `doc/dev-c/.person/reports/task-036/` 보고서 확인

## 작업 목표

- 직원 등록/수정 화면의 `퇴사 일시` 입력을 `날짜 + 시` 방식으로 단순화한다.
- 직원 수정 화면에서는 `재직 상태` 옆에 `퇴사 일시`를 배치해 상태와 예약 시점을 함께 볼 수 있게 한다.
- 비활성 직원은 수정 화면 진입은 가능하지만 읽기 전용으로만 보이게 하고, SSR 저장과 관리자 수정 API에서도 수정 시도를 차단한다.
- 등록/수정 SSR/API가 같은 서비스 조합 규칙을 타도록 공통 로직을 정리하고, 관련 테스트와 문서를 완료 상태까지 마감한다.

## 보고서 소스

- `report-20260324-1524-task-36-1.md`
- `report-20260324-1538-task-36-2.md`
- `report-20260324-1559-task-36-3.md`
- `report-20260324-1614-task-36-4.md`
- `report-20260324-1654-task-36-5.md`
- `report-20260324-1654-task-36-6.md`
- `report-20260324-1654-task-36-7.md`
- `report-20260324-1654-task-36-8.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/staff/dto/CreateAdminStaffRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/UpdateAdminStaffRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/UpdateAdminStaffApiRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffFormResponse.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffApiController.java`
- `src/main/resources/templates/admin/staff-form.mustache`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffApiControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffServiceTest.java`
- `doc/dev-c/task/task-036.md`
- `doc/dev-c/workflow/workflow-036.md`

## 구현 내용

### 1. 현재 화면 구조와 입력 계약 점검

- 초기 점검에서 직원 등록/수정 화면이 같은 `staff-form.mustache`를 공유하고 있고, `퇴사 일시`는 아직 `datetime-local` 한 칸 입력이라는 점을 확인했다.
- `CreateAdminStaffRequest`, `UpdateAdminStaffRequest`, `UpdateAdminStaffApiRequest`는 모두 `LocalDateTime retiredAt` 하나를 직접 받는 구조라서, 화면 UX를 바꾸면 저장 계약도 같이 정리해야 한다는 점을 먼저 고정했다.
- 비활성 직원은 당시 `재직 상태` 일부만 잠겨 있고 다른 값은 여전히 수정 가능한 상태였으며, 서비스도 재활성화만 막고 있어 “비활성 직원은 조회만 가능” 규칙이 아직 닫혀 있지 않았다.

### 2. `퇴사 일시`를 날짜 + 시간 입력으로 단순화하는 방향 설계

- 저장 모델 자체는 `Staff.retiredAt: LocalDateTime`으로 유지하고, 입력 UI만 `retiredAtDate` + `retiredAtHour` 두 조각으로 나누는 방향으로 정리했다.
- 두 값이 모두 비어 있으면 `retiredAt = null`, 둘 다 있으면 `yyyy-MM-ddTHH:00` 형식으로 합치고, 하나만 입력된 경우는 검증 에러로 처리하는 기준을 잡았다.
- 이 단계에서는 서버 DTO를 즉시 전면 교체하지 않고, 공통 조합 로직은 이후 `Task 36-6`에서 서비스 기준으로 올리는 방향으로 범위를 나눴다.

### 3. 수정 화면 UI 리팩토링

- 수정 화면에서는 `재직 상태` 옆에 `퇴사 일시`를 한 줄로 배치하고, `datetime-local` 대신 `날짜 input + 시간 select + hidden retiredAt` 구조로 바꿨다.
- `AdminStaffFormResponse`와 `AdminStaffService`는 수정 화면 렌더링을 위해 `retiredAtDate`, `retiredAtHour`, `retiredAtHourOptions`를 내려주도록 보강됐다.
- 브라우저 스크립트는 제출 직전 `retiredAtDate`와 `retiredAtHour`를 `yyyy-MM-ddTHH:00` 형식의 hidden `retiredAt` 값으로 합치고, 날짜나 시간 중 하나만 입력된 경우는 same-view 제출 전 단계에서 막도록 정리됐다.
- 이 단계에서 수정 화면 전용 레이아웃과 새 입력 구조를 `AdminStaffControllerTest` 기준으로 고정했다.

### 4. 등록 화면에 같은 입력 형식 반영

- 등록 화면은 큰 레이아웃을 건드리지 않고, `퇴사 일시` 입력만 수정 화면과 같은 `날짜 input + 시간 select + hidden retiredAt` 구조로 바꿨다.
- 수정 화면에서 만든 `syncRetiredAtHidden()` 패턴을 등록 화면에도 그대로 재사용해, 화면은 단순해지고 hidden 값은 계속 `HH:00` 형식으로 유지되도록 맞췄다.
- 이 단계로 등록/수정 화면은 같은 사용자 입력 규칙을 공유하게 됐다.

### 5. 비활성 직원 조회 전용 처리

- `AdminStaffService.updateStaff(...)` 초입에 비활성 직원 수정 차단 검증을 추가해, SSR 저장과 관리자 수정 API가 같은 서비스 규칙을 타도록 맞췄다.
- `buildEditFormResponse(...)`는 `readOnly = !staff.isActive()`를 계산해 비활성 직원 수정 화면을 읽기 전용으로 렌더링하도록 정리됐다.
- `staff-form.mustache`는 `model.readOnly`일 때 주요 입력 필드를 disabled로 보여주고, 저장 버튼은 숨기며 `비활성화된 직원입니다. 조회만 가능합니다.` 안내 문구만 노출하도록 바뀌었다.
- 차단 메시지는 `비활성화된 직원은 수정할 수 없습니다.` 기준으로 서비스와 테스트를 맞췄고, `AdminStaffControllerTest`, `AdminStaffApiControllerTest`, `AdminStaffServiceTest`도 현재 규칙 기준으로 복구됐다.

### 6. 서비스/API 공통 조합 로직 정리

- `CreateAdminStaffRequest`, `UpdateAdminStaffRequest`, `UpdateAdminStaffApiRequest`에 `retiredAtDate`, `retiredAtHour`를 공통 요청 값으로 추가해 SSR과 API가 같은 입력 계약을 쓰도록 정리했다.
- `AdminStaffService.resolveRetiredAt(...)`는 날짜와 시간을 `LocalDateTime`으로 조합하고, 한쪽만 입력된 경우 `퇴사 일시는 날짜와 시간을 모두 선택해야 합니다.` 검증 에러를 던지도록 구현됐다.
- `normalizeRetiredAt(...)` 계열 헬퍼를 통해 분은 항상 `00`으로 고정되도록 맞췄다.
- `AdminStaffApiController`도 새 필드를 서비스 요청 DTO로 그대로 넘기도록 바뀌어, SSR/API가 동일한 서비스 조합 규칙을 타게 됐다.

### 7. 테스트 보강과 문서 마감

- `AdminStaffControllerTest`에는 날짜만 입력된 상태에서 저장 시 `retiredAtError`가 same-view에 연결되는 SSR 케이스를 추가했다.
- `AdminStaffServiceTest`는 날짜만 입력 / 시간만 입력된 경우 모두 서비스 공통 검증에서 차단되고, 정상 저장 시 `퇴사 일시`가 항상 `HH:00`으로 들어가는지 확인하도록 보강됐다.
- `AdminStaffApiControllerTest`는 비활성 직원 수정 차단을 유지하면서, `retiredAtDate`, `retiredAtHour`가 서비스 DTO까지 그대로 전달되는지 검증하도록 정리됐다.
- 마지막 단계에서 `workflow-036`, `task-036`를 현재 구현과 테스트 상태 기준으로 완료 처리했고, 범위 테스트와 전체 테스트 결과를 문서에 반영했다.

## 검증 결과

- 구조 점검/설계 단계(`Task 36-1`, `Task 36-2`)는 문서 정리 중심이라 별도 테스트 실행 없음
- `./gradlew cleanTest test --tests 'com.smartclinic.hms.admin.staff.*'` : `BUILD SUCCESSFUL`
- `./gradlew test` : `BUILD SUCCESSFUL`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-036.md`
- 로컬: `doc/dev-c/workflow/workflow-036.md`

## 남은 TODO / 리스크

- report 기준으로 `task-036` 구현, 테스트, 문서 마감까지 모두 완료된 상태다.
- 현재 남은 즉시 TODO는 없다.
- 이후 같은 영역을 다시 손볼 때는 `retiredAtDate + retiredAtHour -> LocalDateTime(HH:00)` 조합 규칙과 비활성 직원 read-only 정책을 함께 유지해야 회귀를 줄일 수 있다.
