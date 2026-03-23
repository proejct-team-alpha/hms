# Task 026 - 규칙 등록 경로 통일 및 저장

## 목적
- [x] `workflow-026` 범위를 실제 구현 가능한 작업 단위로 분해한다.
- [x] 관리자 규칙 등록 기능 `S31`을 `/admin/rule/new` 기준으로 구현하고 검증한다.
- [x] 경로 통일, 폼 재사용, 검증 실패 재렌더링, 성공 메시지까지 한 흐름으로 완성한다.

## Task 26-1. 현재 규칙 등록 구조와 문서 계약 점검
- [x] `AdminRuleController`, `AdminRuleService`, `HospitalRule`, 기존 템플릿 구조를 다시 확인한다.
- [x] 현재 `/admin/rule/form` GET/POST 흐름과 목록 화면의 등록 링크 위치를 정리한다.
- [x] `HospitalRule.title` 200자 제한과 PRD/API 문서의 내용 3000자 제한을 구현 기준으로 고정한다.
- [x] `API.md`의 `/admin/rule/create` 계약과 이번 `/admin/rule/new` 통일 결정의 차이를 메모한다.

## Task 26-2. 규칙 등록 요청 DTO와 검증 모델 설계
- [x] `admin.rule` 범위에 규칙 등록 요청 DTO를 추가한다.
- [x] `title` 필수, 최대 200자 검증을 DTO에 반영한다.
- [x] `content` 필수, 최대 3000자 검증을 DTO에 반영한다.
- [x] `category` 필수 선택 검증을 DTO에 반영한다.
- [x] `active` 체크박스 입력을 안전하게 받을 수 있는 필드 구조를 정리한다.

## Task 26-3. `/admin/rule/new` 컨트롤러와 저장 흐름 구현
- [x] `GET /admin/rule/new`가 등록 화면을 렌더링하도록 컨트롤러를 정리한다.
- [x] `POST /admin/rule/new`가 DTO 검증 후 서비스로 저장을 위임하도록 구현한다.
- [x] 저장 시 `title`, `content`, `category`, `active`가 모두 반영되도록 서비스/엔티티 호출부를 정리한다.
- [x] 저장 성공 후 `redirect:/admin/rule/list`로 이동하도록 PRG 흐름을 맞춘다.
- [x] 목록 화면에서 성공 플래시 메시지를 읽어 렌더링할 수 있도록 모델 계약을 정리한다.

## Task 26-4. 기존 `/admin/rule/form` 호환 경로와 링크 정리
- [x] 기존 `GET /admin/rule/form` 접근은 `redirect:/admin/rule/new`로 정리한다.
- [x] 기존 `POST /admin/rule/form` 처리 방식은 새 경로 기준으로 정리하거나 호환 리다이렉트 전략을 반영한다.
- [x] 규칙 목록 화면의 등록 버튼 링크를 `/admin/rule/new`로 교체한다.
- [x] 관리자 사이드바/기타 내부 링크 중 등록 화면 진입 경로가 있으면 함께 `/new`로 통일한다.

## Task 26-5. 규칙 등록 Mustache 화면과 실패 UX 구현
- [x] 기존 등록 템플릿 자산을 재사용해 `rule-new.mustache`로 정리한다.
- [x] 폼 `action`을 `POST /admin/rule/new`로 변경한다.
- [x] `active` 기본값 `true` 체크박스를 화면에 추가한다.
- [x] 검증 실패 시 제목, 내용, 카테고리, 활성 체크 상태가 유지되도록 Mustache 바인딩을 반영한다.
- [x] 필드 오류 메시지와 전역 오류 메시지를 화면에 표시한다.
- [x] 목록 화면에는 성공 메시지 영역을 추가한다.

## Task 26-6. 서비스 보강과 저장 로직 점검
- [x] `AdminRuleService.createRule(...)`가 활성 여부까지 저장하도록 시그니처와 내부 로직을 정리한다.
- [x] 카테고리 문자열 변환 시 잘못된 값 처리 방식을 검증 친화적으로 정리한다.
- [x] 엔티티 생성 경로에서 기본 활성값과 명시 입력값이 충돌하지 않도록 정리한다.
- [x] 필요하면 `HospitalRule.create(...)` 팩토리나 보조 메서드를 등록 요구사항에 맞게 보강한다.

## Task 26-7. 테스트 보강
- [x] 컨트롤러 테스트에 `GET /admin/rule/new` 렌더링 검증을 추가한다.
- [x] 컨트롤러 테스트에 저장 성공 시 목록 리다이렉트와 성공 메시지 플래시 검증을 추가한다.
- [x] 컨트롤러 테스트에 제목/내용/카테고리 검증 실패 시 재렌더링과 입력값 유지 검증을 추가한다.
- [x] 컨트롤러 테스트에 `active` 체크박스 기본값과 해제 시 `false` 처리 검증을 추가한다.
- [x] 필요 시 서비스 테스트에 저장 인자 반영과 카테고리 처리 검증을 추가한다.

## Task 26-8. 문서 및 최종 검증 마무리
- [x] `workflow-026` 기준 구현 결과를 다시 점검한다.
- [x] `task-026` 체크리스트를 완료 상태로 정리한다.
- [x] `admin.rule` 범위 테스트를 실행한다.
- [x] 필요 시 전체 `./gradlew test`를 확인한다.
- [x] 로컬 문서와 실제 구현 경로 차이가 남으면 후속 문서 정리 TODO를 남긴다.

## 완료 기준
- [x] `/admin/rule/new` GET/POST 흐름이 동작한다.
- [x] 제목, 내용, 카테고리, 활성 여부를 저장할 수 있다.
- [x] 활성 체크박스 기본값이 `true`로 동작한다.
- [x] 성공 시 목록 리다이렉트와 성공 메시지가 노출된다.
- [x] 실패 시 등록 화면 재렌더링, 입력값 유지, 오류 메시지 표시가 동작한다.
- [x] `/admin/rule/form` 경로와 목록 링크가 `/admin/rule/new` 기준으로 정리된다.
- [x] 관련 테스트가 통과한다.

## 추천 진행 순서
- [x] Task 26-1 현재 구조와 문서 계약을 먼저 점검한다.
- [x] Task 26-2 DTO와 검증 모델을 먼저 고정한다.
- [x] Task 26-3 컨트롤러/서비스 저장 흐름을 구현한다.
- [x] Task 26-4 호환 경로와 링크를 정리한다.
- [x] Task 26-5 템플릿과 실패 UX를 반영한다.
- [x] Task 26-6 서비스/엔티티 저장 로직을 보강한다.
- [x] Task 26-7 테스트를 보강한다.
- [x] Task 26-8 문서와 최종 검증을 마무리한다.

## 메모
- `workflow-026`은 사용자 인터뷰 결과를 반영한 구현 명세다.
- 이번 작업 기준 표준 경로는 `/admin/rule/new`다.
- 기존 로컬 API 문서에는 `POST /admin/rule/create`가 남아 있어 구현 후 문서 정합성 점검이 필요하다.
- 기존 등록 폼 자산은 버리지 않고 재사용하는 방향이므로 템플릿 rename 또는 내용 이동 방식 중 충돌이 적은 쪽을 선택한다.

## Task 26-1 점검 메모
- `AdminRuleController`
  - 현재 등록 화면은 `GET /admin/rule/form`, 저장은 `POST /admin/rule/form`으로 구현되어 있다.
  - 등록 화면 반환 뷰는 당시 기존 등록 뷰였다.
  - 저장 성공 후에는 단순히 `redirect:/admin/rule/list`만 수행하고, 성공 플래시 메시지는 아직 없다.
  - 입력은 `@RequestParam` 기반 `title`, `content`, `category`만 받고 있어 `active`와 검증/재렌더링 구조가 없다.
- `AdminRuleService`
  - 현재 `createRule(String title, String content, String category)` 시그니처만 제공한다.
  - 등록 시 `HospitalRule.create(title, content, HospitalRuleCategory.valueOf(category))`를 호출하므로 `active`를 명시 저장하지 못한다.
- `HospitalRule`
  - `title` 컬럼은 `length = 200`으로 정의되어 있어 제목 최대 길이 200자는 로컬 코드 기준으로 확정 가능하다.
  - `content`는 `TEXT` 컬럼이라 엔티티 자체에는 3000자 제한이 없다.
  - `active`는 엔티티 필드 기본값이 `true`이며, `update(...)`는 `active`를 받지만 `create(...)`는 받지 않는다.
- 기존 템플릿 구조
  - 등록 폼 템플릿은 초기에는 단일 등록 템플릿 하나였고, 폼 액션은 `/admin/rule/form`이었다.
  - 현재 폼에는 `category`, `title`, `content`만 있고 `active` 체크박스와 오류 메시지/입력값 유지 바인딩이 없다.
  - 규칙 목록 화면 `templates/admin/rule-list.mustache`의 등록 버튼 링크도 `/admin/rule/form`을 사용한다.
- 문서 계약 정리
  - `PRD.md`는 `F12-2`에서 규칙 등록 필드를 `제목`, `카테고리`, `내용(3000자 제한)`으로 정의하고, `S31` 경로를 `/admin/rule/new`로 정의한다.
  - `API.md`는 등록 화면을 `GET /admin/rule/new`, 반환 뷰를 `"admin/rule/new"`로 정의한다.
  - 반면 `API.md`의 등록 처리와 `SKILL_DEV_C.md`는 아직 `POST /admin/rule/create` 계약을 유지하고 있어, 현재 구현 목표인 `POST /admin/rule/new`와 차이가 있다.
- Task 26-1 결론
  - 구현 기준 경로는 사용자 결정과 PRD의 `S31`에 맞춰 `/admin/rule/new`로 잡는다.
  - 입력 제한은 `title 200자`, `content 3000자`를 기준으로 가져간다.
  - 다음 단계에서는 DTO + 검증 + `active` 입력 + 실패 재렌더링 + 성공 메시지 구조가 새로 필요하다.

## Task 26-2 구현 메모
- `AdminRuleCreateRequest` record를 `admin.rule` 패키지에 추가했다.
- DTO 필드는 `title`, `content`, `category`, `active` 4개로 정리했다.
- `title`은 `@NotBlank`와 `@Size(max = 200)`으로 검증한다.
- `content`는 `@NotBlank`와 `@Size(max = 3000)`으로 검증한다.
- `category`는 `HospitalRuleCategory` enum으로 받고 `@NotNull`로 필수 선택만 검증한다.
- `active`는 `Boolean`으로 받아 체크박스 미전송(`null`)과 기본값 주입(`true`)을 구분할 수 있게 했고, `defaultForm()`과 `isActiveChecked()` 보조 메서드를 추가했다.
- 다음 단계 컨트롤러는 초기 진입 시 `defaultForm()`을 모델에 넣고, POST에서는 `isActiveChecked()`와 enum 바인딩된 `category`를 사용해 저장 흐름을 연결하면 된다.

## Task 26-3 구현 메모
- `AdminRuleController`에 `GET /admin/rule/new`, `POST /admin/rule/new`를 추가하고, 기존 `GET/POST /admin/rule/form`은 같은 폼/저장 로직을 재사용하도록 유지했다.
- POST는 `@Valid @ModelAttribute("model") CreateAdminRuleRequest`, `BindingResult`, `RedirectAttributes`, `HttpServletRequest` 조합으로 정리했다.
- 검증 실패 시 `SsrValidationViewSupport.applyErrors(...)`를 적용하고 같은 `admin/rule-new` 뷰를 다시 렌더링한다.
- 성공 시 `RedirectView#setExposeModelAttributes(false)`를 사용해 공통 레이아웃 모델이 쿼리스트링으로 섞이지 않도록 막고, `successMessage` 플래시를 담아 `/admin/rule/list`로 보낸다.
- `AdminRuleService.createRule(...)`는 `CreateAdminRuleRequest`를 직접 받아 trim 된 `title/content`, enum `category`, 체크박스 `active`를 반영해 저장하고 `"규칙이 등록되었습니다."`를 반환한다.
- `rule-new.mustache`에는 `POST /admin/rule/new` action, `active` 체크박스, 입력값 유지용 바인딩, 필드 오류/전역 오류 렌더링을 추가했다.
- `rule-list.mustache`에는 `successMessage`, `errorMessage` 플래시 영역을 추가했다.
- 검증은 `./gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"`로 통과했다.

## Task 26-4 구현 메모
- `GET /admin/rule/form`은 더 이상 화면을 직접 렌더링하지 않고 `RedirectView`로 `/admin/rule/new`에 보낸다.
- `POST /admin/rule/form`은 사용자 입력 유실을 막기 위해 제거하지 않고, 새 경로와 같은 `handleCreate(...)` 로직을 타는 legacy 호환 엔드포인트로 유지했다.
- 규칙 목록의 등록 버튼 링크를 `/admin/rule/new`로 교체했다.
- `src/main` 기준으로 `/admin/rule/form`을 직접 가리키는 추가 내부 링크는 더 이상 남아 있지 않다.
- 컨트롤러 테스트에는 legacy GET 리다이렉트 검증과 목록 버튼 `/admin/rule/new` 링크 검증을 추가했다.
- 검증은 `./gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"`로 다시 통과했다.

## Task 26-5 구현 메모
- 규칙 등록 템플릿 파일을 `rule-new.mustache`로 통일하고, 컨트롤러의 신규/실패 렌더링 뷰 이름도 `admin/rule-new`로 맞췄다.
- `POST /admin/rule/form` legacy 호환 엔드포인트는 유지하되, 검증 실패 시에도 `admin/rule-new` same-view를 렌더링하도록 정리했다.
- `AdminRuleControllerTest`에는 legacy POST 검증 실패 시나리오를 추가해 `errorMessage`, 필드 오류, service 미호출을 검증했다.
- `src/main/resources/templates/admin/AI-CONTEXT.md`, `static/js/sidebar-admin.js`, `static/js/header-admin.js`, `workflow-026`, `task-026`에서 구 등록 템플릿/뷰 이름 참조를 제거했다.
- 과거 리포트, worklog, 코드리뷰 문서는 기록 보존을 위해 수정하지 않았다.

## Task 26-6 구현 메모
- `HospitalRule.create(...)`는 `active` 인자를 직접 받아 생성 시점에 최종 활성 상태를 확정하도록 보강했다.
- `AdminRuleService.createRule(...)`는 trim 된 제목/내용, enum 카테고리, `request.isActiveChecked()` 결과를 그대로 팩토리에 넘기고, 생성 후 상태를 뒤집는 토글 후처리를 제거했다.
- `HospitalRule`에는 이후 수정 흐름에서도 방향성 있게 상태를 다룰 수 있도록 `activate()`와 `deactivate()` 보조 메서드를 추가했다.
- `AdminRuleController.applyFormErrors(...)`를 추가해 기본 SSR 검증 오류를 세팅한 뒤 `category`의 enum 바인딩 실패(`typeMismatch`)는 `올바른 카테고리를 선택해 주세요.`로 공통 보정하도록 정리했다.
- 이 보정은 `POST /admin/rule/new`와 legacy `POST /admin/rule/form`이 모두 같은 로직을 재사용한다.

## Task 26-7 구현 메모
- `AdminRuleControllerTest`는 `GET /admin/rule/new` 렌더링, 저장 성공 시 목록 리다이렉트와 `successMessage` 플래시, missing category 재렌더링, invalid category 친절 메시지, `activeChecked` true/false 유지, legacy `GET/POST /admin/rule/form` 호환 흐름을 모두 검증한다.
- `AdminRuleServiceTest`는 trim 된 제목/내용 저장, enum 카테고리 반영, `active=true/false/null` 저장 결과, 목록 응답 DTO 매핑과 페이징 옵션을 검증한다.
- invalid category는 new POST와 legacy POST 모두 `admin/rule-new` same-view로 재렌더링되며 service 미호출을 확인한다.
- 기준 재검증 명령은 `./gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"`다.

## Task 26-8 구현 메모
- `workflow-026`의 구현 범위와 수용 기준을 현재 코드 기준으로 다시 점검했고, `/admin/rule/new` 표준 경로, legacy `/admin/rule/form` 호환, 성공 플래시, 실패 재렌더링이 모두 반영된 것을 확인했다.
- `task-026`의 목적, 완료 기준, 추천 진행 순서, Task 26-6/26-7/26-8 체크 상태를 현재 코드 truth에 맞게 동기화했다.
- `./gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"` 재실행 결과는 `BUILD SUCCESSFUL`이었다.
- 전체 회귀 확인용 `./gradlew.bat test --no-watch-fs`는 수행했지만, `DoctorTreatmentServiceTest`, `ReservationControllerTest`에서 기존과 무관한 실패가 있어 전체 테스트는 아직 녹색이 아니다.
- 후속 TODO는 로컬 `API.md`에 남아 있는 `POST /admin/rule/create` 계약을 현재 구현과 맞게 `/admin/rule/new`로 정리하는 것이다.
