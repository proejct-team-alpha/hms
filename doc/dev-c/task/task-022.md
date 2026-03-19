# task-022

## 목적
- [x] `workflow-022`의 환자 관리 목록·상세 범위를 실제 구현 가능한 작업 단위로 분해한다.
- [x] 환자 관리 메인 화면을 검색 중심 인덱스 화면으로 구현한다.
- [x] 환자 상세 화면은 기본 정보와 전체 예약 이력 확인에 집중하도록 분리한다.
- [x] 목록은 가볍게, 상세는 깊게 보여주는 구조를 유지한다.
- [x] `admin.patient` 범위 컨트롤러/서비스/템플릿/테스트를 순서대로 정리한다.

## Task 22-1. 현재 환자/예약 조회 구조 점검
- [x] `Patient`, `Reservation` 엔티티와 연관관계를 점검한다.
- [x] 환자 기준 목록 조회에 필요한 현재 리포지토리/쿼리 가능 범위를 확인한다.
- [x] 같은 환자가 여러 예약을 가진 경우 환자 1명당 1줄로 묶기 위한 기준을 정리한다.
- [x] 연락처 검색 시 하이픈 포함/미포함 입력 처리 방법을 메모한다.
- [x] 존재하지 않는 `patientId` 상세 요청 시 처리 방식을 프로젝트 규칙 기준으로 정리한다.

## Task 22-2. 환자 목록 응답 구조와 서비스 설계
- [x] `AdminPatientController`, `AdminPatientService`, `AdminPatientRepository`의 기본 책임을 설계한다.
- [x] 환자 목록용 응답 DTO를 정의한다.
- [x] 목록 응답에 검색 조건, 총 건수, page, size, totalPages, pageLinks 구조를 반영한다.
- [x] 환자 1명당 1줄로 반환되도록 목록 조회 기준을 정리한다.
- [x] 기본 정렬을 최신 등록순 기준으로 맞춘다.

## Task 22-3. 환자 목록 검색 + 페이징 구현
- [x] `GET /admin/patient/list`를 추가한다.
- [x] 이름/연락처 검색을 구현한다.
- [x] 기본 size=20 페이징을 구현한다.
- [x] 검색 결과가 없을 때 빈 목록 메시지가 자연스럽게 보이도록 한다.
- [x] 결과 요약(총 건수, 현재 검색 조건)을 목록 화면에 반영한다.

## Task 22-4. 환자 상세 + 전체 예약 이력 구현
- [x] `GET /admin/patient/detail?patientId={id}`를 추가한다.
- [x] 환자 기본 정보(이름, 연락처, 이메일, 주소, 메모)를 표시한다.
- [x] 해당 환자의 전체 예약 이력을 함께 조회한다.
- [x] 예약 이력에는 예약번호, 날짜, 시간, 진료과, 의사, 상태를 우선 검토한다.
- [x] 없는 `patientId` 상세 요청은 404 또는 프로젝트 규칙에 맞는 에러 화면으로 처리한다.

## Task 22-5. 목록/상세 Mustache 화면 구현
- [x] `patient-list.mustache`를 검색 중심 인덱스 화면으로 구현한다.
- [x] `patient-detail.mustache`를 기본 정보 + 예약 이력 중심으로 구현한다.
- [x] 관리자 목록 화면의 `model + pageTitle` 패턴을 적용한다.
- [x] 목록에서는 과도한 상세 정보/통계/예약 이력 미리보기를 넣지 않는다.
- [x] 관리 컬럼은 우선 `상세보기` 기준으로 시작하고, 환자명 링크 연결은 후속 검토 가능하게 둔다.

## Task 22-6. 테스트 보강 및 문서 갱신
- [x] `AdminPatientControllerTest`를 추가하거나 보강한다.
- [x] `AdminPatientServiceTest`를 추가하거나 보강한다.
- [x] 목록 검색, 페이징, 빈 결과, 상세 조회, 없는 patientId 시나리오를 검증한다.
- [x] `workflow-022`, `task-022` 완료 기준을 현재 구현 상태에 맞게 갱신한다.
- [x] PR 리뷰 포인트와 남은 확장 메모를 문서에 반영한다.

## 완료 기준
- [x] `/admin/patient/list`에서 이름/연락처 검색이 가능하다.
- [x] `/admin/patient/list`에서 기본 20건 페이징이 적용된다.
- [x] 환자 목록은 환자 1명당 1줄로 렌더링된다.
- [x] `/admin/patient/detail?patientId={id}`에서 환자 기본 정보와 전체 예약 이력이 보인다.
- [x] 목록 화면은 검색 중심 인덱스 역할에 집중한다.
- [x] 관련 `admin.patient` 범위 테스트가 통과한다.

## 추천 진행 순서
- [x] Task 22-1 현재 환자/예약 조회 구조 점검
- [x] Task 22-2 환자 목록 응답 구조와 서비스 설계
- [x] Task 22-3 환자 목록 검색 + 페이징 구현
- [x] Task 22-4 환자 상세 + 전체 예약 이력 구현
- [x] Task 22-5 목록/상세 Mustache 화면 구현
- [x] Task 22-6 테스트 보강 및 문서 갱신

## 메모
- [x] 환자 목록은 예약 목록의 변형이 아니라 환자 기준 목록이어야 한다.
- [x] 예약 목록은 같은 환자가 여러 예약을 가지면 중복 row가 생기지만, 환자 목록은 환자 1명당 1줄이어야 한다.
- [x] 연락처 검색은 하이픈 포함/미포함 입력 모두 고려해야 한다.
- [x] 상세 화면의 예약 이력은 전체 이력 확인용이며, 목록 화면에 이력 미리보기를 넣지 않는다.
- [x] 이후 단계에서 예약 목록 환자명 클릭을 환자 상세로 연결하는 UX 개선을 검토할 수 있다.
- [x] 현재 연관관계는 `Reservation -> Patient`의 `@ManyToOne`이며, `reservation.patient_id` FK로 환자를 참조한다.
- [x] 온라인 예약, 전화 예약, 방문 접수 모두 전화번호로 기존 환자를 찾고 없으면 새 `Patient`를 생성한 뒤 예약에 연결한다.
- [x] 현재 `PatientRepository`는 `findByPhone(String phone)`만 제공하므로, 환자 목록 검색/페이징을 위해 `admin.patient` 범위 전용 조회 리포지토리가 추가로 필요하다.
- [x] `src/main/java/com/smartclinic/hms/admin/patient/FILES.md`에는 `AdminPatientController`, `AdminPatientService`가 예정 파일로만 정의되어 있고 실제 구현 파일은 아직 없다.
- [x] `templates/admin` 아래에는 `patient-list.mustache`, `patient-detail.mustache`가 아직 없고, `src/test/java/com/smartclinic/hms/admin`에도 환자 관리 테스트가 아직 없다.
- [x] 같은 환자가 여러 예약을 가진 경우 환자 목록은 `patient.id` 기준으로 1줄이어야 하며, 상세 화면에서만 해당 환자의 전체 예약 이력을 보여주는 방향이 자연스럽다.
- [x] 연락처 검색은 현재 시스템이 동일 환자 판별에 전화번호 문자열을 사용하므로, 하이픈 포함/미포함을 정규화해서 처리하는 것이 안전하다.
- [x] 없는 `patientId` 상세 요청은 기존 관리자 SSR 패턴에 맞춰 404 또는 프로젝트 규칙에 맞는 에러 화면으로 처리하는 방향이 적절하다.
- [x] `admin.patient` 범위에 `AdminPatientController`, `AdminPatientService`, `AdminPatientRepository` 스켈레톤을 추가했다.
- [x] 목록 응답 구조는 `AdminPatientListResponse`, `AdminPatientPageLinkResponse`, `AdminPatientSummary` 기준으로 정리했고, 기본 page/size/이전·다음 URL 구조를 포함한다.
- [x] 상세 응답 골격은 `AdminPatientDetailResponse`, `AdminPatientReservationHistoryItemResponse`로 미리 정의해 다음 단계에서 예약 이력 구현을 바로 연결할 수 있게 했다.
- [x] 목록 검색 파라미터는 `nameKeyword`, `contactKeyword` 두 축으로 분리했고, 연락처는 하이픈 제거 후 검색하는 방향으로 쿼리 구조를 잡았다.
- [x] 기본 페이지 크기는 20건, 기본 정렬은 `createdAt desc, id desc` 기준으로 설계했다.
- [x] `GET /admin/patient/list`를 실제로 추가했고, 기본 size=20과 `nameKeyword`/`contactKeyword` 검색 파라미터를 받도록 연결했다.
- [x] `patient-list.mustache`를 기본 동작 가능한 검색형 목록 화면으로 추가해 페이지 제목, 설명 문구, 검색 영역, 결과 요약, 목록 테이블, 빈 상태, 페이지네이션을 우선 반영했다.
- [x] `AdminPatientControllerTest`, `AdminPatientServiceTest`를 추가해 기본 페이징, 요청 파라미터 전달, 이름/연락처 검색, 최신 등록순 정렬을 검증했다.
- [x] `GET /admin/patient/detail`를 추가했고, `AdminPatientService.getPatientDetail(...)`에서 환자 기본 정보와 예약 이력을 함께 반환하도록 구현했다.
- [x] 예약 이력은 `Reservation -> Doctor -> Staff`, `Reservation -> Department`를 조회해 예약번호/날짜/시간/진료과/의사/상태 문구를 표시하는 구조로 정리했다.
- [x] 없는 `patientId` 상세 요청은 기존 관리자 SSR 상세 패턴에 맞춰 404 상태와 `error/404` 화면으로 처리하도록 연결했다.
- [x] `patient-detail.mustache`를 추가해 환자 기본 정보 카드와 예약 이력 테이블을 우선 구현했다.
- [x] `AdminPatientControllerTest`, `AdminPatientServiceTest`에 상세 조회/없는 patientId/예약 이력 매핑 시나리오를 추가해 `admin.patient` 범위 테스트 통과를 확인했다.
- [x] `patient-list.mustache`는 `환자 관리 > 환자 목록` 톤으로 정리했고, 검색/요약/테이블/페이지네이션만 유지해 인덱스 화면 성격을 분명히 했다.
- [x] `patient-detail.mustache`는 기본 정보 카드와 예약 이력 섹션을 분리해 목록보다 더 많은 정보를 읽는 화면으로 역할을 분리했다.
- [x] 관리 컬럼은 `상세보기`만 유지하고, 환자명 링크/추가 액션은 후속 UX 개선 포인트로 남겼다.
- [x] `AdminPatientControllerTest`에 빈 결과 메시지와 핵심 화면 문구 렌더링을 명시적으로 검증하는 시나리오를 추가했다.
- [x] `AdminPatientServiceTest`에 검색 결과가 없는 경우 빈 목록 응답이 안정적으로 내려오는 시나리오를 추가했다.
- [x] `workflow-022`, `task-022` 완료 기준과 리뷰 포인트를 현재 구현 상태에 맞춰 최종 갱신했다.

## PR 리뷰 포인트
- 환자 관리 메인을 검색형 인덱스 화면으로 두고, 상세 정보와 예약 이력을 상세 페이지로 분리한 방향이 팀 기준에 맞는지 확인 부탁드립니다.
- 연락처 검색을 하이픈 제거 기준으로 정규화했는데, 이후 환자 식별 규칙과 같은 방향으로 계속 유지해도 되는지 봐주시면 좋겠습니다.
- 예약 목록에서 환자 상세로 바로 가는 링크는 아직 후속 UX 개선 포인트로 남겨두었고, 현재는 `상세보기` 버튼만 유지한 점 참고 부탁드립니다.
