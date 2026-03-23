# task-026 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-026.md` 확인
- [x] `doc/dev-c/workflow/workflow-026.md` 확인
- [x] `doc/dev-c/.person/reports/task-026/` 보고서 확인

## 작업 목표

- 관리자 규칙 등록 기능 `S31`을 `/admin/rule/new` 기준으로 완성한다.
- 제목, 내용, 카테고리, 활성 여부를 검증 가능한 DTO와 SSR 폼 흐름으로 정리한다.
- 기존 `/admin/rule/form` 경로는 사용자를 깨뜨리지 않도록 호환하되, 신규 기준 경로와 템플릿 이름은 `/admin/rule/new`, `admin/rule-new`로 통일한다.
- 성공 플래시, 실패 재렌더링, invalid category 보정, 컨트롤러/서비스 테스트, 문서 마감까지 한 번에 정리한다.
- 작업 중간에 발견된 관리자 DTO 네이밍/패키지 구조도 `dto/` + `Request/Response/ItemResponse` 규칙으로 같이 정리한다.

## 보고서 소스

- `report-20260321-1548-task-26-1.md`
- `report-20260321-1555-task-26-2.md`
- `report-20260321-1601-task-26-2.md`
- `report-20260321-1629-task-26-dto-refactor.md`
- `report-20260321-1651-task-26-3.md`
- `report-20260321-1658-task-26-4.md`
- `report-20260321-1713-task-26-5.md`
- `report-20260321-1733-task-26-6.md`
- `report-20260321-1746-task-26-7.md`
- `report-20260321-1753-task-26-8.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleController.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleService.java`
- `src/main/java/com/smartclinic/hms/admin/rule/HospitalRuleRepository.java`
- `src/main/java/com/smartclinic/hms/admin/rule/dto/CreateAdminRuleRequest.java`
- `src/main/java/com/smartclinic/hms/admin/rule/dto/AdminRuleItemResponse.java`
- `src/main/java/com/smartclinic/hms/admin/rule/dto/AdminRuleListResponse.java`
- `src/main/java/com/smartclinic/hms/admin/rule/dto/AdminRuleFilterOptionResponse.java`
- `src/main/java/com/smartclinic/hms/admin/rule/dto/AdminRulePageLinkResponse.java`
- `src/main/java/com/smartclinic/hms/domain/HospitalRule.java`
- `src/main/resources/templates/admin/rule-new.mustache`
- `src/main/resources/templates/admin/rule-list.mustache`
- `src/main/resources/templates/admin/AI-CONTEXT.md`
- `src/main/resources/static/js/sidebar-admin.js`
- `src/main/resources/static/js/header-admin.js`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleServiceTest.java`
- `src/main/java/com/smartclinic/hms/admin/item/AdminItemService.java`
- `src/main/java/com/smartclinic/hms/admin/item/dto/AdminItemListItemResponse.java`
- `src/main/java/com/smartclinic/hms/admin/mypage/AdminMypageService.java`
- `src/main/java/com/smartclinic/hms/admin/mypage/dto/AdminMypageResponse.java`
- `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentController.java`
- `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentService.java`
- `src/main/java/com/smartclinic/hms/admin/department/dto/CreateAdminDepartmentRequest.java`
- `src/main/java/com/smartclinic/hms/admin/department/dto/UpdateAdminDepartmentRequest.java`
- `src/main/java/com/smartclinic/hms/admin/department/dto/AdminDepartmentDetailResponse.java`
- `src/main/java/com/smartclinic/hms/admin/department/dto/AdminDepartmentItemResponse.java`
- `src/main/java/com/smartclinic/hms/admin/department/dto/AdminDepartmentListResponse.java`
- `src/main/java/com/smartclinic/hms/admin/department/dto/AdminDepartmentPageLinkResponse.java`
- `doc/dev-c/task/task-026.md`
- `doc/dev-c/workflow/workflow-026.md`

## 구현 내용

### 1. 규칙 등록 요구사항과 기존 구현의 차이를 먼저 정리했다

- 시작 시점의 규칙 등록은 `GET/POST /admin/rule/form` 기반이었고, `title`, `content`, `category`만 단순 파라미터로 받는 구조였다.
- `active` 체크박스, 성공 플래시, 실패 재렌더링, 입력값 유지, 필드 오류 노출이 없어서 SSR 등록 폼으로는 미완성 상태였다.
- 문서상으로도 `S31 = /admin/rule/new`, 로컬 `API.md`의 일부는 `POST /admin/rule/create`로 갈려 있어 먼저 구현 기준을 `/admin/rule/new`로 고정했다.

### 2. 규칙 등록 DTO와 관리자 DTO 구조를 함께 정리했다

- 규칙 등록 입력은 `CreateAdminRuleRequest` record로 분리하고, `title 200자`, `content 3000자`, `category enum`, `active Boolean` 검증 구조를 넣었다.
- `category`는 문자열 + 정규식 대신 `HospitalRuleCategory` enum으로 정리해 컨트롤러 바인딩과 서비스 호출을 더 안전하게 만들었다.
- 같은 작업 흐름 안에서 관리자 DTO 구조도 손봤다.
- `admin/rule`, `admin/item`, `admin/mypage`, `admin/department`의 DTO를 `dto/` 패키지로 모으고 `Request/Response/ItemResponse` 규칙으로 통일했다.
- 예외적으로 `item`은 이름 중복을 피하려고 `AdminItemListItemResponse`를 사용했다.

### 3. `/admin/rule/new` 저장 흐름과 legacy `/form` 호환을 완성했다

- `AdminRuleController`에 `GET /admin/rule/new`, `POST /admin/rule/new`를 추가했다.
- 성공 시에는 PRG 패턴으로 `/admin/rule/list`에 리다이렉트하고 `successMessage` 플래시를 전달하도록 정리했다.
- 기존 `GET /admin/rule/form`은 `/admin/rule/new`로 리다이렉트되게 바꿨고, `POST /admin/rule/form`은 사용자 호환을 위해 같은 저장 로직을 타는 alias로 유지했다.
- 목록 화면의 등록 버튼과 내부 템플릿 참조도 `/admin/rule/new`와 `admin/rule-new` 기준으로 통일했다.

### 4. Mustache 폼 UX와 도메인 저장 규칙을 실제 운영 흐름에 맞게 다듬었다

- 템플릿은 `rule-form`에서 `rule-new.mustache`로 정리하고, 검증 실패 시 같은 `admin/rule-new` 화면으로 재렌더링되도록 맞췄다.
- `active` 체크박스는 기본 체크 상태로 두고, 실패 후에도 사용자가 고른 값이 유지되도록 Mustache 바인딩을 보강했다.
- `HospitalRule.create(...)`는 `active`를 직접 받는 팩토리로 바뀌어, 서비스가 생성 후 토글하는 우회 로직 없이 최종 상태를 바로 확정한다.
- 컨트롤러의 `applyFormErrors(...)`에서 enum 바인딩 실패를 공통 처리해 잘못된 카테고리 입력은 `올바른 카테고리를 선택해 주세요.`로 친절하게 보정했다.

### 5. 테스트와 문서를 끝까지 동기화했다

- `AdminRuleControllerTest`는 신규/legacy 경로, 성공 플래시, missing category, invalid category, `activeChecked` true/false, same-view 실패 흐름까지 모두 검증하도록 보강했다.
- `AdminRuleServiceTest`는 trim 처리, enum category 반영, `active=true/false/null` 저장 결과를 검증하도록 정리했다.
- 마지막 단계에서는 `task-026.md`, `workflow-026.md`를 실제 코드와 테스트 상태에 맞춰 완료 처리했다.
- 전체 회귀도 다시 돌렸고, `admin.rule` 범위는 성공했지만 프로젝트 전체 테스트는 이번 작업과 무관한 다른 모듈 실패가 남아 있음을 문서에 기록했다.

## 검증 결과

- 구조 점검 단계: 설계 확인 중심이라 별도 테스트 실행 없음
- `.\gradlew.bat compileJava` : `BUILD SUCCESSFUL`
- `.\gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"` : `BUILD SUCCESSFUL`
- `.\gradlew.bat test --no-watch-fs` : `FAILED`
- 전체 테스트 실패 상세:
- `DoctorTreatmentServiceTest > getTodayReceivedList — 오늘 RECEIVED 상태 예약 목록을 반환한다`
- `ReservationControllerTest > @Valid 검증 실패 - 전화번호 빈 값 시 폼 뷰 재표시 및 에러 메시지`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-026.md`
- 로컬: `doc/dev-c/workflow/workflow-026.md`

## 남은 TODO / 리스크

- 로컬 `API.md`와 관련 문서에는 아직 `POST /admin/rule/create` 계약이 남아 있어 실제 구현인 `POST /admin/rule/new`와 정합성을 맞춰야 한다.
- `admin.rule` 범위 검증은 통과했지만, 전체 테스트 스위트는 `doctor`, `reservation` 영역의 기존 실패 2건 때문에 아직 완전 녹색이 아니다.
- Task 026 안에서 DTO 패키지/네이밍 리팩터링까지 같이 진행했기 때문에, 이후 다른 관리자 모듈도 같은 규칙으로 확장할 때 현재 패턴을 기준선으로 삼는 것이 좋다.
