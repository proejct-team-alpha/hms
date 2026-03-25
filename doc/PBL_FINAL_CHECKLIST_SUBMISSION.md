# PBL 프로젝트 최종 제출용 점검서

- 작성일: 2026-03-25
- 프로젝트명: HMS (Hospital Management System)
- 기준 문서: 사용자 제공 `PBL 프로젝트 최소 요구사항 정의서`
- 점검 기준: 현재 로컬 프로젝트 코드, 설정 파일, 테스트 실행 결과
- 최종 판정: 제출 가능 (설명 보완 권장)

---

## 1. 한눈에 보기

| 번호 | 요구사항 | 판정 | 요약 |
| --- | --- | --- | --- |
| 1 | REST API + Ajax | 충족 | REST 스타일 JSON API와 `fetch` 기반 비동기 호출 구현 확인 |
| 2 | DB 테이블 및 연관관계 | 충족 | 도메인 엔티티 15개, `@ManyToOne`, `@OneToOne` 등 JPA 연관관계 다수 확인 |
| 3 | 인증 및 인가 | 부분 충족 | 로그인과 역할 분리는 구현, 계정 생성은 관리자 직원 등록 방식으로 운영 |
| 4 | Redis 세션 저장소 | 충족 | `spring-session-data-redis` 적용, 운영 프로필에서 Redis 세션 저장소 활성화 |
| 5 | Mustache SSR | 충족 | Mustache starter 사용, 템플릿 74개, SSR 컨트롤러 다수 구현 |
| 6 | 예외처리 및 유효성검증 | 충족 | `@ControllerAdvice`, `@RestControllerAdvice`, `@Valid + BindingResult` 구현 |
| 7 | 페이징 및 정렬 | 충족 | `Pageable`, `Page<T>`, `PageRequest` 기반 목록 조회 다수 구현 |

---

## 2. 요구사항별 상세 점검

### 2-1. REST API

- 판정: 충족
- 확인 근거:
  - `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationApiController.java`
    - `GET /api/reservation/departments`
    - `GET /api/reservation/doctors`
    - `POST /api/reservation/create`
    - `GET /api/reservation/booked-slots`
  - `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardApiController.java`
    - `GET /admin/dashboard/stats`
    - `GET /admin/dashboard/chart`
  - `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffApiController.java`
    - `POST /admin/api/staff/{id}`
  - `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientApiController.java`
    - `POST /admin/api/patients/{id}/update`
- 코드 검색 결과:
  - `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` 어노테이션 총 141건 확인
- 판단:
  - 현재 프로젝트는 JSON 응답 기반 API를 충분히 제공하고 있으며, REST API 사용 요구사항을 충족한다.
  - 일부 관리자 기능은 엄격한 REST 메서드 분리보다 실무형 `POST` 중심으로 구현되어 있으나, 과제 최소 요구사항 충족 여부에는 문제가 없다.

### 2-2. Ajax 호출

- 판정: 충족
- 확인 근거:
  - `src/main/resources/static/js/pages/admin-dashboard.js`
    - `fetch('/admin/dashboard/chart', { method: 'GET' })`
  - 예약 및 LLM 관련 화면에서도 `fetch` 기반 비동기 호출 구조 사용 확인
- 판단:
  - 프론트엔드에서 REST API를 Ajax 방식으로 호출하는 요구사항은 충족한다.

### 2-3. DB 테이블 및 연관관계

- 판정: 충족
- 확인 근거:
  - `src/main/java/com/smartclinic/hms/domain` 아래 `@Entity` 기준 도메인 엔티티 15개 확인
- 대표 엔티티:
  - `Patient`
  - `Reservation`
  - `Staff`
  - `Doctor`
  - `Department`
  - `TreatmentRecord`
  - `HospitalRule`
  - `Item`
- 대표 연관관계:
  - `src/main/java/com/smartclinic/hms/domain/Reservation.java`
    - `@ManyToOne Patient`
    - `@ManyToOne Doctor`
    - `@ManyToOne Department`
  - `src/main/java/com/smartclinic/hms/domain/Doctor.java`
    - `@OneToOne Staff`
    - `@ManyToOne Department`
  - `src/main/java/com/smartclinic/hms/domain/TreatmentRecord.java`
    - `@OneToOne Reservation`
    - `@ManyToOne Doctor`
- 판단:
  - 최소 4개 이상의 엔티티와 JPA 연관관계 요구를 충분히 초과 충족한다.

### 2-4. 인증 및 인가

- 판정: 부분 충족
- 충족 근거:
  - `src/main/java/com/smartclinic/hms/auth/AuthController.java`
    - `GET /login` 로그인 화면 제공
  - `src/main/java/com/smartclinic/hms/auth/CustomUserDetailsService.java`
    - `StaffRepository.findByUsernameAndActiveTrue(...)` 기반 사용자 조회
  - `src/main/java/com/smartclinic/hms/config/SecurityConfig.java`
    - 역할 기반 접근 제어 구현
    - `ADMIN`, `DOCTOR`, `NURSE`, `STAFF`, `ITEM_MANAGER` 역할 사용
  - `src/main/java/com/smartclinic/hms/domain/StaffRole.java`
    - 5개 역할 정의 확인
- 애매한 지점:
  - `/signup`, `/register`, `/join` 같은 공개 회원가입 엔드포인트는 확인되지 않음
  - 대신 `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`에서 관리자용 직원 등록(`/admin/staff/create`) 기능이 존재함
- 판단:
  - 로그인과 역할 분리 자체는 명확히 구현되어 있다.
  - 다만 요구사항의 "회원가입"을 공개 자기 등록으로 엄격하게 해석할 경우에는 부분 충족으로 보는 것이 안전하다.
  - 제출 시에는 "본 시스템은 내부 직원 계정을 관리자 등록 방식으로 생성한다"는 운영 정책 설명을 함께 첨부하는 것이 적절하다.

### 2-5. Redis 세션 저장소

- 판정: 충족
- 확인 근거:
  - `build.gradle`
    - `org.springframework.boot:spring-boot-starter-data-redis` 존재
    - `org.springframework.session:spring-session-data-redis` 추가 확인
  - `src/main/resources/application.properties`
    - `spring.data.redis.repositories.enabled=false`
    - `spring.config.activate.on-profile=prod`
    - `spring.session.store-type=redis`
    - `spring.session.redis.namespace=hms:session`
    - `server.servlet.session.timeout=30m`
  - `src/main/resources/application-prod.properties.example`
    - 운영 예시 설정에도 동일한 Redis 세션 항목 반영
  - `Dockerfile`
    - `-Dspring.profiles.active=prod`로 운영 프로필 실행
  - `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationCompleteInfo.java`
    - `Serializable` 적용으로 세션/플래시 직렬화 호환성 보완
- 판단:
  - 현재 프로젝트는 운영 프로필 기준으로 Spring Session Redis 구성을 완료했다.
  - 개발 프로필(`dev`)은 H2 기반 로컬 개발 환경을 사용하고, 운영 프로필(`prod`)에서 Redis 세션 저장소를 사용하도록 분리한 구조다.
  - 따라서 Redis 세션 저장소 요구사항은 충족으로 판단한다.

### 2-6. Mustache SSR

- 판정: 충족
- 확인 근거:
  - `build.gradle`
    - `org.springframework.boot:spring-boot-starter-mustache` 사용
  - `src/main/resources/templates` 아래 `.mustache` 파일 74개 확인
  - 대표 SSR 컨트롤러:
    - `src/main/java/com/smartclinic/hms/auth/AuthController.java`
      - `return "auth/login"`
    - `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationController.java`
      - 예약 화면, 완료 화면, 조회 화면 등 SSR 반환
- 판단:
  - Mustache 기반 Server Side Rendering 요구사항은 충분히 충족한다.

### 2-7. 예외처리 및 유효성검증

- 판정: 충족
- 확인 근거:
  - 글로벌 예외 처리:
    - `src/main/java/com/smartclinic/hms/common/exception/GlobalExceptionHandler.java`
      - `@RestControllerAdvice`
    - `src/main/java/com/smartclinic/hms/common/exception/SsrExceptionHandler.java`
      - `@ControllerAdvice`
  - 서버 사이드 유효성 검증:
    - `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentController.java`
      - `@Valid @ModelAttribute ... BindingResult`
    - `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
      - `@Valid @ModelAttribute ... BindingResult`
    - `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationController.java`
      - `@Valid @ModelAttribute ... BindingResult`
    - `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientApiController.java`
      - `@Valid @RequestBody`
    - `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationApiController.java`
      - `@Valid @RequestBody`
- 판단:
  - 요구사항의 `@ControllerAdvice` 및 `@Valid + BindingResult` 조건은 구현되어 있다.

### 2-8. 페이징 및 정렬

- 판정: 충족
- 확인 근거:
  - `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffRepository.java`
    - `Page<AdminStaffListProjection> findStaffListPage(..., Pageable pageable)`
  - `src/main/java/com/smartclinic/hms/admin/patient/AdminPatientRepository.java`
    - `Page<Patient> search(..., Pageable pageable)`
  - `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentRepository.java`
    - `Page<Department> findAllByOrderByIdDesc(Pageable pageable)`
  - `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentService.java`
    - `Page<DoctorReservationDto>` 반환
    - `PageRequest.of(...)` 사용
  - `src/main/java/com/smartclinic/hms/llm/controller/ChatController.java`
    - `Pageable` 사용
- 판단:
  - 목록 조회에 대한 페이징 및 정렬 요구사항은 명확히 충족한다.

---

## 3. 현재 기준 최종 판정

현재 프로젝트는 필수 요구사항 중 다음 항목을 충족한다.

- REST API + Ajax
- 4개 이상 엔티티 및 JPA 연관관계
- Redis 세션 저장소
- Mustache SSR
- 글로벌 예외처리 및 유효성검증
- 페이징 및 정렬

인증 및 인가 또한 로그인과 역할 분리 측면에서는 구현이 완료되어 있다.
다만 계정 생성 방식이 공개 회원가입이 아니라 관리자 직원 등록 방식이므로, 제출 시 이 운영 정책을 설명하는 것이 안전하다.

따라서 현재 HMS 프로젝트의 최종 판정은 다음과 같이 정리할 수 있다.

> 기술 구현 기준으로는 PBL 최소 요구사항을 대부분 충족하며 제출 가능하다.  
> 단, "회원가입" 항목은 시스템 성격상 관리자 등록 방식으로 운영된다는 설명을 함께 제출하는 것이 바람직하다.

---

## 4. 제출 시 함께 설명하면 좋은 항목

### 4-1. 계정 생성 방식

본 프로젝트는 일반 공개 회원가입 방식이 아니라, 내부 직원 계정을 관리자가 등록하는 방식으로 운영된다.

- 로그인 주체: 내부 직원 (`STAFF`, `DOCTOR`, `NURSE`, `ADMIN`, `ITEM_MANAGER`)
- 계정 생성 주체: 관리자
- 관련 기능:
  - `/login`
  - `/admin/staff/create`

따라서 인증 및 인가 요구사항은 내부 업무 시스템 관점에서 충족한다고 설명할 수 있다.

### 4-2. 개발 환경과 운영 환경 분리

현재 프로젝트는 개발 환경과 운영 환경을 다음과 같이 분리하여 구성한다.

- 개발 환경(`dev`)
  - H2 사용
  - 빠른 로컬 개발 및 테스트 목적
- 운영 환경(`prod`)
  - MySQL 사용
  - Redis 세션 저장소 사용

즉, H2를 사용한다고 해서 Redis 세션 저장소 요구사항이 미충족인 것은 아니며, 운영 프로필에서 Redis 세션이 활성화되는 구조로 구현되어 있다.

### 4-3. 프레임워크 버전

- `build.gradle` 기준 현재 프로젝트는 Spring Boot `4.0.3`, Java `21` 사용
- 일부 수업 문서가 Spring Boot `3.x`를 기준으로 작성되었을 가능성이 있으므로, 버전 제한이 엄격한지 확인이 필요할 수 있다.

기능 구현 자체와는 별개로, 제출 평가 기준이 특정 버전을 요구하는 경우에는 간단한 설명을 덧붙이는 것이 좋다.

---

## 5. 최종 검증

- 실행 일시: 2026-03-25
- 실행 명령: `./gradlew clean test`
- 결과: 성공
- 비고:
  - 컴파일, 테스트, 리소스 처리까지 정상 완료
  - 실행 중 deprecated API 관련 경고는 있었으나 테스트는 정상 통과함

---

## 6. 제출용 한 줄 결론

현재 HMS 프로젝트는 PBL 최소 요구사항을 기술 구현 기준으로 충족하며 제출 가능하고, `회원가입` 항목은 관리자 직원 등록 방식으로 운영된다는 설명을 함께 첨부하는 것이 가장 안전하다.
