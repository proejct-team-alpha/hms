# 코드 리뷰 보고서

**리뷰 대상:** `feature/dev-a` 브랜치 전체 코드베이스
**리뷰 일자:** 2026-03-23
**리뷰 기준:** `doc/rules/` 규칙 문서 6종 + 아키텍처/보안/성능 종합
**총 이슈:** 25건 (CRITICAL 2건 / HIGH 9건 / MEDIUM 9건 / LOW 5건)

---

## 팀 구성 및 코드 소유권


| 역할           | 이름  | 담당 영역                                                                                                  |
| ------------ | --- | ------------------------------------------------------------------------------------------------------ |
| 책임개발자 (Lead) | 김민구 | `config/`**, `common/**`, `domain/**`, `llm/**`, `application*.properties`, `templates/common/**`      |
| 개발자 A        | 강태오 | `reservation/**`, `home/**`, `templates/reservation/**`, `templates/home/**`                           |
| 개발자 B        | 조유지 | `staff/**`, `doctor/**`, `nurse/**`, `templates/staff/**`, `templates/doctor/**`, `templates/nurse/**` |
| 개발자 C        | 강상민 | `admin/**`, `item/**`, `templates/admin/**`                                                            |


---

## 2026-03-20 리뷰 이후 수정 완료 항목


| ID   | 이슈                                         | 수정 내용                                                                                                  |
| ---- | ------------------------------------------ | ------------------------------------------------------------------------------------------------------ |
| C-02 | GlobalExceptionHandler SSR 예외 처리 공백        | `SsrExceptionHandler`를 `@ControllerAdvice(annotations = Controller.class)`로 분리. 403/404/500 상태코드 정확 반영 |
| C-04 | ChatController/MedicalController 소유권 검증 없음 | `SecurityUtils.resolveStaffId()` 공통 유틸 도입 + `ChatController:62-65`에서 인증 staffId와 경로 staffId 비교         |
| H-01 | Reservation.cancel() 상태 전이 결함              | `cancelFully()` 메서드 추가. 비회원 취소·예약 변경 시 `cancelFully(null)` 사용으로 명확히 분리                                 |
| H-03 | resolveStaffId() 완전 중복                     | `SecurityUtils` 공통 유틸로 추출. ChatController, MedicalController 양쪽 동일하게 주입                                |
| H-04 | DTO 네이밍 불일치 (Form 접미사)                     | `CreateReservationRequest`, `UpdateReservationRequest`로 네이밍 규칙 준수 완료                                   |
| M-02 | SsrExceptionHandler HTTP 상태 코드 불일치         | `CustomException.httpStatus` 기반 분기 — 403/404 정확 반환                                                     |
| M-03 | CreateReservationRequest @Pattern 검증 결함    | `^$|` 제거, `@NotBlank`와 `@Pattern` 역할 명확히 분리                                                            |


---

## 심각도 요약


| 심각도      | 건수  | 상태    | 조치 기준       |
| -------- | --- | ----- | ----------- |
| CRITICAL | 2   | 수정 필수 | 병합 전 반드시 수정 |
| HIGH     | 9   | 수정 필요 | 병합 전 수정 권장  |
| MEDIUM   | 9   | 수정 권장 | 가능한 빨리 수정   |
| LOW      | 5   | 참고    | 일정 여유 시 수정  |


---

## Part 1. 규칙 준수 점검 (doc/rules/ 6종)

### 1.1 rule-controller.md 준수 점검


| #   | 규칙                                                     | 준수    | 위반 내용                                                                                                        | 담당자     |
| --- | ------------------------------------------------------ | ----- | ------------------------------------------------------------------------------------------------------------ | ------- |
| 1   | SSR Controller는 String(뷰 경로) 반환                        | O     | 모든 SSR Controller 준수                                                                                         | -       |
| 2   | API Controller는 ResponseEntity + Resp.ok() 사용          | **X** | `ReservationApiController`에서 `List<DepartmentDto>`, `List<DoctorDto>`, `List<String>` 직접 반환. `Resp.ok()` 미사용 | **강태오** |
| 3   | Controller는 Service만 호출 (Repository 직접 호출 금지)          | O     | 전 영역 준수 확인                                                                                                   | -       |
| 4   | DTO 네이밍: Request/Response 규칙                           | O     | `CreateReservationRequest`, `UpdateReservationRequest` 네이밍 완료                                                | -       |
| 5   | 비즈니스 로직이 Controller에 없을 것                              | O     | 전 영역 준수 확인                                                                                                   | -       |
| 6   | 예외 응답은 GlobalExceptionHandler / SsrExceptionHandler 사용 | O     | SSR: `SsrExceptionHandler` @ControllerAdvice 적용. REST: `GlobalExceptionHandler` 처리                           | -       |
| 7   | GET SSR 핸들러는 HttpServletRequest 사용                     | **X** | `ReservationController.symptomReservation()` — GET이지만 `Model model` 파라미터 사용                                  | **강태오** |


### 1.2 rule-repository.md 준수 점검


| #   | 규칙                                    | 준수    | 위반 내용                                                                                                           | 담당자            |
| --- | ------------------------------------- | ----- | --------------------------------------------------------------------------------------------------------------- | -------------- |
| 1   | Repository는 소유 기능 모듈 패키지에 배치          | O     | 각 모듈별 Repository가 해당 패키지에 위치                                                                                    | -              |
| 2   | JpaRepository 인터페이스 기반 작성             | O     | 모든 Repository가 JpaRepository 상속                                                                                 | -              |
| 3   | 쿼리 우선순위: 파생 메서드 > Projection > @Query | **X** | `AdminReservationRepository`, `AdminStaffRepository`에서 nativeQuery 다수 사용. JPQL로 대체 가능                           | **강상민**        |
| 4   | 다중 도메인 집계는 Service 조합 처리              | O     | `AdminDashboardStatsService` Service 조합 — 준수                                                                    | -              |
| 5   | 동일 엔티티 중복 Repository 방지               | **X** | `Reservation` 4개(reservation/admin/nurse/staff 패키지), `Patient` 3개, `Staff` 2개, `Department` 2개 Repository 중복 존재 | **김민구** (아키텍처) |
| 6   | EntityManager 직접 사용 금지                | O     | 모든 Repository가 인터페이스 기반 — 준수                                                                                    | -              |


### 1.3 rule_spring.md 준수 점검


| #   | 규칙                                           | 준수    | 위반 내용                                                                                   | 담당자     |
| --- | -------------------------------------------- | ----- | --------------------------------------------------------------------------------------- | ------- |
| 1   | 환경 변수로 민감 정보 관리                              | O     | API 키는 `${CLAUDE_API_KEY:}` 환경변수 사용                                                     | -       |
| 2   | 전역 에러 핸들러 ErrorResponse 포맷 통일                | **X** | `Resp`(status, msg, body)와 일반 `ErrorResponse` 두 포맷 공존. 시스템 전체 응답 포맷 불일치                 | **김민구** |
| 3   | 비즈니스 예외는 CustomException 사용                  | O     | `Reservation.java`에서 상태 전이 이상 시 `IllegalStateException` 사용 (도메인 내부 Guard — 의도적 선택으로 허용) | -       |
| 4   | Service 클래스 레벨 @Transactional(readOnly=true) | O     | 모든 Service 일관 적용                                                                        | -       |
| 5   | DTO는 Java Record 우선                          | **X** | `ReservationCompleteInfo` — `@Getter` + 생성자 방식 일반 클래스. Java Record로 교체 필요               | **강태오** |
| 6   | @Valid로 요청 검증                                | O     | `POST /reservation/create`, `POST /reservation/modify/{id}` 모두 `@Valid` 적용              | -       |
| 7   | 페이징은 Pageable 기본 사용                          | O     | 전 영역 준수                                                                                 | -       |


### 1.4 rule_test.md 준수 점검


| #   | 규칙                                       | 준수    | 위반 내용                                                                                                                | 담당자               |
| --- | ---------------------------------------- | ----- | -------------------------------------------------------------------------------------------------------------------- | ----------------- |
| 1   | 핵심 로직에 대한 테스트 존재                         | **X** | `LlmReservationService`, `SymptomAnalysisService` 서비스 단위 테스트 전무. `ReceptionService` 테스트 없음                           | **김민구** / **조유지** |
| 2   | LocalDateTime.now() 직접 사용 금지 (Clock 추상화) | **X** | `NurseService`, `ReceptionService`, `AdminDashboardStatsService` 등에서 `LocalDate.now()` / `LocalDateTime.now()` 직접 사용 | **조유지** / **강상민** |
| 3   | Given-When-Then 주석 필수                    | O     | 기존 테스트 모두 준수                                                                                                         | -                 |
| 4   | BDDMockito given().willReturn() 사용       | O     | 전 테스트 파일 준수                                                                                                          | -                 |
| 5   | @DisplayName 필수                          | O     | 전 테스트 파일 + 클래스 레벨 @DisplayName 준수                                                                                    | -                 |
| 6   | TestSecurityConfig 중복 방지                 | **X** | 10개 이상 WebMvcTest 클래스에 거의 동일한 `TestSecurityConfig` 내부 클래스 반복 선언                                                      | **김민구** (공통)      |


### 1.5 rule_css.md 준수 점검


| #   | 규칙             | 준수    | 위반 내용                                                                                                        | 담당자               |
| --- | -------------- | ----- | ------------------------------------------------------------------------------------------------------------ | ----------------- |
| 1   | BEM 네이밍 규칙     | O     | `common.css`에서 BEM 패턴 사용                                                                                     | -                 |
| 2   | CSS 변수 사용      | O     | `:root`에 111개 CSS 변수 정의 후 사용                                                                                 | -                 |
| 3   | id 셀렉터 사용 금지   | O     | CSS에서 id 셀렉터 미사용 확인                                                                                          | -                 |
| 4   | 재사용 가능한 단위로 분리 | **X** | `admin-style.css` = `style-admin.css` 완전 동일 중복. `doctor-style.css` = `style-doctor.css` 동일. 역할별 CSS 파일 내용 중복 | **강상민** / **조유지** |
| 5   | 인라인 스타일 금지     | O     | Mustache 템플릿에서 인라인 style 미사용                                                                                 | -                 |


### 1.6 rule_javascript.md 준수 점검


| #   | 규칙                              | 준수    | 위반 내용                                                                                                  | 담당자               |
| --- | ------------------------------- | ----- | ------------------------------------------------------------------------------------------------------ | ----------------- |
| 1   | var 사용 금지                       | **X** | `staff-reception-detail.js`, `doctor-treatment-detail.js` 등 다수 파일에서 `var` 사용                           | **조유지** / **강상민** |
| 2   | const 우선 사용                     | O     | 대부분 const/let 사용                                                                                       | -                 |
| 3   | async/await 기본 (then/catch 지양)  | O     | 전체적으로 async/await 사용 준수                                                                                | -                 |
| 4   | fetch 응답 에러 처리 (response.ok 검사) | O     | 주요 fetch 호출에서 response.ok 검사                                                                           | -                 |
| 5   | 전역 변수 금지                        | **X** | `data-admin.js`, `data-staff.js`, `data-nurse.js` 등에서 전역 함수 중복 정의                                      | **전원**            |
| 6   | innerHTML 직접 사용 금지 (XSS)        | **X** | `admin-rule-form.js:6-8`, `staff-reception-detail.js:3`, `doctor-treatment-detail.js:9`에서 innerHTML 사용 | **강상민** / **조유지** |


---

## Part 2. 이슈 상세 및 담당자 배정

### CRITICAL (2건) — 병합 전 반드시 수정


| ID   | 이슈                                                                                                                                                                                                                            | 위반 규칙               | 위치                                                                                                      | 담당자               |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- | ------------------------------------------------------------------------------------------------------- | ----------------- |
| C-01 | **tools.jackson 비표준 import** — `tools.jackson.core.JacksonException`, `tools.jackson.databind.ObjectMapper` 사용. Spring Boot 번들 `com.fasterxml.jackson`과 충돌 시 런타임 `ClassNotFoundException` 발생. 직렬화 실패 시 `"{}"` 조용히 반환하여 디버깅 불가 | rule_spring (1)     | `llm/service/MedicalService.java:22-23`                                                                 | **김민구**           |
| C-02 | **innerHTML XSS 취약점** — 현재는 SVG 아이콘 고정 문자열이나, 이 패턴이 확산될 경우 사용자 입력이 DOM에 직접 삽입되는 XSS 경로가 열릴 위험. `createElementNS`/`DOMParser` 또는 `feather.replace()` API로 교체 필요                                                                | rule_javascript (6) | `static/js/pages/admin-rule-form.js:6-8`, `staff-reception-detail.js:3`, `doctor-treatment-detail.js:9` | **강상민** / **조유지** |


### HIGH (9건) — 병합 전 수정 권장


| ID   | 이슈                                                                                                                                                                                                         | 위반 규칙               | 위치                                                                                        | 담당자               |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- | ----------------------------------------------------------------------------------------- | ----------------- |
| H-01 | **동일 Entity에 다중 Repository** — `Reservation` 4개(reservation/admin/nurse/staff 패키지), `Patient` 3개, `Staff` 2개, `Department` 2개. 쿼리 중복, `countByReservationDate` 동일 메서드 여러 곳 정의. 변경 시 누락 위험                  | rule-repository (5) | `reservation/`, `admin/`, `nurse/`, `staff/` 패키지 전체                                       | **김민구** (아키텍처)    |
| H-02 | **ReservationApiController Resp.ok() 미사용** — `GET /api/reservation/departments`, `/doctors`, `/booked-slots` 세 엔드포인트 모두 객체/리스트 직접 반환. `ResponseEntity<Resp<T>>` + `Resp.ok()` 규칙 미준수                       | rule-controller (2) | `reservation/reservation/ReservationApiController.java:27-45`                             | **강태오**           |
| H-03 | **ReservationController.symptomReservation() Model 사용** — GET SSR 핸들러임에도 `Model model` 파라미터 사용. 규칙상 GET은 `HttpServletRequest request`로 통일해야 함. 팀 컨벤션 일관성 훼손                                                | rule-controller (7) | `reservation/reservation/ReservationController.java:53-57`                                | **강태오**           |
| H-04 | **AdminItemController.useItem() Resp.ok() 미사용** — `POST /admin/item/use`가 `@Controller` 클래스 내 `@ResponseBody` + `ResponseEntity<?>` + `Map.of()` 방식 반환. SSR/REST 혼재 + Resp 규칙 위반                           | rule-controller (2) | `admin/item/AdminItemController.java:92-108`                                              | **강상민**           |
| H-05 | **ReceptionService 중복 체크 비효율** — `createPhoneReservation()`에서 당일 전체 예약 목록 로드(`findTodayExcludingStatus()`) 후 Java 스트림 필터링으로 중복 확인. `existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot()` 한 번으로 충분 | rule_spring (8)     | `staff/reception/ReceptionService.java:78-85`                                             | **조유지**           |
| H-06 | **ReceptionService.getReservations() N+1** — 예약 목록을 로드 후 스트림 내부에서 건별로 `countByPatient_IdAndStatus()` 호출(초재진 판별). 예약 100건 → DB 쿼리 101번 발생. Projection 또는 배치 조회 필요                                           | rule_spring (8)     | `staff/reception/ReceptionService.java:164-168`                                           | **조유지**           |
| H-07 | **NurseService.getPatientDetail() N+1** — 환자 과거 히스토리 루프 내부에서 건별로 `treatmentRecordRepository.findByReservation_Id()` 호출. 히스토리 10건 → 10번 쿼리 발생. `findAllByReservation_IdIn()` 배치 조회 후 인메모리 매핑으로 교체 필요        | rule_spring (8)     | `nurse/NurseService.java:208-224`                                                         | **조유지**           |
| H-08 | **역할별 CSS 중복 파일** — `admin-style.css` = `style-admin.css`, `doctor-style.css` = `style-doctor.css` 내용 100% 동일 중복. 중복 파일 제거 후 단일 파일 참조로 통일 필요                                                               | rule_css (4)        | `static/css/admin-style.css`, `style-admin.css`, `doctor-style.css`, `style-doctor.css` 외 | **강상민** / **조유지** |
| H-09 | **NurseReceptionController SSR/REST 혼재** — `@Controller` 클래스에 `@PostMapping("/item/use") @ResponseBody` 추가. SSR 뷰 반환과 REST JSON 반환이 하나의 클래스에 공존. `NurseItemApiController`로 분리 필요                           | rule-controller (1) | `nurse/NurseReceptionController.java:343-363`                                             | **조유지**           |


### MEDIUM (9건) — 가능한 빨리 수정


| ID   | 이슈                                                                                                                                                                                  | 위반 규칙                                | 위치                                                                                                                             | 담당자               |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------ | ----------------- |
| M-01 | **Resp vs 일반 응답 포맷 불일치** — `ReservationApiController`는 raw List 반환, `AdminReservationApiController`는 `Resp.ok()` 사용. 동일 `@RestController` 계층에서 응답 래퍼 사용 여부가 불일치                     | rule_spring (2), rule-controller (2) | `reservation/reservation/ReservationApiController.java` vs `admin/reservation/AdminReservationApiController.java`              | **강태오**           |
| M-02 | **ReservationCompleteInfo 일반 클래스 — Record 미적용** — `@Getter` + 전체 생성자 방식. `ReservationInfoDto`, `DepartmentDto` 등 동일 모듈의 다른 DTO들은 Record로 선언되어 불일치. Java Record로 교체 시 boilerplate 제거 | rule_spring (5)                      | `reservation/reservation/ReservationCompleteInfo.java`                                                                         | **강태오**           |
| M-03 | **LocalDateTime.now() 직접 호출 — Clock 추상화 미적용** — 시간 의존 로직 테스트 시 결정적 실행 불가. `NurseService:37`, `ReceptionService:130`, `AdminDashboardStatsService` 등 다수 파일                           | rule_test (2)                        | `nurse/NurseService.java:37`, `staff/reception/ReceptionService.java:130`, `admin/dashboard/AdminDashboardStatsService.java` 외 | **조유지** / **강상민** |
| M-04 | **TestSecurityConfig 10중 중복** — 10개 이상 `@WebMvcTest` 클래스에 완전히 동일한 `TestSecurityConfig` 내부 클래스 반복 선언. `test/common/TestSecurityConfig.java`로 추출 필요                                   | rule_test (6)                        | `src/test/` 전체                                                                                                                 | **김민구** (공통)      |
| M-05 | **LlmReservationService/SymptomAnalysisService 서비스 단위 테스트 전무** — 핵심 LLM 예약 추천 및 증상 분석 로직에 대한 테스트 없음. 로직 변경 시 회귀 감지 불가                                                               | rule_test (1)                        | `src/test/java/.../llm/`                                                                                                       | **김민구**           |
| M-06 | **var 키워드 사용** — `staff-reception-detail.js`, `doctor-treatment-detail.js` 등 다수 파일에서 `var` 사용. `const`/`let` 전환 필요                                                                  | rule_javascript (1)                  | `static/js/pages/` 전체                                                                                                          | **조유지** / **강상민** |
| M-07 | **_sample 패키지 운영 빌드 포함** — `SampleReservation` 엔티티 DDL이 운영 환경에서도 실행됨. `@Profile("dev")` 제한 또는 테스트 소스셋 이동 필요                                                                         | rule_spring (1)                      | `_sample/` 패키지 전체                                                                                                              | **김민구**           |
| M-08 | **NurseService.toKoreanDayOfWeek() 미사용 메서드** — `private` 메서드로 선언되어 있으나 클래스 내부에서 한 번도 호출되지 않음. 데드 코드 정리 필요                                                                           | -                                    | `nurse/NurseService.java:86-97`                                                                                                | **조유지**           |
| M-09 | **ReservationRepository.countByCreatedAtBetween() 미사용** — 예약번호 생성 시 `countByReservationDate()` 사용으로 교체되었으나 기존 메서드가 그대로 남아 있음. 혼란 방지를 위해 제거 필요                                       | -                                    | `reservation/reservation/ReservationRepository.java:41`                                                                        | **강태오**           |


### LOW (5건) — 참고


| ID   | 이슈                                                                                                                                      | 위치                                                                             | 담당자     |
| ---- | --------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ------- |
| L-01 | **doctor.availableDays 문자열 저장 — 정규화 없음** — "MON, TUE" 공백 포함 입력 시 조용히 null 필터링. `DoctorAvailableDay` 연관 테이블로 정규화 권장                      | `domain/Doctor.java`                                                           | **김민구** |
| L-02 | **LayoutModelInterceptor 26개 boolean 플래그** — `path.contains()` 방식 혼용으로 부정확한 매칭 가능. enum 기반 레이아웃 타입으로 리팩터링 권장                            | `common/interceptor/LayoutModelInterceptor.java:71-103`                        | **김민구** |
| L-03 | **StaffRole enum switch 중복** — `SecurityConfig`, `LayoutModelInterceptor` 양쪽에 동일한 역할 switch 반복. enum에 `getDashboardUrl()` 메서드 추가로 제거 가능 | `config/SecurityConfig.java`, `common/interceptor/LayoutModelInterceptor.java` | **김민구** |
| L-04 | **전역 JS 함수 중복 정의** — `data-admin.js`, `data-staff.js`, `data-nurse.js`에서 전역 함수 중복 정의. 대부분 죽은 코드로 추정                                     | `static/js/data-*.js`                                                          | **전원**  |
| L-05 | **AdminReservationRepository nativeQuery 사용** — JPQL 또는 파생 메서드로 대체 가능한 쿼리에 nativeQuery 사용. DB 벤더 의존성 증가                                 | `admin/reservation/AdminReservationRepository.java`                            | **강상민** |


---

## Part 3. 담당자별 수정 사항 요약

### 김민구 (책임개발자/Lead) — 8건


| 우선순위     | ID   | 수정 사항                                                                               |
| -------- | ---- | ----------------------------------------------------------------------------------- |
| CRITICAL | C-01 | `MedicalService.java` `tools.jackson` → `com.fasterxml.jackson` 경로 수정. 직렬화 실패 예외 전파 |
| HIGH     | H-01 | 동일 Entity 다중 Repository 통합 — 단일 공용 Repository + 패키지별 Service 쿼리 호출                  |
| MEDIUM   | M-04 | `TestSecurityConfig` 공통 추출 — `test/common/` 패키지에 공유 설정 클래스 작성                       |
| MEDIUM   | M-05 | `LlmReservationService`, `SymptomAnalysisService` 단위 테스트 작성                         |
| MEDIUM   | M-07 | `_sample/` 패키지 `@Profile("dev")` 또는 테스트 소스셋 이동                                      |
| LOW      | L-01 | `availableDays` 정규화 개선 검토                                                           |
| LOW      | L-02 | `LayoutModelInterceptor` boolean 플래그 정리                                             |
| LOW      | L-03 | `StaffRole` enum에 `getDashboardUrl()` 추가로 switch 중복 제거                              |


### 강태오 (개발자 A) — 5건


| 우선순위   | ID   | 수정 사항                                                                                          |
| ------ | ---- | ---------------------------------------------------------------------------------------------- |
| HIGH   | H-02 | `ReservationApiController` 세 엔드포인트를 `ResponseEntity<Resp<T>>` + `Resp.ok()` 방식으로 수정            |
| HIGH   | H-03 | `ReservationController.symptomReservation()` — `Model model` → `HttpServletRequest request` 교체 |
| MEDIUM | M-01 | `ReservationApiController` 응답 포맷을 `AdminReservationApiController`와 동일한 `Resp.ok()` 방식으로 통일     |
| MEDIUM | M-02 | `ReservationCompleteInfo` 일반 클래스 → Java Record 변환                                              |
| MEDIUM | M-09 | `ReservationRepository.countByCreatedAtBetween()` 미사용 메서드 제거                                   |


### 조유지 (개발자 B) — 9건


| 우선순위     | ID   | 수정 사항                                                                                                                        |
| -------- | ---- | ---------------------------------------------------------------------------------------------------------------------------- |
| CRITICAL | C-02 | `staff-reception-detail.js` `btn.innerHTML` → `textContent` + Feather API 교체                                                 |
| CRITICAL | C-02 | `doctor-treatment-detail.js` `btn.innerHTML` → `textContent` 교체                                                              |
| HIGH     | H-05 | `ReceptionService.createPhoneReservation()` 중복 체크 → `existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot()` 단일 쿼리로 교체 |
| HIGH     | H-06 | `ReceptionService.getReservations()` 초재진 판별 N+1 — 배치 조회 또는 JPQL Projection으로 개선                                              |
| HIGH     | H-07 | `NurseService.getPatientDetail()` 히스토리 루프 N+1 — `findAllByReservation_IdIn()` 배치 조회 후 인메모리 매핑                                |
| HIGH     | H-09 | `NurseReceptionController.useItem()` → 별도 `NurseItemApiController`(@RestController)로 분리                                      |
| MEDIUM   | M-03 | `NurseService`, `ReceptionService` 등 `LocalDate.now()` → `Clock` 주입으로 교체                                                     |
| MEDIUM   | M-06 | 담당 JS 파일 `var` → `const`/`let` 전환                                                                                            |
| MEDIUM   | M-08 | `NurseService.toKoreanDayOfWeek()` 미사용 private 메서드 제거                                                                        |


### 강상민 (개발자 C) — 5건


| 우선순위     | ID   | 수정 사항                                                                                                |
| -------- | ---- | ---------------------------------------------------------------------------------------------------- |
| CRITICAL | C-02 | `admin-rule-form.js` `container.innerHTML` → `DOMParser` 또는 `feather.replace()` API 교체               |
| HIGH     | H-04 | `AdminItemController.useItem()` → 별도 `AdminItemApiController`(@RestController) + `Resp.ok()` 방식으로 분리 |
| HIGH     | H-08 | `admin-style.css`, `style-admin.css` 등 중복 CSS 파일 통합 삭제                                               |
| MEDIUM   | M-03 | `AdminDashboardStatsService` `LocalDate.now()` → `Clock` 주입으로 교체                                     |
| MEDIUM   | M-06 | 담당 JS 파일 `var` → `const`/`let` 전환                                                                    |
| LOW      | L-05 | `AdminReservationRepository` nativeQuery → JPQL 전환                                                   |


### 전원 공동 — 1건


| 우선순위 | ID   | 수정 사항                                                                      |
| ---- | ---- | -------------------------------------------------------------------------- |
| LOW  | L-04 | `data-admin.js`, `data-staff.js`, `data-nurse.js` 등 죽은 코드 각자 담당 영역 정리 후 삭제 |


---

## Part 4. 아키텍처 강점 (유지 사항)

- **예약 상태 전이 모델 개선** — `cancel()`/`cancelFully()` 분리로 접수 롤백과 완전 취소 의미 명확화. `checkPaid()` Guard 메서드로 수납 완료 후 변경 차단
- **소유권 검증 중앙화** — `SecurityUtils.resolveStaffId()` 공통 유틸. `ChatController`, `MedicalController` 양쪽 일관 적용. IDOR 방어 완료
- **SSR/REST 예외 이중 핸들러** — `SsrExceptionHandler`(뷰 반환 + 올바른 HTTP 상태) + `GlobalExceptionHandler`(Resp JSON) 분리. HTTP 상태 코드 정확 반영
- **PRG 패턴 + Flash Attribute** — 예약 생성/취소/변경 완료 화면에서 `addFlashAttribute("info", dto)`로 DTO 통째 전달. URL에 개인정보 노출 없음
- **비관적 락 + DataIntegrityViolation 이중 방어** — `findByIdForUpdate()` + flush 후 `DataIntegrityViolationException` catch. 동시 예약 변경 Race Condition 방어
- **전화번호 정규화 일관 적용** — `cancelReservation()`, `updateReservation()`, `findByPhoneAndName()` 모두 `replaceAll("[^0-9]", "")` 정규화 적용
- **3계층 구조** (Controller → Service → Repository) 전 영역 일관 적용
- **@Transactional(readOnly=true)** — 클래스 레벨 기본 설정, 쓰기 메서드만 별도 선언. 전체 Service 준수
- **테스트 패턴** — Given-When-Then + BDDMockito + @DisplayName. 기존 테스트 파일 전체 일관 적용
- **common.css CSS 변수 시스템** — `:root` 기반 111개 변수로 디자인 토큰 체계 완성

---

## Part 5. 수정 우선순위 일정표

### P0 — 즉시 수정 (CRITICAL 2건)


| 담당자 | 작업                                                                   | 예상 노력      |
| --- | -------------------------------------------------------------------- | ---------- |
| 김민구 | C-01 tools.jackson import 수정                                         | 낮음 (30분)   |
| 강상민 | C-02 admin-rule-form.js innerHTML → DOM API                          | 낮음 (30분)   |
| 조유지 | C-02 staff-reception-detail.js, doctor-treatment-detail.js innerHTML | 낮음 (각 30분) |


### P1 — 단기 (HIGH 9건)


| 담당자     | 작업                                                 | 예상 노력           |
| ------- | -------------------------------------------------- | --------------- |
| 김민구     | H-01 다중 Repository 통합                              | 높음 (전체 아키텍처 영향) |
| 강태오     | H-02 ReservationApiController Resp.ok() 적용         | 낮음 (1시간)        |
| 강태오     | H-03 symptomReservation Model → HttpServletRequest | 낮음 (30분)        |
| 강상민     | H-04 AdminItemController.useItem() 분리              | 낮음 (1시간)        |
| 조유지     | H-05 ReceptionService 중복 체크 쿼리 교체                  | 낮음 (30분)        |
| 조유지     | H-06 ReceptionService N+1 개선                       | 중간 (2시간)        |
| 조유지     | H-07 NurseService 히스토리 N+1 개선                      | 중간 (2시간)        |
| 강상민/조유지 | H-08 중복 CSS 파일 통합 삭제                               | 낮음 (1시간)        |
| 조유지     | H-09 NurseReceptionController SSR/REST 분리          | 낮음 (1시간)        |


### P2 — 중기 (MEDIUM 9건)


| 담당자 | 작업                                                                     |
| --- | ---------------------------------------------------------------------- |
| 강태오 | M-01 응답 포맷 통일, M-02 ReservationCompleteInfo Record 전환, M-09 미사용 메서드 제거 |
| 조유지 | M-03 Clock 주입, M-06 var 제거, M-08 미사용 메서드 제거                            |
| 강상민 | M-03 Clock 주입, M-06 var 제거                                             |
| 김민구 | M-04 TestSecurityConfig 공통화, M-05 LLM 서비스 테스트, M-07 _sample 정리         |


