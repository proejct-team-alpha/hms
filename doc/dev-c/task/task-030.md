# task-030 규칙 등록/수정 DTO 기본 검증 및 SSR 오류 메시지 정리

## 작업 전 기준 문서/항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/workflow/workflow-030.md` 확인

## 작업 목표
- 규칙 등록 DTO와 수정 DTO의 기본 검증 규칙을 문서 기준으로 정리한다.
- `title`, `content`, `category`의 필수/길이 제한 계약을 DTO 어노테이션과 메시지로 고정한다.
- 등록 실패는 `rule-new`, 수정 실패는 `rule-detail` same-view 재렌더링으로 처리한다.
- 두 화면 모두 입력값 유지, 필드별 오류 메시지 표시, 카테고리 옵션 재세팅이 되도록 맞춘다.
- 컨트롤러/서비스 테스트에서 빈 값, 길이 초과, category 누락 시나리오를 보강한다.

## 작업 단계

### Task 30-1. 현재 DTO/컨트롤러 검증 구조 점검
- [x] `CreateAdminRuleRequest`, `UpdateAdminRuleRequest`의 현재 검증 상태 확인
- [x] `AdminRuleController` 등록/수정 실패 재렌더링 흐름 확인
- [x] `rule-new`, `rule-detail`의 오류 메시지 바인딩 포인트 확인
- [x] 구현 메모를 남긴다

구현 메모:
- `CreateAdminRuleRequest`, `UpdateAdminRuleRequest` 모두 `title`, `content`, `category`에 대해 `@NotBlank` / `@Size` / `@NotNull` 기본 검증을 이미 가지고 있다.
- 수정 DTO에는 추가로 `ruleId`에 대한 `@NotNull` 검증이 들어가 있어 수정 요청 식별자 누락도 방지하고 있다.
- 다만 현재 메시지 문구는 인터뷰에서 새로 고정한 계약보다 이전 표현(`필수입니다`, `200자 이하로 입력해 주세요`)에 가까워서, 다음 단계에서 문구 정렬이 필요하다.
- `AdminRuleController`는 등록과 수정 모두 `@Valid @ModelAttribute + BindingResult` 패턴을 사용하고 있으며, 실패 시 등록은 `admin/rule-new`, 수정은 `admin/rule-detail` same-view 재렌더링 구조가 이미 잡혀 있다.
- 오류 속성은 `SsrValidationViewSupport.applyErrors(...)`가 `titleError`, `contentError`, `categoryError` 같은 요청 속성으로 올려주고, invalid category는 `applyFormErrors(...)`에서 친절한 별도 메시지로 덮어쓴다.
- `admin/_rule-form.mustache`는 이미 `titleError`, `contentError`, `categoryError`를 출력하고 있고, 컨트롤러의 `populateFormAttributes(...)`는 카테고리 선택 상태와 `activeChecked`를 다시 세팅하므로 실패 재렌더링에 필요한 기본 뼈대는 확보되어 있다.

### Task 30-2. 등록 DTO 기본 검증 규칙 정리
- [x] `title` 필수 + 200자 제한 반영
- [x] `content` 필수 + 3000자 제한 반영
- [x] `category` 필수 반영
- [x] 메시지 문자열을 DTO 어노테이션에 직접 선언한다

구현 메모:
- `CreateAdminRuleRequest`의 기본 검증 구조 자체는 이미 존재했기 때문에, 이번 단계에서는 규칙을 새로 추가하기보다 메시지 계약을 인터뷰에서 고정한 문구로 정렬했다.
- 등록 DTO 메시지는 다음 기준으로 맞췄다.
  - `제목을 입력해 주세요.`
  - `내용을 입력해 주세요.`
  - `제목은 200자 이하로 입력해 주세요.`
  - `내용은 3000자 이하로 입력해 주세요.`
  - `카테고리를 선택해 주세요.`
- 기존 `AdminRuleControllerTest`의 등록 실패 기대값도 새 메시지 문구 기준으로 최소 범위만 함께 보정했다.

### Task 30-3. 수정 DTO 기본 검증 규칙 정리
- [x] 등록 DTO와 동일한 검증 규칙을 수정 DTO에 반영한다
- [x] 메시지 문자열도 등록과 동일하게 맞춘다
- [x] 수정 요청에 필요한 다른 필드와 충돌하지 않는지 확인한다

구현 메모:
- `UpdateAdminRuleRequest`의 `title`, `content`, `category` 메시지를 등록 DTO와 동일한 계약으로 맞췄다.
- 수정 DTO는 등록 DTO와 달리 `ruleId`가 추가로 필요하므로, 이 필드의 `@NotNull` 메시지도 읽기 가능한 형태인 `규칙 ID는 필수입니다.`로 함께 정리했다.
- 수정 실패 기대값을 검증하는 `AdminRuleControllerTest`도 새 메시지 문구 기준으로 최소 범위만 함께 보정했다.

### Task 30-4. 등록 실패 same-view 재렌더링 점검/보강
- [x] `POST /admin/rule/new` 실패 시 `admin/rule-new` 반환 확인
- [x] 입력값 유지와 필드 오류 메시지 표시 확인
- [x] 카테고리 옵션 데이터 재세팅 확인
- [x] 기존 invalid category 처리와 충돌하지 않도록 정리한다

구현 메모:
- 컨트롤러의 등록 실패 same-view 구조 자체는 이미 `handleCreate(...) -> applyFormErrors(...) -> renderForm(...)` 흐름으로 잡혀 있어서, 이번 단계에서는 production 코드보다 회귀 테스트 보강이 중심이었다.
- 등록 실패 시나리오로 다음 두 케이스를 추가해 동작을 고정했다.
  - `title` 200자 초과 시 `admin/rule-new` same-view 재렌더링 + `titleError` 노출 + 선택한 `category` 유지
  - `category` 누락 시 `admin/rule-new` same-view 재렌더링 + `categoryError` 노출 + `title` / `content` 입력값 유지
- 기존 invalid category 친절 메시지 케이스는 그대로 두고, 이번 단계에서 추가한 category 누락 required 메시지 케이스와 충돌하지 않는다는 점을 함께 확인했다.

### Task 30-5. 수정 실패 same-view 재렌더링 점검/보강
- [x] `POST /admin/rule/update` 실패 시 `admin/rule-detail` 반환 확인
- [x] 입력값 유지와 필드 오류 메시지 표시 확인
- [x] 카테고리 옵션 데이터 재세팅 확인
- [x] 상세 정보 모델과 수정 폼 모델 충돌이 없는지 확인한다

구현 메모:
- 수정 실패 same-view 구조도 이미 `renderUpdateValidationFailure(...) -> renderDetail(...)` 흐름으로 구현돼 있어서, 이번 단계에서는 테스트 보강으로 동작을 고정했다.
- 수정 실패 시나리오로 다음 두 케이스를 추가했다.
  - `title` 200자 초과 시 `admin/rule-detail` same-view 재렌더링 + `titleError` 노출 + 선택한 `category` 유지
  - `category` 누락 시 `admin/rule-detail` same-view 재렌더링 + `categoryError` 노출 + 수정한 `title` / `content` 유지
- 두 테스트 모두 `request().attribute("rule", detail)`와 `request().attribute("model", invalidRequest)`를 함께 검증해 상세 정보 모델과 수정 폼 모델이 동시에 유지된다는 점을 고정했다.
- 체크박스를 보내지 않은 수정 요청은 DTO의 `active=false`가 아니라 `active=null`로 바인딩되고, 컨트롤러가 이를 `activeChecked=false`로 해석한다는 점도 이번 단계에서 테스트로 확인했다.

### Task 30-6. 템플릿 오류 메시지 바인딩 점검
- [x] `rule-new.mustache`에서 필드별 오류 메시지 노출 확인
- [x] `rule-detail.mustache`에서 필드별 오류 메시지 노출 확인
- [x] 공통 partial 사용 시 등록/수정 모두 같은 문구가 보이는지 확인
- [x] 필요하면 최소 범위로 템플릿 바인딩을 보강한다

구현 메모:
- `admin/_rule-form.mustache`가 이미 `titleError`, `contentError`, `categoryError`를 같은 마크업 구조로 출력하고 있어 등록/수정 폼이 같은 오류 메시지 계약을 공유한다.
- `rule-new.mustache`와 `rule-detail.mustache` 모두 상단 `errorMessage` 영역을 가지고 있고, 실제 폼 출력은 같은 partial을 include하므로 인터뷰에서 정한 "공통 문구 + same-view 재렌더링" 계약과 현재 템플릿 구조가 일치한다.
- 이번 단계에서는 템플릿 바인딩을 추가로 수정할 필요가 없었고, 대신 `AdminRuleControllerTest` 재실행으로 등록/수정 실패 렌더링 회귀가 없는지만 다시 확인했다.

### Task 30-7. 테스트 보강
- [x] `AdminRuleControllerTest`에 등록/수정 빈 값 실패 시나리오 추가
- [x] `AdminRuleControllerTest`에 길이 초과 실패 시나리오 추가
- [x] `AdminRuleControllerTest`에 category 누락 실패 시나리오 추가
- [x] `AdminRuleServiceTest`에서 관련 계약 검증이 필요하면 보강한다
- [x] 검증 명령과 결과를 메모한다

구현 메모:
- 등록/수정의 빈 값 실패 시나리오는 기존 `createRule_withValidationFailure...`, `updateRule_withValidationFailure...` 테스트에서 이미 고정돼 있었고, category 누락 시나리오도 앞선 단계에서 등록/수정 각각 추가돼 있었다.
- 이번 단계에서는 길이 초과 검증 범위를 문서 계약과 맞추기 위해 `content` 3000자 초과 케이스를 등록/수정 각각에 추가했다.
  - `createRule_withContentTooLong_rerendersFormAndPreservesInputs`
  - `updateRule_withContentTooLong_rerendersDetailViewAndPreservesEditedInputs`
- 두 테스트 모두 same-view 재렌더링, `contentError` 문구 노출, 선택한 `category` 유지, 수정 중 입력값 유지까지 함께 확인한다.
- `AdminRuleServiceTest`는 DTO 검증이 서비스 진입 전 컨트롤러에서 종료되는 구조라 별도 검증 계약 추가는 하지 않았고, 대신 회귀 확인 차원에서 함께 재실행했다.
- 검증 명령:
  - `.\gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"`
- 검증 결과:
  - `BUILD SUCCESSFUL`

### Task 30-8. 문서 및 최종 검증 마감
- [x] `task-030.md` 체크리스트를 실제 구현 기준으로 갱신한다
- [x] 필요 시 `AI-CONTEXT.md`, `FILES.md` 또는 workflow와 차이를 정리한다
- [x] 최종 테스트 결과와 남은 리스크를 정리한다
- [x] `doc/dev-c/.person/reports/task-030/` 아래 리포트를 작성한다

구현 메모:
- `task-030.md`의 Task 30-1 ~ 30-7 결과를 기준으로 Task 30-8, 완료 기준, 검증 결과를 모두 완료 상태로 갱신했다.
- `workflow-030.md`의 수용 기준도 현재 구현과 테스트 결과를 기준으로 완료 처리했다.
- `src/main/java/com/smartclinic/hms/admin/rule/AI-CONTEXT.md`, `src/main/java/com/smartclinic/hms/admin/rule/FILES.md`는 이번 작업으로 파일 역할이나 패키지 설명이 달라지지 않아 추가 수정 없이 유지했다.
- 최종 검증은 `AdminRuleControllerTest`, `AdminRuleServiceTest` 대상 명령을 다시 실행해 `BUILD SUCCESSFUL`을 확인했다.

## 완료 기준
- [x] 등록 DTO와 수정 DTO에 `title`, `content`, `category` 기본 검증이 선언된다
- [x] 등록/수정 모두 동일한 오류 메시지 계약을 사용한다
- [x] 등록 실패 시 `admin/rule-new` same-view 재렌더링이 된다
- [x] 수정 실패 시 `admin/rule-detail` same-view 재렌더링이 된다
- [x] 입력값 유지, 필드 오류 메시지, 카테고리 옵션 재세팅이 동작한다
- [x] 관련 테스트가 통과한다

## 검증 결과
- [x] `.\gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"` 통과

## 남은 TODO / 리스크
- 기존 invalid category 친절 메시지 처리와 이번 기본 검증 메시지가 계속 충돌하지 않도록 이후 검증 로직 변경 시 회귀 테스트를 같이 유지할 필요가 있다.
- 메시지를 DTO 어노테이션에 직접 넣는 방식은 이번 범위에는 적합하지만, 이후 메시지 수가 늘어나면 공통 메시지 체계 분리를 다시 검토할 수 있다.
- 수정 화면은 상세/수정 통합 화면이라 same-view 재렌더링 시 상세 정보 모델과 폼 모델이 함께 깨지지 않는지 주의가 필요하다.
