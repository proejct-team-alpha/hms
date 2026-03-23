# Task 028 - 규칙 삭제 JSON API 구현

## 목적
- [x] `workflow-028` 범위를 실제 구현 가능한 작업 단위로 분해한다.
- [x] 관리자 전용 규칙 삭제 API를 `POST /admin/api/rules/{id}` 기준으로 구현하고 검증한다.
- [x] `Resp.ok(...)` JSON 응답, 물리 삭제, not found 공통 실패 처리, API 테스트, 문서 마감까지 한 흐름으로 완성한다.

## Task 28-1. 현재 규칙 모듈 API 구조와 삭제 계약 점검
- [x] `AdminRuleController`, `AdminRuleService`, `HospitalRuleRepository`, `HospitalRule` 현재 구조를 다시 확인한다.
- [x] 기존 관리자 JSON API 패턴(`AdminReservationApiController`, `AdminStaffApiController`, `AdminPatientApiController`)을 참고해 `Resp.ok(...)` 규칙을 정리한다.
- [x] 규칙 모듈에 아직 API 컨트롤러가 없는지, 새 `AdminRuleApiController`가 필요한지 확인한다.
- [x] 삭제 계약을 `DELETE /api/rules/{id}`가 아니라 `POST /admin/api/rules/{id}`로 구현 기준에 고정한다.

## Task 28-2. 삭제 성공 응답 DTO 설계
- [x] `admin.rule.dto` 범위에 규칙 삭제 성공 응답 DTO를 추가한다.
- [x] 응답 body 필드는 `ruleId`, `message`로 최소화한다.
- [x] 성공 메시지 기본값을 `규칙이 삭제되었습니다.` 기준으로 정리한다.
- [x] 공통 `Resp` 래퍼가 있으므로 상태/성공 여부 필드는 body에 중복해서 넣지 않도록 정리한다.

## Task 28-3. `POST /admin/api/rules/{id}` API 컨트롤러 구현
- [x] 관리자 전용 JSON API 컨트롤러를 추가하거나 기존 구조에 맞게 배치한다.
- [x] `@RestController`, `/admin/api/rules` 기준 매핑을 사용한다.
- [x] `@PostMapping("/{id}")`로 요청 바디 없이 path variable만 받도록 구현한다.
- [x] 성공 시 `ResponseEntity<Resp<...>>` + `Resp.ok(response)`를 반환하도록 맞춘다.

## Task 28-4. 서비스 삭제 로직 구현
- [x] `AdminRuleService`에 규칙 삭제 메서드를 추가한다.
- [x] 삭제 대상 규칙을 조회하고 없으면 `CustomException.notFound(...)`를 던진다.
- [x] 삭제는 `active=false` 전환이 아니라 실제 엔티티 물리 삭제로 처리한다.
- [x] 삭제 성공 시 API 응답 DTO 생성을 위한 최소 정보(`ruleId`, `message`)를 반환하도록 구성한다.

## Task 28-5. 공통 실패 처리와 보안 규칙 정리
- [x] 삭제 API가 별도 실패 JSON 바디를 만들지 않고 `GlobalExceptionHandler -> Resp.fail(...)` 흐름을 타도록 정리한다.
- [x] 존재하지 않는 규칙 삭제 요청 시 404 성격의 공통 실패 응답으로 내려가는지 확인한다.
- [x] 관리자 권한 아닌 호출은 기존 보안 규칙에 따라 차단되는지 확인한다.
- [x] SSR 리다이렉트 로직이 끼지 않고 JSON API 책임만 유지하도록 정리한다.

## Task 28-6. API 테스트 추가
- [x] API 컨트롤러 테스트에 관리자 권한 성공 케이스를 추가한다.
- [x] 성공 시 `status`, `msg`, `body.ruleId`, `body.message` JSON 구조를 검증한다.
- [x] 존재하지 않는 `id` 삭제 요청 시 공통 실패 응답을 검증한다.
- [x] 권한 없는 사용자 호출 차단 케이스를 검증한다.
- [x] 서비스 테스트에 물리 삭제 호출과 not found 예외 처리를 검증한다.

## Task 28-7. 관련 문서/컨텍스트 파일 정리
- [x] `admin.rule` 범위 `AI-CONTEXT.md`, `FILES.md`에 API 컨트롤러/DTO가 생기면 반영한다.
- [x] `task-028` 진행 결과에 맞춰 체크리스트를 갱신한다.
- [x] 필요 시 `workflow-028` 수용 기준과 구현 결과를 맞춘다.
- [x] 규칙 삭제 API 계약이 로컬 문서와 다르면 후속 TODO를 명시한다.

## Task 28-8. 최종 검증과 보고서 마감
- [x] `admin.rule` 범위 컴파일과 API 테스트를 실행한다.
- [x] 필요 시 전체 테스트를 확인하되 비관련 기존 실패는 분리해서 기록한다.
- [x] `report` 형식으로 task-028 구현 리포트를 작성한다.
- [x] 구현 결과, 검증 결과, 남은 TODO/리스크를 최종 문서에 반영한다.

## 완료 기준
- [x] `POST /admin/api/rules/{id}`가 관리자 권한에서 동작한다.
- [x] 요청 바디 없이 `id`만 받아 규칙을 실제 삭제한다.
- [x] 성공 시 `Resp.ok(...)` 형식의 JSON 응답을 반환한다.
- [x] 성공 body에 `ruleId`, `message`가 포함된다.
- [x] 존재하지 않는 대상은 `CustomException.notFound(...)` 기반 공통 실패 응답으로 처리된다.
- [x] 관련 API 컨트롤러/서비스 테스트가 통과한다.

## 추천 진행 순서
- [x] Task 28-1 현재 구조와 API 응답 패턴을 먼저 확인한다.
- [x] Task 28-2 삭제 응답 DTO를 먼저 고정한다.
- [x] Task 28-3 API 컨트롤러를 추가한다.
- [x] Task 28-4 서비스 물리 삭제 로직을 구현한다.
- [x] Task 28-5 예외/보안 흐름을 정리한다.
- [x] Task 28-6 API 테스트를 보강한다.
- [x] Task 28-7 문서를 정리한다.
- [x] Task 28-8 검증과 리포트를 마무리한다.

## 메모
- `workflow-028` 인터뷰 결과를 반영한 규칙 삭제 JSON API 구현 명세다.
- 이번 작업 기준 삭제 경로는 `POST /admin/api/rules/{id}`다.
- `DELETE /api/rules/{id}`와 `DELETE /admin/api/rules/{id}`는 이번 구현 기준에서 채택하지 않는다.
- 삭제 성공 응답은 `Resp.ok(...)` 공통 래퍼를 사용한다.
- 성공 body는 `ruleId`, `message`만 담는 최소 구조로 간다.
- 삭제는 비활성화가 아니라 물리 삭제다.
- 삭제 이후 목록 갱신, 행 제거, 페이지 이동, toast 같은 후속 UX는 클라이언트가 결정한다.

## Task 28-1 점검 메모
- 현재 규칙 모듈은 `AdminRuleController` 기준 SSR 컨트롤러만 존재하고, 목록/등록/상세/수정 흐름만 담당한다.
- 서비스는 `AdminRuleService` 기준 목록/등록/상세/수정까지만 구현돼 있고 삭제 메서드는 없었다.
- 리포지토리 `HospitalRuleRepository`가 `JpaRepository<HospitalRule, Long>`을 상속하고 있어 `delete(...)`, `deleteById(...)` 같은 물리 삭제 기본 메서드를 바로 사용할 수 있다.
- 도메인 `HospitalRule`에는 `active` 필드가 있지만 soft delete 전용 규약은 없고, 현재는 생성/수정/활성/비활성만 다루므로 삭제 요구는 별도 물리 삭제 로직으로 보는 편이 맞다.
- 기존 관리자 JSON API는 `AdminReservationApiController`, `AdminStaffApiController`, `AdminPatientApiController`처럼 전용 `@RestController` + `/admin/api/...` + `ResponseEntity<Resp<...>>` + `Resp.ok(...)` 패턴을 따른다.
- 규칙 모듈에는 아직 `/admin/api/rules`를 담당하는 전용 API 컨트롤러가 없으므로 다음 단계에서 `AdminRuleApiController`를 새로 만드는 방향이 가장 자연스럽다.
- 보안은 `SecurityConfig` 기준 `/admin/**` 전체가 `ROLE_ADMIN` 전용이라 새 삭제 API도 별도 보안 규칙 없이 관리자 전용으로 보호된다.

## Task 28-2 점검 메모
- 삭제 성공 응답 DTO는 `AdminRuleDeleteResponse`로 정했다.
- 이름은 규칙 모듈의 `...Response` 규칙을 따르면서도 `AdminReservationCancelResponse`와 비슷한 `자원명 + 동작명 + Response` 패턴으로 맞췄다.
- body 필드는 `ruleId`, `message`만 두고, 상태값과 성공 여부는 공통 `Resp` 래퍼에 맡긴다.
- 성공 메시지 기본값 `규칙이 삭제되었습니다.`는 DTO 내부 `success(ruleId)` 팩토리에 묶어 다음 단계에서 컨트롤러/서비스가 중복 문구를 만들지 않도록 정리했다.

## Task 28-3 구현 메모
- 관리자 전용 JSON API 컨트롤러 `AdminRuleApiController`를 추가했다.
- 경로는 `@RequestMapping("/admin/api/rules")`, 메서드는 `@PostMapping("/{id}")`로 두고 요청 바디 없이 path variable `id`만 받는다.
- 성공 응답은 `ResponseEntity<Resp<AdminRuleDeleteResponse>>` + `Resp.ok(response)` 형식으로 기존 관리자 JSON API 관례와 동일하게 맞췄다.

## Task 28-4 구현 메모
- 컨트롤러를 실제 동작 가능한 상태로 만들기 위해 `AdminRuleService`에 `deleteRule(Long ruleId)`를 추가했다.
- 삭제 대상은 기존 `findRule(...)`을 사용해 찾고, 없으면 `CustomException.notFound(...)`가 그대로 동작한다.
- 삭제는 `hospitalRuleRepository.delete(rule)`로 실제 물리 삭제를 수행하고, 성공 시 `AdminRuleDeleteResponse.success(ruleId)`를 반환하도록 정리했다.
- 이 단계에서는 예외/보안/테스트 보강보다도 실제 API 컨트롤러가 빈 껍데기가 되지 않도록 삭제 서비스까지 연결하는 데 초점을 뒀다.

## Task 28-5 구현 메모
- `GlobalExceptionHandler`는 `@RestController` 범위의 `CustomException`을 `Resp.fail(...)`로 공통 처리하므로, 새 삭제 API도 별도 실패 바디를 만들지 않고 같은 JSON 실패 규칙을 그대로 따른다.
- `AdminRuleService.deleteRule(...)`는 기존 `findRule(...)`을 재사용하므로, 존재하지 않는 규칙 삭제 요청은 `CustomException.notFound(...)`로 연결되고 결과적으로 404 공통 실패 응답으로 정리된다.
- `SecurityConfig` 기준 `/admin/**` 전체가 `ROLE_ADMIN` 전용이므로 `POST /admin/api/rules/{id}` 역시 별도 규칙 추가 없이 관리자 전용으로 보호된다.
- 이번 단계에서는 새 JSON 인증/인가 예외 핸들러를 확장하지 않고, 기존 관리자 API들이 따르는 보안 차단 규칙과 비즈니스 예외 JSON 규칙을 삭제 API에도 동일하게 적용하는 범위로 정리했다.

## Task 28-6 구현 메모
- 삭제 API 전용 테스트 [AdminRuleApiControllerTest.java](/c:/Users/HSystem/hms/src/test/java/com/smartclinic/hms/admin/rule/AdminRuleApiControllerTest.java)를 추가해 관리자 성공, 비관리자 403, 존재하지 않는 규칙 404 시나리오를 검증했다.
- 성공 케이스에서는 `status`, `body.ruleId`, `body.message`를 확인해 `Resp.ok(...)` 래퍼와 삭제 응답 DTO 구조가 실제로 맞는지 고정했다.
- 서비스 테스트 [AdminRuleServiceTest.java](/c:/Users/HSystem/hms/src/test/java/com/smartclinic/hms/admin/rule/AdminRuleServiceTest.java)에는 `deleteRule(...)` 물리 삭제 호출과 not found 예외 케이스를 추가했다.
- 검증은 `./gradlew.bat compileTestJava --no-watch-fs` 와 `./gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleApiControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"` 기준으로 통과했다.

## Task 28-7 구현 메모
- [AI-CONTEXT.md](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/admin/rule/AI-CONTEXT.md)에 `AdminRuleApiController`, `AdminRuleDeleteResponse`, 삭제 JSON API 계약을 추가해 `admin.rule` 모듈의 현재 책임을 문서화했다.
- [FILES.md](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/admin/rule/FILES.md)에도 새 API 컨트롤러와 삭제 응답 DTO를 반영해 파일 인벤토리를 현재 코드와 맞췄다.
- [workflow-028.md](/c:/Users/HSystem/hms/doc/dev-c/workflow/workflow-028.md)는 이미 구현 계약과 충돌이 없어 별도 수정 없이 유지했다.
- 로컬 문서 기준으로 삭제 API 계약은 `POST /admin/api/rules/{id}`로 정리돼 있고, 이전 인터뷰 초안의 `DELETE /api/rules/{id}` 계열은 비채택 경로로 유지했다.

## Task 28-8 구현 메모
- 최종 검증으로 `./gradlew.bat compileJava --no-watch-fs`, `./gradlew.bat compileTestJava --no-watch-fs`, `./gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleApiControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"`를 다시 실행했고 모두 통과했다.
- 전체 테스트도 `./gradlew.bat test --no-watch-fs`로 확인했지만, 현재 실패는 규칙 삭제 API와 무관한 기존 2건으로 분리됐다.
- 비관련 전체 테스트 실패:
  - `DoctorTreatmentServiceTest > getTodayReceivedList — 오늘 RECEIVED 상태 예약 목록을 반환한다`
  - `ReservationControllerTest > @Valid 검증 실패 - 전화번호 빈 값 시 폼 뷰 재표시 및 에러 메시지`
- 따라서 `admin.rule` 범위 구현과 테스트는 완료로 보고, 전체 스위트 녹색 전환은 후속 별도 범위로 남긴다.
