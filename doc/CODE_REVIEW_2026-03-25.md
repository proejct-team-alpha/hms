# 코드 리뷰 보고서

**리뷰 대상:** `feature/dev-a` 브랜치 전체 코드베이스
**리뷰 일자:** 2026-03-25 (2차 업데이트 — 커밋 `de37d49` 반영)
**리뷰 기준:** `doc/rules/` 규칙 문서 6종 + 아키텍처/보안/성능 종합
**총 이슈:** 29건 (CRITICAL 3건 / HIGH 12건 / MEDIUM 9건 / LOW 5건)

---

## 팀 구성 및 코드 소유권


| 역할           | 이름  | 담당 영역                                                                                                  |
| ------------ | --- | ------------------------------------------------------------------------------------------------------ |
| 책임개발자 (Lead) | 김민구 | `config/`**, `common/**`, `domain/**`, `llm/**`, `application*.properties`, `templates/common/**`      |
| 개발자 A        | 강태오 | `reservation/**`, `home/**`, `templates/reservation/**`, `templates/home/**`                           |
| 개발자 B        | 조유지 | `staff/**`, `doctor/**`, `nurse/**`, `templates/staff/**`, `templates/doctor/**`, `templates/nurse/**` |
| 개발자 C        | 강상민 | `admin/**`, `item/**`, `templates/admin/**`                                                            |


---

## 2026-03-23 리뷰 이후 수정 완료 항목


| ID   | 이슈                                         | 수정 내용                                                                                          |
| ---- | ------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| H-02 | ReservationApiController Resp.ok() 미사용      | 세 엔드포인트 모두 `ResponseEntity<Resp<T>>` + `Resp.ok()` 방식으로 수정 완료                                  |
| H-03 | ReservationController.symptomReservation() Model 사용 | `HttpServletRequest request`로 교체 완료                                                     |
| M-01 | ReservationApiController 응답 포맷 불일치         | `Resp.ok()` 방식으로 통일 완료                                                                        |
| M-02 | ReservationCompleteInfo 일반 클래스             | Java Record로 변환 완료                                                                            |
| M-09 | ReservationRepository.countByCreatedAtBetween() 미사용 | 메서드 제거 완료                                                                              |
| M-03 | AdminDashboardStatsService LocalDate.now() | `getDashboardStats(LocalDate)` / `getDashboardChart(LocalDate)` 오버로딩 추가로 테스트 가능성 개선 (부분 수정) |


---

## 심각도 요약


| 심각도      | 건수  | 이월  | 신규  | 상태    | 조치 기준       |
| -------- | --- | --- | --- | ----- | ----------- |
| CRITICAL | 3   | 2   | 1   | 수정 필수 | 병합 전 반드시 수정 |
| HIGH     | 12  | 9   | 3   | 수정 필요 | 병합 전 수정 권장  |
| MEDIUM   | 9   | 7   | 2   | 수정 권장 | 가능한 빨리 수정   |
| LOW      | 5   | 5   | 0   | 참고    | 일정 여유 시 수정  |


---

## Part 1. 규칙 준수 점검 (doc/rules/ 6종)

### 1.1 rule-controller.md 준수 점검


| #   | 규칙                                                     | 준수    | 위반 내용                                                                                                                         | 담당자     |
| --- | ------------------------------------------------------ | ----- | ------------------------------------------------------------------------------------------------------------------------------- | ------- |
| 1   | SSR Controller는 String(뷰 경로) 반환                        | **X** | `DoctorTreatmentController`, `ReceptionController`, `WalkinController` — `@Controller` 클래스에 `@ResponseBody` REST 반환 공존         | **조유지** |
| 2   | API Controller는 ResponseEntity + Resp.ok() 사용          | **X** | `AdminItemController.useItem()` — `Map.of()` 방식. `ReceptionController` AJAX 엔드포인트 — `Map<String,Object>` 직접 반환. Resp 미사용      | **조유지** / **강상민** |
| 3   | Controller는 Service만 호출 (Repository 직접 호출 금지)          | O     | 전 영역 준수 확인                                                                                                                    | -       |
| 4   | DTO 네이밍: Request/Response 규칙                           | O     | 전 영역 준수                                                                                                                       | -       |
| 5   | 비즈니스 로직이 Controller에 없을 것                              | **X** | `ReceptionController.list()` — 페이징 처리, 탭 필터링, 파라미터 빌딩 등 200줄 이상의 비즈니스/뷰 로직이 Controller에 직접 구현됨                              | **조유지** |
| 6   | 예외 응답은 GlobalExceptionHandler / SsrExceptionHandler 사용 | O     | 전 영역 준수                                                                                                                       | -       |
| 7   | GET SSR 핸들러는 HttpServletRequest 사용                     | **X** | `DoctorTreatmentController`, `ReceptionController`, `WalkinController`, `PhoneReservationController` — GET 핸들러에 `Model model` 사용 | **조유지** |


### 1.2 rule-repository.md 준수 점검


| #   | 규칙                                    | 준수    | 위반 내용                                                                                                           | 담당자            |
| --- | ------------------------------------- | ----- | --------------------------------------------------------------------------------------------------------------- | -------------- |
| 1   | Repository는 소유 기능 모듈 패키지에 배치          | O     | 각 모듈별 Repository가 해당 패키지에 위치                                                                                    | -              |
| 2   | JpaRepository 인터페이스 기반 작성             | O     | 모든 Repository가 JpaRepository 상속                                                                                 | -              |
| 3   | 쿼리 우선순위: 파생 메서드 > Projection > @Query | **X** | `AdminReservationRepository`, `AdminStaffRepository`에서 nativeQuery 다수 사용 — 이월 미수정                               | **강상민**        |
| 4   | 다중 도메인 집계는 Service 조합 처리              | O     | 준수                                                                                                              | -              |
| 5   | 동일 엔티티 중복 Repository 방지               | **X** | `Reservation` 4개, `Patient` 3개, `Staff` 2개, `Department` 2개 Repository 중복 — 이월 미수정                             | **김민구** (아키텍처) |
| 6   | EntityManager 직접 사용 금지                | O     | 준수                                                                                                              | -              |


### 1.3 rule_spring.md 준수 점검


| #   | 규칙                                           | 준수    | 위반 내용                                                                                                      | 담당자     |
| --- | -------------------------------------------- | ----- | ---------------------------------------------------------------------------------------------------------- | ------- |
| 1   | 환경 변수로 민감 정보 관리                              | O     | API 키 환경변수 사용                                                                                             | -       |
| 2   | 전역 에러 핸들러 ErrorResponse 포맷 통일                | **X** | `ReceptionController` AJAX — `Map<String,Object>` 직접 반환. `AdminItemController.useItem()` — `Map.of()` 반환 | **조유지** / **강상민** |
| 3   | 비즈니스 예외는 CustomException 사용                  | O     | 전 영역 준수                                                                                                   | -       |
| 4   | Service 클래스 레벨 @Transactional(readOnly=true) | O     | 모든 Service 일관 적용                                                                                          | -       |
| 5   | DTO는 Java Record 우선                          | O     | `ReservationCompleteInfo` Record 변환 완료                                                                    | -       |
| 6   | @Valid로 요청 검증                                | O     | 주요 POST 엔드포인트 `@Valid` 적용                                                                                | -       |
| 7   | 페이징은 Pageable 기본 사용                          | **X** | `ReceptionController.list()` — Pageable 미사용. 직접 `subList()` + 수동 페이지 계산으로 서비스 레이어 없이 Controller에서 처리 | **조유지** |


### 1.4 rule_test.md 준수 점검


| #   | 규칙                                       | 준수    | 위반 내용                                                                                             | 담당자               |
| --- | ---------------------------------------- | ----- | ----------------------------------------------------------------------------------------------------- | ----------------- |
| 1   | 핵심 로직에 대한 테스트 존재                         | **X** | `LlmReservationService`, `SymptomAnalysisService`, `DoctorTreatmentService` 단위 테스트 전무 — 이월 미수정      | **김민구** / **조유지** |
| 2   | LocalDateTime.now() 직접 사용 금지 (Clock 추상화) | **X** | `NurseService:37`, `ReceptionService:178`, `DoctorTreatmentService:34,106` 등 다수 — 이월 미수정          | **조유지** / **강상민** |
| 3   | Given-When-Then 주석 필수                    | O     | 모든 테스트 준수                                                                                          | -                 |
| 4   | BDDMockito given().willReturn() 사용       | O     | 전 테스트 파일 준수                                                                                        | -                 |
| 5   | @DisplayName 필수                          | O     | 전 테스트 파일 준수                                                                                        | -                 |
| 6   | TestSecurityConfig 중복 방지                 | **X** | 10개 이상 WebMvcTest 클래스에 동일한 `TestSecurityConfig` 내부 클래스 반복 선언 — 이월 미수정                            | **김민구** (공통)      |


### 1.5 rule_css.md 준수 점검


| #   | 규칙             | 준수    | 위반 내용                                                                                                        | 담당자               |
| --- | -------------- | ----- | ------------------------------------------------------------------------------------------------------------ | ----------------- |
| 1   | BEM 네이밍 규칙     | O     | `common.css` BEM 패턴 사용                                                                                       | -                 |
| 2   | CSS 변수 사용      | O     | `:root` 기반 CSS 변수 사용                                                                                        | -                 |
| 3   | id 셀렉터 사용 금지   | O     | 미사용 확인                                                                                                      | -                 |
| 4   | 재사용 가능한 단위로 분리 | **X** | `admin-style.css` = `style-admin.css`, `doctor-style.css` = `style-doctor.css` 100% 동일 — 이월 미수정              | **강상민** / **조유지** |
| 5   | 인라인 스타일 금지     | O     | Mustache 템플릿 인라인 style 미사용                                                                                 | -                 |


### 1.6 rule_javascript.md 준수 점검


| #   | 규칙                              | 준수    | 위반 내용                                                                                                                | 담당자               |
| --- | ------------------------------- | ----- | ---------------------------------------------------------------------------------------------------------------------- | ----------------- |
| 1   | var 사용 금지                       | **X** | `admin-rule-form.js`, `admin-staff-form.js`, `doctor-treatment-detail.js`, `staff-reception-detail.js` 등 — 이월 미수정      | **조유지** / **강상민** |
| 2   | const 우선 사용                     | O     | 대부분 신규 파일 const/let 사용                                                                                              | -                 |
| 3   | async/await 기본                  | O     | 준수                                                                                                                    | -                 |
| 4   | fetch 응답 에러 처리                  | O     | 주요 fetch 호출 response.ok 검사                                                                                          | -                 |
| 5   | 전역 변수 금지                        | **X** | `data-admin.js`, `data-staff.js` 등 전역 함수 중복 — 이월 미수정                                                               | **전원**            |
| 6   | innerHTML 직접 사용 금지 (XSS)        | **X** | `admin-rule-form.js:6-8`, `staff-reception-detail.js:3`, `doctor-treatment-detail.js:9`, `admin-dashboard.js:225,229` | **강상민** / **조유지** |


---

## Part 2. 이슈 상세 및 담당자 배정

### CRITICAL (3건) — 병합 전 반드시 수정


| ID   | 이슈                                                                                                                                                                                                                    | 위반 규칙               | 위치                                                                                                    | 담당자               | 구분       |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- | ------------------------------------------------------------------------------------------------------- | ----------------- | -------- |
| C-01 | **tools.jackson 비표준 import** — `tools.jackson.core.JacksonException`, `tools.jackson.databind.ObjectMapper` 사용. Spring Boot 번들 `com.fasterxml.jackson`과 충돌 시 런타임 `ClassNotFoundException` 발생 위험. **3주 연속 미수정**       | rule_spring (1)     | `llm/service/MedicalService.java:22-23`                                                                 | **김민구**           | 이월 (3주) |
| C-02 | **innerHTML XSS 취약점** — SVG 아이콘/스피너 고정 문자열이나 패턴 확산 시 XSS 경로 노출. `lucide.createIcons()` API 또는 `createElement` + `textContent`로 교체 필요. **3주 연속 미수정**                                                              | rule_javascript (6) | `admin-rule-form.js:6-8`, `staff-reception-detail.js:3`, `doctor-treatment-detail.js:9`, `admin-dashboard.js:225,229` | **강상민** / **조유지** | 이월 (3주) |
| C-03 | **AdminStaffService 문자열 상수 인코딩 깨짐** — `STAFF_CREATED_MESSAGE`, `STAFF_UPDATED_MESSAGE` 등 14개 상수(lines 55-68)가 깨진 바이트열로 저장됨. 런타임에 사용자에게 의미 없는 문자가 그대로 노출됨. `INACTIVE_STAFF_UPDATE_NOT_ALLOWED_MESSAGE`(line 69)부터는 정상 Unicode 이스케이프 사용. 파일 UTF-8 재저장 + 상수 올바른 문자열로 수정 필요 | -  | `admin/staff/AdminStaffService.java:55-68`                                                              | **강상민**           | 신규       |


### HIGH (12건) — 병합 전 수정 권장


| ID     | 이슈                                                                                                                                                                                                           | 위반 규칙                | 위치                                                                           | 담당자               | 구분  |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------- | ------------------------------------------------------------------------------ | ----------------- | --- |
| H-01   | **동일 Entity에 다중 Repository** — `Reservation` 4개, `Patient` 3개, `Staff` 2개, `Department` 2개. 쿼리 중복, 변경 시 누락 위험. **이월 미수정**                                                                                   | rule-repository (5)  | `reservation/`, `admin/`, `nurse/`, `staff/` 패키지 전체                           | **김민구**           | 이월  |
| H-04   | **AdminItemController SSR/REST 혼재** — `@Controller` 클래스에 `@PostMapping("/use") @ResponseBody`. `Resp.ok()` 미사용. **이월 미수정**                                                                                   | rule-controller (1,2) | `admin/item/AdminItemController.java:108-131`                                  | **강상민**           | 이월  |
| H-05   | **ReceptionService 중복 체크 비효율** — `createPhoneReservation()`에서 당일 전체 예약 로드 후 Java 스트림 필터링. 단일 쿼리로 충분. **이월 미수정**                                                                                             | rule_spring (8)      | `staff/reception/ReceptionService.java:92-98`                                  | **조유지**           | 이월  |
| H-06   | **ReceptionService.getReservations() N+1** — 스트림 내부에서 건별로 `countByPatient_IdAndStatus()` 호출(초재진 판별). 예약 100건 → DB 쿼리 101번 발생. **이월 미수정**                                                                    | rule_spring (8)      | `staff/reception/ReceptionService.java:218`                                    | **조유지**           | 이월  |
| H-07   | **NurseService.getPatientDetail() N+1** — 히스토리 루프 내부에서 건별로 `treatmentRecordRepository.findByReservation_Id()` 호출. **이월 미수정**                                                                                | rule_spring (8)      | `nurse/NurseService.java:211`                                                  | **조유지**           | 이월  |
| H-08   | **역할별 CSS 중복 파일** — `admin-style.css` = `style-admin.css`, `doctor-style.css` = `style-doctor.css` 100% 동일. **이월 미수정**                                                                                       | rule_css (4)         | `static/css/admin-style.css`, `style-admin.css`, `doctor-style.css` 외          | **강상민** / **조유지** | 이월  |
| H-09   | **NurseReceptionController SSR/REST 혼재** — `@Controller` 클래스에 `/item/use`, `/item/cancel` `@ResponseBody` 엔드포인트. **이월 미수정**                                                                                 | rule-controller (1)  | `nurse/NurseReceptionController.java:382-415`                                  | **조유지**           | 이월  |
| N-H-01 | **DoctorTreatmentController SSR/REST 혼재** — `@Controller` 클래스에 `poll`, 아이템 사용 `@ResponseBody` 엔드포인트. **이월 미수정**                                                                                             | rule-controller (1)  | `doctor/treatment/DoctorTreatmentController.java:36-42`                        | **조유지**           | 이월  |
| N-H-02 | **DoctorTreatmentController GET 핸들러 Model 사용** — `treatmentList()` 등 GET SSR 핸들러 `Model model` 사용. **이월 미수정**                                                                                                | rule-controller (7)  | `doctor/treatment/DoctorTreatmentController.java:44-50`                        | **조유지**           | 이월  |
| N-H-03 | **ReceptionController SSR/REST 혼재 + Resp 미사용** — `@Controller` 클래스에 `receiveAjax`, `cancelAjax`, `payAjax`, `updatePatientInfo` 등 4개 `@ResponseBody` 엔드포인트 추가. `Map<String,Object>` 직접 반환으로 `Resp.ok()` 미준수 | rule-controller (1,2) | `staff/reception/ReceptionController.java:273-337`                             | **조유지**           | 신규  |
| N-H-04 | **GET SSR 핸들러 Model 사용 확산** — `ReceptionController.list()`(line 41), `ReceptionController.detail()`(line 224), `WalkinController.walkinPage()`(line 33), `PhoneReservationController.phoneReservation()`(line 31) 모두 `Model model` 파라미터 사용 | rule-controller (7) | `ReceptionController.java`, `WalkinController.java`, `PhoneReservationController.java` | **조유지** | 신규  |
| N-H-05 | **ReceptionController 비즈니스 로직 Controller 침투** — `list()` 메서드에 탭별 필터링, 수동 페이징(subList), 파라미터 빌더, 상태 텍스트 변환 등 200줄 이상의 로직 직접 구현. Service로 위임 및 `Pageable` 사용 필요 | rule-controller (5), rule_spring (7) | `staff/reception/ReceptionController.java:41-221`                     | **조유지**           | 신규  |


### MEDIUM (9건) — 가능한 빨리 수정


| ID     | 이슈                                                                                                                                                              | 위반 규칙               | 위치                                                                                                            | 담당자               | 구분  |
| ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- | --------------------------------------------------------------------------------------------------------------- | ----------------- | --- |
| M-03   | **LocalDateTime.now() 직접 호출** — `NurseService:37`, `ReceptionService:178,303`, `DoctorTreatmentService:34,106` 등. `AdminDashboardStatsService`는 오버로딩으로 부분 개선됨 | rule_test (2)       | `nurse/NurseService.java`, `staff/reception/ReceptionService.java`, `doctor/treatment/DoctorTreatmentService.java` | **조유지** / **강상민** | 이월  |
| M-04   | **TestSecurityConfig 10중 중복** — 10개 이상 `@WebMvcTest` 클래스에 동일한 내부 클래스 반복. `test/common/`으로 추출 필요. **이월 미수정**                                                      | rule_test (6)       | `src/test/` 전체                                                                                                  | **김민구**           | 이월  |
| M-05   | **LlmReservationService/SymptomAnalysisService 단위 테스트 전무** — 핵심 LLM 로직 테스트 없음. **이월 미수정**                                                                      | rule_test (1)       | `src/test/java/.../llm/`                                                                                         | **김민구**           | 이월  |
| M-06   | **var 키워드 사용** — `admin-rule-form.js`, `doctor-treatment-detail.js`, `staff-reception-detail.js` 등 다수. **이월 미수정**                                               | rule_javascript (1) | `static/js/pages/` 전체                                                                                           | **조유지** / **강상민** | 이월  |
| M-07   | **_sample 패키지 운영 빌드 포함** — `SampleReservation` 엔티티 DDL 운영 환경 실행. **이월 미수정**                                                                                   | rule_spring (1)     | `_sample/` 패키지                                                                                                  | **김민구**           | 이월  |
| M-08   | **NurseService.toKoreanDayOfWeek() 미사용 메서드** — private 선언이나 미호출. **이월 미수정**                                                                                   | -                   | `nurse/NurseService.java:86-97`                                                                                  | **조유지**           | 이월  |
| N-M-01 | **admin-dashboard.js innerHTML 사용** — `container.innerHTML = renderItemFlowEmptyState()` 및 템플릿 리터럴 할당. 서버 데이터 포함 시 XSS 위험. **이월 미수정**                          | rule_javascript (6) | `static/js/pages/admin-dashboard.js:225,229`                                                                     | **강상민**           | 이월  |
| N-M-02 | **DoctorTreatmentService LocalDate.now() 직접 사용** — 6곳에서 `LocalDate.now()` 직접 호출. **이월 미수정**                                                                    | rule_test (2)       | `doctor/treatment/DoctorTreatmentService.java:34,106,118,167,183,228`                                            | **조유지**           | 이월  |
| N-M-03 | **InactiveStaffLogoutInterceptor 매 요청 DB 조회** — 모든 인증된 요청마다 `staffRepository.findByUsername()` 호출. 고트래픽 시 DB 부하 증가. Spring Security의 세션 어트리뷰트 캐싱 또는 `@EventListener(SessionDestroyedEvent)` 활용 권장 | - | `common/interceptor/InactiveStaffLogoutInterceptor.java:31`                                                      | **김민구**           | 신규  |
| N-M-04 | **AdminStaffController 메시지 contains 기반 오류 라우팅** — `applyCreateViewErrors()`, `applyUpdateViewErrors()`에서 `ex.getMessage().contains("부서")` 등 문자열 포함 여부로 오류 유형 판별. 메시지 변경 시 라우팅 깨질 수 있음. 전용 에러 코드 기반으로 분기 필요 | - | `admin/staff/AdminStaffController.java:138-183`                                                                  | **강상민**           | 신규  |


### LOW (5건) — 참고


| ID   | 이슈                                                                                                                                 | 위치                                                                             | 담당자     |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ------- |
| L-01 | **doctor.availableDays 문자열 저장 — 정규화 없음** — 공백 포함 입력 시 조용히 null 필터링. 연관 테이블 정규화 권장                                                  | `domain/Doctor.java`                                                           | **김민구** |
| L-02 | **LayoutModelInterceptor 26개 boolean 플래그** — `path.contains()` 방식 혼용으로 부정확한 매칭 가능. enum 기반 리팩터링 권장                                  | `common/interceptor/LayoutModelInterceptor.java:71-103`                        | **김민구** |
| L-03 | **StaffRole enum switch 중복** — `SecurityConfig`, `LayoutModelInterceptor` 양쪽에 동일 switch 반복. enum에 `getDashboardUrl()` 추가로 제거 가능     | `config/SecurityConfig.java`, `common/interceptor/LayoutModelInterceptor.java` | **김민구** |
| L-04 | **전역 JS 함수 중복 정의** — `data-admin.js`, `data-staff.js`, `data-nurse.js` 전역 함수 중복. 대부분 죽은 코드                                          | `static/js/data-*.js`                                                          | **전원**  |
| L-05 | **AdminReservationRepository nativeQuery 사용** — JPQL 또는 파생 메서드로 대체 가능                                                             | `admin/reservation/AdminReservationRepository.java`                            | **강상민** |


---

## Part 3. 담당자별 수정 사항 요약

### 김민구 (책임개발자/Lead) — 9건


| 우선순위     | ID     | 수정 사항                                                                                     |
| -------- | ------ | ----------------------------------------------------------------------------------------- |
| CRITICAL | C-01   | `MedicalService.java` `tools.jackson` → `com.fasterxml.jackson` 경로 수정                    |
| HIGH     | H-01   | 동일 Entity 다중 Repository 통합 — 단일 공용 Repository + 패키지별 Service 쿼리 호출                      |
| MEDIUM   | M-04   | `TestSecurityConfig` 공통 추출 — `test/common/` 패키지에 공유 설정 클래스 작성                           |
| MEDIUM   | M-05   | `LlmReservationService`, `SymptomAnalysisService` 단위 테스트 작성                               |
| MEDIUM   | M-07   | `_sample/` 패키지 `@Profile("dev")` 또는 테스트 소스셋 이동                                          |
| MEDIUM   | N-M-03 | `InactiveStaffLogoutInterceptor` — 매 요청 DB 조회를 세션 어트리뷰트 캐싱으로 개선                         |
| LOW      | L-01   | `availableDays` 정규화 개선 검토                                                                |
| LOW      | L-02   | `LayoutModelInterceptor` boolean 플래그 정리                                                  |
| LOW      | L-03   | `StaffRole` enum에 `getDashboardUrl()` 추가로 switch 중복 제거                                   |


### 강태오 (개발자 A) — 0건

> 배정 이슈 전부 수정 완료. 이번 리뷰 신규 배정 이슈 없음. 유지 상태 양호.


### 조유지 (개발자 B) — 14건


| 우선순위     | ID     | 수정 사항                                                                                                           |
| -------- | ------ | ----------------------------------------------------------------------------------------------------------------- |
| CRITICAL | C-02   | `staff-reception-detail.js`, `doctor-treatment-detail.js` `btn.innerHTML` → `textContent` 교체                     |
| HIGH     | H-05   | `ReceptionService.createPhoneReservation()` 중복 체크 → `existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot()` 단일 쿼리 |
| HIGH     | H-06   | `ReceptionService.getReservations()` N+1 — 배치 조회 또는 Projection으로 개선                                             |
| HIGH     | H-07   | `NurseService.getPatientDetail()` N+1 — `findAllByReservation_IdIn()` 배치 조회                                      |
| HIGH     | H-09   | `NurseReceptionController` → 별도 `NurseItemApiController`(@RestController)로 분리                                    |
| HIGH     | N-H-01 | `DoctorTreatmentController` → `DoctorTreatmentApiController`(@RestController) 분리                                  |
| HIGH     | N-H-02 | `DoctorTreatmentController` GET 핸들러 `Model` → `HttpServletRequest` 교체                                           |
| HIGH     | N-H-03 | `ReceptionController` AJAX 엔드포인트 → 별도 `ReceptionApiController`(@RestController) + `Resp.ok()` 분리               |
| HIGH     | N-H-04 | `ReceptionController`, `WalkinController`, `PhoneReservationController` GET 핸들러 `Model` → `HttpServletRequest` 교체 |
| HIGH     | N-H-05 | `ReceptionController.list()` 비즈니스 로직 → Service로 위임 + `Pageable` 사용으로 수동 페이징 제거                                |
| MEDIUM   | M-03   | `NurseService`, `ReceptionService`, `DoctorTreatmentService` `LocalDate.now()` → `Clock` 주입                      |
| MEDIUM   | M-06   | 담당 JS 파일 `var` → `const`/`let` 전환                                                                               |
| MEDIUM   | M-08   | `NurseService.toKoreanDayOfWeek()` 미사용 메서드 제거                                                                   |
| MEDIUM   | N-M-02 | `DoctorTreatmentService` `LocalDate.now()` → `Clock` 주입                                                          |


### 강상민 (개발자 C) — 9건


| 우선순위     | ID     | 수정 사항                                                                                                 |
| -------- | ------ | ------------------------------------------------------------------------------------------------------- |
| CRITICAL | C-02   | `admin-rule-form.js` `innerHTML` → `lucide.createIcons()` API 교체                                       |
| CRITICAL | C-03   | `AdminStaffService.java:55-68` 깨진 문자열 상수 — 파일 UTF-8 재저장 + 올바른 문자열로 수정                              |
| HIGH     | H-04   | `AdminItemController.useItem()` → `AdminItemApiController`(@RestController) + `Resp.ok()` 분리            |
| HIGH     | H-08   | `admin-style.css`, `style-admin.css` 등 중복 CSS 파일 통합 삭제                                               |
| MEDIUM   | M-03   | `AdminItemController.resolveDateRange()`의 `LocalDate.now()` — `AdminDashboardStatsService` 오버로딩 패턴 동일 적용 |
| MEDIUM   | M-06   | 담당 JS 파일 `var` → `const`/`let` 전환                                                                     |
| MEDIUM   | N-M-01 | `admin-dashboard.js` `innerHTML` → `createElement` + `textContent` 교체                                  |
| MEDIUM   | N-M-04 | `AdminStaffController` 메시지 contains 판별 → 에러 코드 기반 분기로 교체                                             |
| LOW      | L-05   | `AdminReservationRepository` nativeQuery → JPQL 전환                                                     |


### 전원 공동 — 1건


| 우선순위 | ID   | 수정 사항                                                              |
| ---- | ---- | ------------------------------------------------------------------ |
| LOW  | L-04 | `data-admin.js`, `data-staff.js` 등 죽은 코드 각자 담당 영역 정리 후 삭제         |


---

## Part 4. 아키텍처 강점 (유지 사항)

- **예약 상태 전이 모델** — `cancel()`/`cancelFully()` 분리, `checkPaid()` Guard 메서드. 의미론적으로 명확
- **소유권 검증 중앙화** — `SecurityUtils.resolveStaffId()`. ChatController, MedicalController IDOR 방어 완료
- **SSR/REST 예외 이중 핸들러** — `SsrExceptionHandler` + `GlobalExceptionHandler` 분리. HTTP 상태 코드 정확 반영
- **PRG 패턴 + Flash Attribute** — 예약/접수 전 흐름 URL 개인정보 노출 없음
- **비관적 락 + DataIntegrityViolation 이중 방어** — 동시 예약 Race Condition 방어
- **비활성 직원 세션 자동 강제 로그아웃** — `InactiveStaffLogoutInterceptor` 신규 도입. 퇴사 처리 즉시 세션 만료
- **퇴사 일정 자동 비활성화** — `StaffRetirementScheduler` @Scheduled(1시간) 도입. 퇴사 일시 도래 시 자동 처리
- **AdminStaff 테스트 보강** — `AdminStaffServiceTest`, `AdminStaffControllerTest`, `StaffRetirementSchedulerTest`, `InactiveStaffLogoutInterceptorTest` 대폭 업데이트
- **@Transactional(readOnly=true)** — 클래스 레벨 기본, 쓰기 메서드만 별도. 전체 Service 준수
- **테스트 패턴** — Given-When-Then + BDDMockito + @DisplayName. 전 테스트 파일 준수
- **AdminDashboardStatsService 테스트 가능성 개선** — `getDashboardStats(LocalDate)` 오버로딩으로 테스트 가능한 구조 마련

---

## Part 5. 수정 우선순위 일정표

### P0 — 즉시 수정 (CRITICAL 3건)


| 담당자 | 작업                                                                   | 예상 노력    |
| --- | -------------------------------------------------------------------- | -------- |
| 김민구 | C-01 `tools.jackson` → `com.fasterxml.jackson` import 수정              | 낮음 (30분) |
| 강상민 | C-02 `admin-rule-form.js` innerHTML → `lucide.createIcons()` API      | 낮음 (30분) |
| 강상민 | C-03 `AdminStaffService.java:55-68` 깨진 문자열 상수 UTF-8 재저장 + 수정         | 낮음 (1시간) |
| 조유지 | C-02 `staff-reception-detail.js`, `doctor-treatment-detail.js` innerHTML 제거 | 낮음 (30분) |


### P1 — 단기 (HIGH 12건)


| 담당자  | 작업                                                                      | 예상 노력           |
| ---- | ----------------------------------------------------------------------- | --------------- |
| 김민구  | H-01 다중 Repository 통합                                                    | 높음 (전체 아키텍처 영향) |
| 강상민  | H-04 `AdminItemController.useItem()` 분리                                  | 낮음 (1시간)        |
| 조유지  | H-05 ReceptionService 중복 체크 쿼리 교체                                       | 낮음 (30분)        |
| 조유지  | H-06 ReceptionService N+1 개선                                             | 중간 (2시간)        |
| 조유지  | H-07 NurseService 히스토리 N+1 개선                                            | 중간 (2시간)        |
| 강상민/조유지 | H-08 중복 CSS 파일 통합 삭제                                                   | 낮음 (1시간)        |
| 조유지  | H-09 NurseReceptionController SSR/REST 분리                                | 낮음 (1시간)        |
| 조유지  | N-H-01 DoctorTreatmentController SSR/REST 분리                             | 낮음 (1시간)        |
| 조유지  | N-H-02 DoctorTreatmentController GET HttpServletRequest 교체                | 낮음 (30분)        |
| 조유지  | N-H-03 ReceptionController AJAX → ReceptionApiController 분리              | 중간 (2시간)        |
| 조유지  | N-H-04 ReceptionController/WalkinController/PhoneReservationController GET Model 교체 | 낮음 (1시간) |
| 조유지  | N-H-05 ReceptionController.list() 비즈니스 로직 → Service 이전 + Pageable 적용   | 높음 (3시간)        |


### P2 — 중기 (MEDIUM 9건)


| 담당자 | 작업                                                                                         |
| --- | ------------------------------------------------------------------------------------------ |
| 강상민 | C-03 후속 정리, N-M-01 innerHTML → DOM API 교체, M-06 var 제거, N-M-04 에러코드 분기                  |
| 조유지 | N-M-02 DoctorTreatmentService Clock 주입, M-03 Clock 주입, M-06 var 제거, M-08 미사용 메서드 제거      |
| 김민구 | M-04 TestSecurityConfig 공통화, M-05 LLM 서비스 테스트, M-07 _sample 정리, N-M-03 인터셉터 캐싱 개선 |
