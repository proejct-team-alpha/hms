# Task 036 - 직원 수정 화면 퇴사 일시 UX 정리 및 비활성 직원 조회 전용 처리

## Task 36-1. 현재 직원 등록/수정 화면 구조 점검
- [x] 직원 등록/수정 화면에서 `퇴사 일시` 입력이 현재 어떻게 렌더링되는지 확인한다.
- [x] 수정 화면에서 `재직 상태`와 `퇴사 일시` 배치 변경에 영향을 주는 DTO/템플릿 구조를 점검한다.
- [x] 비활성 직원 수정 화면과 수정 API가 현재 어디까지 허용되는지 확인한다.

## Task 36-2. 퇴사 일시 입력 계약 설계
- [x] `퇴사 일시` 입력을 `날짜 + 시(00~23)` 방식으로 단순화한 계약으로 정리한다.
- [x] 저장 값의 분은 항상 `00`으로 고정하는 규칙을 확정한다.
- [x] 등록/수정 SSR/API가 같은 조합 규칙을 타도록 서비스 공통 로직 기준으로 정리한다.

## Task 36-3. 직원 수정 화면 UI 리팩토링
- [x] 수정 화면에서 `재직 상태` 옆에 `퇴사 일시`를 배치한다.
- [x] `퇴사 일시` 입력을 `날짜 input + 시간 select`로 변경한다.
- [x] 수정 화면의 안내 문구와 필드 정렬이 자연스럽게 이어지도록 레이아웃을 정리한다.

## Task 36-4. 직원 등록 화면 입력 형식 반영
- [x] 등록 화면 레이아웃은 유지하되 `퇴사 일시` 입력 형식만 `날짜 + 시간` 선택 방식으로 바꾼다.
- [x] 등록 시 hidden `retiredAt` 값이 `yyyy-MM-ddTHH:00` 형식으로 들어가도록 연결한다.

## Task 36-5. 비활성 직원 조회 전용 처리
- [x] 비활성 직원 수정 화면을 읽기 전용으로 렌더링한다.
- [x] 입력창을 회색 비활성 스타일로 통일한다.
- [x] 저장 버튼을 숨기고 `비활성화된 직원입니다. 조회만 가능합니다.` 문구를 노출한다.
- [x] 비활성 직원 수정은 SSR 저장과 관리자 수정 API에서 모두 차단한다.
- [x] 차단 메시지는 `비활성화된 직원은 수정할 수 없습니다.`를 사용한다.

## Task 36-6. 서비스/API 로직 정리
- [x] 날짜 + 시간 입력을 `LocalDateTime`으로 조합하는 공통 로직을 서비스에 정리한다.
- [x] 분 `00` 고정 로직을 등록/수정 SSR/API 공통 경로에 반영한다.
- [x] 비활성 직원 수정 차단 규칙을 서비스 기준으로 일관되게 적용한다.

## Task 36-7. 테스트 보강
- [x] SSR 렌더링 테스트에 수정 화면 배치와 비활성 조회 전용 상태를 보강한다.
- [x] 등록/수정 서비스 테스트에 `퇴사 일시`가 `HH:00`으로 저장되는지, 날짜/시간 짝 입력 검증이 동작하는지 확인한다.
- [x] 관리자 수정 API 테스트에 비활성 직원 수정 차단과 `retiredAtDate`, `retiredAtHour` 전달 케이스를 보강한다.

## Task 36-8. 문서 및 최종 검증 마무리
- [x] `workflow-036`을 현재 구현 상태 기준으로 완료 처리한다.
- [x] `task-036` 체크리스트와 구현 메모를 정리한다.
- [x] 관련 범위 테스트와 전체 테스트를 확인한다.

## 전체 완료 기준
- [x] 직원 수정 화면에서 `재직 상태` 옆에 `퇴사 일시`가 배치된다.
- [x] 등록/수정 화면 모두 `퇴사 일시`가 `날짜 + 시간` 선택 방식으로 바뀐다.
- [x] 시간은 `00~23`까지만 선택 가능하다.
- [x] 저장 시 분은 항상 `00`으로 저장된다.
- [x] 비활성 직원 수정 화면은 읽기 전용으로 렌더링된다.
- [x] 비활성 직원 수정 화면에는 저장 버튼이 보이지 않는다.
- [x] 비활성 직원 화면에 `비활성화된 직원입니다. 조회만 가능합니다.` 문구가 보인다.
- [x] 비활성 직원 수정은 SSR 저장과 관리자 수정 API에서 차단된다.
- [x] 차단 메시지는 `비활성화된 직원은 수정할 수 없습니다.`를 사용한다.
- [x] 관련 범위 테스트와 전체 테스트가 통과한다.

## Task 36-1 구현 메모
- `staff-form.mustache`는 등록/수정 공용 템플릿이며, `퇴사 일시`는 초기 상태에서 `datetime-local` 단일 입력이었다.
- 수정 화면에서는 `재직 상태`와 `퇴사 일시`가 분리돼 있어 요청한 “옆 배치” 상태가 아니었다.
- DTO는 초기 상태에서 분 단위까지 포함한 `LocalDateTime retiredAt` 단일 값만 직접 받는 구조였다.
- 비활성 직원은 `재직 상태`만 잠기고 다른 필드는 수정 가능한 상태였고, 서비스도 재활성화만 막고 있어 “비활성 직원은 조회만 가능” 규칙이 공통으로 닫혀 있지 않았다.

## Task 36-2 구현 메모
- 저장 모델은 그대로 `Staff.retiredAt: LocalDateTime`을 유지하고, 화면 입력만 `retiredAtDate` + `retiredAtHour`로 분리하기로 정리했다.
- 두 값이 모두 비어 있으면 `retiredAt = null`, 둘 다 있으면 `LocalDateTime.of(date, hour, 0, 0)`으로 조합한다.
- 하나만 입력된 경우는 검증 에러로 처리해 SSR/API가 같은 규칙을 타도록 방향을 고정했다.
- 화면 렌더링용 응답도 `retiredAtDate`, `retiredAtHour`를 별도로 제공하는 쪽이 자연스럽다고 정리했다.

## Task 36-3 구현 메모
- 직원 수정 화면에서 `재직 상태`와 `퇴사 일시`를 한 줄에 배치했다.
- 수정 화면의 `퇴사 일시`를 `datetime-local` 대신 `날짜 input + 시간 select + hidden retiredAt` 구조로 바꿨다.
- 시간 선택지는 `00:00`부터 `23:00`까지 고정 옵션으로 렌더링했다.
- 제출 직전 스크립트에서 `retiredAtDate`와 `retiredAtHour`를 `yyyy-MM-ddTHH:00` 형식의 hidden `retiredAt` 값으로 합치도록 연결했다.

## Task 36-4 구현 메모
- 등록 화면도 수정 화면과 같은 `날짜 input + 시간 select + hidden retiredAt` 구조로 바꿨다.
- 등록 화면 레이아웃은 유지하고 입력 형식만 단순화했다.
- hidden `retiredAt` 계약을 유지해서 컨트롤러/서비스 쪽 `LocalDateTime retiredAt` 바인딩과도 자연스럽게 이어지게 했다.

## Task 36-5 구현 메모
- `AdminStaffService.updateStaff(...)` 시작 지점에서 비활성 직원 수정 차단 검증을 추가해 SSR 저장과 관리자 수정 API가 같은 규칙을 타도록 맞췄다.
- `buildEditFormResponse(...)`에서 `readOnly = !staff.isActive()`를 기준으로 비활성 직원 수정 화면을 읽기 전용으로 렌더링하도록 정리했다.
- `staff-form.mustache`는 `model.readOnly`일 때 주요 입력 필드를 disabled로 렌더링하고, 저장 버튼을 숨기며 조회 전용 안내 문구를 노출하도록 맞췄다.
- `AdminStaffControllerTest`, `AdminStaffApiControllerTest`, `AdminStaffServiceTest`도 비활성 직원 수정 차단과 읽기 전용 렌더링 기준으로 다시 정리했다.

## Task 36-6 구현 메모
- `CreateAdminStaffRequest`, `UpdateAdminStaffRequest`, `UpdateAdminStaffApiRequest`에 `retiredAtDate`, `retiredAtHour`를 공통 요청 값으로 추가해 SSR과 API가 같은 계약을 쓰도록 맞췄다.
- `AdminStaffService.resolveRetiredAt(...)`에서 날짜와 시간을 `LocalDateTime`으로 조합하고, 한쪽만 입력된 경우 검증 에러를 던지도록 정리했다.
- `normalizeRetiredAt(...)`와 `formatRetiredAt*` 계열 헬퍼를 통해 분을 항상 `00`으로 고정했다.
- `AdminStaffApiController`도 `retiredAtDate`, `retiredAtHour`를 서비스 요청 DTO로 그대로 넘기도록 수정했다.
- `AdminStaffServiceTest`, `AdminStaffApiControllerTest`에 `HH:00` 조합과 API 전달 경로를 검증하는 테스트를 추가했다.

## Task 36-7 구현 메모
- `AdminStaffControllerTest`에 `퇴사 일시`를 날짜만 선택한 상태에서 저장 시 `retiredAtError`가 내려오는 SSR 케이스를 추가했다.
- `AdminStaffServiceTest`에 날짜만 입력 / 시간만 입력된 경우를 각각 검증해 `퇴사 일시는 날짜와 시간을 모두 선택해야 합니다.` 메시지가 서비스 공통 로직에서 나오는지 고정했다.
- `AdminStaffApiControllerTest`는 비활성 직원 수정 차단 케이스를 유지하면서, `retiredAtDate`, `retiredAtHour`가 서비스 DTO까지 그대로 전달되는지 확인하도록 보강했다.

## Task 36-8 구현 메모
- `workflow-036`, `task-036`을 현재 구현과 테스트 상태 기준으로 다시 정리했다.
- `admin.staff` 범위 테스트와 전체 테스트 결과를 문서 기준에도 반영해, 문서와 실제 코드 상태가 어긋나지 않도록 마감했다.