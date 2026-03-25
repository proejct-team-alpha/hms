# Task 027 - 규칙 수정 상세 화면 연동 및 저장

## 목적
- [x] `workflow-027` 범위를 실제 구현 가능한 작업 단위로 분해한다.
- [x] 관리자 규칙 수정 기능 `S32`를 `GET /admin/rule/detail?ruleId={id}`, `POST /admin/rule/update` 기준으로 구현하고 검증한다.
- [x] 상세 화면 조회, 수정 폼 재사용, 저장 성공/실패 흐름을 한 화면 구조 안에서 완성한다.

## Task 27-1. 현재 규칙 상세/수정 구조와 문서 계약 점검
- [x] `AdminRuleController`, `AdminRuleService`, `HospitalRule`, 기존 규칙 상세/등록 템플릿 구조를 다시 확인한다.
- [x] 현재 규칙 상세 조회 경로와 모델 구성 방식을 정리한다.
- [x] 규칙 수정 관련 로컬 문서/API/시퀀스 계약에서 `detail/update`와 `edit/{id}` 차이가 있는지 메모한다.
- [x] `HospitalRule`의 수정 가능 필드와 기존 `update(...)` 메서드 시그니처를 구현 기준으로 고정한다.

## Task 27-2. 수정 요청 DTO와 상세 화면 폼 모델 설계
- [x] `admin.rule.dto` 범위에 규칙 수정 요청 DTO를 추가한다.
- [x] `ruleId`, `title`, `content`, `category`, `isActive` 필드 구조를 정리한다.
- [x] `title` 필수, 최대 200자 검증을 DTO에 반영한다.
- [x] `content` 필수, 최대 3000자 검증을 DTO에 반영한다.
- [x] `category` enum 필수 선택 검증과 `isActive` 체크박스 처리 구조를 등록 흐름과 일관되게 맞춘다.

## Task 27-3. `GET /admin/rule/detail` 상세 조회와 수정 폼 모델 구성
- [x] `GET /admin/rule/detail?ruleId={id}`에서 규칙 상세 정보를 조회하도록 컨트롤러를 정리한다.
- [x] 상세 정보 표시 모델과 수정 폼 모델이 같은 화면에서 함께 렌더링되도록 구성한다.
- [x] 초기 진입 시 기존 규칙 값이 수정 폼에 채워지도록 바인딩 구조를 정리한다.
- [x] 존재하지 않는 `ruleId` 접근 시 not found 처리 방식을 정리한다.

## Task 27-4. `POST /admin/rule/update` 저장 흐름 구현
- [x] `POST /admin/rule/update`가 DTO 검증 후 서비스로 수정을 위임하도록 구현한다.
- [x] 저장 시 `title`, `content`, `category`, `isActive` 전체가 엔티티 수정에 반영되도록 서비스/도메인 호출부를 정리한다.
- [x] 저장 성공 시 `redirect:/admin/rule/detail?ruleId={id}`로 이동하도록 PRG 흐름을 맞춘다.
- [x] 상세 화면에서 성공 플래시 메시지를 읽어 렌더링할 수 있도록 모델 계약을 정리한다.

## Task 27-5. 규칙 상세 Mustache와 공통 폼 재사용 구조 정리
- [x] 기존 등록 폼 구조를 기준으로 수정 폼 재사용 방식을 정한다.
- [x] 가능하면 `admin/_rule-form.mustache` 같은 공통 partial로 등록/수정 공통 필드를 묶는다.
- [x] `rule-detail.mustache`에서 상세 정보 영역과 수정 폼 영역이 함께 동작하도록 화면 구성을 정리한다.
- [x] 수정 폼의 `action`, 버튼 문구, 초기값 바인딩만 수정 맥락에 맞게 분기한다.

## Task 27-6. 검증 실패 same-view 재렌더링과 오류 처리 보강
- [x] 저장 실패 시 같은 상세 화면 구조를 재렌더링하도록 컨트롤러 흐름을 정리한다.
- [x] 실패 시 사용자가 입력한 `title`, `content`, `category`, `isActive`가 유지되도록 모델을 보강한다.
- [x] 필드 오류 메시지와 전역 오류 메시지를 상세 화면 안에 표시한다.
- [x] 잘못된 카테고리 문자열 전송 시 친절한 오류 메시지를 등록 흐름과 같은 방식으로 공통 처리한다.

## Task 27-7. 테스트 보강
- [x] 컨트롤러 테스트에 `GET /admin/rule/detail?ruleId={id}` 상세+수정 폼 렌더링 검증을 추가한다.
- [x] 컨트롤러 테스트에 수정 성공 시 상세 리다이렉트와 성공 메시지 플래시 검증을 추가한다.
- [x] 컨트롤러 테스트에 제목/내용/카테고리 검증 실패 시 same-view 재렌더링과 입력값 유지 검증을 추가한다.
- [x] 컨트롤러 테스트에 `isActive` 체크박스 true/false 처리 검증을 추가한다.
- [x] 서비스 테스트에 수정 인자 반영과 not found/카테고리 처리 검증을 추가한다.

## Task 27-8. 문서 및 최종 검증 마무리
- [x] `workflow-027` 기준 구현 결과를 다시 점검한다.
- [x] `task-027` 체크리스트를 완료 상태로 정리한다.
- [x] `admin.rule` 범위 테스트를 실행한다.
- [x] 필요 시 전체 `./gradlew test`를 확인한다.
- [x] 로컬 문서와 실제 구현 경로 차이가 남으면 후속 문서 정리 TODO를 남긴다.

## 완료 기준
- [x] `GET /admin/rule/detail?ruleId={id}`에서 규칙 상세와 수정 폼이 함께 렌더링된다.
- [x] 제목, 내용, 카테고리, 활성 여부를 수정할 수 있다.
- [x] 저장 성공 시 같은 상세 화면으로 리다이렉트되고 성공 메시지가 노출된다.
- [x] 저장 실패 시 상세 화면 재렌더링, 입력값 유지, 오류 메시지 표시가 동작한다.
- [x] 등록 화면과 수정 화면의 폼 구조가 공통 패턴으로 재사용된다.
- [x] 관련 테스트가 통과한다.

## 추천 진행 순서
- [x] Task 27-1 현재 상세/수정 구조와 문서 계약을 먼저 점검한다.
- [x] Task 27-2 수정 DTO와 폼 모델을 먼저 고정한다.
- [x] Task 27-3 상세 조회와 화면 모델 구성을 구현한다.
- [x] Task 27-4 저장 흐름과 성공 리다이렉트를 구현한다.
- [x] Task 27-5 템플릿과 공통 폼 재사용 구조를 정리한다.
- [x] Task 27-6 실패 재렌더링과 오류 처리를 보강한다.
- [x] Task 27-7 테스트를 보강한다.
- [x] Task 27-8 문서와 최종 검증을 마무리한다.

## 메모
- `workflow-027`은 사용자 인터뷰 결과를 반영한 규칙 수정 구현 명세다.
- 이번 작업 기준 수정 조회 경로는 `GET /admin/rule/detail?ruleId={id}`다.
- 이번 작업 기준 수정 저장 경로는 `POST /admin/rule/update`다.
- `GET/POST /admin/rule/edit/{id}`는 이번 구현 기준에서 채택하지 않는다.
- 수정 화면은 별도 edit 페이지가 아니라 상세 화면 안에서 폼을 함께 보여주는 구조다.
- 등록 화면과 수정 화면은 가능한 한 공통 폼 구조를 재사용한다.

## Task 27 구현 메모
- `AdminRuleController`에 `GET /admin/rule/detail`, `POST /admin/rule/update`를 추가하고, 상세 조회는 `error/404`, 저장 실패는 same-view 재렌더링 또는 목록 리다이렉트로 분기하도록 정리했다.
- `AdminRuleService`에는 `getRuleDetail(...)`, `updateRule(...)`를 추가해 `HospitalRule.update(...)`로 제목, 내용, 카테고리, 활성 여부 전체를 수정하고 `CustomException.notFound(...)`로 누락 케이스를 처리하도록 맞췄다.
- `UpdateAdminRuleRequest`, `AdminRuleDetailResponse`를 추가해 수정 요청 검증과 상세 응답 모델을 분리했다.
- 템플릿은 `admin/_rule-form.mustache` 공통 partial을 추가하고, `rule-new.mustache`, `rule-detail.mustache`가 같은 폼 블록을 재사용하도록 재구성했다.
- `rule-list.mustache`의 제목 링크는 상세 화면 `GET /admin/rule/detail?ruleId={id}`로 연결되도록 바꿨다.
- `AdminRuleControllerTest`, `AdminRuleServiceTest`에는 상세 조회, 수정 성공, 수정 실패, invalid category, not found, list-detail 링크 검증을 보강했다.
- 기준 검증은 `./gradlew.bat test --no-watch-fs --tests "com.smartclinic.hms.admin.rule.AdminRuleControllerTest" --tests "com.smartclinic.hms.admin.rule.AdminRuleServiceTest"`로 통과했다.
- 전체 `./gradlew.bat test --no-watch-fs`는 수행했지만 `DoctorTreatmentServiceTest`, `ReservationControllerTest`의 기존 실패로 아직 전체 녹색은 아니다.
