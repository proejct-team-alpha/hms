# task-035 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-035.md` 확인
- [x] `doc/dev-c/workflow/workflow-035.md` 확인
- [x] `doc/dev-c/.person/reports/task-035/` 보고서 확인

## 작업 목표

- 직원 수정 흐름에서 본인 계정 비활성화와 본인 `퇴사 일시(retiredAt)` 수정 시도를 SSR/API 모두에서 차단한다.
- 직원 등록/수정 화면과 관리자 직원 수정 API에 `퇴사 일시`를 도입하고, 과거 시각이면 즉시 비활성화되도록 규칙을 통합한다.
- 1시간 주기 자동 비활성화 스케줄러와 다음 요청 자동 로그아웃 흐름을 연결한다.
- 로그인 화면에 자동 로그아웃 안내 문구를 제공하고, 관련 테스트와 문서를 현재 구현 상태 기준으로 마감한다.

## 보고서 소스

- `report-20260324-1318-task-35-1.md`
- `report-20260324-1320-task-35-2.md`
- `report-20260324-1339-task-35-3.md`
- `report-20260324-1359-task-35-4.md`
- `report-20260324-1412-task-35-5.md`
- `report-20260324-1423-task-35-6.md`
- `report-20260324-1450-task-35-7.md`
- `report-20260324-1505-task-35-8.md`

## 변경 파일

- `src/main/resources/templates/admin/staff-form.mustache`
- `src/main/resources/templates/auth/login.mustache`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffApiController.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffRepository.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java`
- `src/main/java/com/smartclinic/hms/admin/staff/StaffRetirementScheduler.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/UpdateAdminStaffApiRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/UpdateAdminStaffApiResponse.java`
- `src/main/java/com/smartclinic/hms/auth/AuthController.java`
- `src/main/java/com/smartclinic/hms/auth/StaffRepository.java`
- `src/main/java/com/smartclinic/hms/common/interceptor/InactiveStaffLogoutInterceptor.java`
- `src/main/java/com/smartclinic/hms/config/WebMvcConfig.java`
- `src/main/java/com/smartclinic/hms/domain/Staff.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffApiControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffServiceTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/StaffRetirementSchedulerTest.java`
- `src/test/java/com/smartclinic/hms/auth/AuthControllerTest.java`
- `src/test/java/com/smartclinic/hms/common/interceptor/InactiveStaffLogoutInterceptorTest.java`
- `doc/dev-c/task/task-035.md`
- `doc/dev-c/workflow/workflow-035.md`

## 구현 내용

### 1. 직원 수정/인증 흐름 점검과 규칙 설계

- 초기 점검에서 SSR 수정 경로와 관리자 직원 수정 API가 서로 다른 저장 흐름을 타고 있었고, API 쪽만 일부 보호 규칙을 서비스로 내리기 시작한 상태임을 확인했다.
- `active`, `username`, 역할, 향후 `retiredAt`까지 계정 상태 규칙은 모두 `Staff` 엔티티가 책임지는 구조가 자연스럽다고 정리했다.
- 설계 단계에서 본인 계정에 대해 막아야 하는 범위를 명확히 고정했다.
- `active=true -> false`
- `retiredAt` 신규 입력
- `retiredAt` 변경
- `retiredAt` 제거
- 추가로 비활성 직원의 재활성화는 허용하지 않고, `retiredAt <= now`이면 저장 즉시 비활성화하며, 1시간 주기 자동 비활성화와 다음 요청 자동 로그아웃까지 연결하는 방향으로 규칙을 정리했다.

### 2. SSR 화면과 API 계약에 `retiredAt` 및 잠금 규칙 반영

- `staff-form.mustache`에는 `datetime-local` 기반 `퇴사 일시` 입력을 추가하고, 본인 계정이나 비활성 직원처럼 수정이 잠겨야 하는 경우 `employmentStatusLocked`, `retiredAtLocked` 상태를 기준으로 disabled 렌더링을 하도록 정리했다.
- disabled 필드는 submit payload에서 빠지기 때문에 hidden input을 함께 두는 방식으로 same-view 재렌더링과 저장 경로가 안정적으로 이어지게 했다.
- 관리자 직원 수정 API는 `UpdateAdminStaffApiRequest`, `UpdateAdminStaffApiResponse`에 `retiredAt`을 추가하고, `AdminStaffApiController`가 `Authentication.getName()`을 서비스에 같이 넘기도록 바뀌었다.
- 이 단계로 화면과 API 모두 “본인 계정은 스스로 비활성화하거나 퇴사 일시를 바꿀 수 없다”는 공통 규칙을 서비스 레이어에서 판정할 준비가 됐다.

### 3. 서비스 규칙을 SSR/API 공통 기준으로 통합

- `AdminStaffController.update(...)`가 현재 로그인 사용자명을 서비스까지 넘기도록 바뀌면서, SSR 저장도 API와 동일한 `updateStaff(request, currentUsername)` 경로를 타게 됐다.
- `AdminStaffService`는 `validateSelfAccountUpdate(...)`, `validateReactivation(...)`, `resolveActiveState(...)`를 중심으로 규칙을 통합했다.
- 본인 계정 비활성화 및 본인 `retiredAt` 변경 차단
- 비활성 직원 `active=false -> true` 재활성화 차단
- 과거 `retiredAt` 입력 시 저장 즉시 `active=false`
- 이 규칙은 등록과 수정 양쪽에 공통으로 적용돼, 새 직원 등록 시에도 과거 `retiredAt`이 들어오면 즉시 비활성화되도록 맞춰졌다.
- SSR에서는 same-view 오류 속성으로, API에서는 400 응답과 공통 예외 포맷으로 같은 비즈니스 규칙이 노출되도록 정리됐다.

### 4. 자동 비활성화 스케줄러와 다음 요청 자동 로그아웃 연결

- `AdminStaffRepository.findAllByActiveTrueAndRetiredAtLessThanEqual(...)`를 추가해, 만료된 활성 직원만 정확히 찾는 조회 기준을 만들었다.
- `AdminStaffService.deactivateExpiredStaffs()`는 이 목록을 받아 실제 비활성화만 담당하고, `StaffRetirementScheduler`는 1시간마다 서비스 메서드만 호출하는 얇은 구조로 유지됐다.
- 이미 로그인 중인 비활성 계정은 `InactiveStaffLogoutInterceptor`가 다음 요청의 `preHandle`에서 감지해 세션과 `SecurityContext`를 정리하고 `/login?deactivated=true`로 보낸다.
- `AuthController`와 `login.mustache`는 `deactivated` 쿼리 파라미터를 받아 `계정이 비활성화되어 자동 로그아웃되었습니다.` 안내 문구를 렌더링한다.
- `WebMvcConfig`는 새 인터셉터 때문에 테스트 컨텍스트가 과하게 무거워지지 않도록 `StaffRepository` 빈 존재 조건에서만 인터셉터를 등록하는 쪽으로 정리됐다.

### 5. 테스트 보강과 문서 마감

- `AdminStaffControllerTest`는 SSR same-view에서 본인 계정 비활성화 시도와 비활성 직원 재활성화 시도가 올바른 오류 속성으로 노출되는지 확인하도록 보강됐다.
- `AdminStaffApiControllerTest`는 본인 계정의 `retiredAt` 변경 시도도 400으로 막히는지 확인해, API 우회 경로가 닫혔는지 검증했다.
- `AdminStaffServiceTest`는 과거 `retiredAt` 등록/수정 즉시 비활성화, 본인 계정 차단, 재활성화 차단 시나리오를 통합 검증하도록 정리됐다.
- `StaffRetirementSchedulerTest`, `InactiveStaffLogoutInterceptorTest`, `AuthControllerTest`는 각각 스케줄러 위임, 다음 요청 자동 로그아웃, 로그인 안내 문구 노출을 고정했다.
- 마지막 단계에서 `workflow-035`, `task-035`를 완료 상태로 갱신했고, 범위 테스트와 전체 테스트를 다시 실행해 문서와 코드 상태를 함께 닫았다.

## 검증 결과

- 구조 점검/설계 단계(`Task 35-1`, `Task 35-2`)는 문서 정리 중심이라 별도 테스트 실행 없음
- `./gradlew cleanTest test --tests 'com.smartclinic.hms.admin.staff.*'` : `BUILD SUCCESSFUL`
- `./gradlew cleanTest test --tests 'com.smartclinic.hms.admin.staff.*' --tests 'com.smartclinic.hms.auth.*' --tests 'com.smartclinic.hms.common.interceptor.*'` : `BUILD SUCCESSFUL`
- `./gradlew test` : `BUILD SUCCESSFUL`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-035.md`
- 로컬: `doc/dev-c/workflow/workflow-035.md`

## 남은 TODO / 리스크

- report 기준으로는 `task-035` 범위와 테스트가 모두 완료된 상태다.
- 다만 현재 워크스페이스에서 [task-035.md](C:/workspace/Team/hms/doc/dev-c/task/task-035.md)와 [workflow-035.md](C:/workspace/Team/hms/doc/dev-c/workflow/workflow-035.md)를 읽으면 한글 본문이 깨져 보이는 상태라, 문서 인코딩 정리는 별도 확인이 필요하다.
- 기능 자체는 녹색 빌드까지 확인됐지만, 이후 문서 참고 작업에서는 report 묶음과 worklog를 기준으로 보는 편이 안전하다.
