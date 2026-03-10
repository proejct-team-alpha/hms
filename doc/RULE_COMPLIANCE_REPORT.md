# HMS 프로젝트 규칙 준수 점검 보고서

> **점검일:** 2026-03-10
> **점검 대상:** `dev` 브랜치 (커밋 `d6c6707`)
> **기준 문서:** `doc/RULE.md`, `doc/rules/*`, `.ai/memory.md`, `.ai/workflows/workflow.md`

---

## 1. 점검 요약

| 영역          | 점검 항목 수 | 준수   | 위반   | 해당없음 | 심각도 요약                              |
| ------------- | ------------ | ------ | ------ | -------- | ---------------------------------------- |
| Spring 백엔드 | 10           | 6      | 4      | 0        | HIGH 2, MEDIUM 4, LOW 2                  |
| JavaScript    | 9 (+2 추가)  | 2      | 5      | 4        | CRITICAL 2, HIGH 3, MEDIUM 2             |
| CSS           | 9            | 5      | 4      | 0        | HIGH 1, MEDIUM 4, LOW 2                  |
| 테스트        | 8            | 4      | 4      | 0        | HIGH 2, MEDIUM 3, LOW 1                  |
| 보안          | 7            | 5      | 2      | 0        | CRITICAL 2                               |
| **합계**      | **43**       | **22** | **19** | **4**    | **CRITICAL 4, HIGH 8, MEDIUM 13, LOW 5** |

### 심각도 정의

| 등급     | 의미                             | 조치               |
| -------- | -------------------------------- | ------------------ |
| CRITICAL | 기능 결함 또는 보안 취약점 유발  | 즉시 수정 필수     |
| HIGH     | 규칙 명시적 위반, 향후 장애 가능 | 병합 전 수정 권장  |
| MEDIUM   | 규칙 부분 위반, 일관성 저하      | 다음 스프린트 수정 |
| LOW      | 개선 권장 사항                   | 점진적 개선        |

---

## 2. Spring 백엔드 점검 (rule_spring.md, rule-controller.md, rule-repository.md)

### 2.1 항목별 판정

| #   | 점검 항목                               | 판정     | 비고                                                                                  |
| --- | --------------------------------------- | -------- | ------------------------------------------------------------------------------------- |
| 1   | 환경 변수 관리 (민감정보 하드코딩 금지) | **준수** | `${CLAUDE_API_KEY:}` 환경변수 방식 사용                                               |
| 2   | GlobalExceptionHandler 공통 포맷        | **준수** | ErrorResponse record, Validation/도메인/폴백 예외 모두 처리                           |
| 3   | Controller 분리 (SSR/API)               | **위반** | `ReservationApiController`에서 `Resp.ok()` 미사용                                     |
| 4   | Controller 책임 (비즈니스 로직 금지)    | **준수** | 모든 Controller가 Service 위임 후 반환만 수행                                         |
| 5   | Service @Transactional 패턴             | **준수** | `ReservationService`, `AdminDashboardStatsService` 모두 클래스 레벨 readOnly 선언     |
| 6   | DTO Record 사용 및 네이밍               | **위반** | `ReservationCreateForm`, `ReservationCompleteInfo`, `DoctorDto` — record 미사용 + 네이밍 위반 |
| 7   | Validation (@Valid/@Validated)          | **위반** | 예약 생성 시 `@Valid` 미사용, Bean Validation 어노테이션 전무                         |
| 8   | Repository 규칙                         | **위반** | `PatientRepository`, `DepartmentRepository`가 reservation 패키지에 위치 (소유 모듈 외) |
| 9   | SecurityConfig (Bean 방식, CORS)        | **준수** | SecurityFilterChain Bean, CorsConfigurationSource Bean 사용                           |
| 10  | URI 설계 (RESTful)                      | **준수** | 역할 기반 계층형 URI 적용                                                             |

### 2.2 이슈 목록

| 심각도     | 이슈                                                                                       | 파일                                                    | 수정 방안                                                               |
| ---------- | ------------------------------------------------------------------------------------------ | ------------------------------------------------------- | ----------------------------------------------------------------------- |
| **HIGH**   | 예약 생성 시 입력값 검증 완전 부재 (`@Valid` 없음 + Bean Validation 어노테이션 전무)        | `ReservationController.java:49`, `ReservationCreateForm.java:14` | Form 필드에 `@NotBlank`/`@NotNull` 등 추가, Controller에 `@Valid` 추가  |
| **HIGH**   | `IllegalArgumentException` 사용으로 500 에러 반환 (GlobalExceptionHandler 미처리)           | `ReservationService.java:46-48`                         | `CustomException.notFound("...")` 로 변경                               |
| **MEDIUM** | `ReservationApiController`에서 `Resp.ok()` 미사용                                          | `ReservationApiController.java:21-24`                   | `Resp.ok(data)` 래퍼로 감싸기                                           |
| **MEDIUM** | `ReservationCreateForm` — Record 미사용 + `...Request` 네이밍 위반                         | `ReservationCreateForm.java:14`                         | `public record ReservationCreateRequest(...)` 로 변환                   |
| **MEDIUM** | `ReservationCompleteInfo` — Record 미사용 + `...Response` 네이밍 위반                      | `ReservationCompleteInfo.java:8`                        | `public record ReservationCompleteResponse(...)` 로 변환                |
| **MEDIUM** | `PatientRepository`, `DepartmentRepository`가 reservation 패키지에 위치 (소유 모듈 외)     | `reservation/reservation/PatientRepository.java:12` 등  | 각각 `patient`, `department` 패키지로 이동                              |
| **LOW**    | `DoctorDto` — Record 미사용 + `...Response` 네이밍 위반                                    | `doctor/DoctorDto.java:12`                              | `public record DoctorResponse(...)` 로 변환                             |
| **LOW**    | `POST /reservation/create` URI에 동사 포함                                                 | `ReservationController.java:48`                         | `POST /reservation` 또는 팀 합의로 결정                                 |

---

## 3. JavaScript 점검 (rule_javascript.md)

### 3.1 항목별 판정

| #   | 점검 항목                        | 판정         | 비고                                           |
| --- | -------------------------------- | ------------ | ---------------------------------------------- |
| 1   | `var` 사용 금지                  | **위반**     | pages/ 하위 12개 파일에서 37건 `var` 사용       |
| 2   | `const` 우선, `let` 보조         | **위반**     | pages/ 내 `const` 0건, `let` 0건                |
| 3   | async/await 비동기 처리          | **해당없음** | API 호출 미구현 상태                            |
| 4   | try/catch + response.ok 체크     | **해당없음** | fetch 호출 없음 (pages/에서 try/catch 1건만)    |
| 5   | fire-and-forget 금지             | **해당없음** | 비동기 호출 없음                                |
| 6   | 전역 변수/함수 금지              | **위반**     | 전역 함수 9건 + `document.write` 17건           |
| 7   | `===` 사용 (`==` 금지)           | **준수**     | `==` 사용 0건                                   |
| 8   | `innerHTML` 직접 사용 금지 (XSS) | **위반**     | 4건 직접 할당                                   |
| 9   | 네이밍 컨벤션 (camelCase)        | **준수**     | 모든 함수/변수 camelCase 준수                   |
| +   | `document.write()` 사용          | **위반**     | 17개 파일에서 대량 HTML 삽입 (금지 패턴)         |
| +   | `console.log` 프로덕션 사용      | **해당없음** | 1건 (cross-origin iframe 디버그, 경미)          |

### 3.2 이슈 목록

| 심각도       | 이슈                                                                    | 파일 (대표)                                                           | 수정 방안                                                               |
| ------------ | ----------------------------------------------------------------------- | --------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| **CRITICAL** | `document.write()` 17개 파일 — DOM 덮어쓰기 + XSS 위험                  | `sidebar-admin.js`, `header-admin.js` 등 17개                         | Mustache partial `{{> common/header}}` 으로 서버 렌더링 전환            |
| **CRITICAL** | `innerHTML` 직접 할당 4건 — XSS 위험                                    | `admin-rule-form.js:6,8`, `doctor-treatment-detail.js:9`, `staff-reception-detail.js:3` | `createElement` + `append` DOM API로 대체                               |
| **HIGH**     | `var` 전면 사용 37건 (ES6+ 금지 패턴)                                   | `pages/admin-department-list.js`, `pages/admin-staff-form.js` 등 12개 | 모든 `var`를 `const`(기본)/`let`(재할당 시)으로 교체                    |
| **HIGH**     | `const`/`let` 미사용 — pages/ 내 0건                                    | pages/ 하위 전체                                                      | 위 수정과 동시 진행                                                     |
| **HIGH**     | 전역 함수 9건 + 이름 충돌 위험 (`setActive`, `handleSubmit` 중복)       | `pages/admin-index.js:1`, `pages/doctor-index.js:1` 등                | IIFE 또는 `DOMContentLoaded` 리스너로 격리                              |
| **MEDIUM**   | try/catch 에러 처리 부재 (향후 API 연동 시 필수)                        | pages/ 전체                                                           | fetch 도입 시 `async/await` + `try/catch` + `response.ok` 패턴 적용    |
| **MEDIUM**   | `console.log` 프로덕션 잔존                                             | `staff-index.js:24`                                                   | 제거 또는 빌드 시 strip                                                 |

---

## 4. CSS 점검 (rule_css.md)

### 4.1 항목별 판정

| #   | 점검 항목                                                | 판정          | 비고                                         |
| --- | -------------------------------------------------------- | ------------- | -------------------------------------------- |
| 1   | BEM 네이밍                                               | **부분 준수** | `common.css` 준수, 역할별 CSS 5개 미준수     |
| 2   | id 셀렉터 금지                                           | **준수**      | 0건                                          |
| 3   | `!important` 금지                                        | **준수**      | 0건                                          |
| 4   | 인라인 스타일 금지                                       | **위반**      | Mustache 2개 파일에서 25건                   |
| 5   | float 레이아웃 금지                                      | **준수**      | 0건                                          |
| 6   | CSS 변수 `:root` 사용                                    | **부분 준수** | `common.css` 활용, 역할별 CSS는 하드코딩     |
| 7   | rem/em 활용 (px만 사용 금지)                             | **부분 준수** | `common.css` 준수, 역할별 CSS에서 px 혼용    |
| 8   | 셀렉터 깊이 3단계 이하                                   | **준수**      | 최대 2단계                                   |
| 9   | 파일 구조 분리 (base/components/layouts/pages/utilities) | **위반**      | 단일 `common.css`에 모두 포함, 파일명 비일관 |

### 4.2 이슈 목록

| 심각도     | 이슈                                                                             | 파일                                                                                                  | 수정 방안                                                        |
| ---------- | -------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| **HIGH**   | Mustache 인라인 스타일 25건 (`style=""` 사용 금지 규칙 위반)                     | `admin/dashboard.mustache:69-127`, `item-manager/dashboard.mustache:60-103`                           | CSS 클래스로 대체 (`.bar--h-45 { height: 45%; }`)                |
| **MEDIUM** | 역할별 CSS 5개 파일 BEM 접두어 미사용 + 내용 완전 중복                           | `admin-style.css`, `doctor-style.css`, `style-staff.css`, `style-nurse.css`, `style-item-manager.css` | 공통 파일로 통합, `l-` 접두어 적용                               |
| **MEDIUM** | 역할별 CSS에서 하드코딩 색상/크기 (CSS 변수 미사용, `#f8fafc` 7개 파일 반복)     | `admin-style.css:3,7,14,26` 등                                                                       | `:root` 변수 참조로 변경                                         |
| **MEDIUM** | 역할별 CSS에서 px 단위 사용 (`6px`, `20px`)                                      | `admin-style.css:20,27` 등 5개 파일                                                                   | `0.375rem`, `1.25rem`으로 변환                                   |
| **MEDIUM** | 파일 구조 미분리 — `common.css`에 모든 역할 통합                                 | `static/css/` 전체                                                                                    | `base/`, `components/`, `layouts/` 디렉터리 분리                 |
| **LOW**    | 파일 네이밍 불일치 (`admin-style.css` vs `style-staff.css`)                       | CSS 전체                                                                                              | `style-{role}.css` 로 통일                                       |
| **LOW**    | `home-style.css`, `style-patient.css`에서 `common.css` 폰트와 다른 font-family 지정 | `home-style.css:4`, `style-patient.css:4`                                                             | `var(--font-sans)` 사용으로 통일                                 |

---

## 5. 테스트 점검 (rule_test.md)

### 5.1 항목별 판정

| #   | 점검 항목                | 판정          | 비고                                                                                                          |
| --- | ------------------------ | ------------- | ------------------------------------------------------------------------------------------------------------- |
| 1   | 핵심 로직 테스트 존재    | **위반**      | `ReservationService` 테스트 부재 (핵심 비즈니스 로직)                                                        |
| 2   | Given-When-Then 주석     | **부분 위반** | `AdminDashboardControllerTest:48`에 GWT 주석 누락                                                             |
| 3   | AssertJ 사용             | **준수**      | JUnit Assertions import 0건                                                                                   |
| 4   | BDDMockito 사용          | **준수**      | `given().willReturn()` 전면 사용, `when().thenReturn()` 0건                                                   |
| 5   | `@DisplayName` 필수      | **위반**      | 4개 테스트 클래스 모두 클래스 레벨 `@DisplayName` 누락                                                        |
| 6   | 테스트 유형별 어노테이션 | **준수**      | Service->MockitoExtension, Repository->DataJpaTest, Controller->WebMvcTest                                    |
| 7   | 시간/랜덤 결정성         | **위반**      | `ReservationService:51`에 `System.currentTimeMillis()`, `AdminDashboardStatsService:32,49`에 `LocalDate.now()` |
| 8   | 외부 DB 의존 금지        | **준수**      | H2 인메모리 또는 Mock 사용                                                                                    |

### 5.2 이슈 목록

| 심각도     | 이슈                                                                                   | 파일                                                                   | 수정 방안                                                            |
| ---------- | -------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- | -------------------------------------------------------------------- |
| **HIGH**   | `ReservationService` 핵심 로직 테스트 부재 (테스트 없는 배포 금지 규칙 위반)            | 테스트 파일 미존재                                                     | `ReservationServiceTest` 생성 (생성/예외/환자 재사용 시나리오)       |
| **HIGH**   | `System.currentTimeMillis()` / `LocalDate.now()` 직접 사용 — 비결정적 + 중복 위험      | `ReservationService.java:51`, `AdminDashboardStatsService.java:32,49`  | `Clock` Bean 주입, `LocalDate.now(clock)` 사용                       |
| **MEDIUM** | `AdminDashboardControllerTest:48`에 `// given`, `// when`, `// then` 주석 누락          | `AdminDashboardControllerTest.java:48`                                 | GWT 주석 추가                                                        |
| **MEDIUM** | 4개 테스트 클래스에 클래스 레벨 `@DisplayName` 누락                                    | 전체 테스트 클래스                                                     | `@DisplayName("관리자 대시보드 ...")` 추가                           |
| **MEDIUM** | `AdminDashboardStatsRepositoryTest`에 `@AutoConfigureTestDatabase` 누락                 | `AdminDashboardStatsRepositoryTest.java:26`                            | `@AutoConfigureTestDatabase(replace = ANY)` 추가                     |
| **LOW**    | Controller 테스트의 `// given`, `// when` 구간이 비어 있음 (구조적으로는 준수)          | `AdminDashboardApiControllerTest.java:58-62`                           | MockMvc `perform()`을 `// when`으로, `andExpect()`를 `// then`으로 분리 |

---

## 6. 보안 점검 (rule_spring.md §10, SecurityConfig)

### 6.1 항목별 판정

| #   | 점검 항목                                     | 판정     | 비고                                                              |
| --- | --------------------------------------------- | -------- | ----------------------------------------------------------------- |
| 1   | 역할별 URL 접근 권한                          | **준수** | 모든 URL 패턴이 명세와 일치                                       |
| 2   | CSRF 정책                                     | **준수** | SSR 폼 보호 활성화, `/llm/symptom/**`만 제외                      |
| 3   | 세션 관리                                     | **준수** | `maximumSessions(1)`, 로그아웃 시 세션 무효화                     |
| 4   | 민감정보 커밋 여부                            | **준수** | `.env.example`만 추적 (빈 값)                                     |
| 5   | `.gitignore` 시크릿 파일 포함                 | **준수** | `.env`, `application-local.*`, `application-prod.properties` 제외 |
| 6   | H2 Console FilterChain `@Profile("dev")` 가드 | **위반** | `@Profile` 없이 CSRF 비활성화 — 프로덕션 노출 위험               |
| 7   | CSP `unsafe-inline` 사용                      | **위반** | `script-src`와 `style-src` 모두 `unsafe-inline` 포함              |

### 6.2 이슈 목록

| 심각도       | 이슈                                                                               | 파일                        | 수정 방안                                                                      |
| ------------ | ---------------------------------------------------------------------------------- | --------------------------- | ------------------------------------------------------------------------------ |
| **CRITICAL** | H2 Console FilterChain에 `@Profile("dev")` 없이 CSRF 비활성화 — 프로덕션 DB 노출  | `SecurityConfig.java:78-87` | `@Profile("dev")` 또는 `@ConditionalOnProperty` 추가                           |
| **CRITICAL** | CSP에 `'unsafe-inline'` 포함 — XSS 보호 무력화                                     | `SecurityConfig.java:113`   | 인라인 스크립트를 외부 `.js`로 이동, nonce 기반 CSP 적용                       |

---

## 7. 이전 점검(fix/security) 대비 변경 사항

| 항목                                     | fix/security 브랜치                       | dev 브랜치                                | 변화      |
| ---------------------------------------- | ---------------------------------------- | ---------------------------------------- | --------- |
| `ReservationRepository` 빈 class         | **위반** (빈 class, JpaRepository 미상속) | **해소** (정상 interface + JpaRepository) | 개선      |
| `PatientRepository` 빈 class             | **위반** (빈 class)                       | **해소** (정상 interface + JpaRepository) | 개선      |
| `reservation.reservation.Reservation` 충돌 | **위반** (domain.Reservation과 중복)      | **해소** (파일 삭제됨)                    | 개선      |
| `ReservationService` @Transactional      | **위반** (클래스 레벨 누락)               | **해소** (클래스 레벨 readOnly 선언)      | 개선      |
| 입력값 검증 (`@Valid`)                   | 해당없음 (API 미구현)                     | **신규 위반** (Form 검증 부재)            | 신규 이슈 |
| `IllegalArgumentException` 오용          | 해당없음                                  | **신규 위반** (500 에러 반환)             | 신규 이슈 |
| DTO 네이밍/Record                        | 준수                                      | **신규 위반** (Form/Info/Dto 네이밍)      | 신규 이슈 |
| `Resp.ok()` 미사용                       | 해당없음                                  | **신규 위반** (ReservationApiController)  | 신규 이슈 |

---

## 8. 우선 수정 로드맵

### Phase 1: 즉시 수정 (CRITICAL — 4건)

| #   | 이슈                                                    | 대상 파일                                  |
| --- | ------------------------------------------------------- | ------------------------------------------ |
| C-1 | H2 Console FilterChain `@Profile("dev")` 추가           | `SecurityConfig.java:78-87`                |
| C-2 | CSP `'unsafe-inline'` 제거 + nonce 기반 전환            | `SecurityConfig.java:113`                  |
| C-3 | `document.write()` 제거 -> Mustache partial 전환        | `header-*.js`, `sidebar-*.js` 등 17개       |
| C-4 | `innerHTML` 제거 -> `createElement` + `append`          | `admin-rule-form.js`, `doctor-treatment-detail.js`, `staff-reception-detail.js` |

### Phase 2: 병합 전 수정 (HIGH — 8건)

| #   | 이슈                                                       | 대상 파일                                                                       |
| --- | ---------------------------------------------------------- | ------------------------------------------------------------------------------- |
| H-1 | 예약 생성 입력값 검증 추가 (`@Valid` + Bean Validation)    | `ReservationController.java:49`, `ReservationCreateForm.java`                   |
| H-2 | `IllegalArgumentException` -> `CustomException.notFound()` | `ReservationService.java:46-48`                                                 |
| H-3 | `ReservationService` 테스트 작성                           | 신규 `ReservationServiceTest.java`                                              |
| H-4 | `System.currentTimeMillis()` / `LocalDate.now()` -> Clock 주입 | `ReservationService.java:51`, `AdminDashboardStatsService.java:32,49`          |
| H-5 | `var` 전면 교체 -> `const`/`let`                           | `static/js/pages/*.js` (12개 파일, 37건)                                        |
| H-6 | `const`/`let` 도입 (H-5와 동시 작업)                       | 동일                                                                            |
| H-7 | 전역 함수 IIFE/모듈로 격리 + 이름 충돌 해소                | `pages/*.js` (9개 함수)                                                         |
| H-8 | Mustache 인라인 스타일 25건 -> CSS 클래스                   | `admin/dashboard.mustache`, `item-manager/dashboard.mustache`                   |

### Phase 3: 다음 스프린트 (MEDIUM — 13건)

| #    | 이슈                                                         | 대상                                                   |
| ---- | ------------------------------------------------------------ | ------------------------------------------------------ |
| M-1  | `ReservationApiController` `Resp.ok()` 적용                  | `ReservationApiController.java`                        |
| M-2  | `ReservationCreateForm` -> `ReservationCreateRequest` record | `ReservationCreateForm.java`                           |
| M-3  | `ReservationCompleteInfo` -> `ReservationCompleteResponse` record | `ReservationCompleteInfo.java`                     |
| M-4  | `PatientRepository`, `DepartmentRepository` 소유 모듈 이동   | `reservation/reservation/` -> `patient/`, `department/` |
| M-5  | 테스트 클래스 `@DisplayName` 추가                            | 테스트 4개 클래스                                      |
| M-6  | `AdminDashboardControllerTest:48` GWT 주석 추가              | 테스트 1개 메서드                                      |
| M-7  | `@AutoConfigureTestDatabase` 추가                            | `AdminDashboardStatsRepositoryTest`                    |
| M-8  | 역할별 CSS 통합 + BEM 접두어                                 | CSS 5개 파일                                           |
| M-9  | CSS 하드코딩 -> `:root` 변수 참조                            | 역할별 CSS                                             |
| M-10 | CSS px -> rem 변환                                           | 역할별 CSS                                             |
| M-11 | CSS 파일 구조 분리 (base/components/layouts/pages/utilities) | `static/css/`                                          |
| M-12 | try/catch 에러 처리 패턴 표준화 (API 호출 도입 시)           | `pages/*.js`                                           |
| M-13 | `console.log` 제거                                           | `staff-index.js:24`                                    |

### Phase 4: 점진적 개선 (LOW — 5건)

| #   | 이슈                                            | 대상                                       |
| --- | ----------------------------------------------- | ------------------------------------------ |
| L-1 | `DoctorDto` -> `DoctorResponse` record 전환     | `doctor/DoctorDto.java`                    |
| L-2 | `POST /reservation/create` URI 동사 제거 검토   | `ReservationController.java:48`            |
| L-3 | CSS 파일 네이밍 통일 (`style-{role}.css`)        | CSS 전체                                   |
| L-4 | `home-style.css`/`style-patient.css` 폰트 통일  | `home-style.css:4`, `style-patient.css:4`  |
| L-5 | 테스트 GWT 구간 코드 배치 개선 (perform/expect 분리) | Controller 테스트 전체                 |

---

## 9. 규칙 준수율

```
Spring 백엔드   ==================..........  60% (6/10 준수)
JavaScript      ========......................  33% (2/6 해당 항목 중)
CSS             ==============................  56% (5/9 준수)
테스트          ================..............  50% (4/8 준수)
보안            ====================..........  71% (5/7 준수)
──────────────────────────────────────────────
전체            ================..............  51% (22/43 준수)
```

---

## 10. 결론

`dev` 브랜치는 이전 `fix/security` 브랜치 대비 **Repository 스캐폴딩(빈 class -> 정상 interface)** 및 **Service @Transactional 패턴**이 개선되었으나, 예약 기능 구현(`feature/reservation` 병합)으로 인해 **입력값 검증 부재**, **예외 처리 오용**, **DTO 네이밍 위반** 등 신규 이슈가 발생하였다.

**보안 CRITICAL 2건**(H2 Console 프로덕션 노출, CSP unsafe-inline)과 **프론트엔드 CRITICAL 2건**(document.write, innerHTML)은 프로덕션 배포 전 반드시 해결해야 한다.

**백엔드 HIGH 2건**(입력값 검증 부재, IllegalArgumentException 500 에러)은 비회원 예약 기능의 데이터 무결성에 직접 영향을 미치므로 우선 수정이 필요하다.

---

> **작성:** Claude Code (자동 점검)
> **다음 점검 예정:** 규칙 준수율 70% 이상 달성 후
