# task-030 작업 로그

## 작업 전 기준 문서/항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-030.md` 확인
- [x] `doc/dev-c/workflow/workflow-030.md` 확인
- [x] `doc/dev-c/.person/reports/task-030/` 보고서 확인

## 작업 목표

- 규칙 등록/수정 DTO의 기본 검증 계약을 문서 기준으로 코드에 맞춘다.
- `title`, `content`, `category`의 필수/길이 제한 메시지를 등록/수정 DTO 모두 동일한 계약으로 정리한다.
- 등록 실패는 `admin/rule-new`, 수정 실패는 `admin/rule-detail` same-view 재렌더링으로 유지하고 입력값/오류 메시지/카테고리 옵션 재세팅이 안정적으로 동작하는지 확인한다.
- Mustache 템플릿과 컨트롤러 테스트를 통해 SSR 오류 메시지 바인딩 계약을 명확히 고정한다.
- 단계별 보고서를 바탕으로 `task-030`, `workflow-030` 문서를 실제 구현 상태에 맞게 마감한다.

## 보고서/리포트 소스

- `doc/dev-c/.person/reports/task-030/report-20260321-2042-task-30-1.md`
- `doc/dev-c/.person/reports/task-030/report-20260321-2048-task-30-2.md`
- `doc/dev-c/.person/reports/task-030/report-20260321-2050-task-30-3.md`
- `doc/dev-c/.person/reports/task-030/report-20260321-2053-task-30-4.md`
- `doc/dev-c/.person/reports/task-030/report-20260321-2057-task-30-5.md`
- `doc/dev-c/.person/reports/task-030/report-20260321-2103-task-30-6.md`
- `doc/dev-c/.person/reports/task-030/report-20260321-2112-task-30-7.md`
- `doc/dev-c/.person/reports/task-030/report-20260321-2121-task-30-8.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/rule/dto/CreateAdminRuleRequest.java`
- `src/main/java/com/smartclinic/hms/admin/rule/dto/UpdateAdminRuleRequest.java`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleControllerTest.java`
- `doc/dev-c/task/task-030.md`
- `doc/dev-c/workflow/workflow-030.md`

## 구현 내용

### 1. 기존 DTO/컨트롤러/템플릿 구조를 먼저 점검했다

- `CreateAdminRuleRequest`, `UpdateAdminRuleRequest`, `AdminRuleController`, `rule-new.mustache`, `rule-detail.mustache`, `admin/_rule-form.mustache`를 먼저 읽어 현재 검증 구조와 SSR 재렌더링 흐름을 확인했다.
- 점검 결과, 기본 검증 뼈대와 same-view 재렌더링 구조는 이미 있었고 이번 작업의 핵심은 “문구 계약 정렬 + 테스트 보강 + 문서 마감”이라는 점을 확인했다.

### 2. 등록 DTO의 기본 검증 메시지를 문서 계약으로 맞췄다

- `CreateAdminRuleRequest`의 `title`, `content`, `category` 메시지를 인터뷰에서 고정한 문구로 정렬했다.
- 적용 메시지:
  - `제목을 입력해 주세요.`
  - `내용을 입력해 주세요.`
  - `제목은 200자 이하로 입력해 주세요.`
  - `내용은 3000자 이하로 입력해 주세요.`
  - `카테고리를 선택해 주세요.`
- 이 변경에 맞춰 등록 실패 기대값은 `AdminRuleControllerTest`에서 함께 보정했다.

### 3. 수정 DTO도 등록 DTO와 같은 계약으로 정리했다

- `UpdateAdminRuleRequest`의 `title`, `content`, `category` 메시지를 등록 DTO와 동일한 기준으로 통일했다.
- 수정 DTO에만 존재하는 `ruleId`는 별도 필수 메시지 `규칙 ID는 필수입니다.`로 정리했다.
- 그 결과 등록/수정 화면이 같은 오류 문구 계약을 사용하게 됐다.

### 4. 등록 실패 same-view 흐름을 테스트로 고정했다

- `AdminRuleControllerTest`에서 등록 실패 시 `admin/rule-new` 재렌더링, 입력값 유지, `titleError` / `categoryError` 노출, 카테고리 선택 상태 유지까지 검증했다.
- 이 단계에서 `title` 길이 초과와 `category` 누락 케이스를 추가해 기존 invalid category 친절 메시지와 required 메시지 케이스가 충돌하지 않는다는 점도 확인했다.

### 5. 수정 실패 same-view 흐름도 테스트로 고정했다

- 수정 실패 시 `admin/rule-detail` 재렌더링, `rule` 상세 모델과 `model` 폼 모델의 공존, 입력값 유지, `titleError` / `categoryError` 노출을 검증했다.
- 체크박스를 보내지 않은 수정 요청이 DTO에서는 `active=null`로 바인딩되고, 화면에서는 `activeChecked=false`로 처리된다는 현재 구현 특성도 테스트로 확인했다.

### 6. 템플릿 오류 메시지 바인딩은 이미 공통 partial 구조로 정리돼 있음을 확인했다

- `_rule-form.mustache`가 이미 `titleError`, `contentError`, `categoryError`를 공통 마크업으로 출력하고 있어 등록/수정 양쪽이 같은 오류 키를 공유하고 있었다.
- `rule-new.mustache`, `rule-detail.mustache` 둘 다 상단 `errorMessage` 영역을 가지고 있고 실제 필드 출력은 같은 partial을 include하므로, 이번 단계에서는 템플릿 코드를 더 수정할 필요가 없었다.

### 7. 테스트 공백이던 `content` 길이 초과 케이스를 추가했다

- `AdminRuleControllerTest`에 다음 두 케이스를 추가해 `content` 3000자 초과 검증도 등록/수정 양쪽에서 보장되도록 했다.
  - `createRule_withContentTooLong_rerendersFormAndPreservesInputs`
  - `updateRule_withContentTooLong_rerendersDetailViewAndPreservesEditedInputs`
- 서비스 레이어는 DTO 검증 전에 호출되지 않는 구조라 `AdminRuleServiceTest`에는 새 검증 케이스를 넣지 않고, 회귀 확인 차원에서 함께 재실행했다.

### 8. 문서와 최종 검증을 현재 구현 상태 기준으로 마감했다

- `task-030.md`의 Task 30-1 ~ 30-8, 완료 기준, 검증 결과를 모두 완료 상태로 갱신했다.
- `workflow-030.md` 수용 기준도 실제 구현과 테스트 결과에 맞춰 완료 처리했다.
- `AI-CONTEXT.md`, `FILES.md`는 이번 작업으로 파일 역할 설명이 달라지지 않아 수정 없이 유지했다.

## 검증 결과

- `.\gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest"` : `BUILD SUCCESSFUL`
- `.\gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"` : `BUILD SUCCESSFUL`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-030.md`
- 로컬: `doc/dev-c/workflow/workflow-030.md`

## 남은 TODO / 리스크

- 기존 invalid category 친절 메시지와 기본 required 메시지가 이후에도 충돌하지 않도록 검증 로직 변경 시 회귀 테스트를 같이 유지할 필요가 있다.
- 메시지를 DTO 어노테이션에 직접 두는 방식은 이번 범위에는 적합하지만, 메시지 수가 늘어나면 공통 메시지 체계 분리를 다시 검토할 수 있다.
- 수정 화면은 상세/수정 통합 화면이라 same-view 재렌더링 시 상세 정보 모델과 폼 모델이 함께 유지되는지 계속 주의가 필요하다.
