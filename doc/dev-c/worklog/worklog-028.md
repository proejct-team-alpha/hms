# task-028 작업 로그

## 작업 전 기준 문서/항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-028.md` 확인
- [x] `doc/dev-c/workflow/workflow-028.md` 확인
- [x] `doc/dev-c/.person/reports/task-028/` 보고서 확인

## 작업 목표

- 관리자 전용 규칙 삭제 JSON API를 `POST /admin/api/rules/{id}` 기준으로 구현한다.
- 응답은 `Resp.ok(...)` 공통 래퍼를 사용하고, body는 `ruleId`, `message`만 담는 최소 구조로 유지한다.
- 삭제는 `active=false` 비활성화가 아니라 실제 엔티티 물리 삭제로 처리한다.
- 존재하지 않는 규칙 삭제는 `CustomException.notFound(...)`와 `GlobalExceptionHandler -> Resp.fail(...)` 공통 실패 응답으로 처리한다.
- 관리자 권한, JSON 응답 구조, 물리 삭제 호출, 문서/컨텍스트 파일까지 함께 정리해 `task-028`을 마감한다.

## 보고서/리포트 소스

- `doc/dev-c/.person/reports/task-028/report-20260321-1913-task-28-1.md`
- `doc/dev-c/.person/reports/task-028/report-20260321-1920-task-28-2.md`
- `doc/dev-c/.person/reports/task-028/report-20260321-1927-task-28-3.md`
- `doc/dev-c/.person/reports/task-028/report-20260321-1933-task-28-5.md`
- `doc/dev-c/.person/reports/task-028/report-20260321-1937-task-28-6.md`
- `doc/dev-c/.person/reports/task-028/report-20260321-1938-task-28-7.md`
- `doc/dev-c/.person/reports/task-028/report-20260321-1943-task-28-8.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleApiController.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleService.java`
- `src/main/java/com/smartclinic/hms/admin/rule/dto/AdminRuleDeleteResponse.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AI-CONTEXT.md`
- `src/main/java/com/smartclinic/hms/admin/rule/FILES.md`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleApiControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleServiceTest.java`
- `doc/dev-c/task/task-028.md`
- `doc/dev-c/workflow/workflow-028.md`

## 구현 내용

### 1. 규칙 삭제 계약을 관리자 JSON API 기준으로 고정했다

- 인터뷰와 워크플로우 기준으로 삭제 경로를 `POST /admin/api/rules/{id}`로 확정했다.
- 초기 후보였던 `DELETE /api/rules/{id}`와 `DELETE /admin/api/rules/{id}`는 비채택 경로로 정리했다.
- 이 API는 SSR 리다이렉트용이 아니라 AJAX 호출용 JSON API로 두고, 후속 UX는 클라이언트가 결정하게 했다.

### 2. 삭제 성공 응답 DTO를 최소 구조로 설계했다

- [AdminRuleDeleteResponse.java](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/admin/rule/dto/AdminRuleDeleteResponse.java)를 추가했다.
- 응답 body는 `ruleId`, `message`만 담도록 최소화했다.
- 성공 메시지 기본값은 `규칙이 삭제되었습니다.`로 고정했고, `success(ruleId)` 팩토리 메서드로 중복 생성을 줄였다.

### 3. 관리자 삭제 API 컨트롤러와 서비스 삭제 로직을 연결했다

- [AdminRuleApiController.java](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/admin/rule/AdminRuleApiController.java)를 추가해 `POST /admin/api/rules/{id}`를 처리하도록 만들었다.
- 성공 응답은 `ResponseEntity<Resp<AdminRuleDeleteResponse>>` + `Resp.ok(response)` 형식으로 기존 관리자 API 패턴에 맞췄다.
- [AdminRuleService.java](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/admin/rule/AdminRuleService.java)에는 `deleteRule(Long ruleId)`를 추가했다.
- 삭제는 `hospitalRuleRepository.delete(rule)`를 호출하는 실제 물리 삭제로 구현했다.

### 4. 공통 실패 처리와 관리자 보안 규칙을 그대로 재사용했다

- 삭제 대상 규칙 조회는 기존 `findRule(...)`을 재사용해, 대상이 없으면 `CustomException.notFound(...)`가 그대로 동작하도록 했다.
- [GlobalExceptionHandler.java](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/common/exception/GlobalExceptionHandler.java)의 `Resp.fail(...)` 공통 실패 응답 흐름을 그대로 적용했다.
- [SecurityConfig.java](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/config/SecurityConfig.java) 기준 `/admin/**` 전체가 `ROLE_ADMIN` 전용이므로 새 삭제 API도 별도 보안 규칙 추가 없이 관리자 전용으로 보호된다.

### 5. API 테스트와 서비스 테스트를 추가해 계약을 고정했다

- [AdminRuleApiControllerTest.java](/c:/Users/HSystem/hms/src/test/java/com/smartclinic/hms/admin/rule/AdminRuleApiControllerTest.java)를 추가했다.
- 테스트 항목은 다음을 포함한다.
  - 관리자 성공 삭제
  - 비관리자 403 차단
  - 존재하지 않는 규칙 삭제 404 공통 실패 응답
- [AdminRuleServiceTest.java](/c:/Users/HSystem/hms/src/test/java/com/smartclinic/hms/admin/rule/AdminRuleServiceTest.java)에는 `deleteRule(...)` 물리 삭제 호출과 not found 예외 케이스를 추가했다.

### 6. 모듈 문서와 task/workflow 문서를 현재 코드 기준으로 마감했다

- [AI-CONTEXT.md](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/admin/rule/AI-CONTEXT.md)에 `AdminRuleApiController`, `AdminRuleDeleteResponse`, 삭제 JSON API 계약을 반영했다.
- [FILES.md](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/admin/rule/FILES.md)에도 새 API 컨트롤러와 DTO를 추가했다.
- [task-028.md](/c:/Users/HSystem/hms/doc/dev-c/task/task-028.md)는 모든 task와 완료 기준을 완료 상태로 맞췄다.
- [workflow-028.md](/c:/Users/HSystem/hms/doc/dev-c/workflow/workflow-028.md) 수용 기준도 현재 구현 결과 기준으로 완료 처리했다.

## 검증 결과

- `.\gradlew.bat compileJava --no-watch-fs` : `BUILD SUCCESSFUL`
- `.\gradlew.bat compileTestJava --no-watch-fs` : `BUILD SUCCESSFUL`
- `.\gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleApiControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"` : `BUILD SUCCESSFUL`
- `.\gradlew.bat test --no-watch-fs` : `FAILED`

전체 테스트의 비관련 실패:
- `DoctorTreatmentServiceTest > getTodayReceivedList — 오늘 RECEIVED 상태 예약 목록을 반환한다`
- `ReservationControllerTest > @Valid 검증 실패 - 전화번호 빈 값 시 폼 뷰 재표시 및 에러 메시지`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-028.md`
- 로컬: `doc/dev-c/workflow/workflow-028.md`

## 남은 TODO / 리스크

- `admin.rule` 범위 구현과 타깃 테스트는 완료됐지만, 프로젝트 전체 테스트는 `doctor`, `reservation` 영역의 기존 실패 2건 때문에 아직 완전 녹색이 아니다.
- 로컬 `API.md` 같은 보조 문서에 삭제 계약이 남아 있다면, 실제 구현 기준인 `POST /admin/api/rules/{id}`로 후속 정리가 필요할 수 있다.
- 다음 후속 작업으로는 관리자 규칙 목록/상세 화면에서 이 JSON 삭제 API를 실제 호출하는 프런트 흐름을 별도 task로 분리해 진행할 수 있다.
