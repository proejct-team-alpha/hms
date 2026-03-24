# Task 032 - 직원 등록/수정 화면의 의사 전용 정보 구조 정리

## Task 32-1. 현재 직원 등록/수정 화면 구조 점검
- [x] `AdminStaffController`, `AdminStaffService`, 관련 DTO 현재 구조 확인
- [x] `staff-form.mustache`의 공통 정보 / 의사 전용 정보 필드 배치 확인
- [x] `departmentId`, `specialty`, `employmentStatus` 처리 위치 메모 정리
- [x] 등록/수정 테스트가 전제하는 현재 필드 구조 점검

## Task 32-2. 등록/수정 요청 DTO와 서비스 책임 정리
- [x] `Staff` 공통 정보와 `Doctor` 전용 정보 책임 분리 설계
- [x] `departmentId`를 의사 전용 정보 기준으로만 다루는 방향 정리
- [x] `specialty` 자유입력 제거 기준 정리
- [x] `DOCTOR`일 때만 `departmentId` 검증이 필요하도록 방향 정리
- [x] `employmentStatus`를 공통 정보 필드로 재배치하는 방향 정리

## Task 32-3. 직원 등록 화면 리팩토링
- [x] 공통 정보 영역에서 `부서` 제거
- [x] 공통 정보 영역에 `재직 상태`를 작은 선택 필드로 재배치
- [x] 의사 전용 정보 영역에 `부서 select` 추가
- [x] 등록 화면에서 `전문분야` 자유입력 제거
- [x] 등록 검증 실패 시 `역할`, `재직 상태`, `부서` 선택값 유지 확인

## Task 32-4. 직원 수정 화면 리팩토링
- [x] 공통 정보 영역에서 `부서` 제거
- [x] 공통 정보 영역에 `재직 상태` 배치
- [x] 의사 전용 정보 영역에서 `부서 select`만 사용하도록 정리
- [x] 기존 의사 정보가 있는 경우 현재 부서를 select 선택값으로 보이도록 정리
- [x] 수정 검증 실패 시 `재직 상태`, `부서` 선택값 유지 확인

## Task 32-5. 직원 생성/수정 서비스 로직 정리
- [x] `DOCTOR` 생성 시 선택한 부서를 기준으로 의사 정보 생성
- [x] `DOCTOR` 수정 시 선택한 부서를 기준으로 의사 정보 갱신
- [x] `DOCTOR`가 아닌 경우 의사 전용 정보가 저장 로직에 영향 주지 않도록 정리
- [x] 기존 자유입력 `specialty` 저장 로직이 있다면 부서 기준으로 치환

## Task 32-6. 테스트 보강
- [x] `AdminStaffControllerTest`의 등록/수정 화면 필드 구조 변경 기대값 반영
- [x] `AdminStaffControllerTest`에 `DOCTOR` 등록 시 부서 select 노출 검증 추가
- [x] `AdminStaffServiceTest`에 부서 기준 의사 정보 생성/수정 검증 추가
- [x] 자유입력 전문분야 제거 후 누락 회귀가 없는지 검증 추가
- [x] 비의사 역할에서 의사 전용 정보 무시 시나리오 검증 추가

## Task 32-7. 문서 및 최종 검증 마무리
- [x] `workflow-032` 구현 상태 반영
- [ ] `task-032` 전체 완료 처리
- [x] `admin.staff` 범위 테스트 실행
- [x] 전체 테스트 확인

## 완료 기준
- [x] 직원 등록 화면 공통 정보에서 부서 입력이 제거된다.
- [x] 직원 수정 화면 공통 정보에서 부서 입력이 제거된다.
- [x] 재직 상태가 공통 정보 영역의 작은 필드로 자연스럽게 배치된다.
- [x] 의사 전용 정보에서만 부서 select가 보인다.
- [x] 전문분야 자유입력 필드가 제거된다.
- [x] `DOCTOR` 등록/수정 시 선택한 부서를 기준으로 의사 정보가 저장된다.
- [x] 관련 컨트롤러/서비스 테스트가 통과한다.
- [ ] 전체 `./gradlew test`가 통과한다.

## 추천 진행 순서
- [x] Task 32-1 현재 구조를 먼저 점검한다.
- [x] Task 32-2 DTO/서비스 책임을 먼저 고정한다.
- [x] Task 32-3 등록 화면부터 구조를 정리한다.
- [x] Task 32-4 수정 화면도 같은 패턴으로 맞춘다.
- [x] Task 32-5 서비스 로직을 엔티티 책임과 맞게 정리한다.
- [x] Task 32-6 테스트를 보강한다.
- [ ] Task 32-7 문서와 검증을 완전히 마무리한다.

## 메모
- `workflow-032`는 직원 등록/수정 화면을 현재 엔티티 책임과 맞게 다시 정렬하는 작업이다.
- 핵심은 부서를 `Staff` 공통 정보에서 빼고, `Doctor` 전용 정보로만 다루는 것이다.
- `전문분야` 자유입력은 제거하고, 존재하는 `Department`만 선택하게 만들어 잘못된 문자열 입력을 막는다.
- `재직 상태`는 공통 정보 영역 안의 작은 선택 필드로 옮겨 화면 무게감을 줄인다.

## Task 32-1 점검 메모
- `AdminStaffController`는 등록/수정 모두 같은 `staff-form.mustache`를 사용하고, `getCreateForm()` / `getEditForm()`으로 화면 모델을 구성한다.
- 당시 `CreateAdminStaffRequest`, `UpdateAdminStaffRequest` 모두 `departmentId`를 공통 필수값으로 받고 있었고 `specialty` 자유입력 필드도 유지되고 있었다.
- `AdminStaffService`는 `Staff.create(..., department)`와 `Doctor.create(..., department, ..., specialty)`를 모두 사용하고 있어 부서 책임이 중복되어 있었다.
- `staff-form.mustache`는 공통 정보 영역에 `departmentId` select가 있고, 의사 전용 정보 영역에는 `specialty` 자유입력 + `availableDays` 체크박스가 있었다.
- `employmentStatus`는 등록 화면에만 있고 수정 화면에는 없었다.
- `AdminStaffControllerTest`, `AdminStaffServiceTest`, `AdminStaffApiControllerTest` 모두 `departmentId` 공통 필수, `specialty` round-trip을 전제로 하고 있었다.

## Task 32-2 설계 메모
- `departmentId`는 더 이상 `Staff` 공통 정보의 무조건 필수값이 아니라 `DOCTOR`일 때만 의미를 가지는 의사 전용 필드로 옮긴다.
- 따라서 등록/수정 요청 DTO에서 `departmentId`를 unconditional `@NotNull`로 두지 않고, 서비스에서 조건부 검증하는 쪽이 맞다.
- `specialty` 자유입력은 SSR과 admin API 모두에서 제거 대상이다.
- 의사 정보에서 전문분야 역할은 선택한 `departmentId`로 대체하고, 저장 시에는 부서명 기준으로 정리하는 방향이 자연스럽다.
- `employmentStatus`는 `Staff` 공통 필드이므로 등록/수정 화면 모두에서 공통 정보로 옮긴다.

## Task 32-3 구현 메모
- 등록 화면 공통 정보에서 `departmentId` select를 제거하고, 같은 줄에 `재직 상태` select를 배치했다.
- 의사 전용 정보 영역은 등록 화면에서 `DOCTOR`일 때만 `departmentId` select가 노출되도록 분기했다.
- 등록 DTO `CreateAdminStaffRequest`에서 `specialty`를 제거하고 `departmentId`를 조건부 필드로 완화했다.
- 생성 서비스는 비의사 직원 생성 시 `Staff.department`를 비우고, 의사 생성 시 `Doctor.department`와 `Doctor.specialty`를 선택한 부서 기준으로 저장하도록 정리했다.
- 등록 컨트롤러는 의사 등록 시 부서 누락이 서비스 검증으로 들어오는 경우 `departmentIdError`를 같은 화면에 주입하도록 보완했다.
- `AdminStaffControllerTest`, `AdminStaffServiceTest`는 등록 화면 구조와 생성 서비스 계약 기준으로 다시 정리했다.

## Task 32-4 구현 메모
- 수정 화면 공통 정보에서도 `부서`를 제거하고, `재직 상태`를 역할 옆의 작은 select로 이동했다.
- 의사 수정 화면은 `전문분야` 입력을 제거하고, 의사 전용 정보 영역에 `부서 select + 진료 가능 요일`만 남겼다.
- 수정 DTO `UpdateAdminStaffRequest`에 `active`를 포함하고 `specialty`를 제거해 화면 submit 계약을 단순화했다.
- admin API update 계약도 같은 방향으로 맞춰 `UpdateAdminStaffApiRequest`, `UpdateAdminStaffApiResponse`, `AdminStaffApiController`를 함께 정리했다.
- 수정 서비스는 `active`를 공통 정보로 반영하고, 의사 수정 시 선택한 부서를 기준으로 `Doctor.department`, `Doctor.specialty`를 함께 갱신하도록 보완했다.
- 수정 컨트롤러는 의사 수정 시 부서 누락이 서비스 검증으로 들어오는 경우 `departmentIdError`를 다시 주입하도록 정리했다.
- `AdminStaffControllerTest`, `AdminStaffServiceTest`, `AdminStaffApiControllerTest`를 현재 수정 계약 기준으로 갱신했고 `./gradlew test --tests 'com.smartclinic.hms.admin.staff.*'` 통과를 확인했다.

## Task 32-5 구현 메모
- 생성과 수정 모두 `Staff.department`는 더 이상 저장 책임을 갖지 않도록 정리했다.
- 의사 정보는 `Doctor.department`만 기준으로 조회하고, 직원 목록 쿼리도 의사 부서를 `Doctor`에서 읽도록 바꿨다.
- 수정 서비스는 역할과 무관하게 `Staff.update(..., null, active)`를 사용해 공통 직원 엔티티에 부서가 남지 않도록 맞췄다.
- 비의사 직원 수정 시 `departmentId`가 들어와도 저장 로직에는 영향을 주지 않도록 서비스 테스트로 확인했다.
- `Doctor.specialty`는 자유입력 저장이 아니라 항상 선택한 부서명 기준으로 유지되도록 create/update 흐름을 다시 맞췄다.

## Task 32-6 구현 메모
- 등록/수정 컨트롤러 렌더링 테스트에서 `departmentId` select가 보이고 `specialty` 입력이 더 이상 나오지 않는다는 점을 직접 확인했다.
- 수정 API 응답 테스트에도 `specialty` 필드가 더 이상 존재하지 않는다는 검증을 추가해 SSR과 API 계약이 같이 움직이도록 닫았다.
- 서비스 테스트는 의사 생성/수정 시 부서명 기반 전문분야 저장, 비의사 수정 시 부서 입력 무시, 직원 목록의 의사 부서 노출까지 포함하도록 정리했다.
- 이 단계로 화면, 서비스, API, 테스트가 모두 같은 “부서는 Doctor 전용 책임” 규칙을 보게 됐다.

## Task 32-7 검증 메모
- `admin.staff` 범위 테스트는 통과했다.
- 전체 `./gradlew test`에서는 현재 작업 범위 밖의 실패 2건이 남아 있다.
  - `AdminDepartmentControllerTest > list renders empty state`
  - `ReservationTest > cancel — COMPLETED면 IllegalStateException`
- 따라서 `workflow-032`, `task-032`는 구현 자체는 거의 닫혔지만, 전체 테스트 초록이 아니어서 최종 완료 체크는 보류 상태로 둔다.