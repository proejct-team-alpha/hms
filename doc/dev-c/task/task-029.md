# task-029 규칙 목록 상세 진입 및 삭제 UX 개선

## 작업 전 기준 문서/항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/workflow/workflow-029.md` 확인

## 작업 목표
- 관리자 규칙 목록 화면에서 각 행 전체 클릭으로 상세 화면에 진입할 수 있게 한다.
- 규칙 목록 각 행에 별도 삭제 버튼을 추가하고, 기존 `POST /admin/api/rules/{id}` 삭제 API를 연결한다.
- 삭제 성공 시 목록은 유지한 채 해당 행만 제거하고 성공 toast를 보여준다.
- 삭제 실패 시 같은 화면에서 실패 toast를 보여준다.
- 마지막 행 삭제 시 empty state가 자연스럽게 보이도록 처리한다.

## 작업 단계

### Task 29-1. 현재 목록 화면 구조와 연동 포인트 점검
- [x] `rule-list.mustache`의 현재 제목 링크, hover 스타일, 행 구조 확인
- [x] 기존 `AdminRuleApiController` / `AdminRuleDeleteResponse` / 삭제 API 응답 형식 재확인
- [x] 목록 화면에 toast 메시지를 둘 위치와 DOM 구조 메모
- [x] 구현 메모를 남긴다

구현 메모:
- 목록 화면은 기존에 제목 anchor만 상세 진입 포인트였다.
- 삭제 API는 이미 `POST /admin/api/rules/{id}` + `Resp.ok(...)` 계약으로 구현되어 있었다.
- 목록 유지형 UX를 위해 별도 toast DOM과 행별 data attribute 기반 JS 연결이 적합하다고 판단했다.

### Task 29-2. 목록 행 전체 클릭 상세 이동 구조 반영
- [x] 제목 링크 중심 구조를 행 전체 클릭 구조로 바꾼다
- [x] 상세 이동 URL은 `GET /admin/rule/detail?ruleId={id}`로 유지한다
- [x] 접근성/시각적 힌트를 해치지 않는 선에서 row clickable UX를 정리한다
- [x] hover 스타일과 충돌이 없는지 확인한다

구현 메모:
- 각 데이터 행에 `data-rule-row`, `data-rule-detail-url`, `tabindex="0"`를 부여했다.
- 마우스 클릭은 행 전체에서 상세 이동되고, Enter 키로도 이동 가능하게 처리했다.

### Task 29-3. 행별 삭제 버튼 UI 추가
- [x] 각 행에 별도 `삭제` 버튼을 추가한다
- [x] 삭제 버튼은 행 클릭과 시각적으로 구분되게 배치한다
- [x] empty state 행과 일반 데이터 행의 구조를 함께 점검한다
- [x] 구현 메모를 남긴다

구현 메모:
- 테이블에 `관리` 컬럼을 추가하고, 각 행 우측에 `삭제` 버튼을 배치했다.
- empty state의 `colspan`은 관리 컬럼 추가에 맞춰 4로 조정했다.

### Task 29-4. 삭제 AJAX 연동 및 이벤트 분리
- [x] 삭제 버튼 클릭 시 `confirm()` 확인 절차를 추가한다
- [x] `POST /admin/api/rules/{id}`를 AJAX로 호출한다
- [x] CSRF 토큰을 포함해 기존 관리자 API 패턴과 맞춘다
- [x] 삭제 버튼 클릭 시 행 상세 이동이 트리거되지 않도록 이벤트 전파를 차단한다

구현 메모:
- 삭제 버튼은 `data-rule-delete-button`, `data-rule-delete-url` 속성으로 연결했다.
- `event.preventDefault()`와 `event.stopPropagation()`으로 행 클릭과 삭제 버튼 동작을 분리했다.
- 응답은 JSON 우선 파싱하고, 비JSON 응답도 안전하게 실패 toast로 처리하도록 보강했다.

### Task 29-5. 성공/실패 toast와 DOM 제거 처리
- [x] 삭제 성공 시 성공 toast를 보여준다
- [x] 삭제 성공 시 목록은 유지하고 해당 행만 DOM에서 제거한다
- [x] 삭제 실패 시 실패 toast를 보여준다
- [x] 마지막 행 삭제 시 “조회된 규칙이 없습니다.” empty state로 전환한다

구현 메모:
- 목록 상단에 고정형 toast 영역을 추가했다.
- 삭제 성공 시 해당 행만 제거하고, 남은 행이 없으면 JS에서 empty state row를 동적으로 생성한다.
- 실패 시 `RESOURCE_NOT_FOUND`, `ACCESS_DENIED`, 일반 서버 오류를 구분해 메시지를 보여준다.

### Task 29-6. 테스트 보강
- [x] `AdminRuleControllerTest`에서 목록 렌더링에 상세 이동/삭제 연동 마크업이 포함되는지 검증한다
- [x] 필요 시 삭제 API 테스트와 충돌이 없는지 함께 확인한다
- [x] 최소 범위 테스트 명령을 정리한다
- [x] 검증 메모를 남긴다

검증 메모:
- `AdminRuleControllerTest`에서 `data-rule-list-page`, `data-rule-detail-url`, `data-rule-delete-url`, `data-rule-delete-button` 렌더링을 검증했다.
- `AdminRuleApiControllerTest`도 함께 실행해 삭제 API 계약이 유지되는지 재확인했다.

### Task 29-7. 문서 동기화
- [x] `admin.rule` 관련 `AI-CONTEXT.md`, `FILES.md`가 변경되면 반영한다
- [x] 필요 시 `workflow-029.md`와 실제 구현 차이를 정리한다
- [x] 작업 결과를 기준으로 `task-029.md` 체크를 갱신한다

구현 메모:
- 이번 작업은 템플릿/테스트 중심이라 `AI-CONTEXT.md`, `FILES.md` 수정은 필요하지 않았다.
- 구현은 `workflow-029.md` 범위 안에서 마감됐다.

### Task 29-8. 최종 검증 및 보고서
- [x] 실행한 테스트 결과를 정리한다
- [x] 남은 리스크와 후속 TODO를 정리한다
- [x] `doc/dev-c/.person/reports/task-029/` 아래 보고서를 작성한다
- [x] 필요 시 `worklog` 연결 포인트를 메모한다

## 완료 기준
- [x] 규칙 목록 행 전체 클릭으로 상세 화면에 이동한다
- [x] 같은 행의 삭제 버튼 클릭은 상세 이동 없이 동작한다
- [x] 삭제 버튼은 기존 `POST /admin/api/rules/{id}`를 사용한다
- [x] 삭제 성공 시 해당 행만 제거되고 성공 toast가 보인다
- [x] 삭제 실패 시 실패 toast가 보인다
- [x] 마지막 행 삭제 시 empty state가 정상 노출된다
- [x] 관련 테스트가 통과한다

## 검증 결과
- [x] `.\gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleApiControllerTest"` 통과

## 남은 TODO / 리스크
- 목록 행 전체 클릭 UX는 JS 기반이므로, 브라우저별 포커스/키보드 동작은 추후 수동 확인이 있으면 더 좋다.
- 현재 toast는 목록 화면 내부 구현이므로, 이후 관리자 화면 전반에 공통 toast 컴포넌트가 필요해질 수 있다.
- 전체 테스트 스위트는 이번 작업 범위 밖 기존 실패가 있을 수 있으므로, 이번 태스크는 `admin.rule` 범위 테스트 기준으로 검증했다.
