# Task 031 - 예약 목록 검색 추가 및 환자 목록 검색 통합

## Task 31-1. 현재 예약/환자 목록 검색 구조 점검
- [x] `AdminReservationController`, `AdminReservationService`, Repository 현재 구조 확인
- [x] `AdminPatientController`, `AdminPatientService`, Repository 현재 검색 파라미터 구조 확인
- [x] 병원 규칙 관리 검색 바 패턴을 기준 화면으로 쓸 수 있는지 메모 정리

## Task 31-2. 예약 목록 keyword 검색 설계
- [x] 예약 목록 요청 파라미터에 `keyword` 추가
- [x] 검색 대상을 `patient.name`, `patient.phone`으로 고정
- [x] 상태 필터(`status`)와 `keyword` 조합 규칙 정리
- [x] 페이지 링크/이전/다음 URL에 `keyword` 유지 구조 설계

## Task 31-3. 예약 목록 검색 + 상태 필터 + 페이지네이션 구현
- [x] `GET /admin/reservation/list`에 `keyword` 검색 연결
- [x] 환자명 검색 구현
- [x] 연락처 검색 구현
- [x] 상태 필터와 검색어 동시 적용 구현
- [x] 페이지 이동 시 `status`, `keyword` 유지 구현

## Task 31-4. 예약 목록 검색 UI를 병원 규칙 관리 패턴으로 정리
- [x] 예약 목록 상단 검색 바를 병원 규칙 관리 패턴에 맞춰 정리
- [x] 검색 입력창 placeholder를 `환자명 또는 연락처 검색`으로 정리
- [x] 조회 버튼과 초기화 버튼 추가 및 동일 패턴으로 정리
- [x] 상태 필터와 검색 UI가 함께 자연스럽게 보이도록 정리

## Task 31-5. 환자 목록 검색 파라미터를 단일 keyword로 통합
- [x] `nameKeyword`, `contactKeyword`를 `keyword` 하나로 통합
- [x] 환자명 검색 유지
- [x] 연락처 정규화 검색 유지
- [x] 기존 상세보기/페이지네이션 흐름과 충돌 없는지 정리

## Task 31-6. 환자 목록 검색 UI를 예약/규칙 관리 패턴에 맞춰 정리
- [x] 환자 목록 상단 검색 바를 병원 규칙 관리 패턴에 맞춰 정리
- [x] 입력창과 버튼 배치를 예약 목록과 유사한 흐름으로 통일
- [x] 기존 검색 요약/총 건수/페이지네이션 UI와 충돌 없는지 확인
- [x] footer 및 전체 레이아웃이 흔들리지 않는지 확인

## Task 31-7. 테스트 보강
- [x] 예약 목록 컨트롤러 테스트에 `keyword` 파라미터 전달 검증 추가
- [x] 예약 목록 서비스 테스트에 환자명/연락처 검색 검증 추가
- [x] 환자 목록 컨트롤러 테스트에 `keyword` 단일 검색 검증 추가
- [x] 환자 목록 서비스 테스트에 이름/연락처 통합 검색 검증 추가
- [x] 필터/검색 조건 유지 페이지네이션 검증 추가

## Task 31-8. 문서 및 최종 검증 마무리
- [x] `workflow-031` 완료 처리
- [x] `task-031` 완료 처리
- [x] `admin.reservation`, `admin.patient` 범위 테스트 실행
- [x] 전체 `./gradlew test` 확인

## 완료 기준
- [x] 예약 목록에서 상태 필터와 별개로 `keyword` 검색이 동작한다.
- [x] 예약 목록에서 환자명과 연락처로 검색할 수 있다.
- [x] 예약 목록에서 페이지 이동 시 `status`, `keyword`가 유지된다.
- [x] 환자 목록은 `keyword` 단일 검색으로 동작한다.
- [x] 환자 목록은 기존 `nameKeyword`, `contactKeyword`가 제거된다.
- [x] 예약 목록과 환자 목록의 검색 UI가 병원 규칙 관리 패턴과 유사한 흐름으로 정리된다.
- [x] 관련 테스트가 통과한다.

## 추천 진행 순서
- [x] Task 31-1 현재 구조를 먼저 점검한다.
- [x] Task 31-2 예약 목록 검색 계약을 먼저 고정한다.
- [x] Task 31-3 예약 목록 백엔드 검색을 구현한다.
- [x] Task 31-4 예약 목록 UI를 먼저 정리한다.
- [x] Task 31-5 환자 목록 검색 파라미터를 `keyword`로 통합한다.
- [x] Task 31-6 환자 목록 UI를 같은 패턴으로 맞춘다.
- [x] Task 31-7 테스트를 보강한다.
- [x] Task 31-8 문서와 최종 검증을 마무리한다.

## 메모
- `workflow-031`은 예약 목록 검색 추가와 환자 목록 검색 통합을 하나의 작업으로 묶는다.
- 구현 순서는 예약 목록을 먼저 기준 화면으로 정리하고, 환자 목록을 같은 패턴으로 맞춘다.
- 병원 규칙 관리 검색 바 패턴을 관리자 검색형 목록의 공통 기준으로 사용한다.

## Task 31-1 점검 메모
- 예약 목록은 현재 `page`, `size`, `status`만 받고 `keyword`가 없다.
- 예약 목록 Repository는 이미 `patient.name`, `patient.phone` projection을 가져오므로 검색 조건 추가가 가능한 구조다.
- 환자 목록은 현재 `nameKeyword`, `contactKeyword`를 분리해 받고 있다.
- 병원 규칙 관리 검색 바는 입력 1개 + 필터 + 조회/초기화 버튼 구조라 기준 화면으로 적합하다.

## Task 31-2 설계 메모
- 예약 목록 파라미터 기본값은 `page=1`, `size=10`, `status=ALL`, `keyword=''`로 고정한다.
- 검색 대상은 `patient.name`, `patient.phone`으로 제한한다.
- 조회 규칙은 `status AND (name OR phone)` 조합으로 정리한다.
- 연락처 검색은 하이픈 제거 기준으로 비교하고, 페이지 링크/상태 옵션/취소 후 redirect까지 `keyword`를 유지한다.

## Task 31-3 구현 메모
- `AdminReservationController`에 `keyword` 파라미터를 추가하고 목록/취소 redirect에서 함께 유지하도록 정리했다.
- `AdminReservationService`는 `getReservationList(page, size, status, keyword)` 시그니처로 확장하고, 상태 옵션/페이지 링크/이전/다음 URL에 `keyword`를 유지한다.
- `AdminReservationRepository`는 `patient.name LIKE`와 `REPLACE(patient.phone, '-', '') LIKE` 조건을 추가해 상태 + 검색어 조합 조회를 지원한다.
- `AdminReservationListResponse`는 현재 검색어 `keyword`를 포함해 템플릿에서 그대로 사용할 수 있게 했다.
- 예약 취소 POST 폼은 현재 검색 상태를 잃지 않도록 `keyword` hidden field를 포함한다.

## Task 31-4 구현 메모
- `reservation-list.mustache` 상단을 병원 규칙 관리 검색 바 패턴과 유사한 구조로 정리했다.
- 검색 입력 1개 + 상태 select + 조회/초기화 버튼 흐름으로 맞췄다.
- 예약 목록 설명 문구와 빈 상태 메시지 한글도 같이 복구했다.
- 기존 테이블, 취소 액션, 페이지네이션은 유지한 채 검색 UI만 공통 패턴으로 정리했다.

## Task 31-5 구현 메모
- `AdminPatientController`는 목록 요청 파라미터를 `keyword` 하나로 단순화했다.
- `AdminPatientService`는 `getPatientList(page, size, keyword)` 시그니처로 정리하고, 이름 검색어와 연락처 비교용 검색어를 내부에서 각각 정규화한다.
- `AdminPatientRepository`는 `keyword OR normalized phone` 조건으로 조회하도록 변경해 이름/연락처를 하나의 입력으로 검색하게 만들었다.
- `AdminPatientListResponse`는 `nameKeyword`, `contactKeyword` 대신 `keyword` 하나만 유지한다.
- `patient-list.mustache`는 단일 검색 입력 구조로 최소 반영했고, 컨트롤러/서비스 테스트를 현재 계약에 맞게 정리했다.

## Task 31-6 구현 메모
- `patient-list.mustache`를 예약 목록과 같은 검색형 목록 패턴으로 정리하고, 상단 설명/총 건수/현재 페이지 영역을 같은 톤으로 맞췄다.
- 검색 요약 줄은 제거하고 검색어는 입력창 value로만 유지해 화면 무게를 줄였다.
- `body`와 `main` 레이아웃 클래스를 예약 목록과 동일한 구조로 맞춰 footer가 콘텐츠 길이에 덜 흔들리도록 정리했다.
- `AdminPatientController`의 페이지 타이틀 한글도 정상화해서 화면 제목이 깨지지 않도록 보강했다.
- `AdminPatientControllerTest`는 새 설명 문구, placeholder, 조회/초기화 버튼, 빈 결과 메시지 기준으로 기대값을 갱신했다.

## Task 31-7 구현 메모
- 예약 목록 컨트롤러 테스트는 이미 `keyword` 파라미터 전달과 keyword 유지 렌더링, 취소 후 keyword 유지 redirect를 검증하고 있어 이번 단계에서 체크 완료 처리했다.
- 예약 목록 서비스 테스트에는 환자명 검색, 연락처 정규화 검색, 상태+검색어 조합 조회 검증이 이미 들어가 있었고, 이번에 페이지 링크와 상태 옵션 URL이 `status`, `keyword`를 유지하는 시나리오를 추가했다.
- 환자 목록 컨트롤러 테스트는 `keyword` 단일 파라미터 전달과 입력창 value 유지 검증으로 현재 UI 계약을 닫았다.
- 환자 목록 서비스 테스트에는 이름 검색, 연락처 검색, 빈 결과 검증이 이미 있었고, 이번에 이전/다음 링크와 pageLinks가 `keyword`를 유지하는 시나리오를 추가했다.
- `admin.reservation` 범위 테스트를 다시 신뢰 가능하게 만들기 위해 주석 처리돼 있던 예약 취소 API 엔드포인트도 복구했다.

## Task 31-8 구현 메모
- `workflow-031`, `task-031`을 현재 구현 상태 기준으로 완료 처리했다.
- 예약/환자 목록 검색 통합 작업의 구현 점검 결과와 리뷰 포인트를 문서에 반영했다.
- 범위 테스트와 전체 테스트를 다시 실행해 최종 상태를 확인했다.

## 검증 결과
- `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.*' --tests 'com.smartclinic.hms.admin.patient.*'` 통과
- `./gradlew test` 통과