# task-023

## 목적
- [x] `workflow-023`의 환자 정보 부분 수정 REST API 범위를 실제 구현 가능한 작업 단위로 분해한다.
- [x] 관리자 환자 수정 API를 `POST /admin/api/patients/{id}/update` 기준으로 정리한다.
- [x] 수정 가능 필드를 `name`, `phone`, `note`로 제한한다.
- [x] 기본 검증, 전화번호 중복 방지, 공통 API 응답 패턴을 함께 정리한다.
- [x] `admin.patient` 범위 API 테스트와 문서를 순서대로 마무리한다.

## Task 23-1. 현재 환자 수정 가능 구조 점검
- [x] `Patient` 엔티티의 수정 가능 필드와 현재 변경 메서드를 점검한다.
- [x] 전화번호 중복 판단 기준과 현재 환자 재사용 규칙을 확인한다.
- [x] `admin.patient` 범위에 API 컨트롤러가 필요한지 구조를 정리한다.
- [x] 현재 `Resp`, `CustomException`, `GlobalExceptionHandler` 패턴을 다시 확인한다.
- [x] 없는 `patientId`, 공백 입력, 중복 전화번호 시나리오를 메모한다.

## Task 23-2. 환자 수정 요청/응답 DTO 설계
- [x] 수정 요청 DTO를 추가한다.
- [x] `name`, `phone`에 `@NotBlank` 검증을 적용한다.
- [x] `note`는 선택 입력으로 둔다.
- [x] 수정 결과 응답 DTO를 정의한다.
- [x] 응답 DTO에 수정 후 필요한 환자 정보만 담도록 범위를 정리한다.

## Task 23-3. 환자 수정 서비스 로직 구현
- [ ] `AdminPatientService`에 환자 수정 메서드를 추가한다.
- [ ] 없는 `patientId`는 not found 예외로 처리한다.
- [ ] `name`, `phone`, `note` 수정 로직을 구현한다.
- [ ] 자기 자신을 제외한 다른 환자와 전화번호 중복 시 수정을 차단한다.
- [ ] 기존 전화번호와 동일한 값으로 다시 저장하는 경우는 정상 허용한다.

## Task 23-4. 관리자 환자 수정 API 구현
- [ ] `POST /admin/api/patients/{id}/update` 엔드포인트를 추가한다.
- [ ] `@Valid @RequestBody` 기반 JSON 요청을 받는다.
- [ ] 성공 시 `Resp.ok(...)` 형식으로 응답한다.
- [ ] 관리자 권한 범위(`/admin/api/**`)를 유지한다.
- [ ] 기존 SSR 환자 목록/상세 기능과 충돌하지 않도록 책임을 분리한다.

## Task 23-5. API 테스트 보강
- [ ] 성공 수정 시나리오를 검증한다.
- [ ] `name` 공백 입력 검증 실패를 검증한다.
- [ ] `phone` 공백 입력 검증 실패를 검증한다.
- [ ] 전화번호 중복 수정 차단 시나리오를 검증한다.
- [ ] 없는 `patientId` 수정 요청 시나리오를 검증한다.

## Task 23-6. 문서 갱신 및 범위 검증
- [ ] `admin.patient` API 범위 테스트를 재확인한다.
- [ ] 필요 시 서비스 테스트에 전화번호 중복 수정 시나리오를 추가한다.
- [ ] `workflow-023`, `task-023` 완료 기준을 현재 구현 상태에 맞게 갱신한다.
- [ ] PR 리뷰 포인트와 남은 확장 메모를 문서에 반영한다.
- [ ] 전체 테스트 또는 영향 범위 테스트를 최종 확인한다.

## 완료 기준
- [ ] `POST /admin/api/patients/{id}/update`가 동작한다.
- [ ] `name`, `phone`, `note`를 수정할 수 있다.
- [ ] `name`, `phone` 공백 입력은 차단된다.
- [ ] 다른 환자와 전화번호 중복 시 수정이 차단된다.
- [ ] 성공 시 `Resp.ok(...)` 형식으로 수정 결과를 반환한다.
- [ ] 관련 `admin.patient` API 테스트가 통과한다.

## 추천 진행 순서
- [x] Task 23-1 현재 환자 수정 가능 구조 점검
- [x] Task 23-2 환자 수정 요청/응답 DTO 설계
- [ ] Task 23-3 환자 수정 서비스 로직 구현
- [ ] Task 23-4 관리자 환자 수정 API 구현
- [ ] Task 23-5 API 테스트 보강
- [ ] Task 23-6 문서 갱신 및 범위 검증

## 메모
- [x] `Patient` 엔티티는 `updateInfo(name, phone, email, address, note)` 메서드만 제공하므로, 부분 수정 API에서는 이메일/주소를 유지한 채 이름/연락처/메모만 안전하게 바꾸는 서비스 로직이 필요하다.
- [x] 현재 환자 동일성 판단과 재사용 규칙은 전화번호 기반이다. 온라인 예약, 전화 예약, 방문 접수 모두 전화번호로 기존 환자를 찾고 없으면 새 `Patient`를 생성한다.
- [x] `AdminPatientRepository`에는 현재 목록 검색(`search`)과 상세 이력 조회만 있고, 전화번호 중복 확인용 메서드는 아직 없다.
- [x] `admin.patient` 범위에는 아직 API 컨트롤러가 없으므로, 기존 패턴에 맞는 `AdminPatientApiController`가 새로 필요하다.
- [x] 관리자 API 패턴은 `@RequestMapping("/admin/api/...")`, `@Valid @RequestBody`, `Resp.ok(...)` 응답 형태로 구현되어 있다.
- [x] `GlobalExceptionHandler`는 `@RestController` 범위에서 `MethodArgumentNotValidException`, `CustomException` 등을 `Resp.fail(...)` 형식으로 처리한다.
- [x] 핵심 예외 시나리오는 없는 `patientId`, `name`/`phone` 공백 입력, 다른 환자와 전화번호 중복이다.
- [x] `note`는 선택 입력으로 두고, 이번 DTO 단계에서는 null/blank 모두 허용한다. 실제 저장 시 정규화 정책은 서비스 구현 단계에서 정리한다.
- [x] `UpdateAdminPatientApiRequest`에는 `name`, `phone`, `note`만 두고, 수정 결과 응답은 `patientId`, `name`, `phone`, `note`, `message`만 담는 작은 DTO로 정리했다.

## PR 리뷰 포인트
- [ ] 환자 수정 API의 URL과 수정 가능 필드 범위가 팀 기준에 맞는지 확인 부탁드립니다.
- [ ] 전화번호 중복 방지 규칙을 환자 동일성 판단 기준과 같은 방향으로 유지해도 되는지 봐주시면 좋겠습니다.
- [ ] 이번 범위에서 `note`를 선택 입력으로 둔 판단과 null/blank 처리 방식이 적절한지 확인 부탁드립니다.