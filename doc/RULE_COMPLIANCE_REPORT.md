# HMS 프로젝트 규칙 준수 점검 보고서

> **점검일:** 2026-03-10
> **점검 대상:** `fix/security` 브랜치 (커밋 `0c91b92`)
> **기준 문서:** `doc/RULE.md`, `doc/rules/*`, `.ai/memory.md`, `.ai/workflows/workflow.md`

---

## 1. 점검 요약

| 영역          | 점검 항목 수 | 준수   | 위반   | 해당없음 | 심각도 요약                              |
| ------------- | ------------ | ------ | ------ | -------- | ---------------------------------------- |
| Spring 백엔드 | 10           | 6      | 3      | 1        | HIGH 3, MEDIUM 3, LOW 4                  |
| JavaScript    | 9            | 2      | 4      | 3        | CRITICAL 2, HIGH 3                       |
| CSS           | 9            | 5      | 4      | 0        | MEDIUM 4                                 |
| 테스트        | 8            | 4      | 4      | 0        | CRITICAL 1, HIGH 1, MEDIUM 3, LOW 1      |
| 보안          | 5            | 4      | 1      | 0        | HIGH 2, LOW 1                            |
| **합계**      | **41**       | **21** | **16** | **4**    | **CRITICAL 3, HIGH 9, MEDIUM 10, LOW 6** |

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

| #   | 점검 항목                               | 판정     | 비고                                                                    |
| --- | --------------------------------------- | -------- | ----------------------------------------------------------------------- |
| 1   | 환경 변수 관리 (민감정보 하드코딩 금지) | **준수** | `${CLAUDE_API_KEY:}` 환경변수 방식 사용                                 |
| 2   | GlobalExceptionHandler 공통 포맷        | **준수** | ErrorResponse record, Validation/도메인/폴백 예외 모두 처리             |
| 3   | Controller 분리 (SSR/API)               | **준수** | `@Controller` / `@RestController` 분리, `Resp.ok()` 사용                |
| 4   | Controller 책임 (비즈니스 로직 금지)    | **준수** | 모든 Controller가 Service 위임 후 반환만 수행                           |
| 5   | Service @Transactional 패턴             | **위반** | `ReservationService`에 클래스 레벨 `@Transactional(readOnly=true)` 누락 |
| 6   | DTO Record 사용 및 네이밍               | **준수** | record 사용, `...Request`/`...Response` 네이밍 준수                     |
| 7   | Validation (@Valid/@Validated)          | **위반** | 다수 Controller에 `@Validated` 클래스 레벨 누락                         |
| 8   | Repository 규칙                         | **위반** | 빈 class 2개 (interface + JpaRepository 상속 필요), 엔티티 충돌 1개     |
| 9   | SecurityConfig (Bean 방식, CORS)        | **준수** | SecurityFilterChain Bean, CorsConfigurationSource Bean 사용             |
| 10  | URI 설계 (RESTful)                      | **준수** | 역할 기반 계층형 URI 적용                                               |

### 2.2 이슈 목록

| 심각도     | 이슈                                                                              | 파일                                                    | 수정 방안                                                                                   |
| ---------- | --------------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| **HIGH**   | `ReservationRepository`가 interface가 아닌 빈 class (JpaRepository 미상속)        | `reservation/reservation/ReservationRepository.java:3`  | `public interface ReservationRepository extends JpaRepository<Reservation, Long>` 으로 변환 |
| **HIGH**   | `PatientRepository`가 interface가 아닌 빈 class (JpaRepository 미상속)            | `reservation/reservation/PatientRepository.java:3`      | `public interface PatientRepository extends JpaRepository<Patient, Long>` 으로 변환         |
| **HIGH**   | `reservation.reservation.Reservation` 빈 class가 `domain.Reservation`과 이름 충돌 | `reservation/reservation/Reservation.java:3`            | 파일 삭제, `domain.Reservation` 엔티티 사용                                                 |
| **MEDIUM** | `ReservationService`에 `@Transactional(readOnly=true)` 클래스 레벨 누락           | `reservation/reservation/ReservationService.java:5`     | 클래스에 `@Transactional(readOnly=true)` 추가                                               |
| **MEDIUM** | `AdminDashboardApiController`에 `@Validated` 클래스 레벨 누락                     | `admin/dashboard/AdminDashboardApiController.java:10`   | `@Validated` 추가                                                                           |
| **MEDIUM** | `ReservationController`에 `@Validated` 클래스 레벨 누락                           | `reservation/reservation/ReservationController.java:11` | `@Validated` 추가                                                                           |
| **LOW**    | `Resp.java`가 `@Data` mutable class — record 전환 권장                            | `common/util/Resp.java:9`                               | record로 변환                                                                               |
| **LOW**    | CORS allowedMethods에 `PATCH` 누락                                                | `config/SecurityConfig.java:258`                        | `"PATCH"` 추가                                                                              |
| **LOW**    | 샘플 Controller AJAX 메서드에서 Service 위임 없이 하드코딩 반환                   | `_sample/SampleReservationController.java:311-332`      | Service 위임 패턴으로 수정                                                                  |
| **LOW**    | 샘플 URI가 동사 기반 RPC 스타일 (`getSlots`, `getDoctors`)                        | `_sample/SampleReservationController.java:311,347`      | 명사 기반 URI로 변경                                                                        |

---

## 3. JavaScript 점검 (rule_javascript.md)

### 3.1 항목별 판정

| #   | 점검 항목                        | 판정         | 비고                                  |
| --- | -------------------------------- | ------------ | ------------------------------------- |
| 1   | `var` 사용 금지                  | **위반**     | pages/ 하위 12개 파일 전면 `var` 사용 |
| 2   | `const` 우선, `let` 보조         | **위반**     | pages/ 내 `const` 0건, `let` 0건      |
| 3   | async/await 비동기 처리          | **해당없음** | API 호출 미구현 상태                  |
| 4   | try/catch + response.ok 체크     | **위반**     | pages/ 전체에서 try/catch 1건만 존재  |
| 5   | fire-and-forget 금지             | **해당없음** | 비동기 호출 없음                      |
| 6   | 전역 변수/함수 금지              | **위반**     | 전역 함수 9건, `document.write` 17건  |
| 7   | `===` 사용 (`==` 금지)           | **준수**     | `==` 사용 0건                         |
| 8   | `innerHTML` 직접 사용 금지 (XSS) | **위반**     | 4건 직접 할당                         |
| 9   | 네이밍 컨벤션 (camelCase)        | **준수**     | 모든 함수/변수 camelCase 준수         |

### 3.2 이슈 목록

| 심각도       | 이슈                                                                   | 파일 (대표)                                                           | 수정 방안                                                               |
| ------------ | ---------------------------------------------------------------------- | --------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| **CRITICAL** | `var` 전면 사용 (ES6+ 금지 패턴)                                       | `pages/admin-department-list.js`, `pages/admin-staff-form.js` 등 12개 | 모든 `var`를 `const`(기본)/`let`(재할당 시)으로 교체                    |
| **CRITICAL** | `const`/`let` 미사용 — pages/ 내 0건                                   | pages/ 하위 전체                                                      | 위 수정과 동시 진행                                                     |
| **HIGH**     | 전역 함수 선언 9건 + 이름 충돌 위험 (`setActive`, `handleSubmit` 중복) | `pages/admin-index.js:1`, `pages/doctor-index.js:1` 등                | IIFE 또는 `DOMContentLoaded` 리스너로 격리                              |
| **HIGH**     | `document.write` 17건 — DOM 덮어쓰기 위험                              | `header-admin.js`, `sidebar-admin.js` 등 17개                         | Mustache partial `{{> common/header}}` 또는 `insertAdjacentHTML`로 대체 |
| **HIGH**     | `innerHTML` 직접 할당 4건 (XSS 위험)                                   | `pages/admin-rule-form.js:6,8`, `pages/doctor-treatment-detail.js:9`  | `createElement` + `append` DOM API로 대체                               |

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
| 7   | rem/em 활용 (px만 사용 금지)                             | **부분 준수** | `common.css` 준수, 역할별 CSS에서 px 사용    |
| 8   | 셀렉터 깊이 3단계 이하                                   | **준수**      | 최대 2단계                                   |
| 9   | 파일 구조 분리 (base/components/layouts/pages/utilities) | **위반**      | 단일 `common.css`에 모두 포함, 파일명 비일관 |

### 4.2 이슈 목록

| 심각도     | 이슈                                                     | 파일                                                                                                  | 수정 방안                                                                  |
| ---------- | -------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| **MEDIUM** | 역할별 CSS 5개 파일이 BEM 접두어 미사용 + 내용 완전 중복 | `admin-style.css`, `doctor-style.css`, `style-staff.css`, `style-nurse.css`, `style-item-manager.css` | 하나의 공통 파일로 통합, BEM 접두어 적용                                   |
| **MEDIUM** | Mustache 인라인 스타일 25건                              | `admin/dashboard.mustache:69-127`, `item-manager/dashboard.mustache:60-103`                           | CSS 클래스로 대체 (`.bar--h-45 { height: 45%; }`)                          |
| **MEDIUM** | 역할별 CSS에서 하드코딩 색상/크기 (CSS 변수 미사용)      | `admin-style.css:3,7,14,26` 등                                                                        | `:root` 변수 참조로 변경 (`#f8fafc` -> `var(--color-bg)`)                  |
| **MEDIUM** | 파일 구조 미분리 — `common.css`에 모든 역할 통합         | `static/css/` 전체                                                                                    | `base/`, `components/`, `layouts/`, `pages/`, `utilities/` 디렉터리로 분리 |

---

## 5. 테스트 점검 (rule_test.md)

### 5.1 항목별 판정

| #   | 점검 항목                | 판정          | 비고                                                                                                          |
| --- | ------------------------ | ------------- | ------------------------------------------------------------------------------------------------------------- |
| 1   | 핵심 로직 테스트 존재    | **위반**      | `ReservationService`, `CustomUserDetailsService` 테스트 부재                                                  |
| 2   | Given-When-Then 주석     | **부분 위반** | `AdminDashboardControllerTest:48`에 GWT 주석 누락                                                             |
| 3   | AssertJ 사용             | **준수**      | JUnit Assertions import 0건                                                                                   |
| 4   | BDDMockito 사용          | **준수**      | `given().willReturn()` 전면 사용, `when().thenReturn()` 0건                                                   |
| 5   | `@DisplayName` 필수      | **위반**      | 4개 테스트 클래스 모두 클래스 레벨 `@DisplayName` 누락                                                        |
| 6   | 테스트 유형별 어노테이션 | **준수**      | Service->MockitoExtension, Repository->DataJpaTest, Controller->WebMvcTest                                    |
| 7   | 시간/랜덤 결정성         | **위반**      | `ReservationService:51`에 `System.currentTimeMillis()` 직접 사용, 도메인 엔티티에 `LocalDate.now()` 직접 사용 |
| 8   | 외부 DB 의존 금지        | **준수**      | H2 인메모리 또는 Mock 사용                                                                                    |

### 5.2 이슈 목록

| 심각도       | 이슈                                                                           | 파일                                                              | 수정 방안                                                      |
| ------------ | ------------------------------------------------------------------------------ | ----------------------------------------------------------------- | -------------------------------------------------------------- |
| **CRITICAL** | `System.currentTimeMillis()`로 예약번호 생성 — 동시 요청 시 중복 + 비결정적    | `reservation/reservation/ReservationService.java:51`              | UUID/DB 시퀀스/날짜+원자 카운터로 교체, `Clock` 주입           |
| **HIGH**     | `ReservationService` 핵심 로직 테스트 부재 (테스트 없는 배포 금지 규칙 위반)   | 테스트 파일 미존재                                                | `ReservationServiceTest` 생성 (생성/예외/환자 재사용 시나리오) |
| **MEDIUM**   | `AdminDashboardControllerTest:48`에 `// given`, `// when`, `// then` 주석 누락 | `AdminDashboardControllerTest.java:48`                            | GWT 주석 추가                                                  |
| **MEDIUM**   | 4개 테스트 클래스에 클래스 레벨 `@DisplayName` 누락                            | 전체 테스트 클래스                                                | `@DisplayName("관리자 대시보드 ...")` 추가                     |
| **MEDIUM**   | 도메인 엔티티에서 `LocalDate.now()` / `LocalDateTime.now()` 직접 사용          | `Patient.java:46`, `Reservation.java:65-66`, `Item.java:44-45` 등 | `Clock` Bean 주입 방식으로 추상화                              |
| **LOW**      | `AdminDashboardStatsRepositoryTest`에 `@AutoConfigureTestDatabase` 누락        | `AdminDashboardStatsRepositoryTest.java:26`                       | `@AutoConfigureTestDatabase(replace = ANY)` 추가               |

---

## 6. 보안 점검 (rule_spring.md 10, SecurityConfig)

### 6.1 항목별 판정

| #   | 점검 항목                     | 판정     | 비고                                                              |
| --- | ----------------------------- | -------- | ----------------------------------------------------------------- |
| 1   | 역할별 URL 접근 권한          | **준수** | 모든 URL 패턴이 명세와 일치                                       |
| 2   | CSRF 정책                     | **준수** | SSR 폼 보호 활성화, `/llm/symptom/**`만 제외                      |
| 3   | 세션 관리                     | **준수** | `maximumSessions(1)`, 로그아웃 시 세션 무효화                     |
| 4   | 민감정보 커밋 여부            | **준수** | `.env.example`만 추적 (빈 값)                                     |
| 5   | `.gitignore` 시크릿 파일 포함 | **준수** | `.env`, `application-local.*`, `application-prod.properties` 제외 |

### 6.2 추가 보안 이슈

| 심각도   | 이슈                                                                               | 파일                        | 수정 방안                                                |
| -------- | ---------------------------------------------------------------------------------- | --------------------------- | -------------------------------------------------------- |
| **HIGH** | H2 Console FilterChain에 `@Profile("dev")` 없이 CSRF 비활성화 — 프로덕션 노출 위험 | `SecurityConfig.java:79-84` | `@Profile("dev")` 또는 `@ConditionalOnProperty` 추가     |
| **HIGH** | CSP에 `'unsafe-inline'` 포함 — XSS 보호 약화                                       | `SecurityConfig.java:113`   | 인라인 스크립트를 외부 `.js`로 이동, nonce 기반 CSP 적용 |
| **LOW**  | CORS allowedMethods에 `PATCH` 누락                                                 | `SecurityConfig.java:258`   | `"PATCH"` 추가                                           |

---

## 7. 우선 수정 로드맵

### Phase 1: 즉시 수정 (CRITICAL — 3건)

| #   | 이슈                                                              | 대상 파일                          |
| --- | ----------------------------------------------------------------- | ---------------------------------- |
| C-1 | `System.currentTimeMillis()` 예약번호 -> UUID/시퀀스 + Clock 주입 | `ReservationService.java:51`       |
| C-2 | `var` 전면 교체 -> `const`/`let`                                  | `static/js/pages/*.js` (12개 파일) |
| C-3 | `const`/`let` 도입 (C-2와 동시 작업)                              | 동일                               |

### Phase 2: 병합 전 수정 (HIGH — 9건)

| #   | 이슈                                                      | 대상 파일                                                                       |
| --- | --------------------------------------------------------- | ------------------------------------------------------------------------------- |
| H-1 | `ReservationRepository` -> interface + JpaRepository 상속 | `ReservationRepository.java`                                                    |
| H-2 | `PatientRepository` -> interface + JpaRepository 상속     | `PatientRepository.java`                                                        |
| H-3 | `reservation.reservation.Reservation` 빈 class 삭제       | `Reservation.java`                                                              |
| H-4 | `ReservationService` 핵심 로직 테스트 작성                | 신규 `ReservationServiceTest.java`                                              |
| H-5 | H2 Console FilterChain에 `@Profile("dev")` 추가           | `SecurityConfig.java:79`                                                        |
| H-6 | CSP `'unsafe-inline'` 제거 계획 수립                      | `SecurityConfig.java:113`                                                       |
| H-7 | 전역 함수 IIFE/모듈로 격리 + 이름 충돌 해소               | `pages/*.js` (9개 함수)                                                         |
| H-8 | `document.write` 제거 -> Mustache partial 또는 DOM API    | `header-*.js`, `sidebar-*.js` 등 17개                                           |
| H-9 | `innerHTML` 제거 -> `createElement` + `append`            | `admin-rule-form.js`, `doctor-treatment-detail.js`, `staff-reception-detail.js` |

### Phase 3: 다음 스프린트 (MEDIUM — 10건)

| #    | 이슈                                                         | 대상                                                   |
| ---- | ------------------------------------------------------------ | ------------------------------------------------------ |
| M-1  | `ReservationService` `@Transactional(readOnly=true)` 추가    | `ReservationService.java`                              |
| M-2  | Controller `@Validated` 클래스 레벨 추가                     | `AdminDashboardApiController`, `ReservationController` |
| M-3  | 테스트 클래스 `@DisplayName` 추가                            | 테스트 4개 클래스                                      |
| M-4  | `AdminDashboardControllerTest:48` GWT 주석 추가              | 테스트 1개 메서드                                      |
| M-5  | 도메인 엔티티 `LocalDate.now()` -> Clock 주입                | `Patient`, `Reservation`, `Item`, `Staff`              |
| M-6  | 역할별 CSS 통합 + BEM 접두어                                 | CSS 5개 파일                                           |
| M-7  | Mustache 인라인 스타일 -> CSS 클래스                         | `dashboard.mustache` 2개                               |
| M-8  | CSS 하드코딩 -> `:root` 변수 참조                            | 역할별 CSS                                             |
| M-9  | CSS 파일 구조 분리 (base/components/layouts/pages/utilities) | `static/css/`                                          |
| M-10 | try/catch 에러 처리 패턴 표준화 (API 호출 도입 시)           | `pages/*.js`                                           |

### Phase 4: 점진적 개선 (LOW — 6건)

| #   | 이슈                              | 대상                                       |
| --- | --------------------------------- | ------------------------------------------ |
| L-1 | `Resp.java` record 전환           | `common/util/Resp.java`                    |
| L-2 | CORS `PATCH` 추가                 | `SecurityConfig.java`                      |
| L-3 | 샘플 Controller Service 위임 패턴 | `_sample/SampleReservationController.java` |
| L-4 | 샘플 URI 명사 기반으로 변경       | `_sample/SampleReservationController.java` |
| L-5 | `@AutoConfigureTestDatabase` 추가 | `AdminDashboardStatsRepositoryTest`        |
| L-6 | CORS allowCredentials 문서화      | `SecurityConfig.java`                      |

---

## 8. 규칙 준수율

```
Spring 백엔드   ==================..........  60% (6/10 준수)
JavaScript      ========......................  33% (2/6 해당 항목 중)
CSS             ==============................  56% (5/9 준수)
테스트          ============..................  50% (4/8 준수)
보안            ========================......  80% (4/5 준수)
──────────────────────────────────────────────
전체            ================..............  51% (21/41 준수)
```

---

## 9. 결론

현재 프로젝트는 **백엔드 아키텍처(예외 처리, Controller 분리, DTO Record, SecurityConfig)** 측면에서 규칙을 잘 준수하고 있으나, **프론트엔드 JavaScript(`var` 전면 사용, 전역 변수 오염)** 와 **Repository 스캐폴딩(빈 class)** 영역에서 구조적 위반이 발견되었습니다.

CRITICAL 3건과 HIGH 9건은 프로덕션 안정성과 보안에 직접 영향을 미치므로 우선 해결이 필요합니다. 특히 `ReservationService`의 `System.currentTimeMillis()` 기반 예약번호 생성은 동시 요청 시 중복 가능성이 있어 즉시 수정이 권고됩니다.

---

> **작성:** Claude Code (자동 점검)
> **다음 점검 예정:** 규칙 준수율 70% 이상 달성 후
