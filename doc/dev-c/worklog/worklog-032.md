# task-032 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-032.md` 확인
- [x] `doc/dev-c/workflow/workflow-032.md` 확인
- [x] `doc/dev-c/.person/reports/task-032/` 보고서 확인

## 작업 목표

- 직원 등록/수정 화면에서 `부서`를 `Staff` 공통 정보가 아니라 `Doctor` 전용 정보로만 다루도록 구조를 정리한다.
- `전문분야` 자유입력 필드를 제거하고, 의사 생성/수정 시 선택한 부서 기준으로 `Doctor.department`, `Doctor.specialty`를 함께 저장한다.
- `재직 상태`를 공통 정보 영역의 작은 필드로 재배치하고, 등록/수정 화면을 같은 패턴으로 맞춘다.
- `admin.staff` 범위 테스트와 문서를 정리하고, 전체 테스트 상태까지 확인해 최종 완료 가능 여부를 판단한다.

## 보고서 소스

- `report-20260323-1528-task-32-1.md`
- `report-20260323-1532-task-32-2.md`
- `report-20260323-1548-task-32-3.md`
- `report-20260323-1600-task-32-4.md`
- `report-20260323-1615-task-32-5.md`
- `report-20260323-1625-task-32-6.md`
- `report-20260323-1630-task-32-7.md`
- `report-20260323-1638-staff-form-header-alignment.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffApiController.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffRepository.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/CreateAdminStaffRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/UpdateAdminStaffRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/UpdateAdminStaffApiRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/UpdateAdminStaffApiResponse.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffFormResponse.java`
- `src/main/resources/templates/admin/staff-form.mustache`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffServiceTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffApiControllerTest.java`
- `doc/dev-c/task/task-032.md`
- `doc/dev-c/workflow/workflow-032.md`

## 구현 내용

### 1. 직원/의사 책임 경계 재정의

- 초기 점검에서 `Staff`와 `Doctor`가 같은 `department`를 중복으로 들고 있고, `specialty` 자유입력도 함께 유지되고 있음을 확인했다.
- 이후 설계 단계에서 `departmentId`는 `DOCTOR`일 때만 의미를 가지는 필드로 재정의하고, `employmentStatus`는 공통 직원 정보로 유지하는 방향을 문서와 서비스 책임에 고정했다.
- SSR 화면뿐 아니라 admin API 계약도 같은 전제를 갖고 있어, 화면 변경과 DTO/API 변경을 함께 묶어야 한다는 점을 명확히 했다.

### 2. 등록 화면 구조 리팩토링

- `CreateAdminStaffRequest`에서 `specialty`를 제거하고, `departmentId`를 공통 필수값이 아니라 의사 전용 조건부 필드로 완화했다.
- `staff-form.mustache` 등록 화면 공통 정보 영역에서는 `부서`를 제거하고 `재직 상태`를 같은 줄의 작은 선택 필드로 재배치했다.
- 의사 전용 정보 영역에는 등록 화면일 때만 `부서 select`를 노출하고, `전문분야` 자유입력은 제거했다.
- `AdminStaffService.createStaff(...)`는 비의사 생성 시 `Staff.department`를 비우고, 의사 생성 시에만 `Doctor.department`와 `Doctor.specialty`를 선택한 부서 기준으로 저장하도록 정리했다.

### 3. 수정 화면과 admin API 계약 리팩토링

- 수정 화면 공통 정보에서도 `부서`를 제거하고, 역할 옆에 `재직 상태`를 두는 구조로 맞췄다.
- `UpdateAdminStaffRequest`, `UpdateAdminStaffApiRequest`, `UpdateAdminStaffApiResponse`, `AdminStaffFormResponse`에서 `specialty`를 제거하고 `departmentId`와 `active` 중심 계약으로 재정리했다.
- `AdminStaffApiController`와 수정 서비스는 의사 수정 시 선택한 부서를 기준으로 `Doctor.department`, `Doctor.specialty`를 함께 갱신하도록 보완됐다.
- same-view 검증 실패 시 `departmentIdError`를 다시 주입해 의사 수정 화면에서도 원인이 바로 보이도록 유지했다.

### 4. 서비스 저장 규칙과 조회 규칙 정렬

- 생성은 이미 `Staff.department = null` 기준으로 맞춰져 있었고, 이번에 수정과 비활성화까지 같은 규칙으로 맞췄다.
- `AdminStaffService.updateStaff(...)`는 이제 역할과 무관하게 공통 직원 엔티티에는 이름/재직 상태만 저장하고, 의사인 경우에만 `Doctor` 엔티티가 부서와 전문분야를 책임지도록 정리됐다.
- 직원 목록 조회 쿼리도 `Staff.department` 대신 `Doctor.department`를 기준으로 의사 부서를 보도록 바뀌어 저장 규칙과 조회 화면이 어긋나지 않게 맞춰졌다.
- 비의사 역할에서 `departmentId`가 들어와도 실제 저장 로직에 영향 주지 않는 방어도 서비스 테스트로 닫았다.

### 5. 테스트 보강과 문서 상태 정리

- `AdminStaffControllerTest`는 등록/수정 화면에서 `departmentId` select는 남고 `specialty` 입력은 사라졌다는 점을 실제 렌더링 HTML 기준으로 검증하도록 강화됐다.
- `AdminStaffApiControllerTest`는 수정 응답에 `specialty`가 더 이상 존재하지 않는다는 점을 검증해 SSR과 API 계약이 함께 움직이도록 맞췄다.
- `AdminStaffServiceTest`는 의사 생성/수정 시 부서 기반 저장, 비의사 수정 시 부서 입력 무시, 직원 목록의 의사 부서 노출 시나리오까지 포함하도록 정리됐다.
- `workflow-032`, `task-032`는 현재 구현 상태와 전체 테스트 상태를 반영하도록 갱신됐지만, 전체 테스트 실패 2건 때문에 최종 완료 체크는 보류 상태로 남았다.

### 6. 추가 UI 정렬 리팩토링

- 후속 리팩토링으로 `staff-form.mustache` 상단 헤더를 `item-form.mustache` 톤에 맞춰 정렬했다.
- 기존 텍스트형 `목록으로 돌아가기` 링크를 아이콘형 뒤로가기 버튼으로 바꾸고, 제목/설명/상단 여백과 `body`/`main` 레이아웃 밀도를 물품 폼과 비슷하게 맞췄다.
- 이 작업은 템플릿 마크업 조정 중심이었고, 등록/수정 공용 템플릿이라 두 상태 모두 같은 헤더 구조를 공유하게 됐다.

## 검증 결과

- 구조 점검/설계 단계(`Task 32-1`, `Task 32-2`)는 문서 정리 중심이라 별도 테스트 실행 없음
- `./gradlew test --tests 'com.smartclinic.hms.admin.staff.*'` : `BUILD SUCCESSFUL`
- `./gradlew test` : `BUILD FAILED`
- 전체 테스트 잔여 실패:
- `AdminDepartmentControllerTest > list renders empty state`
- `ReservationTest > cancel — COMPLETED면 IllegalStateException`
- 추가 메모:
- 마지막 헤더 정렬 리팩토링(`report-20260323-1638-staff-form-header-alignment.md`)은 자동 테스트를 돌리지 않았고, 템플릿 diff와 마크업 확인만 수행해 `검증 필요` 상태로 기록됐다.

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-032.md`
- 로컬: `doc/dev-c/workflow/workflow-032.md`

## 남은 TODO / 리스크

- 현재 `admin.staff` 변경 범위 자체는 거의 닫혔지만, 전체 `./gradlew test` 실패 2건 때문에 `task-032` 전체 완료 체크는 보류 상태다.
- 마지막 헤더 정렬 작업은 자동 테스트와 브라우저 실화면 검증이 남아 있다.
- 전체 테스트가 초록이 되기 전까지는 `workflow-032` / `task-032`를 완전 완료로 보기보다 `검증 필요` 상태로 관리하는 편이 안전하다.
