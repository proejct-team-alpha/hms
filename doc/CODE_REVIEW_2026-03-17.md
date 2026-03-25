# 코드 리뷰 보고서

**리뷰 대상:** `dev` 브랜치 전체 코드베이스
**리뷰 일자:** 2026-03-17
**리뷰 기준:** `doc/rules/` 규칙 문서 6종 + 아키텍처/보안/프론트엔드/테스트 종합
**변경 규모:** 430 files changed, +31,170 / -869 lines (main 대비)
**총 이슈:** 37건 (CRITICAL 5건 / HIGH 13건 / MEDIUM 11건 / LOW 8건)

---

## 팀 구성 및 코드 소유권


| 역할           | 이름  | 담당 영역                                                                                                  |
| ------------ | --- | ------------------------------------------------------------------------------------------------------ |
| 책임개발자 (Lead) | 김민구 | `config/`**, `common/**`, `domain/**`, `llm/**`, `application*.properties`, `templates/common/**`      |
| 개발자 A        | 강태오 | `reservation/**`, `home/**`, `templates/reservation/**`, `templates/home/**`                           |
| 개발자 B        | 조유지 | `staff/**`, `doctor/**`, `nurse/**`, `templates/staff/**`, `templates/doctor/**`, `templates/nurse/**` |
| 개발자 C        | 강상민 | `admin/**`, `item/**`, `templates/admin/**`                                                            |


---

## 심각도 요약


| 심각도      | 건수  | 상태    | 조치 기준       |
| -------- | --- | ----- | ----------- |
| CRITICAL | 5   | 수정 필수 | 병합 전 반드시 수정 |
| HIGH     | 13  | 수정 필요 | 병합 전 수정 권장  |
| MEDIUM   | 11  | 수정 권장 | 가능한 빨리 수정   |
| LOW      | 8   | 참고    | 일정 여유 시 수정  |


---

## Part 1. 규칙 준수 점검 (doc/rules/ 6종)

### 1.1 rule-controller.md 준수 점검


| #   | 규칙                                            | 준수    | 위반 내용                                                                                                                           | 담당자     |
| --- | --------------------------------------------- | ----- | ------------------------------------------------------------------------------------------------------------------------------- | ------- |
| 1   | SSR Controller는 String(뷰 경로) 반환               | O     | 모든 SSR Controller가 준수                                                                                                           | -       |
| 2   | API Controller는 ResponseEntity + Resp.ok() 사용 | O     | AdminDashboardApiController, AdminReservationApiController, ReservationApiController 모두 준수                                      | -       |
| 3   | Controller는 Service만 호출 (Repository 직접 호출 금지) | O     | 모든 Controller가 준수                                                                                                               | -       |
| 4   | DTO 네이밍: Request/Response 규칙                  | **X** | `ReservationCreateForm`, `ReservationUpdateForm` -- Form 접미사 사용. 규칙상 `CreateReservationRequest`, `UpdateReservationRequest`여야 함 | **강태오** |
| 5   | 비즈니스 로직이 Controller에 없을 것                     | **X** | `ReceptionController:40-48`에서 subList 수동 페이징 로직 직접 수행. Service 위임 필요                                                            | **조유지** |
| 6   | 예외 응답은 공통 처리기(GlobalExceptionHandler) 사용      | **X** | `ReceptionService`에서 RuntimeException 직접 사용 -- GlobalExceptionHandler 우회                                                        | **조유지** |


### 1.2 rule-repository.md 준수 점검


| #   | 규칙                                           | 준수    | 위반 내용                                                                                                            | 담당자            |
| --- | -------------------------------------------- | ----- | ---------------------------------------------------------------------------------------------------------------- | -------------- |
| 1   | Repository는 소유 기능 모듈 패키지에 배치                 | O     | 각 모듈별 Repository가 해당 패키지에 위치                                                                                     | -              |
| 2   | JpaRepository 인터페이스 기반 작성                    | O     | 모든 Repository가 JpaRepository 상속                                                                                  | -              |
| 3   | 금지 네이밍 미사용 (AdminDashboardStatsRepository 등) | **X** | `AdminDashboardStatsRepositoryTest`라는 테스트 파일 존재. 프로덕션에 해당 Repository는 없으나, 테스트 파일명이 존재하지 않는 클래스를 참조하므로 혼란 유발     | **강상민**        |
| 4   | 쿼리 우선순위: 파생 메서드 > Projection > @Query        | **X** | `AdminReservationRepository`, `AdminStaffRepository`에서 @Query(nativeQuery=true) 다수 사용. 파생 메서드/JPQL로 대체 가능한 쿼리 존재 | **강상민**        |
| 5   | 다중 도메인 집계는 Service 조합 처리                     | O     | `AdminDashboardStatsService`에서 4개 Repository 결과를 Service에서 조합 -- 준수                                              | -              |
| 6   | EntityManager 직접 사용 금지                       | O     | 모든 Repository가 인터페이스 기반 -- 준수                                                                                    | -              |
| 7   | 동일 엔티티 중복 Repository 방지                      | **X** | Reservation 엔티티에 5개 Repository 존재, Staff/Patient/Department/Item 각 2개씩 중복                                        | **김민구** (아키텍처) |


### 1.3 rule_spring.md 준수 점검


| #   | 규칙                                             | 준수    | 위반 내용                                                                                           | 담당자               |
| --- | ---------------------------------------------- | ----- | ----------------------------------------------------------------------------------------------- | ----------------- |
| 1   | 환경 변수로 민감 정보 관리                                | O     | API 키는 `${CLAUDE_API_KEY:}` 환경변수 사용, .gitignore에 .env 포함                                        | -                 |
| 2   | 전역 에러 핸들러 ErrorResponse 포맷 통일                  | **X** | `Resp`(status,msg,body)와 `ErrorResponse`(success,errorCode,message,timestamp,...) 두 가지 응답 포맷 공존 | **김민구**           |
| 3   | 비즈니스 예외는 CustomException(BusinessException) 사용 | **X** | `ReceptionService`에서 RuntimeException 5회 직접 사용. `Reservation` 엔티티에서 IllegalStateException 사용    | **조유지** / **김민구** |
| 4   | Service 클래스 레벨 @Transactional(readOnly=true)   | O     | 모든 Service가 일관되게 적용                                                                             | -                 |
| 5   | DTO는 Java Record 우선                            | O     | 대부분의 DTO가 Record로 선언                                                                            | -                 |
| 6   | @Valid로 요청 검증                                  | **X** | `WalkinController:37`, `PhoneReservationController:36`에서 @Valid 누락                              | **조유지**           |
| 7   | 페이징은 Pageable 기본 사용                            | **X** | `ReceptionController`에서 subList 수동 페이징 -- Pageable/Page 미사용                                     | **조유지**           |


### 1.4 rule_test.md 준수 점검


| #   | 규칙                                       | 준수    | 위반 내용                                                                                              | 담당자               |
| --- | ---------------------------------------- | ----- | -------------------------------------------------------------------------------------------------- | ----------------- |
| 1   | 핵심 로직에 대한 테스트 존재                         | **X** | Auth, Doctor, Nurse, Staff 접수, Walk-in, Phone Reservation, 도메인 상태 전이 테스트 전무                        | **조유지** / **김민구** |
| 2   | LocalDateTime.now() 직접 사용 금지 (Clock 추상화) | **X** | DoctorTreatmentService(5회), NurseService(2회), ReceptionService(2회), AdminDashboardStatsService(2회) | **조유지** / **강상민** |
| 3   | Given-When-Then 주석 필수                    | O     | 기존 테스트 모두 // given, // when, // then 주석 사용                                                         | -                 |
| 4   | BDDMockito given().willReturn() 사용       | O     | ReservationServiceTest 등에서 BDDMockito 사용                                                           | -                 |
| 5   | @DisplayName 필수                          | **X** | `AdminReservationServiceTest`의 @DisplayName 한글 인코딩 깨짐                                              | **강상민**           |
| 6   | 단일 책임: 한 테스트 = 한 시나리오                    | **X** | `AdminDashboardStatsRepositoryTest`에서 4종 쿼리를 1개 테스트에서 일괄 검증 (Mega-test)                            | **강상민**           |
| 7   | TestSecurityConfig 중복 방지                 | **X** | 10개 WebMvcTest 클래스에 거의 동일한 TestSecurityConfig 내부 클래스 반복 선언                                         | **김민구** (공통)      |


### 1.5 rule_css.md 준수 점검


| #   | 규칙                        | 준수    | 위반 내용                                                               | 담당자    |
| --- | ------------------------- | ----- | ------------------------------------------------------------------- | ------ |
| 1   | BEM 네이밍 규칙                | O     | common.css에서 BEM 패턴 사용                                              | -      |
| 2   | CSS 변수 사용 (하드코딩 색상/크기 금지) | O     | :root에 CSS 변수 정의 후 사용                                               | -      |
| 3   | id 셀렉터 사용 금지              | O     | CSS에서 id 셀렉터 미사용 확인                                                 | -      |
| 4   | 재사용 가능한 단위로 분리            | **X** | 역할별 CSS 7개 파일(style-admin.css, admin-style.css 등)이 내용 100% 동일 -- 중복 | **전원** |
| 5   | 인라인 스타일 금지                | O     | Mustache 템플릿에서 인라인 style 미사용                                        | -      |


### 1.6 rule_javascript.md 준수 점검


| #   | 규칙                               | 준수    | 위반 내용                                                                                                   | 담당자     |
| --- | -------------------------------- | ----- | ------------------------------------------------------------------------------------------------------- | ------- |
| 1   | var 사용 금지                        | O     | 모든 JS 파일에서 const/let 사용                                                                                 | -       |
| 2   | const 우선 사용                      | O     | const 기본, 재할당 시만 let                                                                                    | -       |
| 3   | async/await 기본 (.then/.catch 지양) | **X** | `direct-reservation.mustache:198-214`에서 .then() 체인 사용 + .catch() 누락                                     | **강태오** |
| 4   | fetch 응답 에러 처리 (response.ok 검사)  | **X** | `direct-reservation.mustache`의 두 fetch 호출에서 response.ok 미검사, catch 없음                                   | **강태오** |
| 5   | 전역 변수 금지                         | **X** | `data-admin.js`, `data-staff.js`, `data-nurse.js` 등에서 전역 함수(getReservations 등) 중복 정의 -- 단, 이 파일들은 죽은 코드 | **전원**  |
| 6   | innerHTML 직접 사용 금지 (XSS)         | **X** | `chatbot.mustache`의 escapeHtml()이 큰따옴표/작은따옴표 미이스케이프                                                     | **조유지** |


---

## Part 2. 이슈 상세 및 담당자 배정

### CRITICAL (5건) -- 병합 전 반드시 수정


| ID   | 이슈                                                                                            | 위반 규칙                                | 위치                                                               | 담당자     |
| ---- | --------------------------------------------------------------------------------------------- | ------------------------------------ | ---------------------------------------------------------------- | ------- |
| C-01 | **ReceptionService에서 RuntimeException 직접 사용** -- GlobalExceptionHandler 우회, 사용자에게 500 에러 노출   | rule_spring (3), rule-controller (5) | `ReceptionService.java:62,66,94,131,139`                         | **조유지** |
| C-02 | **WalkinController, PhoneReservationController에서 @Valid 누락** -- DTO의 @NotBlank 등 검증 완전 무효화    | rule_spring (6)                      | `WalkinController.java:37`, `PhoneReservationController.java:36` | **조유지** |
| C-03 | **PhoneReservationRequestDto 검증 메시지 오류** -- phone 필드에 "환자 이름은 필수입니다", time 필드에 "예약 날짜는 필수입니다" | rule_spring (6)                      | `PhoneReservationRequestDto.java:15,27`                          | **조유지** |
| C-04 | **staff/dashboard.mustache 빈 목록 조건 불일치** -- recentList로 순회하지만 hasRecent로 빈 체크. 빈 목록 안내 미표시    | -                                    | `staff/dashboard.mustache:101-115`                               | **조유지** |
| C-05 | **direct-reservation.mustache fetch 오류 처리 부재** -- catch 없음, response.ok 미검사                   | rule_javascript (3,4)                | `direct-reservation.mustache:198-214, 262-272`                   | **강태오** |


### HIGH (13건) -- 병합 전 수정 권장


| ID   | 이슈                                                                                            | 위반 규칙                                  | 위치                                                                                         | 담당자               |
| ---- | --------------------------------------------------------------------------------------------- | -------------------------------------- | ------------------------------------------------------------------------------------------ | ----------------- |
| H-01 | **IDOR -- 비인증 예약 취소/변경 소유권 미검증**: 예약 ID만 알면 타인 예약 취소/변경 가능                                    | rule_spring (10)                       | `ReservationController.java:131-165`                                                       | **강태오**           |
| H-02 | **예약번호 생성 이중화**: System.nanoTime() vs ReservationNumberGenerator 공존                           | rule_spring (5)                        | `ReservationService.java:76`                                                               | **강태오**           |
| H-03 | **예약 변경 TOCTOU 레이스 컨디션**: 기존 예약 취소 후 새 예약 실패 시 데이터 불일치                                        | rule_spring (5)                        | `ReservationService.java:138-174`                                                          | **강태오**           |
| H-04 | **마이페이지 비밀번호 변경 복잡도 미검증**: 1자 비밀번호 설정 가능 (생성 시 @Size(min=8) 있으나 변경 시 없음)                      | rule_spring (7)                        | `AdminMypageService.java:35-45`, 외 4개 Mypage                                               | **김민구** (공통)      |
| H-05 | **RateLimitFilter Race Condition + IP 스푸핑**: Bucket.count 비-atomic, X-Forwarded-For 위조로 우회 가능 | rule_spring (10)                       | `RateLimitFilter.java:55-64, 109-117`                                                      | **김민구**           |
| H-06 | **Repository 쿼리/Projection 완전 중복**: ReservationRepository와 AdminReservationRepository에 동일 쿼리  | rule-repository (7)                    | `ReservationRepository.java:64-123`, `AdminReservationRepository.java:18-77`               | **강태오** + **강상민** |
| H-07 | **LocalDate.now() 직접 사용** (Clock 미주입) -- 테스트 불가능                                              | rule_test (2)                          | `DoctorTreatmentService`, `NurseService`, `ReceptionService`, `AdminDashboardStatsService` | **조유지** + **강상민** |
| H-08 | **Auth/Doctor/Nurse/Staff 접수 모듈 테스트 전무** -- 핵심 로직 테스트 없이 배포 금지 규칙 위반                          | rule_test (1)                          | `src/test/`                                                                                | **조유지**           |
| H-09 | **AdminReservationServiceTest 한글 인코딩 깨짐** -- @DisplayName 손상                                  | rule_test (5)                          | `AdminReservationServiceTest.java`                                                         | **강상민**           |
| H-10 | **error/500.mustache DOCTYPE/html/head 누락** -- 깨진 HTML 반환                                     | -                                      | `error/500.mustache`                                                                       | **김민구**           |
| H-11 | **죽은 코드 ~33개 파일** -- 17개 프로토타입 JS, 5개 data JS, 7개 중복 CSS, 기타                                  | rule_css (4), rule_javascript (5)      | `static/js/header-*.js`, `static/js/data-*.js`, `static/css/style-*.css` 등                 | **전원**            |
| H-12 | **ReceptionController에서 subList 수동 페이징** -- Service에 위임하지 않고 Controller에서 비즈니스 로직 수행          | rule-controller (5,7), rule_spring (8) | `ReceptionController.java:40-48`                                                           | **조유지**           |
| H-13 | **DTO 네이밍 불일치**: ReservationCreateForm/UpdateForm -- 규칙상 Request 접미사 필요                       | rule-controller (3,4)                  | `ReservationCreateForm.java`, `ReservationUpdateForm.java`                                 | **강태오**           |


### MEDIUM (11건) -- 가능한 빨리 수정


| ID   | 이슈                                                                  | 위반 규칙                                | 위치                                                    | 담당자               |
| ---- | ------------------------------------------------------------------- | ------------------------------------ | ----------------------------------------------------- | ----------------- |
| M-01 | Resp vs ErrorResponse 응답 포맷 불일치                                     | rule_spring (2), rule-controller (4) | `Resp.java`, `GlobalExceptionHandler.java`            | **김민구**           |
| M-02 | SsrExceptionHandler가 CustomException 메시지 무시 -- 일괄 "오류가 발생했습니다" 반환   | rule_spring (2)                      | `SsrExceptionHandler.java:19-25`                      | **김민구**           |
| M-03 | 프로덕션 DTO에 전화번호/시간 @Pattern 미검증 (SampleDTO에는 있음)                     | rule_spring (7)                      | `ReservationCreateForm.java`, `WalkinRequestDto.java` | **강태오** + **조유지** |
| M-04 | NurseService 전체 데이터 로드 후 stream 필터링 (카운트만 필요)                       | rule_spring (8)                      | `NurseService.java:25-39`                             | **조유지**           |
| M-05 | ReservationNumberGenerator ConcurrentHashMap 메모리 누수 (과거 날짜 카운터 미정리) | rule_spring (5)                      | `ReservationNumberGenerator.java:56`                  | **김민구**           |
| M-06 | session.cookie.secure=false 공통 설정에 하드코딩                             | rule_spring (1)                      | `application.properties:43`                           | **김민구**           |
| M-07 | CSP unsafe-inline 허용 -- XSS 방어 약화                                   | rule_spring (10)                     | `SecurityConfig.java:113-119`                         | **김민구**           |
| M-08 | 미사용 JWT 의존성 (com.auth0:java-jwt:4.3.0) -- 불필요한 공격 표면                | rule_spring (1)                      | `build.gradle:34`                                     | **김민구**           |
| M-09 | Permissions-Policy 헤더 미설정 (주석에 명시되어 있으나 구현 누락)                      | rule_spring (10)                     | `SecurityConfig.java`                                 | **김민구**           |
| M-10 | home/index.mustache 인라인 헤더/푸터 -- 공통 partial 미사용                     | -                                    | `home/index.mustache`                                 | **강태오**           |
| M-11 | direct-reservation.mustache 진료과 드롭다운 하드코딩 + JS DEPT_ID_MAP 이중 관리    | -                                    | `direct-reservation.mustache:90-95, 181`              | **강태오**           |


### LOW (8건) -- 참고


| ID   | 이슈                                                               | 위치                                                          | 담당자     |
| ---- | ---------------------------------------------------------------- | ----------------------------------------------------------- | ------- |
| L-01 | DoctorTreatmentController diagnosis/prescription 입력 미검증          | `DoctorTreatmentController.java:46-53`                      | **조유지** |
| L-02 | NurseReceptionController phone 파라미터 형식 미검증                       | `NurseReceptionController.java:69-83`                       | **조유지** |
| L-03 | AdminReservationService URL 직접 문자열 결합 (UriComponentsBuilder 미사용) | `AdminReservationService.java:164-166`                      | **강상민** |
| L-04 | chatbot escapeHtml() 큰따옴표/작은따옴표 미이스케이프                           | `nurse/chatbot.mustache:134`, `doctor/chatbot.mustache:135` | **조유지** |
| L-05 | 테스트 데이터 모든 계정 동일 비밀번호 (password123)                              | `sql_test.sql:15-25`                                        | **김민구** |
| L-06 | AdminDashboardStatsRepositoryTest Mega-test (4종 1개 테스트)          | `AdminDashboardStatsRepositoryTest.java`                    | **강상민** |
| L-07 | 로그인 Rate Limit 10회/분 -- 계정 잠금 정책 없음                              | `RateLimitFilter.java:35`                                   | **김민구** |
| L-08 | TestSecurityConfig 10중 중복                                        | `src/test/` 전체                                              | **김민구** |


---

## Part 3. 담당자별 수정 사항 요약

### 김민구 (책임개발자/Lead) -- 10건


| 우선순위   | ID   | 수정 사항                                                                 |
| ------ | ---- | --------------------------------------------------------------------- |
| HIGH   | H-04 | 마이페이지 비밀번호 변경에 복잡도 정책 추가 (min 8자, 대소문자+숫자+특수문자)                       |
| HIGH   | H-05 | RateLimitFilter Bucket.count를 AtomicInteger로 변경, trust-proxy 문서 경고 추가 |
| HIGH   | H-10 | error/500.mustache 완전한 HTML5 문서 구조로 재작성                               |
| MEDIUM | M-01 | Resp vs ErrorResponse 응답 포맷 통일                                        |
| MEDIUM | M-02 | SsrExceptionHandler에 CustomException 전용 핸들러 추가                        |
| MEDIUM | M-05 | ReservationNumberGenerator 과거 날짜 카운터 @Scheduled 정리                    |
| MEDIUM | M-06 | session.cookie.secure 기본값 true, dev에서만 false로 변경                      |
| MEDIUM | M-07 | CSP unsafe-inline 장기 제거 계획 수립 (단기: 현상 유지, 장기: nonce 기반)               |
| MEDIUM | M-08 | build.gradle에서 미사용 JWT 의존성 제거                                         |
| MEDIUM | M-09 | Permissions-Policy 헤더 구현 추가                                           |


### 강태오 (개발자 A) -- 8건


| 우선순위     | ID   | 수정 사항                                                                        |
| -------- | ---- | ---------------------------------------------------------------------------- |
| CRITICAL | C-05 | direct-reservation.mustache fetch에 catch + response.ok 검사 추가, async/await 전환 |
| HIGH     | H-01 | ReservationController 예약 취소/변경에 소유권 검증 추가 (예약번호+전화번호)                        |
| HIGH     | H-02 | ReservationService 예약번호 생성을 ReservationNumberGenerator로 통일                   |
| HIGH     | H-03 | 예약 변경 TOCTOU 수정 (비관적 락 또는 전체 트랜잭션 롤백 보장)                                     |
| HIGH     | H-06 | ReservationRepository에서 Admin 전용 중복 쿼리 블록 제거 (강상민과 협의)                       |
| HIGH     | H-13 | ReservationCreateForm/UpdateForm을 Request 접미사로 네이밍 변경                        |
| MEDIUM   | M-03 | ReservationCreateForm에 전화번호/시간 @Pattern 검증 추가                                |
| MEDIUM   | M-10 | home/index.mustache에서 공통 partial 사용으로 전환                                     |
| MEDIUM   | M-11 | direct-reservation.mustache 진료과를 서버 데이터 바인딩으로 전환                             |


### 조유지 (개발자 B) -- 12건


| 우선순위     | ID   | 수정 사항                                                                              |
| -------- | ---- | ---------------------------------------------------------------------------------- |
| CRITICAL | C-01 | ReceptionService의 5개 RuntimeException을 CustomException.notFound()로 교체              |
| CRITICAL | C-02 | WalkinController, PhoneReservationController에 @Valid + BindingResult 추가            |
| CRITICAL | C-03 | PhoneReservationRequestDto 검증 메시지 오류 수정                                            |
| CRITICAL | C-04 | staff/dashboard.mustache 빈 목록 조건을 recentList로 통일                                   |
| HIGH     | H-07 | DoctorTreatmentService, NurseService, ReceptionService에 Clock 주입                   |
| HIGH     | H-08 | ReceptionServiceTest, WalkinServiceTest, TreatmentServiceTest, NurseServiceTest 작성 |
| HIGH     | H-12 | ReceptionController 수동 페이징을 Service로 위임, DB 레벨 Pageable 사용                         |
| MEDIUM   | M-03 | WalkinRequestDto, PhoneReservationRequestDto에 @Pattern 검증 추가                       |
| MEDIUM   | M-04 | NurseService 대시보드 통계를 DB 쿼리 레벨 집계로 전환                                              |
| LOW      | L-01 | DoctorTreatmentController 진료 입력 DTO + @Valid 추가                                    |
| LOW      | L-02 | NurseReceptionController phone 형식 검증 추가                                            |
| LOW      | L-04 | chatbot escapeHtml()에 큰따옴표/작은따옴표 이스케이프 추가                                          |


### 강상민 (개발자 C) -- 4건


| 우선순위 | ID   | 수정 사항                                                 |
| ---- | ---- | ----------------------------------------------------- |
| HIGH | H-06 | AdminReservationRepository 중복 쿼리 정리 (강태오와 협의)         |
| HIGH | H-07 | AdminDashboardStatsService에 Clock 주입                  |
| HIGH | H-09 | AdminReservationServiceTest UTF-8 인코딩 수정              |
| LOW  | L-03 | AdminReservationService URL을 UriComponentsBuilder로 변경 |
| LOW  | L-06 | AdminDashboardStatsRepositoryTest를 4개 독립 테스트로 분리      |


### 전원 공동 -- 1건


| 우선순위 | ID   | 수정 사항                             |
| ---- | ---- | --------------------------------- |
| HIGH | H-11 | 죽은 코드 ~33개 파일 일괄 삭제 (각자 담당 영역 정리) |


---

## Part 4. 아키텍처 강점 (유지 사항)

- **3계층 구조** (Controller -> Service -> Repository) 일관적 적용
- **SecurityConfig** 체계적 구성 (역할 기반 접근 제어, CSRF, CSP, HSTS, 세션 관리)
- **도메인 엔티티** 팩토리 메서드, 상태 전이 캡슐화 (receive/complete/cancel)
- **DTO Record** 불변성 보장, @Transactional(readOnly=true) 클래스 레벨 적용
- **SSR/API Controller 분리** 일관된 패턴
- **LayoutModelInterceptor** 공통 레이아웃 데이터 자동 주입
- **CSRF 토큰** 모든 POST 폼에 빠짐없이 포함
- **BCrypt 패스워드 인코딩**, **사용자 열거 방지**, **세션 동시 접속 제한**
- **pages/admin-dashboard.js** -- IIFE, 중복 요청 방지, response.ok 검사 등 모범 패턴

---

## Part 5. 수정 우선순위 일정표

### P0 -- 즉시 수정 (CRITICAL 5건 + HIGH 일부)


| 담당자 | 작업                     | 예상 노력         |
| --- | ---------------------- | ------------- |
| 조유지 | C-01, C-02, C-03, C-04 | 낮음 (각 30분 이내) |
| 강태오 | C-05                   | 낮음 (1시간)      |
| 강태오 | H-01 (IDOR 수정)         | 중간 (2시간)      |


### P1 -- 단기 (HIGH 나머지)


| 담당자 | 작업               | 예상 노력          |
| --- | ---------------- | -------------- |
| 강태오 | H-02, H-03, H-13 | 중간             |
| 조유지 | H-07, H-08, H-12 | 높음 (테스트 작성 포함) |
| 강상민 | H-06, H-07, H-09 | 중간             |
| 김민구 | H-04, H-05, H-10 | 중간             |
| 전원  | H-11 (죽은 코드 삭제)  | 낮음             |


### P2 -- 중기 (MEDIUM)


| 담당자 | 작업                                 |
| --- | ---------------------------------- |
| 김민구 | M-01, M-02, M-05, M-06, M-08, M-09 |
| 강태오 | M-03, M-10, M-11                   |
| 조유지 | M-03, M-04                         |
| 김민구 | M-07 (장기 계획)                       |


