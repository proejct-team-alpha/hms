# 코드 리뷰 보고서

**리뷰 대상:** `dev` ← `main` 브랜치 차이
**리뷰 일자:** 2026-03-12
**변경 파일 수:** 271개 | **상세 검토 파일:** 32개 (Java, JS, 설정, 템플릿)
**총 이슈:** 19건

---

## 팀 구성 및 코드 소유권

| 역할              | 이름   | 담당 영역                                                                                              |
| ----------------- | ------ | ------------------------------------------------------------------------------------------------------ |
| 책임개발자 (Lead) | 김민구 | `config/**`, `common/**`, `domain/**`, `llm/**`, `application*.properties`, `templates/common/**`      |
| 개발자 A          | 강태오 | `reservation/**`, `home/**`, `templates/reservation/**`, `templates/home/**`                           |
| 개발자 B          | 조유지 | `staff/**`, `doctor/**`, `nurse/**`, `templates/staff/**`, `templates/doctor/**`, `templates/nurse/**` |
| 개발자 C          | 강상민 | `admin/**`, `api/**`, `templates/admin/**`                                                             |

---

## 심각도 요약

| 심각도   | 건수 | 조치 기준           |
| -------- | ---- | ------------------- |
| CRITICAL | 3    | 병합 전 반드시 수정 |
| HIGH     | 7    | 병합 전 수정 권장   |
| MEDIUM   | 6    | 가능한 빨리 수정    |
| LOW      | 3    | 개선 고려           |

---

## 작업자별 이슈 분류 요약

### 김민구 (책임개발자) — 8건

| 이슈 | 심각도   | 파일                                           | 요약                                          |
| ---- | -------- | ---------------------------------------------- | --------------------------------------------- |
| C1   | CRITICAL | `config/SecurityConfig.java`                   | H2 콘솔 무인증 노출 위험                      |
| C2   | CRITICAL | `config/RateLimitFilter.java`                  | JSON 인젝션 취약점                            |
| C3   | CRITICAL | `config/RateLimitFilter.java`                  | X-Forwarded-For IP 스푸핑으로 Rate Limit 우회 |
| H4   | HIGH     | `config/RateLimitFilter.java`                  | ConcurrentHashMap 메모리 누수                 |
| H7   | HIGH     | `node_modules/*`                               | node_modules Git 커밋 제거                    |
| M4   | MEDIUM   | `common/exception/GlobalExceptionHandler.java` | SSR 컨트롤러에 JSON 응답 반환                 |
| M6   | MEDIUM   | `config/SecurityConfig.java`                   | CSP unsafe-inline 허용                        |
| L3   | LOW      | `META-INF/MANIFEST.MF`                         | 불필요 파일 제거                              |

### 강태오 (개발자 A) — 3건

| 이슈 | 심각도 | 파일                                                                   | 요약                               |
| ---- | ------ | ---------------------------------------------------------------------- | ---------------------------------- |
| H1-A | HIGH   | `reservation/ReservationCreateForm.java`, `ReservationUpdateForm.java` | DTO 입력 검증 누락 (@Valid 미적용) |
| H2   | HIGH   | `reservation/ReservationService.java:78-85`                            | 예약번호 생성 동시성 경합          |
| H3   | HIGH   | `reservation/ReservationService.java:89-93`                            | 중복 예약 검사 TOCTOU 문제         |

### 조유지 (개발자 B) — 7건

| 이슈 | 심각도 | 파일                                                                                                                                                 | 요약                                     |
| ---- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------- |
| H1-B | HIGH   | `staff/walkin/dto/WalkinRequestDto.java`, `staff/reception/dto/ReceptionUpdateRequest.java`, `staff/reservation/dto/PhoneReservationRequestDto.java` | DTO 입력 검증 누락 (@Valid 미적용)       |
| H5   | HIGH   | `staff/reception/ReceptionService.java:32-43`                                                                                                        | N+1 쿼리 문제 (findAll + Lazy Loading)   |
| H6   | HIGH   | `staff/walkin/WalkinService.java:38-42`                                                                                                              | orElseThrow() 예외 메시지 누락           |
| M1   | MEDIUM | `staff/reception/ReceptionService.java:50`                                                                                                           | System.out.println 사용 (환자 이름 노출) |
| M2   | MEDIUM | `staff/walkin/WalkinService.java:45`                                                                                                                 | WALKIN 예약번호 중복 가능성              |
| M5   | MEDIUM | `staff/reception/ReceptionService.java:31`                                                                                                           | 읽기 전용 메서드에 쓰기 트랜잭션 설정    |
| L2   | LOW    | `static/js/pages/staff-reception-detail.js`                                                                                                          | 하드코딩된 환자 mock 데이터              |

### 강상민 (개발자 C) — 1건

| 이슈 | 심각도 | 파일                                 | 요약                        |
| ---- | ------ | ------------------------------------ | --------------------------- |
| L1   | LOW    | `static/js/pages/admin-dashboard.js` | 하드코딩된 차트 mock 데이터 |

### 공통 (전체 담당자 확인) — 1건

| 이슈 | 심각도 | 파일                      | 요약                          |
| ---- | ------ | ------------------------- | ----------------------------- |
| M3   | MEDIUM | 각자 담당 Mustache 템플릿 | CSRF 토큰 포함 여부 점검 필요 |

---

## CRITICAL — 반드시 수정 필요

### C1. H2 콘솔 무인증 노출 위험 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/config/SecurityConfig.java:78-87`
- **내용:** `@Order(1)` 필터 체인이 H2 콘솔에 대해 인증 없이 모든 요청을 허용하고 CSRF도 비활성화되어 있다. `application.properties`에 `spring.profiles.active=dev`가 하드코딩되어 있어, 프로필 오버라이드 없이 배포될 경우 전체 데이터베이스가 외부에 노출된다.
- **수정 방안:**
  - `@ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")` 추가
  - 또는 `@Profile("dev")` 어노테이션으로 개발 환경에서만 등록되도록 제한

### C2. Rate Limit 필터 JSON 인젝션 취약점 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/config/RateLimitFilter.java:66`
- **내용:** 에러 응답 생성 시 요청 URI 경로를 수동 문자열 결합으로 JSON에 삽입하고 있다. `"` 문자만 이스케이프하고 있어, 백슬래시(`\`), 줄바꿈(`\n`), 유니코드 이스케이프(`\u0022`) 등을 통해 임의의 JSON을 주입할 수 있다. 클라이언트에서 이 JSON을 파싱하는 경우 XSS로 이어질 수 있다.
- **수정 방안:**
  ```java
  // Jackson ObjectMapper를 사용한 안전한 직렬화
  Map<String, Object> error = Map.of(
      "success", false,
      "errorCode", "RATE_LIMIT_EXCEEDED",
      "message", "요청 한도를 초과했습니다.",
      "timestamp", Instant.now().toString(),
      "path", path
  );
  new ObjectMapper().writeValue(response.getOutputStream(), error);
  ```

### C3. X-Forwarded-For 헤더를 통한 Rate Limit 우회 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/config/RateLimitFilter.java:95-101`
- **내용:** `X-Forwarded-For` 헤더를 무조건 신뢰하여 클라이언트 IP를 결정한다. 공격자가 이 헤더를 임의로 설정하면 모든 요율 제한(로그인 브루트포스 방어, LLM API 비용 보호 등)을 완전히 우회할 수 있다.
- **수정 방안:**

  ```java
  @Value("${hms.rate-limit.trust-proxy:false}")
  private boolean trustProxy;

  private String resolveClientIp(HttpServletRequest request) {
      if (trustProxy) {
          String xff = request.getHeader("X-Forwarded-For");
          if (xff != null && !xff.isBlank()) {
              return xff.split(",")[0].trim();
          }
      }
      return request.getRemoteAddr();
  }
  ```

---

## HIGH — 병합 전 수정 권장

### H1. 전체 DTO에 입력 검증 누락 — `강태오`, `조유지`

- **파일:**
  - `강태오`: `ReservationCreateForm.java`, `ReservationUpdateForm.java`
  - `조유지`: `WalkinRequestDto.java`, `ReceptionUpdateRequest.java`, `PhoneReservationRequestDto.java`
- **내용:** 모든 요청 DTO에 `@NotBlank`, `@NotNull`, `@Size`, `@Pattern` 등의 Bean Validation 어노테이션이 없고, 컨트롤러에서 `@Valid`도 사용하지 않고 있다. 프로젝트의 샘플 코드(`SampleReservationCreateRequest.java`)에는 올바른 패턴이 작성되어 있으나 실제 운영 DTO에는 적용되지 않았다.
- **영향:** null 이름, 빈 전화번호, 과거 날짜, 무제한 길이 문자열 등이 모두 허용되어 NullPointerException, 잘못된 DB 레코드, 악용 가능성이 있다.
- **수정 방안:** 모든 DTO에 Bean Validation 어노테이션 추가 및 컨트롤러 파라미터에 `@Valid` 적용

### H2. 예약번호 생성 시 동시성 경합(Race Condition) — `강태오`

- **파일:** `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationService.java:78-85`
- **내용:** `countByCreatedAtBetween()`으로 현재 월의 건수를 조회한 후 +1로 예약번호를 생성한다. 동시 요청 시 같은 count 값을 읽어 중복 번호가 생성될 수 있다.
- **수정 방안:** DB 시퀀스 사용, 또는 `reservation_number` 컬럼에 UNIQUE 제약 조건 추가 후 충돌 시 재시도

### H3. 중복 예약 검사의 TOCTOU 문제 — `강태오`

- **파일:** `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationService.java:89-93`
- **내용:** `existsBy...`로 중복 검사 후 저장하는 check-then-act 패턴을 사용하고 있다. DB 수준의 유니크 제약 조건이 없어 동시 요청 시 같은 시간대에 이중 예약이 가능하다.
- **수정 방안:** `(doctor_id, reservation_date, time_slot)` 복합 유니크 제약 조건 추가

### H4. RateLimitFilter의 ConcurrentHashMap 메모리 누수 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/config/RateLimitFilter.java:34`
- **내용:** `ConcurrentHashMap<String, Bucket>`에 만료된 버킷을 제거하는 로직이 없다. `clientIP:category` 키가 무한히 증가하여 서버 메모리를 소진할 수 있다. C3의 IP 스푸핑과 결합하면 더욱 위험하다.
- **수정 방안:**
  ```java
  @Scheduled(fixedRate = 120_000)
  public void cleanupExpiredBuckets() {
      long now = System.currentTimeMillis();
      buckets.entrySet().removeIf(e -> now - e.getValue().windowStart > WINDOW_MS * 2);
  }
  ```

### H5. ReceptionService의 N+1 쿼리 문제 — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/staff/reception/ReceptionService.java:32-43`
- **내용:** `findAll()`로 전체 예약을 조회한 후, 각 예약의 환자·의사·진료과를 Lazy Loading으로 개별 조회한다. 페이징도 없어 예약 1,000건 기준 3,000회 이상의 추가 SQL 쿼리가 발생한다.
- **수정 방안:** `JOIN FETCH` JPQL 쿼리 작성 및 페이징 적용
  ```java
  @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department")
  List<Reservation> findAllWithDetails();
  ```

### H6. WalkinService의 불명확한 예외 메시지 — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/staff/walkin/WalkinService.java:38-42`
- **내용:** `orElseThrow()`를 메시지 없이 사용하여 `NoSuchElementException`이 발생하면 500 에러만 반환된다. 다른 서비스에서는 `orElseThrow(() -> new IllegalArgumentException("..."))`로 메시지를 제공하고 있어 일관성도 부족하다.
- **수정 방안:** 의미 있는 에러 메시지 추가

### H7. node_modules가 Git에 커밋됨 — `김민구`

- **파일:** `node_modules/*`
- **내용:** `.gitignore`에 `node_modules/`가 등록되어 있으나, 규칙 추가 전에 이미 커밋된 파일이 남아 있다. 저장소 용량이 불필요하게 증가하고, 벤더링된 의존성의 보안 감사가 불가능하다.
- **수정 방안:**
  ```bash
  git rm -r --cached node_modules/
  ```

---

## MEDIUM — 가능한 빨리 수정

### M1. System.out.println을 로깅 대신 사용 — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/staff/reception/ReceptionService.java:50`
- **내용:** `System.out.println("전화 예약 생성 요청: " + request.getName())`로 환자 이름이 stdout에 노출된다. SLF4J 로거를 사용해야 한다.

### M2. 방문접수 예약번호 중복 가능성 — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/staff/walkin/WalkinService.java:45`
- **내용:** `"WALKIN-" + System.currentTimeMillis()`는 밀리초 단위이므로 동시 요청 시 중복 번호가 생성될 수 있다.
- **수정 방안:** UUID 또는 DB 시퀀스 기반 번호 생성으로 변경

### M3. Mustache 폼 템플릿의 CSRF 토큰 검증 필요 — `전체`

- **파일:** `<form method="POST">`가 포함된 모든 Mustache 템플릿
- **내용:** `LayoutModelInterceptor`가 `_csrf`를 모델에 주입하고 있으나, 모든 폼에 `<input type="hidden" name="_csrf" value="{{_csrf.token}}"/>`이 포함되어 있는지 확인이 필요하다.
- **담당:**
  - `강태오`: `templates/reservation/**`
  - `조유지`: `templates/staff/**`, `templates/doctor/**`, `templates/nurse/**`
  - `강상민`: `templates/admin/**`

### M4. GlobalExceptionHandler가 SSR 컨트롤러에 JSON 응답 반환 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/common/exception/GlobalExceptionHandler.java`
- **내용:** `@RestControllerAdvice`로 선언되어 모든 예외에 JSON `ResponseEntity`를 반환한다. 그러나 대부분의 컨트롤러는 `@Controller`(Mustache 뷰 반환)이므로 사용자에게 원시 JSON이 표시된다.
- **수정 방안:** `@Controller`용(뷰 반환)과 `@RestController`용(JSON 반환) 예외 핸들러 분리

### M5. 읽기 전용 메서드에 쓰기 가능 트랜잭션 설정 — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/staff/reception/ReceptionService.java:31`
- **내용:** 클래스 수준의 `@Transactional(readOnly = true)`를 메서드 수준의 `@Transactional`이 덮어쓰고 있다. 읽기 전용 작업에 불필요한 쓰기 트랜잭션이 사용된다.

### M6. CSP에서 unsafe-inline 허용으로 XSS 방어 무력화 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/config/SecurityConfig.java:113`
- **내용:** `script-src 'self' 'unsafe-inline'` 설정으로 인라인 스크립트 실행이 허용되어 CSP가 사실상 XSS를 방어하지 못한다.
- **수정 방안:** `'unsafe-inline'` 제거 후 nonce 또는 hash 기반 인라인 스크립트 허용으로 전환

---

## LOW — 개선 고려

### L1. 하드코딩된 차트 mock 데이터 — `강상민`

- **파일:** `src/main/resources/static/js/pages/admin-dashboard.js:8-10`
- **내용:** API에서 데이터를 가져오는 별도의 `page/admin-dashboard.js`와 충돌 가능. mock 버전 제거 필요.

### L2. 접수 상세 JS에 하드코딩된 환자 데이터 — `조유지`

- **파일:** `src/main/resources/static/js/pages/staff-reception-detail.js:14-43`
- **내용:** `if (id === '1') ... else if (id === '2') ...` 형태의 플레이스홀더 코드. 서버 데이터 바인딩으로 교체 필요.

### L3. 루트 디렉토리에 불필요한 META-INF/MANIFEST.MF — `김민구`

- **파일:** `META-INF/MANIFEST.MF`
- **내용:** Gradle Wrapper 아티팩트가 저장소 루트에 커밋됨. 삭제 후 `.gitignore`에 `META-INF/` 추가 권장.

---

## 긍정적 사항

- Mustache 템플릿에서 `{{{unescaped}}}` 미사용 — 자동 이스케이프로 XSS 기본 방어
- 모든 JPQL 쿼리에서 `@Param` 파라미터 바인딩 사용 — SQL 인젝션 위험 없음
- SecurityConfig에 CORS 화이트리스트, HSTS, 세션 동시 접속 제어, 로그인 에러 메시지 통일 적용
- `CustomUserDetailsService`가 활성 직원만 조회하고 BCrypt 사용

---

## 최종 판정: **REQUEST CHANGES (변경 요청)**

### 작업자별 수정 우선순위

#### 김민구 (책임개발자) — CRITICAL 3건 + HIGH 2건 + MEDIUM 2건 + LOW 1건

| 순위 | 이슈         | 대상 파일                     | 심각도          |
| ---- | ------------ | ----------------------------- | --------------- |
| 1    | C2 + C3 + H4 | `RateLimitFilter.java`        | CRITICAL + HIGH |
| 2    | C1           | `SecurityConfig.java`         | CRITICAL        |
| 3    | M4           | `GlobalExceptionHandler.java` | MEDIUM          |
| 4    | M6           | `SecurityConfig.java`         | MEDIUM          |
| 5    | H7           | `node_modules/`               | HIGH            |
| 6    | L3           | `META-INF/MANIFEST.MF`        | LOW             |

#### 강태오 (개발자 A) — HIGH 3건

| 순위 | 이슈           | 대상 파일                                                  | 심각도 |
| ---- | -------------- | ---------------------------------------------------------- | ------ |
| 1    | H2 + H3        | `ReservationService.java`                                  | HIGH   |
| 2    | H1-A           | `ReservationCreateForm.java`, `ReservationUpdateForm.java` | HIGH   |
| 3    | M3 (본인 담당) | `templates/reservation/**` CSRF 점검                       | MEDIUM |

#### 조유지 (개발자 B) — HIGH 3건 + MEDIUM 3건 + LOW 1건

| 순위 | 이슈           | 대상 파일                                                                                 | 심각도 |
| ---- | -------------- | ----------------------------------------------------------------------------------------- | ------ |
| 1    | H1-B           | `WalkinRequestDto.java`, `ReceptionUpdateRequest.java`, `PhoneReservationRequestDto.java` | HIGH   |
| 2    | H5             | `ReceptionService.java` (N+1 쿼리)                                                        | HIGH   |
| 3    | H6             | `WalkinService.java` (예외 메시지)                                                        | HIGH   |
| 4    | M1             | `ReceptionService.java` (System.out.println)                                              | MEDIUM |
| 5    | M2             | `WalkinService.java` (예약번호 중복)                                                      | MEDIUM |
| 6    | M5             | `ReceptionService.java` (@Transactional)                                                  | MEDIUM |
| 7    | M3 (본인 담당) | `templates/staff/**`, `doctor/**`, `nurse/**` CSRF 점검                                   | MEDIUM |
| 8    | L2             | `staff-reception-detail.js`                                                               | LOW    |

#### 강상민 (개발자 C) — LOW 1건 + MEDIUM 1건

| 순위 | 이슈           | 대상 파일                      | 심각도 |
| ---- | -------------- | ------------------------------ | ------ |
| 1    | M3 (본인 담당) | `templates/admin/**` CSRF 점검 | MEDIUM |
| 2    | L1             | `admin-dashboard.js`           | LOW    |

---

---

# 재리뷰 보고서 — `doc/rules/` 규칙 준수 검토

**리뷰 일자:** 2026-03-12 (2차)
**리뷰 기준:** `doc/rules/` 규칙 문서 6종 (`rule-controller.md`, `rule-repository.md`, `rule_css.md`, `rule_javascript.md`, `rule_spring.md`, `rule_test.md`)
**목적:** 1차 리뷰(보안/성능/품질)에 이어, 프로젝트 코딩 규칙 준수 여부를 추가 검토

> 1차 리뷰에서 이미 지적된 이슈는 제외하고, **규칙 기반으로 새로 발견된 이슈만** 기재한다.

---

## 신규 이슈 요약

| 심각도   | 신규 건수 | 조치 기준         |
| -------- | --------- | ----------------- |
| HIGH     | 4         | 병합 전 수정 권장 |
| MEDIUM   | 9         | 가능한 빨리 수정  |
| LOW      | 4         | 개선 고려         |
| **합계** | **17**    |                   |

---

## 작업자별 신규 이슈 분류

### 강태오 (개발자 A) — 1건

| 이슈 | 심각도 | 파일                                        | 요약                                              |
| ---- | ------ | ------------------------------------------- | ------------------------------------------------- |
| R-H1 | HIGH   | `reservation/ReservationApiController.java` | API 응답에 `Resp.ok()` 미사용, raw List 직접 반환 |

### 조유지 (개발자 B) — 1건

| 이슈 | 심각도 | 파일                              | 요약                                                |
| ---- | ------ | --------------------------------- | --------------------------------------------------- |
| R-H2 | HIGH   | `staff/walkin/WalkinService.java` | 클래스 레벨 `@Transactional`에 `readOnly=true` 누락 |

### 강상민 (개발자 C) — 1건

| 이슈 | 심각도 | 파일                                               | 요약                                              |
| ---- | ------ | -------------------------------------------------- | ------------------------------------------------- |
| R-H3 | HIGH   | `admin/dashboard/AdminDashboardApiController.java` | API URL에 `/api/` prefix 없음 → SSR/API 경로 혼재 |

### 공통 (전체 담당자 확인) — 14건

| 이슈 | 심각도 | 파일                                               | 요약                                                            | 담당자         |
| ---- | ------ | -------------------------------------------------- | --------------------------------------------------------------- | -------------- |
| R-H4 | HIGH   | 여러 DTO 파일                                      | Java Record 미사용 (rule_spring.md 위반)                        | 강태오, 조유지 |
| R-M1 | MEDIUM | `admin/dashboard/AdminDashboardStatsService.java`  | Dashboard Service에서 다른 도메인 Repository 직접 참조          | 강상민         |
| R-M2 | MEDIUM | `admin/reservation/`, `admin/staff/`               | Repository가 admin 패키지에 배치됨 (소유 도메인 배치 규칙 위반) | 강상민         |
| R-M3 | MEDIUM | JS 파일 10개+                                      | JavaScript `var` 사용 금지 위반                                 | 전체           |
| R-M4 | MEDIUM | JS 파일 3개+                                       | `innerHTML` 사용 시 DOMPurify 미적용                            | 조유지, 강상민 |
| R-M5 | MEDIUM | `staff-index.js`                                   | `console.log` 잔존                                              | 조유지         |
| R-M6 | MEDIUM | 테스트 파일 2개+                                   | `@DisplayName` 한글 인코딩 깨짐                                 | 강상민         |
| R-M7 | MEDIUM | 전체 테스트 파일 10개                              | 테스트 클래스 레벨 `@DisplayName` 누락                          | 전체           |
| R-M8 | MEDIUM | `AdminDashboardStatsRepositoryTest.java`           | 테스트에서 `EntityManager` 직접 사용 (예외 문서화 없음)         | 강상민         |
| R-M9 | MEDIUM | `common/util/Resp.java`                            | `@Data` 사용으로 setter 노출, 불변성 위반                       | 김민구         |
| R-L1 | LOW    | `reservation/ReservationController.java`           | `request.getParameter()` 대신 `@RequestParam` 사용 권장         | 강태오         |
| R-L2 | LOW    | `admin/dashboard/AdminDashboardApiController.java` | `stats()` 반환 타입 `ResponseEntity<?>` → 타입 안전성 불일치    | 강상민         |
| R-L3 | LOW    | 3개 Java 파일                                      | 파일 끝 newline 누락                                            | 조유지, 강상민 |
| R-L4 | LOW    | `staff/reception/ReceptionService.java:36-80`      | 미구현 메서드 (`TODO` 주석만 존재)                              | 조유지         |

---

## HIGH — 병합 전 수정 권장

### R-H1. ReservationApiController: API 응답에 `Resp.ok()` 미사용 — `강태오`

- **파일:** `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationApiController.java:24-31`
- **위반 규칙:** `rule-controller.md` §3.4 — "API 성공 응답은 `Resp.ok(...)` 사용", §2.2 — "@RestController는 `ResponseEntity<?>` 반환"
- **내용:** `getDepartments()`와 `getDoctors()`가 `List<DepartmentDto>`와 `List<DoctorDto>`를 raw로 직접 반환한다. 프론트엔드 공통 응답 파싱 로직(`resp.body`로 접근)과 불일치한다.
- **수정 방안:**

  ```java
  @GetMapping("/departments")
  public ResponseEntity<Resp<List<DepartmentDto>>> getDepartments() {
      return Resp.ok(reservationService.getDepartments());
  }

  @GetMapping("/doctors")
  public ResponseEntity<Resp<List<DoctorDto>>> getDoctors(@RequestParam("departmentId") Long departmentId) {
      return Resp.ok(reservationService.getDoctorsByDepartment(departmentId));
  }
  ```

### R-H2. WalkinService: 클래스 레벨 `@Transactional`에 `readOnly=true` 누락 — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/staff/walkin/WalkinService.java:21`
- **위반 규칙:** `rule_spring.md` §5 — "클래스 레벨 `@Transactional(readOnly=true)`, 쓰기 메서드에만 개별 `@Transactional`"
- **내용:** `@Transactional`만 클래스 레벨에 선언되어 모든 메서드가 쓰기 트랜잭션으로 동작한다. 향후 조회 메서드가 추가되면 불필요한 dirty checking이 발생한다.
- **수정 방안:**

  ```java
  @Service
  @RequiredArgsConstructor
  @Transactional(readOnly = true)    // ← 변경
  public class WalkinService {

      @Transactional                   // ← 쓰기 메서드에만 개별 선언
      public void createWalkin(WalkinRequestDto request) { ... }
  }
  ```

### R-H3. AdminDashboardApiController: API URL에 `/api/` prefix 없음 — `강상민`

- **파일:** `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardApiController.java:13`
- **위반 규칙:** `rule-controller.md` §2 — "SSR과 API URL을 분리한다"
- **내용:** `@RestController`가 `/admin/dashboard/stats`, `/admin/dashboard/chart`를 처리하고, `@Controller`가 `/admin/dashboard`를 처리한다. 동일 prefix 아래 SSR과 API가 혼재하여 SecurityConfig 권한 규칙, CORS 설정, 프론트엔드 인터셉터 등에서 혼란이 발생할 수 있다.
- **수정 방안:** API 컨트롤러의 `@RequestMapping`을 `/api/admin/dashboard`로 변경한다.

### R-H4. DTO에 Java Record 미사용 — `강태오`, `조유지`

- **파일:**
  - `강태오`: `DepartmentDto.java`, `ReservationInfoDto.java`, `ReservationCompleteInfo.java`
  - `조유지`: `DoctorDto.java`, `ReceptionUpdateRequest.java`, `PhoneReservationRequestDto.java`, `WalkinRequestDto.java`
- **위반 규칙:** `rule_spring.md` §6 — "DTO·설정 값 객체는 Java Record를 우선 사용하여 불변성 보장"
- **내용:** Lombok `@Getter`/`@Data`를 사용한 일반 class로 작성되어 있다. 특히 `@Data`는 setter를 생성하여 불변성을 깨뜨린다. `admin/dashboard/dto/` 패키지의 DTO는 record를 올바르게 사용하고 있어 불일치가 존재한다.
- **수정 방안:** 응답 DTO는 `record`로 변환한다. 폼 바인딩 DTO(`ReservationCreateForm` 등)는 setter가 필요하므로 현재 형태 유지 가능하나, Request DTO도 가능하면 record로 변환한다.

---

## MEDIUM — 가능한 빨리 수정

### R-M1. AdminDashboardStatsService: Dashboard에서 다른 도메인 Repository 직접 참조 — `강상민`

- **파일:** `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsService.java:27-29`
- **위반 규칙:** `rule-repository.md` §2.1 — "dashboard는 조합/집계 서비스 계층으로만 사용하고 Repository를 두지 않는다", §3.2 — "다중 도메인 집계는 Service에서 조합"
- **내용:** `AdminDashboardStatsService`가 `AdminReservationRepository`, `AdminStaffRepository`, `ItemRepository`를 직접 주입받아 사용한다. 규칙상 dashboard Service는 다른 도메인 **Service**를 조합해야 한다.
- **수정 방안:** 각 도메인 Service(`AdminReservationService`, `AdminStaffService` 등)에 통계 메서드를 추가하고, `AdminDashboardStatsService`는 Service를 조합하도록 변경한다.

### R-M2. Repository가 admin 패키지에 배치됨 — `강상민`

- **파일:**
  - `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationRepository.java`
  - `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffRepository.java`
- **위반 규칙:** `rule-repository.md` §2.1 — "Repository는 화면 패키지가 아니라 해당 기능의 소유 도메인 모듈에 둔다"
- **내용:** `admin.reservation`, `admin.staff` 패키지에 Repository가 있으나, 규칙상 도메인 모듈 패키지에 위치해야 한다. 단, 현재 프로젝트 구조상 admin이 독립 도메인으로 기능하고 있어 팀 논의가 필요하다.
- **수정 방안:** 팀 합의 후 도메인 패키지로 이동하거나, 규칙 문서에 admin 패키지 예외를 명시한다.

### R-M3. JavaScript `var` 사용 금지 위반 — `전체`

- **위반 규칙:** `rule_javascript.md` §1.1 — "var는 절대 사용하지 않는다 (ES6 이후 완전 금지)"
- **해당 파일 및 담당자:**

| 파일                                         | 담당자 |
| -------------------------------------------- | ------ |
| `static/js/pages/admin-department-list.js`   | 강상민 |
| `static/js/pages/admin-dashboard.js`         | 강상민 |
| `static/js/pages/admin-rule-form.js`         | 강상민 |
| `static/js/pages/admin-staff-form.js`        | 강상민 |
| `static/js/pages/doctor-treatment-detail.js` | 조유지 |
| `static/js/pages/nurse-patient-detail.js`    | 조유지 |
| `static/js/pages/staff-dashboard.js`         | 조유지 |
| `static/js/pages/staff-index.js`             | 조유지 |
| `static/js/pages/item-manager-dashboard.js`  | 강상민 |
| `static/js/pages/item-manager-item-form.js`  | 강상민 |

- **수정 방안:** 모든 `var`를 `const`(재할당 없는 경우) 또는 `let`(재할당 있는 경우)으로 교체한다.

### R-M4. `innerHTML` 사용 시 DOMPurify 미적용 — `조유지`, `강상민`

- **위반 규칙:** `rule_javascript.md` §6 — "innerHTML 직접 사용 금지, 필요 시 DOMPurify 등으로 sanitization 필수"
- **해당 파일:**
  - `static/js/pages/admin-rule-form.js:6,8` — 강상민
  - `static/js/pages/doctor-treatment-detail.js:9` — 조유지
  - `static/js/pages/staff-reception-detail.js:3` — 조유지
- **내용:** 현재는 하드코딩된 HTML 문자열만 삽입하여 즉각적인 XSS 위험은 낮으나, 규칙 위반이다.
- **수정 방안:** `textContent` + DOM API로 교체하거나, DOMPurify를 통해 sanitize한다.

### R-M5. `console.log` 잔존 — `조유지`

- **파일:** `src/main/resources/static/js/pages/staff-index.js:24`
- **위반 규칙:** `rule_javascript.md` §4.6 — "console.\* 메서드는 개발 환경에서만 사용"
- **수정 방안:** 제거하거나 조건부 로깅으로 교체한다.

### R-M6. 테스트 `@DisplayName` 한글 인코딩 깨짐 — `강상민`

- **파일:**
  - `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationControllerTest.java:44,76`
  - `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationServiceTest.java:35,53,74`
- **위반 규칙:** `rule_test.md` §3.4 — "@DisplayName 필수: 의미 있는 한글 설명 작성"
- **내용:** `@DisplayName` 내용이 `"湲곕낯 ?뚮씪誘명꽣..."` 등 인코딩이 깨진 상태이다. 테스트 결과 리포트에서 테스트 목적을 알 수 없다.
- **수정 방안:** 파일 인코딩을 UTF-8로 확인하고, `@DisplayName`을 올바른 한국어로 재작성한다.

### R-M7. 테스트 클래스 레벨 `@DisplayName` 전체 누락 — `전체`

- **위반 규칙:** `rule_test.md` §3.6.4 — "@DisplayName 필수: 테스트 클래스와 메서드 모두에 의미 있는 한글 설명 작성"
- **해당 파일 (10개):**
  - `AdminDashboardApiControllerTest.java` — 강상민
  - `AdminDashboardControllerTest.java` — 강상민
  - `AdminDashboardStatsRepositoryTest.java` — 강상민
  - `AdminDashboardStatsServiceTest.java` — 강상민
  - `AdminReservationControllerTest.java` — 강상민
  - `AdminReservationServiceTest.java` — 강상민
  - `PatientRepositoryTest.java` — 강태오
  - `ReservationControllerTest.java` — 강태오
  - `ReservationRepositoryTest.java` — 강태오
  - `ReservationServiceTest.java` — 강태오
- **수정 방안:** 각 테스트 클래스에 `@DisplayName("관리자 대시보드 API 컨트롤러 테스트")` 등 한글 설명을 추가한다.

### R-M8. 테스트에서 `EntityManager` 직접 사용 — `강상민`

- **파일:** `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsRepositoryTest.java:40`
- **위반 규칙:** `rule-repository.md` §3.3 — "EntityManager 직접 사용 금지, 예외 시 문서화 + 리뷰 승인 필요"
- **내용:** 테스트 데이터 셋업 목적으로 `entityManager.persist()`를 사용하고 있다. 테스트 셋업에서의 사용은 일반적으로 허용되지만, 규칙 문서에 예외 조항이 명시되어 있지 않다.
- **수정 방안:** 규칙 문서에 "테스트 셋업용 EntityManager 사용은 허용"이라는 예외를 추가하거나, Repository의 `save()` 메서드를 사용한다.

### R-M9. Resp 클래스가 `@Data` 사용 → setter 노출 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/common/util/Resp.java:8`
- **위반 규칙:** `rule_spring.md` §6 — "DTO·설정 값 객체는 Java Record 우선 사용"
- **내용:** 공통 API 응답 래퍼 `Resp`가 Lombok `@Data`를 사용하여 `setStatus()`, `setMsg()`, `setBody()` setter가 노출된다. 응답 객체는 불변이어야 한다.
- **수정 방안:** `record`로 변환하거나, 최소한 `@Getter` + `final` 필드 + `@RequiredArgsConstructor`로 변경한다.

---

## LOW — 개선 고려

### R-L1. `request.getParameter()` 대신 `@RequestParam` 사용 권장 — `강태오`

- **파일:** `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationController.java:89-92`
- **내용:** `request.getParameter()`를 직접 호출하는 대신 `@RequestParam`을 사용하면 Spring의 타입 변환과 유효성 검사를 활용할 수 있다. `cancelPage`, `modifyPage`에서도 동일 패턴이 반복된다.

### R-L2. `stats()` 반환 타입 `ResponseEntity<?>` — `강상민`

- **파일:** `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardApiController.java:19`
- **내용:** `stats()` 메서드가 `ResponseEntity<?>`를 반환하고, `chart()`는 `ResponseEntity<Resp<AdminDashboardChartResponse>>`를 반환한다. 동일 컨트롤러 내에서 타입 안전성이 불일치한다.
- **수정 방안:** `ResponseEntity<Resp<AdminDashboardStatsResponse>>`로 명시한다.

### R-L3. 파일 끝 newline 누락 — `조유지`, `강상민`

- **파일:**
  - `AdminDashboardChartResponse.java` — 강상민
  - `PhoneReservationController.java` — 조유지
  - `WalkinService.java` — 조유지

### R-L4. 미구현 메서드가 프로덕션 코드에 포함 — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/staff/reception/ReceptionService.java:36-80`
- **내용:** 1차 리뷰의 M1(System.out.println)과 관련되나, 해당 메서드가 사실상 미구현 상태(`TODO` 주석만 존재)임을 별도로 기록한다. 미구현 상태로 배포 시 데이터 유실 가능성이 있다.
- **수정 방안:** 구현을 완료하거나, `throw new UnsupportedOperationException("미구현")`으로 명시한다.

---

## 재리뷰 작업자별 수정 우선순위

### 김민구 (책임개발자) — 1건

| 순위 | 이슈 | 대상 파일               | 심각도 |
| ---- | ---- | ----------------------- | ------ |
| 1    | R-M9 | `common/util/Resp.java` | MEDIUM |

### 강태오 (개발자 A) — 4건

| 순위 | 이슈             | 대상 파일                                                                       | 심각도 |
| ---- | ---------------- | ------------------------------------------------------------------------------- | ------ |
| 1    | R-H1             | `ReservationApiController.java`                                                 | HIGH   |
| 2    | R-H4 (본인 담당) | `DepartmentDto.java`, `ReservationInfoDto.java`, `ReservationCompleteInfo.java` | HIGH   |
| 3    | R-M7 (본인 담당) | 테스트 클래스 4개 `@DisplayName` 추가                                           | MEDIUM |
| 4    | R-L1             | `ReservationController.java`                                                    | LOW    |

### 조유지 (개발자 B) — 7건

| 순위 | 이슈             | 대상 파일                                                                                                   | 심각도 |
| ---- | ---------------- | ----------------------------------------------------------------------------------------------------------- | ------ |
| 1    | R-H2             | `WalkinService.java`                                                                                        | HIGH   |
| 2    | R-H4 (본인 담당) | `DoctorDto.java`, `ReceptionUpdateRequest.java`, `PhoneReservationRequestDto.java`, `WalkinRequestDto.java` | HIGH   |
| 3    | R-M3 (본인 담당) | JS 파일 4개 `var` → `const`/`let`                                                                           | MEDIUM |
| 4    | R-M4 (본인 담당) | `doctor-treatment-detail.js`, `staff-reception-detail.js`                                                   | MEDIUM |
| 5    | R-M5             | `staff-index.js`                                                                                            | MEDIUM |
| 6    | R-L3 (본인 담당) | `PhoneReservationController.java`, `WalkinService.java`                                                     | LOW    |
| 7    | R-L4             | `ReceptionService.java`                                                                                     | LOW    |

### 강상민 (개발자 C) — 9건

| 순위 | 이슈             | 대상 파일                                                                 | 심각도 |
| ---- | ---------------- | ------------------------------------------------------------------------- | ------ |
| 1    | R-H3             | `AdminDashboardApiController.java`                                        | HIGH   |
| 2    | R-M1             | `AdminDashboardStatsService.java`                                         | MEDIUM |
| 3    | R-M2             | `AdminReservationRepository.java`, `AdminStaffRepository.java`            | MEDIUM |
| 4    | R-M3 (본인 담당) | JS 파일 6개 `var` → `const`/`let`                                         | MEDIUM |
| 5    | R-M4 (본인 담당) | `admin-rule-form.js`                                                      | MEDIUM |
| 6    | R-M6             | `AdminReservationControllerTest.java`, `AdminReservationServiceTest.java` | MEDIUM |
| 7    | R-M7 (본인 담당) | 테스트 클래스 6개 `@DisplayName` 추가                                     | MEDIUM |
| 8    | R-M8             | `AdminDashboardStatsRepositoryTest.java`                                  | MEDIUM |
| 9    | R-L2             | `AdminDashboardApiController.java`                                        | LOW    |

---

## 전체 이슈 현황 (1차 + 재리뷰 통합)

| 심각도   | 1차 리뷰 | 재리뷰 | 합계   |
| -------- | -------- | ------ | ------ |
| CRITICAL | 3        | 0      | **3**  |
| HIGH     | 7        | 4      | **11** |
| MEDIUM   | 6        | 9      | **15** |
| LOW      | 3        | 4      | **7**  |
| **합계** | **19**   | **17** | **36** |

## 최종 판정: **REQUEST CHANGES (변경 요청)**

CRITICAL 3건 + HIGH 11건을 수정한 후 재리뷰를 요청한다.
