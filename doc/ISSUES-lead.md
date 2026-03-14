# ISSUES-lead.md — 리드 담당 미해결 이슈

> 대상 범위: 공통 인프라, Security, 아키텍처, `/staff/**`
> 우선순위: HIGH → MEDIUM 순

---

## HIGH

### LEAD-H-001 · `LayoutModelInterceptor` — AnonymousAuthenticationToken NPE 위험
- **파일**: `src/main/java/com/smartclinic/hms/common/interceptor/LayoutModelInterceptor.java`
- **문제**: `authentication.getPrincipal()` 호출 시 미인증 사용자는 `AnonymousAuthenticationToken`이 들어와 캐스팅 실패 또는 NPE 발생 가능
- **수정**: `instanceof` 체크 또는 `authentication == null || !authentication.isAuthenticated()` 가드 추가 후 분기 처리

### LEAD-H-002 · `SecurityConfig` — `/sample/**` 허용 경로 제거
- **파일**: `src/main/java/com/smartclinic/hms/common/config/SecurityConfig.java`
- **문제**: `.requestMatchers("/sample/**").permitAll()` 은 개발 잔재이며 프로덕션에서 열려 있으면 안 됨
- **수정**: 해당 라인 삭제

### LEAD-H-003 · `RateLimitFilter` — 동시성 unsafe
- **파일**: `src/main/java/com/smartclinic/hms/common/filter/RateLimitFilter.java`
- **문제**: 내부 카운터가 단순 `HashMap` 또는 비원자 연산으로 관리될 경우 멀티스레드 환경에서 race condition 발생
- **수정**: `ConcurrentHashMap<String, AtomicInteger>` + `AtomicInteger.incrementAndGet()` 사용

### LEAD-H-004 · Repository 소유권 위반 — `PatientRepository` / `DepartmentRepository` 위치
- **파일**:
  - `src/main/java/com/smartclinic/hms/reservation/reservation/PatientRepository.java`
  - `src/main/java/com/smartclinic/hms/reservation/reservation/DepartmentRepository.java`
- **문제**: `PatientRepository`는 예약 패키지 소유가 아니며, `DepartmentRepository`도 단일 기능 모듈 소유여야 함. 여러 패키지에서 직접 참조되고 있음 (`WalkinService`, `StaffReservationService` 등)
- **수정**: 각 도메인 공유 Repository는 `domain` 또는 명확한 단일 모듈(예: `patient/`, `department/`)로 이동하고, 다른 모듈은 해당 모듈의 Service를 통해서만 접근

### LEAD-H-005 · `AdminReservationRepository` — `ReservationRepository`와 중복
- **파일**: `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationRepository.java`
- **문제**: `ReservationRepository`와 동일한 엔티티(`Reservation`)를 대상으로 하는 Repository가 두 개 존재. rule-repository.md: "같은 엔티티에 대한 Repository는 하나만 허용"
- **수정**: `AdminReservationRepository`를 제거하고 기존 `ReservationRepository`에 필요한 쿼리 메서드를 추가한 뒤 `AdminReservationService`에서 참조

### LEAD-H-006 · `StaffMypageController` — Repository 직접 주입
- **파일**: `src/main/java/com/smartclinic/hms/staff/mypage/StaffMypageController.java:17`
- **문제**: `StaffRepository`를 Controller에서 직접 주입하여 사용. rule_spring.md 위반
- **수정**: `StaffMypageService` 생성 → `getMypage(String username): StaffMypageDto` 메서드 구현 → Controller는 Service만 호출

---

## MEDIUM

### LEAD-M-001 · `application.properties` — `spring.profiles.active` 하드코딩
- **파일**: `src/main/resources/application.properties`
- **문제**: `spring.profiles.active=local` 등이 고정되어 있으면 CI/배포 환경에서 오작동
- **수정**: 해당 설정 제거 또는 환경변수 `${SPRING_PROFILES_ACTIVE:local}`로 교체

### LEAD-M-002 · `item` 패키지 — Controller/Service 등록 누락 확인
- **파일**: `src/main/java/com/smartclinic/hms/item/` (또는 유사 경로)
- **문제**: 물품 관련 Staff용 `ItemManagerService`, `ItemManagerMypageController` 등이 분리 패키지에 존재하며 일부 Controller가 Repository를 직접 호출할 가능성
- **수정**: 실제 파일 위치 확인 후 Service 레이어 분리 여부 점검. Repository 직접 주입이 있으면 Service 생성하여 위임

### LEAD-M-003 · `NursePatientRepository` / `NurseReservationRepository` / `DoctorReservationRepository` 소유권
- **파일**:
  - `src/main/java/com/smartclinic/hms/nurse/NursePatientRepository.java`
  - `src/main/java/com/smartclinic/hms/nurse/NurseReservationRepository.java`
  - `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorReservationRepository.java`
- **문제**: `Patient`, `Reservation` 엔티티를 대상으로 하는 Repository가 nurse/doctor 패키지에 중복 정의. 동일 엔티티 Repository 복수 금지 원칙 위반
- **수정**: 기능별 쿼리 메서드를 기존 단일 `ReservationRepository` / `PatientRepository`에 통합. 단, 소유권 이전 시 DevB와 조율 필요

---

_최종 수정일: 2026-03-14_
