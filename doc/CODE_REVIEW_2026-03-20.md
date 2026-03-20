# 코드 리뷰 보고서

**리뷰 대상:** `feature/reservation` 브랜치 전체 코드베이스
**리뷰 일자:** 2026-03-20
**리뷰 기준:** `doc/rules/` 규칙 문서 6종 + 아키텍처/보안/성능 종합
**총 이슈:** 28건 (CRITICAL 4건 / HIGH 10건 / MEDIUM 9건 / LOW 5건)

---

## 팀 구성 및 코드 소유권

| 역할 | 이름 | 담당 영역 |
|------|------|----------|
| 책임개발자 (Lead) | 김민구 | `config/**`, `common/**`, `domain/**`, `llm/**`, `application*.properties`, `templates/common/**` |
| 개발자 A | 강태오 | `reservation/**`, `home/**`, `templates/reservation/**`, `templates/home/**` |
| 개발자 B | 조유지 | `staff/**`, `doctor/**`, `nurse/**`, `templates/staff/**`, `templates/doctor/**`, `templates/nurse/**` |
| 개발자 C | 강상민 | `admin/**`, `item/**`, `templates/admin/**` |

---

## 심각도 요약

| 심각도 | 건수 | 상태 | 조치 기준 |
|--------|------|------|----------|
| CRITICAL | 4 | 수정 필수 | 병합 전 반드시 수정 |
| HIGH | 10 | 수정 필요 | 병합 전 수정 권장 |
| MEDIUM | 9 | 수정 권장 | 가능한 빨리 수정 |
| LOW | 5 | 참고 | 일정 여유 시 수정 |

---

## Part 1. 규칙 준수 점검 (doc/rules/ 6종)

### 1.1 rule-controller.md 준수 점검

| # | 규칙 | 준수 | 위반 내용 | 담당자 |
|---|------|------|----------|-------|
| 1 | SSR Controller는 String(뷰 경로) 반환 | O | 모든 SSR Controller 준수 | - |
| 2 | API Controller는 ResponseEntity + Resp.ok() 사용 | **X** | `AdminItemController`에서 `Resp.ok()` 미사용, `Map.of()` 직접 반환 | **강상민** |
| 3 | Controller는 Service만 호출 (Repository 직접 호출 금지) | **X** | `ChatController.java:61-68`에서 `chatbotHistoryRepository` 직접 호출 | **김민구** |
| 4 | DTO 네이밍: Request/Response 규칙 | **X** | `ReservationCreateForm`, `ReservationUpdateForm` — Form 접미사 사용. `CreateReservationRequest`, `ModifyReservationRequest`여야 함 | **강태오** |
| 5 | 비즈니스 로직이 Controller에 없을 것 | O | 전 영역 준수 확인 | - |
| 6 | 예외 응답은 GlobalExceptionHandler 사용 | **X** | `SsrExceptionHandler`가 `CustomException` 메시지 무시 — 일괄 `error/500` 반환 | **김민구** |

### 1.2 rule-repository.md 준수 점검

| # | 규칙 | 준수 | 위반 내용 | 담당자 |
|---|------|------|----------|-------|
| 1 | Repository는 소유 기능 모듈 패키지에 배치 | O | 각 모듈별 Repository가 해당 패키지에 위치 | - |
| 2 | JpaRepository 인터페이스 기반 작성 | O | 모든 Repository가 JpaRepository 상속 | - |
| 3 | 쿼리 우선순위: 파생 메서드 > Projection > @Query | **X** | `AdminReservationRepository`, `AdminStaffRepository`에서 nativeQuery 다수 사용. JPQL로 대체 가능 | **강상민** |
| 4 | 다중 도메인 집계는 Service 조합 처리 | O | `AdminDashboardStatsService`에서 Service 조합 — 준수 | - |
| 5 | 동일 엔티티 중복 Repository 방지 | **X** | `Reservation` 4개, `Patient` 3개, `Staff` 2개, `Department` 2개 Repository 중복 존재 | **김민구** (아키텍처) |
| 6 | EntityManager 직접 사용 금지 | O | 모든 Repository가 인터페이스 기반 — 준수 | - |

### 1.3 rule_spring.md 준수 점검

| # | 규칙 | 준수 | 위반 내용 | 담당자 |
|---|------|------|----------|-------|
| 1 | 환경 변수로 민감 정보 관리 | O | API 키는 `${CLAUDE_API_KEY:}` 환경변수 사용 | - |
| 2 | 전역 에러 핸들러 ErrorResponse 포맷 통일 | **X** | `Resp`(status, msg, body)와 `ErrorResponse`(errorCode, timestamp, path ...) 두 포맷 공존 | **김민구** |
| 3 | 비즈니스 예외는 CustomException 사용 | **X** | `Reservation.java:130`에서 `IllegalStateException` 직접 사용 | **김민구** |
| 4 | Service 클래스 레벨 @Transactional(readOnly=true) | O | 모든 Service 일관 적용 | - |
| 5 | DTO는 Java Record 우선 | O | 대부분의 DTO가 Record로 선언 | - |
| 6 | @Valid로 요청 검증 | **X** | `WalkinRequestDto.date` 필드에 `@NotNull` 누락 | **조유지** |
| 7 | 페이징은 Pageable 기본 사용 | O | 전 영역 준수 | - |

### 1.4 rule_test.md 준수 점검

| # | 규칙 | 준수 | 위반 내용 | 담당자 |
|---|------|------|----------|-------|
| 1 | 핵심 로직에 대한 테스트 존재 | **X** | `LlmReservationService`, `SymptomAnalysisService` 서비스 단위 테스트 전무 | **김민구** |
| 2 | LocalDateTime.now() 직접 사용 금지 (Clock 추상화) | **X** | `AdminDashboardStatsService`, `DoctorTreatmentService`, `NurseService`, `ReceptionService` | **강상민** / **조유지** |
| 3 | Given-When-Then 주석 필수 | O | 기존 테스트 모두 준수 | - |
| 4 | BDDMockito given().willReturn() 사용 | O | 전 테스트 파일 준수 | - |
| 5 | @DisplayName 필수 | O | 전 테스트 파일 준수 | - |
| 6 | TestSecurityConfig 중복 방지 | **X** | 10개 WebMvcTest 클래스에 거의 동일한 TestSecurityConfig 내부 클래스 반복 선언 | **김민구** (공통) |

### 1.5 rule_css.md 준수 점검

| # | 규칙 | 준수 | 위반 내용 | 담당자 |
|---|------|------|----------|-------|
| 1 | BEM 네이밍 규칙 | O | `common.css`에서 BEM 패턴 사용 | - |
| 2 | CSS 변수 사용 | O | `:root`에 111개 CSS 변수 정의 후 사용 | - |
| 3 | id 셀렉터 사용 금지 | O | CSS에서 id 셀렉터 미사용 확인 | - |
| 4 | 재사용 가능한 단위로 분리 | **X** | `admin-style.css` = `style-admin.css` 완전 동일 중복. `doctor-style.css` = `style-doctor.css` 동일. 역할별 CSS 7개 파일 내용 동일 | **강상민** / **조유지** |
| 5 | 인라인 스타일 금지 | O | Mustache 템플릿에서 인라인 style 미사용 | - |

### 1.6 rule_javascript.md 준수 점검

| # | 규칙 | 준수 | 위반 내용 | 담당자 |
|---|------|------|----------|-------|
| 1 | var 사용 금지 | **X** | `staff-reception-detail.js`, `doctor-treatment-detail.js` 등 10개 이상 파일에서 `var` 사용 | **조유지** / **강상민** |
| 2 | const 우선 사용 | O | 대부분 const/let 사용 | - |
| 3 | async/await 기본 (then/catch 지양) | O | 전체적으로 async/await 사용 준수 | - |
| 4 | fetch 응답 에러 처리 (response.ok 검사) | O | 주요 fetch 호출에서 response.ok 검사 | - |
| 5 | 전역 변수 금지 | **X** | `data-admin.js`, `data-staff.js`, `data-nurse.js` 등에서 전역 함수 중복 정의 | **전원** |
| 6 | innerHTML 직접 사용 금지 (XSS) | **X** | `admin-rule-form.js:6-7`, `staff-reception-detail.js:3`, `doctor-treatment-detail.js:9`에서 innerHTML 사용 | **강상민** / **조유지** |

---

## Part 2. 이슈 상세 및 담당자 배정

### CRITICAL (4건) — 병합 전 반드시 수정

| ID | 이슈 | 위반 규칙 | 위치 | 담당자 |
|----|------|----------|------|-------|
| C-01 | **tools.jackson 비표준 import** — Spring Boot 번들 Jackson과 충돌 시 런타임 `ClassNotFoundException` 발생 가능. 직렬화 실패 시 조용히 `"{}"` 반환하여 추적 불가 | rule_spring (1) | `llm/service/MedicalService.java:19-20` | **김민구** |
| C-02 | **GlobalExceptionHandler SSR 예외 처리 공백** — `@RestControllerAdvice(annotations=RestController.class)`로 선언되어 `@Controller` 계층의 `CustomException`이 공통 핸들러를 거치지 않고 Spring 기본 에러 페이지로 이동 | rule_spring (2), rule-controller (6) | `common/exception/GlobalExceptionHandler.java:1` | **김민구** |
| C-03 | **innerHTML XSS 취약점** — 사용자 입력이 직접 DOM에 삽입될 경우 XSS 공격 가능. 현재는 고정 문자열이나 패턴 확산 시 위험 | rule_javascript (6) | `static/js/admin-rule-form.js:6-7`, `staff-reception-detail.js:3`, `doctor-treatment-detail.js:9` | **강상민** / **조유지** |
| C-04 | **ChatController/MedicalController 히스토리 소유권 검증 없음** — 인증된 직원이 타인의 `staffId`를 URL에 입력하여 챗봇/의료 상담 기록 열람 가능. 민감한 의료 정보 노출 위험 | rule_spring (10) | `llm/controller/ChatController.java:61-68`, `MedicalController.java:87-94` | **김민구** |

### HIGH (10건) — 병합 전 수정 권장

| ID | 이슈 | 위반 규칙 | 위치 | 담당자 |
|----|------|----------|------|-------|
| H-01 | **Reservation.cancel() 상태 전이 결함** — `RECEIVED` 상태에서 `cancel()` 호출 시 `CANCELLED`가 아닌 `RESERVED`로 복원. 비회원 취소 흐름에서도 동일 메서드 사용하여 예상치 못한 동작 발생. `IN_TREATMENT` 상태 처리 누락 | rule_spring (5) | `domain/Reservation.java:122-143` | **김민구** |
| H-02 | **동일 Entity에 다중 Repository 분산 정의** — `Reservation` 4개, `Patient` 3개, `Staff` 2개, `Department` 2개. 쿼리 중복, 유지보수 부담, `countByReservationDate` 중복 정의 | rule-repository (5) | `reservation/`, `admin/`, `doctor/`, `nurse/` 패키지 전체 | **김민구** (아키텍처) |
| H-03 | **resolveStaffId() 완전 중복** — `ChatController`, `MedicalController` 양쪽에 완전히 동일한 메서드 구현. 인증 로직 변경 시 두 곳 모두 수정 필요 | rule_spring (5) | `llm/controller/ChatController.java:71-78`, `MedicalController.java:101-108` | **김민구** |
| H-04 | **DTO 네이밍 불일치** — `ReservationCreateForm`, `ReservationUpdateForm`이 Form 접미사 사용. 규칙상 `Request` 접미사 필요 | rule-controller (4) | `reservation/reservation/dto/ReservationCreateForm.java`, `ReservationUpdateForm.java` | **강태오** |
| H-05 | **AdminItemController Resp.ok() 미사용** — API Controller에서 `Map.of()` 직접 반환. ResponseEntity + Resp.ok() 규칙 미준수 | rule-controller (2) | `admin/item/AdminItemController.java` | **강상민** |
| H-06 | **LlmReservationService N+1 쿼리** — 슬롯 루프 내에서 매 슬롯마다 `countByDoctor_IdAndReservationDateAndStartTime()` DB 카운트 쿼리 수행. 7일 × 슬롯 수만큼 쿼리 발생 | rule_spring (8) | `llm/service/LlmReservationService.java:51-53` | **김민구** |
| H-07 | **LLM Entity에만 @Setter 전체 노출** — `MedicalHistory`, `DoctorSchedule`, `MedicalContent`, `MedicalQa`, `MedicalDomain`에 `@Setter` 적용. 다른 Entity는 `@Getter`만 + 상태 변경 메서드 패턴인데 LLM Entity만 불일치. `createdAt` 임의 변경 가능 | rule_spring (5) | `domain/MedicalHistory.java:17`, `DoctorSchedule.java`, `MedicalContent.java` 외 | **김민구** |
| H-08 | **ReceptionService 대시보드 테스트 더미 데이터 하드코딩** — 서비스 레이어에 테스트용 샘플 데이터. 운영 환경에서 빈 데이터 시 가짜 통계 표시 | rule_spring (5) | `staff/reception/ReceptionService.java:224-229` | **조유지** |
| H-09 | **중복 체크 비효율 (ReceptionService, WalkinService)** — `ReservationService`는 `existsBy...()` 쿼리로 DB 직접 중복 체크하는 반면, 두 서비스는 당일 전체 예약 목록 로드 후 Java 스트림 필터링 | rule_spring (8) | `staff/reception/ReceptionService.java:77-83`, `staff/walkin/WalkinService.java` | **조유지** |
| H-10 | **역할별 CSS 중복 파일** — `admin-style.css` = `style-admin.css`, `doctor-style.css` = `style-doctor.css` 내용 100% 동일 중복 | rule_css (4) | `static/css/admin-style.css`, `style-admin.css`, `doctor-style.css`, `style-doctor.css` 외 | **강상민** / **조유지** |

### MEDIUM (9건) — 가능한 빨리 수정

| ID | 이슈 | 위반 규칙 | 위치 | 담당자 |
|----|------|----------|------|-------|
| M-01 | **Resp vs ErrorResponse 응답 포맷 불일치** — `Resp`(status, msg, body) vs `ErrorResponse`(errorCode, timestamp, path, traceId) 두 포맷 공존 | rule_spring (2), rule-controller (2) | `common/util/Resp.java`, `common/exception/GlobalExceptionHandler.java` | **김민구** |
| M-02 | **SsrExceptionHandler HTTP 상태 코드 불일치** — 404/403/409 모두 `error/500` 뷰 + HTTP 500 응답. `CustomException`의 `httpStatus` 미반영 | rule_spring (2) | `common/exception/SsrExceptionHandler.java:19-24` | **김민구** |
| M-03 | **CreateReservationRequest @Pattern 검증 결함** — `@NotBlank`와 `@Pattern` 동시 적용 시 `^$|` (빈 문자열 허용) 분기가 도달 불가. 혼란스러운 규칙 조합 | rule_spring (6) | `reservation/reservation/dto/CreateReservationRequest.java:15` | **강태오** |
| M-04 | **WalkinRequestDto.date @NotNull 누락** — 다른 필드는 모두 `@NotBlank`/`@NotNull` 적용. `null` 입력 시 서비스 레이어 NPE 발생 | rule_spring (6) | `staff/walkin/dto/WalkinRequestDto.java:26-27` | **조유지** |
| M-05 | **LocalDateTime.now() 직접 호출 — Clock 추상화 미적용** — 시간 의존 테스트 불가능. 결정적 테스트 작성 불가 | rule_test (2) | `admin/dashboard/AdminDashboardStatsService.java`, `doctor/treatment/DoctorTreatmentService.java`, `staff/reception/ReceptionService.java` 외 | **강상민** / **조유지** |
| M-06 | **TestSecurityConfig 10중 중복** — 10개 WebMvcTest 클래스에 거의 동일한 `TestSecurityConfig` 내부 클래스 반복 선언. 공통 추출 필요 | rule_test (6) | `src/test/` 전체 | **김민구** (공통) |
| M-07 | **LlmReservationService/SymptomAnalysisService 서비스 단위 테스트 전무** — 핵심 LLM 서비스 로직 테스트 없음 | rule_test (1) | `src/test/java/.../llm/` | **김민구** |
| M-08 | **var 키워드 사용** — `staff-reception-detail.js`, `doctor-treatment-detail.js` 등 10개 이상 파일에서 `var` 사용 | rule_javascript (1) | `static/js/` 전체 | **조유지** / **강상민** |
| M-09 | **_sample 패키지 운영 빌드 포함** — `SampleReservation` 엔티티 DDL이 실행됨. 테스트 소스셋 이동 또는 `@Profile("dev")` 제한 필요 | rule_spring (1) | `_sample/` 패키지 전체 | **김민구** |

### LOW (5건) — 참고

| ID | 이슈 | 위치 | 담당자 |
|----|------|------|-------|
| L-01 | **doctor.availableDays 문자열 저장 — 정규화 없음** — "MON, TUE" 같은 공백 포함 입력 시 조용히 null 필터링. `DoctorAvailableDay` 연관 테이블로 정규화 권장 | `domain/Doctor.java:31-76` | **김민구** |
| L-02 | **LayoutModelInterceptor 26개 boolean 플래그** — `path.contains()` 방식 혼용으로 부정확한 매칭 가능. `isStaffPhone`이 다른 역할 경로에서도 매칭 가능 | `common/interceptor/LayoutModelInterceptor.java:71-103` | **김민구** |
| L-03 | **StaffRole enum에서 역할 분기 switch문 중복** — `SecurityConfig`, `LayoutModelInterceptor` 양쪽에 동일한 역할 switch 반복. enum에 `getDashboardUrl()` 메서드 추가로 제거 가능 | `config/SecurityConfig.java`, `common/interceptor/LayoutModelInterceptor.java` | **김민구** |
| L-04 | **전역 JS 함수 중복 정의** — `data-admin.js`, `data-staff.js`, `data-nurse.js`에서 전역 함수 `getReservations` 등 중복 정의. 대부분 죽은 코드 | `static/js/data-*.js` | **전원** |
| L-05 | **AdminReservationRepository nativeQuery 사용** — JPQL 또는 파생 메서드로 대체 가능한 쿼리에 nativeQuery 사용. DB 벤더 의존성 증가 | `admin/reservation/AdminReservationRepository.java` | **강상민** |

---

## Part 3. 담당자별 수정 사항 요약

### 김민구 (책임개발자/Lead) — 13건

| 우선순위 | ID | 수정 사항 |
|---------|-----|----------|
| CRITICAL | C-01 | `MedicalService.java` tools.jackson → com.fasterxml.jackson 경로 수정, 직렬화 실패 예외 전파 |
| CRITICAL | C-02 | `GlobalExceptionHandler`를 `@ControllerAdvice`로 변경하여 SSR Controller 예외 포괄 처리 |
| CRITICAL | C-04 | `ChatController`, `MedicalController` 히스토리 조회에 현재 인증 사용자 소유권 검증 추가 |
| HIGH | H-01 | `Reservation.cancel()` 상태 전이 명확화 — `cancelFully()` / `rollbackReception()` 분리 |
| HIGH | H-02 | 동일 Entity 다중 Repository 통합 — 단일 Repository + Service 레이어 권한 검증 |
| HIGH | H-03 | `resolveStaffId()` SecurityUtils 공통 유틸 추출 또는 `@AuthenticationPrincipal` 전환 |
| HIGH | H-06 | `LlmReservationService` 슬롯 루프 N+1 → 7일 예약 한 번 조회 후 인메모리 그룹핑 |
| HIGH | H-07 | LLM Entity `@Setter` 제거 — `@NoArgsConstructor(PROTECTED)` + static factory 패턴 통일 |
| MEDIUM | M-01 | `Resp` vs `ErrorResponse` 포맷 통일 — `errorCode`, `timestamp`, `path` 별도 필드 분리 |
| MEDIUM | M-02 | `SsrExceptionHandler`에서 `CustomException.httpStatus` 기반 HTTP 상태 코드 분기 |
| MEDIUM | M-06 | `TestSecurityConfig` 공통 추출 — `test/common/` 패키지에 공유 설정 클래스 작성 |
| MEDIUM | M-07 | `LlmReservationService`, `SymptomAnalysisService` 단위 테스트 작성 |
| MEDIUM | M-09 | `_sample/` 패키지 `@Profile("dev")` 또는 테스트 소스셋 이동 |

### 강태오 (개발자 A) — 2건

| 우선순위 | ID | 수정 사항 |
|---------|-----|----------|
| HIGH | H-04 | `ReservationCreateForm` → `CreateReservationRequest`, `ReservationUpdateForm` → `ModifyReservationRequest` 네이밍 변경 |
| MEDIUM | M-03 | `CreateReservationRequest` phone 필드 `@Pattern`에서 `^$|` 제거 — `@NotBlank`와 일관성 유지 |

### 조유지 (개발자 B) — 6건

| 우선순위 | ID | 수정 사항 |
|---------|-----|----------|
| CRITICAL | C-03 | `admin-rule-form.js`, `staff-reception-detail.js` innerHTML → `textContent` 또는 DOM API 교체 |
| HIGH | H-08 | `ReceptionService.java:224-229` 테스트 더미 데이터 제거 또는 `@Profile("dev")` 제한 |
| HIGH | H-09 | `ReceptionService`, `WalkinService` 중복 체크를 `existsBy...()` 쿼리로 교체 |
| MEDIUM | M-04 | `WalkinRequestDto.date` 필드에 `@NotNull` 추가 |
| MEDIUM | M-05 | `DoctorTreatmentService`, `NurseService`, `ReceptionService`에 `Clock` 주입 |
| MEDIUM | M-08 | `staff-reception-detail.js` 등 담당 JS 파일 `var` → `const`/`let` 변환 |

### 강상민 (개발자 C) — 5건

| 우선순위 | ID | 수정 사항 |
|---------|-----|----------|
| CRITICAL | C-03 | `admin-rule-form.js` innerHTML → `textContent` 또는 DOM API 교체 |
| HIGH | H-05 | `AdminItemController` API 응답을 `ResponseEntity<Resp>` + `Resp.ok()` 방식으로 수정 |
| HIGH | H-10 | `admin-style.css`, `style-admin.css` 등 중복 CSS 파일 통합 삭제 |
| MEDIUM | M-05 | `AdminDashboardStatsService`에 `Clock` 주입 |
| MEDIUM | M-08 | `doctor-treatment-detail.js` 등 담당 JS 파일 `var` → `const`/`let` 변환 |
| LOW | L-05 | `AdminReservationRepository` nativeQuery → JPQL 전환 |

### 전원 공동 — 1건

| 우선순위 | ID | 수정 사항 |
|---------|-----|----------|
| LOW | L-04 | `data-admin.js`, `data-staff.js`, `data-nurse.js` 등 죽은 코드 파일 각자 담당 영역 정리 후 삭제 |

---

## Part 4. 아키텍처 강점 (유지 사항)

- **3계층 구조** (Controller → Service → Repository) 전 영역 일관 적용
- **SecurityConfig** 체계적 구성 — CSP, HSTS, Referrer-Policy, Permissions-Policy, Rate Limiting 다층 설정
- **도메인 엔티티 캡슐화** — `Reservation`의 `receive()`, `complete()`, `cancel()` 상태 전이 메서드, static factory 패턴
- **예약 중복 방지** — `PESSIMISTIC_WRITE` 락 + `DataIntegrityViolationException` 이중 방어
- **@Transactional(readOnly=true)** — 클래스 레벨 기본 설정, 쓰기 메서드만 별도 선언. 24개 Service 전체 준수
- **테스트 패턴** — Given-When-Then + BDDMockito + @DisplayName. 33개 테스트 파일 전체 일관 적용
- **OPEN-IN-VIEW 비활성화** — `spring.jpa.open-in-view=false` 올바른 설정
- **세션 쿠키** — `http-only`, `same-site=Lax` 설정
- **사용자 열거 방지** — 로그인 실패 시 일관된 에러 메시지
- **admin-dashboard.js** — IIFE + async/await + response.ok + try-catch-finally 모범 패턴
- **common.css CSS 변수 시스템** — `:root` 기반 111개 변수로 디자인 토큰 체계 완성

---

## Part 5. 수정 우선순위 일정표

### P0 — 즉시 수정 (CRITICAL 4건)

| 담당자 | 작업 | 예상 노력 |
|-------|------|----------|
| 김민구 | C-01 tools.jackson import 수정 | 낮음 (30분) |
| 김민구 | C-02 GlobalExceptionHandler SSR 포괄 처리 | 낮음 (1시간) |
| 김민구 | C-04 히스토리 소유권 검증 추가 | 중간 (2시간) |
| 강상민/조유지 | C-03 innerHTML → textContent/DOM API 교체 | 낮음 (각 30분) |

### P1 — 단기 (HIGH 10건)

| 담당자 | 작업 | 예상 노력 |
|-------|------|----------|
| 김민구 | H-01 Reservation.cancel() 분리 | 중간 |
| 김민구 | H-02 다중 Repository 통합 | 높음 (전체 아키텍처 영향) |
| 김민구 | H-03 resolveStaffId() 공통화 | 낮음 (1시간) |
| 김민구 | H-06 N+1 쿼리 개선 | 중간 |
| 김민구 | H-07 LLM Entity @Setter 제거 | 중간 |
| 강태오 | H-04 DTO 네이밍 변경 | 낮음 |
| 강상민 | H-05 AdminItemController Resp.ok() 적용 | 낮음 |
| 강상민/조유지 | H-10 중복 CSS 파일 통합 삭제 | 낮음 |
| 조유지 | H-08 더미 데이터 제거 | 낮음 |
| 조유지 | H-09 중복 체크 쿼리 교체 | 낮음 |

### P2 — 중기 (MEDIUM 9건)

| 담당자 | 작업 |
|-------|------|
| 김민구 | M-01 응답 포맷 통일, M-02 SsrExceptionHandler 상태코드, M-06 TestSecurityConfig 공통화, M-07 LLM 서비스 테스트, M-09 _sample 정리 |
| 강태오 | M-03 @Pattern 검증 수정 |
| 조유지 | M-04 @NotNull 추가, M-05 Clock 주입, M-08 var → const/let |
| 강상민 | M-05 Clock 주입, M-08 var → const/let |
