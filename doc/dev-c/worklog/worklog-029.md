# task-029 작업 로그

## 작업 전 기준 문서/항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-029.md` 확인
- [x] `doc/dev-c/workflow/workflow-029.md` 확인
- [x] `doc/dev-c/.person/reports/task-029/` 보고서 확인

## 작업 목표

- 관리자 규칙 목록 화면에서 제목 링크가 아니라 행 전체 클릭으로 상세 화면에 진입하게 만든다.
- 각 규칙 행에 별도 삭제 버튼을 두고, 기존 `POST /admin/api/rules/{id}` 삭제 API를 재사용한다.
- 삭제 버튼 클릭은 행 클릭과 분리해 상세 이동이 함께 트리거되지 않도록 처리한다.
- 삭제 성공 시 목록은 유지한 채 해당 행만 제거하고 성공 toast를 보여준다.
- 삭제 실패 시 같은 화면에서 실패 toast를 보여주고, 마지막 행 삭제 시 empty state를 자연스럽게 노출한다.

## 보고서/리포트 소스

- `doc/dev-c/.person/reports/task-029/report-20260321-2013-task-29.md`

## 변경 파일

- `src/main/resources/templates/admin/rule-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleControllerTest.java`
- `doc/dev-c/task/task-029.md`
- `doc/dev-c/workflow/workflow-029.md`

## 구현 내용

### 1. 규칙 목록 진입 UX를 제목 링크에서 행 전체 클릭으로 바꿨다

- 기존에는 제목 텍스트만 상세 화면 진입 지점이어서 사용자가 클릭 가능한 영역을 좁게 인식할 수 있었다.
- 각 행에 `data-rule-row`, `data-rule-detail-url`, `tabindex="0"`를 넣어 행 전체가 상세 이동 트리거가 되도록 바꿨다.
- 상세 이동 경로는 기존 계약을 유지해 `GET /admin/rule/detail?ruleId={id}`를 그대로 사용했다.
- 마우스 클릭뿐 아니라 키보드 `Enter`로도 상세 이동이 가능하도록 이벤트를 묶었다.

### 2. 같은 행 안에 삭제 버튼을 별도로 두고 상세 이동과 충돌하지 않게 분리했다

- 목록 테이블에 `관리` 컬럼을 추가하고 각 행마다 `삭제` 버튼을 배치했다.
- 삭제 버튼에는 `data-rule-delete-button`, `data-rule-delete-url` 속성을 붙여 기존 삭제 API와 연결했다.
- 삭제 버튼 클릭 시 `event.preventDefault()`와 `event.stopPropagation()`을 적용해 행 클릭으로 인한 상세 이동이 함께 발생하지 않도록 막았다.
- 삭제 전 확인은 요구사항대로 브라우저 기본 `confirm()`을 사용했다.

### 3. 기존 관리자 삭제 API를 그대로 재사용했다

- 목록 화면에서 새 API를 만들지 않고 기존 `POST /admin/api/rules/{id}`를 `fetch`로 호출하도록 연결했다.
- CSRF 토큰은 템플릿에서 주입해 기존 관리자 API 패턴과 동일하게 요청 헤더에 실었다.
- 응답은 `Resp.ok(...)` 기반 성공 포맷을 그대로 소비하고, 실패 시에도 공통 예외 응답 메시지를 읽어 toast로 보여주도록 처리했다.

### 4. 삭제 성공/실패 후속 UX를 목록 유지 기준으로 정리했다

- 삭제 성공 시 전체 페이지를 다시 불러오지 않고 해당 `<tr>`만 DOM에서 제거하도록 구현했다.
- 마지막 데이터 행이 사라지면 JS가 empty state 행을 동적으로 만들어 `"조회된 규칙이 없습니다."` 상태로 전환한다.
- 화면 상단에는 고정 toast 영역을 추가해 성공 시 성공 메시지, 실패 시 실패 메시지를 같은 위치에 표시하도록 맞췄다.
- 실패 응답은 `RESOURCE_NOT_FOUND`, `ACCESS_DENIED`, 일반 서버 오류를 구분해 가능한 한 자연스러운 문구로 보여주도록 보강했다.

### 5. 렌더링 회귀 테스트와 작업 문서를 현재 상태에 맞춰 마감했다

- `AdminRuleControllerTest`에 목록 화면이 `data-rule-list-page`, `data-rule-detail-url`, `data-rule-delete-url`, `data-rule-delete-button`을 포함하는지 검증을 추가했다.
- 삭제 API 계약 자체는 기존 `AdminRuleApiControllerTest`를 그대로 재사용해 충돌 없이 함께 통과하는지 확인했다.
- `task-029.md`는 완료 체크와 구현 메모 기준으로 마감했고, `workflow-029.md`는 실제 구현이 인터뷰 명세 범위 안에서 끝난 상태로 유지했다.

## 검증 결과

- `.\gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleApiControllerTest"` : `BUILD SUCCESSFUL`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-029.md`
- 로컬: `doc/dev-c/workflow/workflow-029.md`

## 남은 TODO / 리스크

- 행 전체 클릭 UX는 JS 기반이므로 실제 브라우저에서 마우스/키보드 접근성까지 한 번 더 수동 확인하면 더 안전하다.
- toast는 현재 규칙 목록 화면 내부 구현이므로, 이후 관리자 공통 toast 컴포넌트가 필요해질 수 있다.
- 이번 검증은 `admin.rule` 범위 테스트 중심으로 끝냈고, 프로젝트 전체 테스트 상태와는 분리해서 보는 편이 맞다.
