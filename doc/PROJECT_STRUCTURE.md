# HMS 프로젝트 구조 분석서

> **작성일:** 2026-03-03
> **기준 커밋:** main 브랜치 최신

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프로젝트명** | HMS (Hospital Management System) |
| **프레임워크** | Spring Boot 4.0.3 |
| **Java 버전** | 21 |
| **빌드 도구** | Gradle |
| **뷰 엔진** | Mustache (SSR) |
| **DB** | H2 in-memory (dev) / MySQL (prod) |
| **인증** | Spring Security 세션 기반 |
| **LLM** | Claude API (RestClient) |
| **루트 패키지** | `com.smartclinic.hms` |

---

## 2. 디렉터리 트리

```
hospital-reservation-system/
├── doc/                                    # 프로젝트 문서
│   ├── API.md                              # API 명세서 v5.0 (75 엔드포인트)
│   ├── PRE_WORK.md                         # 선행 작업 체크리스트
│   ├── RULE.md                             # 코딩 규칙 요약
│   └── rules/
│       ├── rule_spring.md                  # Spring 백엔드 규칙
│       ├── rule_test.md                    # 테스트 규칙
│       ├── rule_javascript.md              # JS 프론트엔드 규칙
│       └── rule_css.md                     # CSS 스타일 규칙
│
├── src/main/java/com/smartclinic/hms/
│   ├── HmsApplication.java                 # Spring Boot 진입점
│   │
│   ├── config/                             # ── 설정 ──
│   │   ├── SecurityConfig.java             # Spring Security (역할별 URL, 폼 로그인, 세션)
│   │   ├── WebMvcConfig.java               # MVC 인터셉터 등록
│   │   ├── ClaudeApiConfig.java            # Claude API RestClient 빈
│   │   └── ErrorPageController.java        # 403/404 에러 페이지
│   │
│   ├── common/                             # ── 공통 모듈 ──
│   │   ├── exception/
│   │   │   ├── CustomException.java        # 도메인 예외 (팩토리 메서드)
│   │   │   └── GlobalExceptionHandler.java # @ControllerAdvice 전역 예외 처리
│   │   ├── interceptor/
│   │   │   └── LayoutModelInterceptor.java # 공통 모델 자동 주입 (postHandle)
│   │   ├── service/                        # (미구현) SlotService, ReservationValidationService
│   │   └── util/
│   │       └── ReservationNumberGenerator.java # 예약번호 채번 (RES-YYYYMMDD-NNN)
│   │
│   ├── domain/                             # ── 도메인 엔티티 ──
│   │   ├── Patient.java                    # 비회원 환자
│   │   ├── Department.java                 # 진료과
│   │   ├── Staff.java                      # 내부 직원 (로그인 계정)
│   │   ├── StaffRole.java                  # enum: ADMIN, DOCTOR, NURSE, STAFF
│   │   ├── Doctor.java                     # 의사 상세 (Staff 1:1, available_days)
│   │   ├── Reservation.java               # 예약 (상태 전이 메서드 포함)
│   │   ├── ReservationStatus.java          # enum: RESERVED → RECEIVED → COMPLETED / CANCELLED
│   │   ├── ReservationSource.java          # enum: ONLINE, PHONE, WALKIN
│   │   ├── TreatmentRecord.java            # 진료 기록 (Reservation 1:1)
│   │   ├── Item.java                       # 물품
│   │   ├── ItemCategory.java               # enum: MEDICAL_SUPPLIES, MEDICAL_EQUIPMENT, GENERAL_SUPPLIES
│   │   ├── HospitalRule.java               # 병원 규칙
│   │   ├── HospitalRuleCategory.java       # enum: EMERGENCY, SUPPLY, DUTY, HYGIENE, OTHER
│   │   ├── LlmRecommendation.java          # LLM 증상 추천 이력
│   │   └── ChatbotHistory.java             # 챗봇 대화 이력
│   │
│   ├── auth/                               # ── 인증 ──
│   │   └── AuthController.java             # GET /login (로그인 화면)
│   │
│   ├── admin/                              # ── 관리자 모듈 ── (ROLE_ADMIN)
│   │   └── dashboard/
│   │       └── AdminDashboardController.java
│   │
│   ├── staff/                              # ── 접수 직원 모듈 ── (ROLE_STAFF)
│   │   └── dashboard/
│   │       └── StaffDashboardController.java
│   │
│   ├── doctor/                             # ── 의사 모듈 ── (ROLE_DOCTOR)
│   │   └── DoctorDashboardController.java
│   │
│   ├── nurse/                              # ── 간호사 모듈 ── (ROLE_NURSE)
│   │   └── NurseDashboardController.java
│   │
│   ├── reservation/                        # ── 비회원 예약 모듈 ── (placeholder)
│   ├── llm/                                # ── LLM 서비스 모듈 ── (placeholder)
│   │
│   └── _sample/                            # ── 샘플 참고 코드 ──
│       ├── SampleReservation.java          # Entity 패턴 가이드
│       ├── SampleReservationController.java # Controller 패턴 가이드
│       ├── SampleReservationService.java   # Service 패턴 가이드
│       ├── SampleReservationRepository.java # Repository 패턴 가이드
│       ├── SampleBusinessException.java    # 비즈니스 예외 패턴
│       └── dto/
│           ├── SampleReservationCreateRequest.java
│           └── SampleReservationResponse.java
│
├── src/main/resources/
│   ├── application.properties              # 공통 설정 (프로필, Mustache, 파일업로드)
│   ├── application-dev.properties          # 개발 (H2, DEBUG 로깅, Claude API)
│   ├── application-prod.properties.example # 운영 템플릿 (MySQL, Redis)
│   ├── sql_test.sql                        # H2 초기 테스트 데이터
│   ├── static/
│   │   ├── css/common.css                  # 공통 스타일 (CSS 변수, 레이아웃)
│   │   ├── js/                             # (placeholder)
│   │   └── images/                         # (placeholder)
│   └── templates/
│       ├── auth/login.mustache             # 로그인 화면
│       ├── error/403.mustache              # 접근 권한 오류
│       ├── error/404.mustache              # 페이지 없음
│       ├── common/                         # 공통 파셜
│       │   ├── header-public.mustache      # 비회원 헤더
│       │   ├── header-login.mustache       # 로그인 페이지 헤더
│       │   ├── header-staff.mustache       # 내부 직원 헤더
│       │   ├── footer-public.mustache
│       │   ├── footer-staff.mustache
│       │   ├── sidebar-admin.mustache
│       │   ├── sidebar-doctor.mustache
│       │   ├── sidebar-nurse.mustache
│       │   └── sidebar-staff.mustache
│       ├── admin/dashboard.mustache        # 관리자 대시보드
│       ├── staff/dashboard.mustache        # 접수 대시보드
│       ├── doctor/dashboard.mustache       # 의사 대시보드
│       ├── nurse/dashboard.mustache        # 간호사 대시보드
│       ├── home/                           # (placeholder)
│       ├── reservation/                    # (placeholder)
│       └── layouts/                        # (placeholder)
│
├── build.gradle                            # Gradle 빌드 설정
├── settings.gradle                         # 프로젝트명: hms
├── .env.example                            # 환경 변수 템플릿
├── run-dev.ps1                             # 개발 실행 스크립트 (PowerShell)
└── README.md
```

---

## 3. 의존성 (build.gradle)

| 분류 | 라이브러리 | 용도 |
|------|-----------|------|
| Starter | spring-boot-starter-data-jpa | JPA/Hibernate ORM |
| Starter | spring-boot-starter-mustache | 서버 사이드 렌더링 |
| Starter | spring-boot-starter-security | 인증·인가 |
| Starter | spring-boot-starter-validation | Bean Validation (@Valid) |
| Starter | spring-boot-starter-web | Spring MVC |
| Third-party | com.auth0:java-jwt:4.3.0 | JWT (선택적 사용) |
| Runtime | com.h2database:h2 | H2 인메모리 DB |
| Runtime | spring-boot-h2console | H2 웹 콘솔 |
| Compile-only | org.projectlombok:lombok | 보일러플레이트 제거 |
| Test | spring-boot-starter-test | JUnit5 + MockMvc |
| Test | spring-security-test | Security 테스트 |
| Test | spring-restdocs-mockmvc | REST Docs |

---

## 4. 도메인 엔티티 관계도 (ERD 요약)

```
Department (진료과)
  │
  ├── 1:N ── Staff (직원)     ──── StaffRole: ADMIN | DOCTOR | NURSE | STAFF
  │            │
  │            └── 1:1 ── Doctor (의사 상세)
  │                          │  available_days, specialty
  │                          │
  │                          ├── 1:N ── Reservation (예약)
  │                          │            │  reservationNumber, date, timeSlot
  │                          │            │  status: RESERVED → RECEIVED → COMPLETED / CANCELLED
  │                          │            │  source: ONLINE | PHONE | WALKIN
  │                          │            │
  │                          │            ├── N:1 ── Patient (환자)
  │                          │            │            name, phone, email
  │                          │            │
  │                          │            └── 1:1 ── TreatmentRecord (진료기록)
  │                          │                         diagnosis, prescription, remark
  │                          │
  │                          └── 1:N ── TreatmentRecord
  │
  └── 1:N ── Doctor

Item (물품)                    ──── ItemCategory: MEDICAL_SUPPLIES | MEDICAL_EQUIPMENT | GENERAL_SUPPLIES
HospitalRule (병원규칙)        ──── HospitalRuleCategory: EMERGENCY | SUPPLY | DUTY | HYGIENE | OTHER
LlmRecommendation (LLM 추천)  ──── symptomText, recommendedDept/Doctor/Time, isUsed
ChatbotHistory (챗봇 이력)     ──── sessionId, Staff(FK), question, answer
```

---

## 5. 인증·인가 구조

### 5.1 인증 방식

- **세션 기반** (Spring Security 기본) — JWT 미사용
- **InMemoryUserDetailsManager** (현재) — Staff 엔티티 기반 StaffUserDetailsService로 교체 예정
- **BCrypt** 패스워드 인코더

### 5.2 역할별 URL 접근 권한

| URL 패턴 | 접근 권한 | 설명 |
|----------|----------|------|
| `/`, `/reservation/**` | 전체 공개 | 비회원 메인·예약 |
| `/llm/symptom/**` | 전체 공개 | LLM 증상 분석 |
| `/login`, `/logout` | 전체 공개 | 인증 |
| `/staff/**` | STAFF, ADMIN | 접수 직원 |
| `/doctor/**` | DOCTOR, ADMIN | 의사 |
| `/nurse/**` | NURSE, ADMIN | 간호사 |
| `/admin/**` | ADMIN | 관리자 전용 |
| `/llm/rules/**` | DOCTOR, NURSE | 규칙 챗봇 |
| 기타 | authenticated | 인증 필요 |

### 5.3 로그인 성공 리다이렉트

| 역할 | 대시보드 URL |
|------|-------------|
| ROLE_ADMIN | `/admin/dashboard` |
| ROLE_DOCTOR | `/doctor/dashboard` |
| ROLE_NURSE | `/nurse/dashboard` |
| ROLE_STAFF | `/staff/dashboard` |

### 5.4 테스트 계정 (sql_test.sql)

| 아이디 | 비밀번호 | 역할 | 사번 |
|--------|---------|------|------|
| admin01 | password123 | ADMIN | A-20260101 |
| staff01 | password123 | STAFF | S-20260101 |
| doctor01 | password123 | DOCTOR | D-20260101 |
| nurse01 | password123 | NURSE | N-20260101 |

---

## 6. LayoutModelInterceptor — 공통 모델 변수

`postHandle`에서 모든 뷰에 자동 주입되는 변수:

| 변수명 | 타입 | 설명 |
|--------|------|------|
| `pageTitle` | String | 페이지 제목 (컨트롤러에서 설정, 기본값 빈 문자열) |
| `currentPath` | String | 현재 요청 URI |
| `loginName` | String | 로그인한 사용자 이름 (미로그인 시 null) |
| `isAdmin` | boolean | ROLE_ADMIN 여부 |
| `isDoctor` | boolean | ROLE_DOCTOR 여부 |
| `isNurse` | boolean | ROLE_NURSE 여부 |
| `isStaff` | boolean | ROLE_STAFF 여부 |
| `showChatbot` | boolean | 챗봇 표시 (DOCTOR 또는 NURSE) |
| `dashboardUrl` | String | 역할별 대시보드 경로 |

**제외 경로:** `/css/**`, `/js/**`, `/images/**`, `/favicon.ico`, `/error`, `/error/**`, `/h2-console/**`

---

## 7. 예외 처리 구조

### 7.1 CustomException 팩토리 메서드

| 메서드 | HTTP 상태 | 용도 |
|--------|----------|------|
| `notFound(msg)` | 404 | 리소스 없음 |
| `badRequest(code, msg)` | 400 | 잘못된 요청 |
| `unauthorized(msg)` | 401 | 인증 실패 |
| `forbidden(msg)` | 403 | 권한 없음 |
| `conflict(code, msg)` | 409 | 중복·충돌 |
| `invalidStatusTransition(msg)` | 409 | 상태 전이 오류 |
| `serviceUnavailable(msg)` | 503 | LLM 서비스 장애 |

### 7.2 에러 코드 목록

```
DUPLICATE_RESERVATION, INVALID_TIME_SLOT, DOCTOR_NOT_AVAILABLE,
RESERVATION_NOT_FOUND, CANNOT_CANCEL_COMPLETED, INVALID_STATUS_TRANSITION,
ALREADY_CANCELLED, ALREADY_COMPLETED, UNAUTHORIZED, ACCESS_DENIED,
NOT_OWN_PATIENT, LLM_SERVICE_UNAVAILABLE, LLM_PARSE_ERROR,
RESOURCE_NOT_FOUND, DUPLICATE_USERNAME, VALIDATION_ERROR
```

---

## 8. 설정 파일 요약

### application.properties (공통)

| 설정 | 값 |
|------|-----|
| `spring.profiles.active` | dev |
| `spring.mustache.servlet.expose-request-attributes` | true |
| `spring.mustache.servlet.allow-request-override` | true |
| `spring.jpa.open-in-view` | false |
| 파일 업로드 제한 | 파일 10MB / 요청 20MB |

### application-dev.properties

| 설정 | 값 |
|------|-----|
| DB | `jdbc:h2:mem:hmsdb;MODE=MySQL` |
| DDL | `create-drop` |
| 테스트 데이터 | `sql_test.sql` (자동 실행) |
| Claude API | `claude-sonnet-4-6`, 5초 타임아웃 |
| 로깅 | Security DEBUG, Hibernate SQL DEBUG |

---

## 9. Mustache 템플릿 레이아웃 패턴

### 비회원 화면 (L1)
```
header-public → main → footer-public
```

### 로그인 화면 (L3)
```
header-login → main.login-main → (footer 없음)
```

### 내부 직원 화면 (L2)
```
header-staff → div.layout-body { sidebar-{role} + main.main-content } → footer-staff
```

### CSRF 토큰 사용법
```html
<input type="hidden" name="{{_csrf.parameterName}}" value="{{_csrf.token}}">
```

---

## 10. 컨트롤러 반환 패턴 (API 명세서 v5.0 §1.5)

| 유형 | 패턴 | 반환값 |
|------|------|--------|
| **GET 화면** | `request.setAttribute(key, val)` | `"역할/자원/액션"` (뷰 경로) |
| **POST 성공** | `RedirectAttributes.addFlashAttribute` | `"redirect:/다음화면"` (PRG 패턴) |
| **POST 실패** | `request.setAttribute("errorCode", ...)` | 원래 폼 뷰 경로 (재렌더링) |
| **AJAX** | `@ResponseBody` | `{ success, data, message }` JSON |

---

## 11. 현재 구현 상태

### 구현 완료

| 구분 | 파일/기능 | 비고 |
|------|----------|------|
| config | SecurityConfig, WebMvcConfig, ClaudeApiConfig, ErrorPageController | |
| common | CustomException, GlobalExceptionHandler, LayoutModelInterceptor, ReservationNumberGenerator | |
| domain | 10개 엔티티 + 5개 enum | Patient, Staff, Doctor, Department, Reservation, TreatmentRecord, Item, HospitalRule, LlmRecommendation, ChatbotHistory |
| auth | AuthController (GET /login) | |
| dashboard | Admin, Staff, Doctor, Nurse 각 대시보드 컨트롤러 + mustache | |
| templates | login, 403/404, 공통 header/footer/sidebar (9개), 대시보드 4개 | |
| 설정 | application.properties, application-dev.properties, sql_test.sql | |
| 참고 | _sample 패키지 (Entity/Controller/Service/Repository/DTO 패턴 가이드) | |

### 미구현 (placeholder)

| 구분 | 필요 항목 | API 명세서 섹션 |
|------|----------|----------------|
| **Repository** | StaffRepository, DoctorRepository, ReservationRepository, PatientRepository 등 | 전체 |
| **common/service** | SlotService (30분 슬롯 생성, 의사 진료요일 검증) | §1.8 |
| **common/service** | ReservationValidationService (중복 예약 검증) | §3, §5 |
| **auth** | StaffUserDetailsService (DB 기반 인증, InMemoryUserDetailsManager 교체) | §2 |
| **reservation** | 비회원 예약 CRUD (ReservationController, ReservationService) | §3 |
| **staff** | 접수 처리 (ReceptionController/Service), 예약 등록, 당일 접수 | §5 |
| **doctor** | 진료 완료, 진료 기록 작성 (TreatmentController/Service) | §6 |
| **nurse** | 환자 정보 조회/수정, 스케줄 확인 | §7 |
| **admin** | 예약/환자/직원/진료과/물품/규칙 관리, 대시보드 통계 | §9~§14 |
| **llm** | 증상 분석 (POST /llm/symptom/analyze) | §4 |
| **llm** | 규칙 챗봇 (POST /llm/rules/ask, GET /llm/rules/history) | §8 |
| **api** | JSON API 레이어 (/api/**) | §15 |
| **templates** | 각 모듈별 목록/상세/등록/수정 화면 | §3~§14 |

---

## 12. _sample 패키지 — 개발 가이드

실제 구현 시 참고할 패턴이 `_sample` 패키지에 정리되어 있음:

| 파일 | 핵심 패턴 |
|------|----------|
| SampleReservation.java | `@NoArgsConstructor(PROTECTED)`, 정적 팩토리 `create()`, 상태 전이 도메인 메서드, `@PrePersist`/`@PreUpdate` |
| SampleReservationController.java | GET→SSR, POST→PRG redirect, AJAX→`@ResponseBody`, 페이징, `@Validated` |
| SampleReservationService.java | `@Transactional(readOnly=true)` 클래스 레벨, 쓰기 메서드만 `@Transactional`, `IllegalStateException` → `CustomException` 변환 |
| SampleReservationRepository.java | 파생 쿼리, JPQL, 네이티브 SQL, `@Modifying` 벌크 업데이트 |
| DTO (Record) | `@Valid` 검증, 정적 팩토리 `from(Entity)` |

---

## 13. 개발 환경 실행

```bash
# 1. 환경 변수 설정
cp .env.example .env
# .env 파일에 CLAUDE_API_KEY 입력

# 2. 실행 (PowerShell)
./run-dev.ps1

# 또는 직접 실행
./gradlew bootRun

# 3. 접속
# 메인:       http://localhost:8080
# 로그인:     http://localhost:8080/login
# H2 콘솔:   http://localhost:8080/h2-console (URL: jdbc:h2:mem:hmsdb, user: sa)
```
