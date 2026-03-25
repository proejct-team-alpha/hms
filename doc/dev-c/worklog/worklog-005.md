# worklog-005

## 작업 요청
- `doc/dev-c/workflow/workflow-005.md` 실행
- 작업 전 `AGENTS.md/.ai/memory.md/doc/PROJECT_STRUCTURE.md/doc/RULE.md` 확인
- 준수 체크리스트를 먼저 출력한 뒤 구현
- 결과를 `doc/dev-c/worklog/worklog-005.md`로 저장

## 사전 확인 문서
- `AGENTS.md`
- `.ai/memory.md`
- `doc/PROJECT_STRUCTURE.md`
- `doc/RULE.md`
- `doc/dev-c/workflow/workflow-005.md`

## 준수 체크리스트
- [x] 로컬 규칙 문서 우선 적용
- [x] `AdminReceptionController` 삭제
- [x] `AdminPageController` 삭제
- [x] `department/rule/staff/item` 라우트 도메인 컨트롤러로 이관
- [x] 사이드바 링크를 이관 URL과 일치시킴
- [x] `/admin/**` URL prefix 유지
- [x] 테스트 실행으로 회귀 검증

## 변경 파일 목록
- 삭제: `src/main/java/com/smartclinic/hms/admin/AdminPageController.java`
- 삭제: `src/main/java/com/smartclinic/hms/admin/reception/AdminReceptionController.java`
- 추가: `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentController.java`
- 추가: `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleController.java`
- 추가: `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
- 추가: `src/main/java/com/smartclinic/hms/admin/item/AdminItemController.java`
- 수정: `src/main/resources/templates/common/sidebar-admin.mustache`

## 구현 기능 요약
1. 컨트롤러 구조 정리
- 공용 라우터 역할의 `AdminPageController` 제거
- 미사용 판단한 `AdminReceptionController` 제거

2. 도메인별 라우트 이관
- `AdminDepartmentController`
  - `GET /admin/department/list` -> `admin/department-list`
- `AdminRuleController`
  - `GET /admin/rule/list` -> `admin/rule-list`
  - `GET /admin/rule/form` -> `admin/rule-form`
- `AdminStaffController`
  - `GET /admin/staff/list` -> `admin/staff-list`
  - `GET /admin/staff/form` -> `admin/staff-form`
- `AdminItemController`
  - `GET /admin/item/list` -> `admin/item-list`
  - `GET /admin/item/form` -> `admin/item-form`

3. 사이드바 정합성 반영
- `department/rule/staff/item` 링크를 계층형 URL로 변경
  - `/admin/department/list`
  - `/admin/rule/list`
  - `/admin/staff/list`
  - `/admin/item/list`
- `전체 접수 목록` 메뉴 제거(해당 컨트롤러 삭제에 맞춤)

## 검증 결과
- 실행 명령: `./gradlew test`
- 결과: `BUILD SUCCESSFUL`

## 참조 문서
- 로컬: `AGENTS.md`, `.ai/memory.md`, `doc/PROJECT_STRUCTURE.md`, `doc/RULE.md`
- 워크플로: `doc/dev-c/workflow/workflow-005.md`
- 외부 documents 저장소 문서: 해당 작업은 구조 리팩터링 중심으로 미참조

## 남은 TODO / 리스크
- `reservation` 도메인도 향후 list 외 CRUD/취소 라우트를 도메인 컨트롤러 기준으로 확장 필요
- 사이드바 active 상태 플래그(`isAdminDepartment` 등) 세팅 로직 점검 필요
