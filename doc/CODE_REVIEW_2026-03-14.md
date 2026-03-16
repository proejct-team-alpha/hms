# 코드 리뷰 보고서

**리뷰 대상:** `homm` 브랜치 전체 코드베이스 규칙 준수 검토
**리뷰 일자:** 2026-03-14
**리뷰 기준:** `doc/rules/` 규칙 문서 6종 (`rule-controller.md`, `rule-repository.md`, `rule_css.md`, `rule_javascript.md`, `rule_spring.md`, `rule_test.md`)
**총 이슈:** 27건 (CRITICAL 6건 → 전원 수정 완료 / HIGH 14건 / MEDIUM 9건)

---

## 팀 구성 및 코드 소유권

| 역할              | 이름   | 담당 영역                                                                                                   |
| ----------------- | ------ | ----------------------------------------------------------------------------------------------------------- |
| 책임개발자 (Lead) | 김민구 | `config/**`, `common/**`, `domain/**`, `llm/**`, `application*.properties`, `templates/common/**`           |
| 개발자 A          | 강태오 | `reservation/**`, `home/**`, `templates/reservation/**`, `templates/home/**`                                |
| 개발자 B          | 조유지 | `staff/**`, `doctor/**`, `nurse/**`, `templates/staff/**`, `templates/doctor/**`, `templates/nurse/**`      |
| 개발자 C          | 강상민 | `admin/**`, `item/**`, `templates/admin/**`                                                                 |

---

## 심각도 요약

| 심각도   | 건수   | 상태        | 조치 기준           |
| -------- | ------ | ----------- | ------------------- |
| CRITICAL | 6      | ✅ 전원 수정 | 병합 전 반드시 수정 |
| HIGH     | 14     | ⚠️ 수정 필요 | 병합 전 수정 권장   |
| MEDIUM   | 9      | ⚠️ 수정 필요 | 가능한 빨리 수정    |
| LOW      | —      | —           | —                   |

---

## ✅ CRITICAL — 수정 완료 (6건)

이번 리뷰 세션에서 아래 6건이 즉시 수정되었다.

| 이슈 | 파일                                                  | 내용                                            | 수정 내용                                       |
| ---- | ----------------------------------------------------- | ----------------------------------------------- | ----------------------------------------------- |
| C1   | `admin/staff/AdminStaffController.java`               | Controller → Repository 직접 주입               | `AdminStaffService` 신규 생성 후 위임           |
| C2   | `admin/rule/AdminRuleController.java`                 | Controller → Repository 직접 주입               | `AdminRuleService` 신규 생성 후 위임            |
| C3   | `admin/department/AdminDepartmentController.java`     | Controller → Repository 직접 주입               | `AdminDepartmentService` 신규 생성 후 위임      |
| C4   | `admin/item/AdminItemController.java`                 | Controller → Repository 직접 주입               | `AdminItemService` 신규 생성 후 위임            |
| C5   | `staff/walkin/WalkinService.java`                     | `orElseThrow()` 메시지 없이 호출                | `CustomException.notFound("...")` 로 교체       |
| C6   | `staff/walkin/WalkinService.java`                     | `"WALKIN-" + System.currentTimeMillis()` 중복위험 | `ReservationNumberGenerator.generate()` 로 교체 |

---

## 작업자별 잔여 이슈 분류 요약

### 김민구 (책임개발자) — 5건

| 이슈 | 심각도   | 파일                                          | 요약                                          |
| ---- | -------- | --------------------------------------------- | --------------------------------------------- |
| H1   | HIGH     | `common/interceptor/LayoutModelInterceptor.java` | AnonymousAuthenticationToken NPE 위험      |
| H2   | HIGH     | `config/SecurityConfig.java`                  | `/sample/**` 개발 잔재 허용 경로              |
| H3   | HIGH     | `config/RateLimitFilter.java`                 | 동시성 unsafe (비원자 카운터)                 |
| H4   | HIGH     | `admin/reservation/AdminReservationRepository.java` | `ReservationRepository`와 동일 엔티티 중복  |
| M1   | MEDIUM   | `reservation/reservation/PatientRepository.java` 외  | Repository 소유권 위반 (공유 엔티티 중복)  |

### 강태오 (개발자 A) — 6건

| 이슈 | 심각도 | 파일                                              | 요약                                          |
| ---- | ------ | ------------------------------------------------- | --------------------------------------------- |
| H5   | HIGH   | `reservation/reservation/ReservationService.java` | `generateReservationNumber()` Clock 미주입    |
| H6   | HIGH   | `reservation/reservation/ReservationService.java` | `updateReservation()` TOCTOU 순서 문제        |
| H7   | HIGH   | `reservation/reservation/ReservationController.java` | POST 수정 실패 시 select 옵션 미복원       |
| H8   | HIGH   | `reservation/reservation/ReservationService.java` | `IllegalArgumentException` → `CustomException` 미교체 |
| M2   | MEDIUM | `reservation/reservation/ReservationCreateForm.java` 외 | DTO 클래스 네이밍 불일치 (`Form` → `RequestDto`) |
| M3   | MEDIUM | `templates/reservation/direct-reservation.mustache` | fetch `async/await` + `response.ok` + `catch` 누락 |

### 조유지 (개발자 B) — 10건

| 이슈 | 심각도 | 파일                                            | 요약                                                        |
| ---- | ------ | ----------------------------------------------- | ----------------------------------------------------------- |
| H9   | HIGH   | `staff/mypage/StaffMypageController.java`       | Controller → `StaffRepository` 직접 주입                   |
| H10  | HIGH   | `doctor/mypage/DoctorMypageController.java`     | Controller → `DoctorRepository` 직접 주입                  |
| H11  | HIGH   | `nurse/mypage/NurseMypageController.java`       | Controller → `StaffRepository` 직접 주입                   |
| H12  | HIGH   | `doctor/treatment/DoctorTreatmentService.java`  | 클래스 레벨 `@Transactional(readOnly=true)` 누락            |
| H13  | HIGH   | `nurse/NurseService.java`                       | 클래스 레벨 `@Transactional(readOnly=true)` 누락            |
| H14  | HIGH   | `doctor/treatment/DoctorTreatmentService.java` 외 | `IllegalArgumentException` → `CustomException` 미교체    |
| M4   | MEDIUM | `nurse/NurseChatbotController.java`             | `/llm/chatbot/ask` 엔드포인트 누락 (404)                   |
| M5   | MEDIUM | `templates/nurse/chatbot.mustache`              | fetch 오류 처리 누락, 인라인 style 사용                     |
| M6   | MEDIUM | `nurse/NursePatientRepository.java` 외          | `NursePatientRepository` / `NurseReservationRepository` 소유권 위반 |
| M7   | MEDIUM | `doctor/treatment/DoctorReservationRepository.java` | `DoctorReservationRepository` 소유권 위반             |

### 강상민 (개발자 C) — 6건

| 이슈 | 심각도 | 파일                                               | 요약                                               |
| ---- | ------ | -------------------------------------------------- | -------------------------------------------------- |
| H15  | HIGH   | `admin/dashboard/AdminDashboardStatsService.java`  | `LocalDate.now()` 직접 호출 (Clock 미주입)         |
| H16  | HIGH   | `item/ItemManagerService.java`                     | 클래스 레벨 `@Transactional(readOnly=true)` 누락   |
| H17  | HIGH   | `item/ItemManagerService.java`                     | `IllegalArgumentException` → `CustomException` 미교체 |
| H18  | HIGH   | `admin/item/AdminItemController.java` 외           | 등록/수정 POST 핸들러 전체 미구현                  |
| M8   | MEDIUM | `templates/admin/dashboard.mustache`               | fetch 오류 처리 누락, 인라인 style 사용             |
| M9   | MEDIUM | `src/test/java/.../admin/`                         | 테스트 `@DisplayName` / given-when-then / UTF-8 누락 |

---

## HIGH — 병합 전 수정 권장

### H1. LayoutModelInterceptor — AnonymousAuthenticationToken NPE 위험 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/common/interceptor/LayoutModelInterceptor.java`
- **위반 규칙:** `rule_spring.md` §4 — "모든 예외는 적절히 처리"
- **내용:** 미인증 요청(로그인 화면, 정적 리소스 등)에서 `authentication`이 `AnonymousAuthenticationToken`으로 들어올 경우 `getPrincipal()` 캐스팅 실패 또는 NPE가 발생한다.
- **수정 방안:**
  ```java
  if (authentication == null || !authentication.isAuthenticated()
          || authentication instanceof AnonymousAuthenticationToken) {
      return true;
  }
  ```

### H2. SecurityConfig — `/sample/**` 개발 잔재 허용 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/config/SecurityConfig.java`
- **내용:** `.requestMatchers("/sample/**").permitAll()` 라인이 존재한다. 샘플 화면 개발용 경로로, 프로덕션에서 그대로 열려 있어서는 안 된다.
- **수정 방안:** 해당 라인 삭제 후 관련 샘플 컨트롤러·템플릿도 제거 또는 `@Profile("dev")`로 제한

### H3. RateLimitFilter — 동시성 unsafe — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/config/RateLimitFilter.java`
- **위반 규칙:** 이전 리뷰(C2) 후속 — 동시성 구조 미개선 잔존
- **내용:** 요청 카운터 증감이 비원자 연산으로 이루어질 경우 멀티스레드 환경에서 race condition 발생 가능. IP 스푸핑 이슈(이전 리뷰 C3)와 결합 시 Rate Limit 완전 우회 가능.
- **수정 방안:**
  ```java
  ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
  requestCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
  ```

### H4. AdminReservationRepository — 동일 엔티티 Repository 중복 — `김민구`

- **파일:** `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationRepository.java`
- **위반 규칙:** `rule-repository.md` §2.1 — "같은 엔티티에 대한 Repository는 하나만 허용"
- **내용:** `Reservation` 엔티티에 대해 `ReservationRepository`와 `AdminReservationRepository` 두 개가 존재한다. admin용 쿼리가 분산되어 일관성 유지가 어렵다.
- **수정 방안:** `AdminReservationRepository` 제거 → 필요 쿼리 메서드를 `ReservationRepository`에 추가 → `AdminReservationService`는 `ReservationRepository`를 직접 참조

### H5. ReservationService.generateReservationNumber() — Clock 미주입 — `강태오`

- **파일:** `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationService.java:79`
- **위반 규칙:** `rule_spring.md` §7 — "시간 의존 코드는 Clock 주입으로 테스트 가능하게"
- **내용:** `LocalDateTime.now()`를 직접 호출하여 단위 테스트에서 시간 제어가 불가능하다. 월별 누적 카운트 기반 예약번호 생성 로직의 신뢰성 검증이 어렵다.
- **수정 방안:**
  ```java
  private final Clock clock;  // 생성자 주입

  public String generateReservationNumber() {
      LocalDateTime now = LocalDateTime.now(clock);
      // ...
  }
  ```

### H6. updateReservation() — TOCTOU(cancel 순서) 문제 — `강태오`

- **파일:** `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationService.java:157`
- **위반 규칙:** `rule_spring.md` §8 — "동시성 문제를 고려한 설계"
- **내용:** 기존 예약을 `cancel()`한 후 새 슬롯 중복 체크를 수행한다. cancel과 중복 체크 사이에 다른 트랜잭션이 해당 슬롯을 선점할 경우 기존 예약은 취소된 채로 새 예약 생성에 실패한다.
- **수정 방안:** 중복 체크를 `cancel()` 이전으로 이동하거나, 취소-생성을 단일 트랜잭션 내 DB Unique 제약 + catch로만 처리

### H7. POST /reservation/modify/{id} — 검증 실패 시 폼 데이터 미복원 — `강태오`

- **파일:** `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationController.java`
- **내용:** 수정 폼 POST 실패(검증 오류 또는 중복 예약) 시 진료과·의사 select 옵션 목록을 model에 재주입하지 않으면 Mustache 템플릿에서 빈 드롭다운이 렌더링된다.
- **수정 방안:** 검증 실패 분기에서 `getDepartments()`, `getDoctorsByDepartment(form.departmentId())` 결과를 model에 재추가

### H8. ReservationService — IllegalArgumentException / IllegalStateException 사용 — `강태오`

- **파일:** `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationService.java:99,109,111,141`
- **위반 규칙:** `rule_spring.md` §9 — "예외는 CustomException 팩토리 메서드 사용"
- **내용:** `new IllegalArgumentException("의사를 찾을 수 없습니다.")`, `new IllegalStateException("이미 예약된 시간대입니다.")` 등을 직접 사용한다.
- **수정 방안:**
  ```java
  // before
  throw new IllegalArgumentException("의사를 찾을 수 없습니다.");
  // after
  throw CustomException.notFound("의사를 찾을 수 없습니다.");

  // before
  throw new IllegalStateException("이미 예약된 시간대입니다.");
  // after
  throw CustomException.conflict("이미 예약된 시간대입니다.");
  ```

### H9 · H10 · H11. Mypage 컨트롤러 3종 — Repository 직접 주입 — `조유지`

- **파일:**
  - `src/main/java/com/smartclinic/hms/staff/mypage/StaffMypageController.java:17`
  - `src/main/java/com/smartclinic/hms/doctor/mypage/DoctorMypageController.java:17`
  - `src/main/java/com/smartclinic/hms/nurse/mypage/NurseMypageController.java:17`
- **위반 규칙:** `rule-controller.md` §2 — "Controller는 Service만 호출, Repository 직접 주입 금지"
- **내용:** 세 Mypage 컨트롤러 모두 Service 없이 Repository를 직접 주입하여 사용한다. `orElseThrow()`에 `IllegalArgumentException`도 사용 중.
- **수정 방안:** 각각 `StaffMypageService`, `DoctorMypageService`, `NurseMypageService` 생성
  ```java
  // 예: DoctorMypageService
  @Service @RequiredArgsConstructor @Transactional(readOnly = true)
  public class DoctorMypageService {
      private final DoctorRepository doctorRepository;
      public DoctorMypageDto getMypage(String username) {
          return doctorRepository.findByStaff_Username(username)
                  .map(DoctorMypageDto::new)
                  .orElseThrow(() -> CustomException.notFound("의사 정보를 찾을 수 없습니다."));
      }
  }
  ```

### H12. DoctorTreatmentService — 클래스 레벨 @Transactional(readOnly=true) 누락 — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentService.java:20`
- **위반 규칙:** `rule_spring.md` §5 — "클래스 레벨 `@Transactional(readOnly=true)`, 쓰기 메서드에만 개별 `@Transactional`"
- **내용:** 클래스 레벨 `@Transactional` 미선언. `getDashboardData()`, `getTreatmentList()`, `getCompletedList()`, `getTreatmentDetail()` 네 개 메서드 각각에 `@Transactional(readOnly = true)` 중복 선언.
- **수정 방안:** 클래스에 `@Transactional(readOnly = true)` 추가 → `completeTreatment()`만 `@Transactional` 유지

### H13. NurseService — 클래스 레벨 @Transactional(readOnly=true) 누락 — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/nurse/NurseService.java:16`
- **위반 규칙:** `rule_spring.md` §5`
- **내용:** H12와 동일. 클래스 레벨 선언 없이 각 메서드에 개별 `@Transactional` 반복.
- **수정 방안:** 클래스에 `@Transactional(readOnly = true)` 추가 → `receiveReservation()`, `updatePatient()`만 `@Transactional` 유지

### H14. DoctorTreatmentService / NurseService — IllegalArgumentException 사용 — `조유지`

- **파일:**
  - `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentService.java:68,76,78`
  - `src/main/java/com/smartclinic/hms/nurse/NurseService.java:68,75,82`
- **위반 규칙:** `rule_spring.md` §9`
- **내용:** `new IllegalArgumentException("예약을 찾을 수 없습니다.")` 등을 직접 사용한다.
- **수정 방안:** → `CustomException.notFound("...")` 로 전체 교체

### H15. AdminDashboardStatsService — LocalDate.now() 직접 호출 — `강상민`

- **파일:** `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsService.java`
- **위반 규칙:** `rule_spring.md` §7
- **내용:** `LocalDate.now()` 직접 호출로 테스트에서 날짜 제어 불가. 대시보드 통계(오늘/이번 달 집계)의 단위 테스트 작성이 어렵다.
- **수정 방안:**
  ```java
  @Service
  @RequiredArgsConstructor
  public class AdminDashboardStatsService {
      private final Clock clock;  // 주입
      // LocalDate.now(clock) 사용
  }
  ```

### H16. ItemManagerService — 클래스 레벨 @Transactional(readOnly=true) 누락 — `강상민`

- **파일:** `src/main/java/com/smartclinic/hms/item/ItemManagerService.java`
- **위반 규칙:** `rule_spring.md` §5`
- **내용:** 클래스 레벨 `@Transactional(readOnly = true)` 미선언. 조회 메서드에도 쓰기 트랜잭션 적용되어 불필요한 dirty checking 발생 가능.
- **수정 방안:** 클래스에 `@Transactional(readOnly = true)` 추가 → 등록/수정/삭제 메서드만 `@Transactional`

### H17. ItemManagerService — IllegalArgumentException / RuntimeException 사용 — `강상민`

- **파일:** `src/main/java/com/smartclinic/hms/item/ItemManagerService.java`
- **위반 규칙:** `rule_spring.md` §9`
- **내용:** `new IllegalArgumentException(...)` 또는 `new RuntimeException(...)` 직접 사용.
- **수정 방안:** → `CustomException.notFound(...)` / `CustomException.badRequest(...)` 교체

### H18. Admin 등록 POST 핸들러 전체 미구현 — `강상민`

- **파일:**
  - `src/main/java/com/smartclinic/hms/admin/item/AdminItemController.java`
  - `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleController.java`
  - `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
  - `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentController.java`
- **내용:** 각 등록 폼(`GET /admin/*/form`)은 구현되어 있으나, 실제 저장하는 `POST /admin/*/form` 핸들러가 존재하지 않아 등록 버튼이 동작하지 않는다.
- **수정 방안:** 각 Controller에 `@PostMapping("/form")` 추가 → Service에 `create*()` 메서드 구현

---

## MEDIUM — 가능한 빨리 수정

### M1. NursePatientRepository / NurseReservationRepository / DoctorReservationRepository 소유권 위반 — `조유지`

- **파일:**
  - `src/main/java/com/smartclinic/hms/nurse/NursePatientRepository.java`
  - `src/main/java/com/smartclinic/hms/nurse/NurseReservationRepository.java`
  - `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorReservationRepository.java`
- **위반 규칙:** `rule-repository.md` §2.1 — "같은 엔티티에 대한 Repository는 하나만"
- **내용:** `Patient`, `Reservation` 엔티티를 대상으로 하는 Repository가 nurse/doctor 패키지에 별도 정의되어 있다. 엔티티별 단일 Repository 원칙 위반.
- **수정 방안:** 필요한 쿼리 메서드를 기존 `ReservationRepository` / `PatientRepository`에 통합. 단, `reservation/` 패키지 소유권 이슈가 있으므로 Lead와 조율 필요

### M2. ReservationCreateForm / ReservationUpdateForm — 네이밍 불일치 — `강태오`

- **파일:**
  - `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationCreateForm.java`
  - `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationUpdateForm.java`
- **위반 규칙:** `rule_spring.md` §6 — "요청 DTO 접미사는 `RequestDto`, 응답은 `ResponseDto`"
- **수정 방안:** `ReservationCreateRequestDto`, `ReservationUpdateRequestDto`로 리네이밍 후 참조 전체 갱신

### M3. direct-reservation.mustache — fetch 오류 처리 누락 — `강태오`

- **파일:** `src/main/resources/templates/reservation/direct-reservation.mustache`
- **위반 규칙:** `rule_javascript.md` §4 — "fetch는 async/await + response.ok 체크 + catch 필수"
- **내용:** 의사 목록 조회 fetch에 `async/await` / `response.ok` 체크 / `catch` 블록이 없어 네트워크 오류 시 사용자 피드백이 없다.
- **수정 방안:**
  ```javascript
  try {
      const response = await fetch(`/api/reservation/doctors?departmentId=${deptId}`);
      if (!response.ok) throw new Error('서버 오류');
      const data = await response.json();
      // ...
  } catch (e) {
      alert('의사 목록을 불러오지 못했습니다.');
  }
  ```

### M4. /llm/chatbot/ask — 엔드포인트 미구현(404) — `조유지`

- **파일:** `src/main/java/com/smartclinic/hms/nurse/NurseChatbotController.java` 또는 LLM 컨트롤러
- **내용:** 간호사 챗봇 화면에서 호출하는 `/llm/chatbot/ask` 엔드포인트가 없거나 URL 매핑 불일치로 404 발생.
- **수정 방안:** 엔드포인트 존재 여부 확인 후 `@PostMapping("/llm/chatbot/ask")` 추가 또는 프론트 URL 정정

### M5. nurse/chatbot.mustache — fetch 오류 처리 누락 + 인라인 style — `조유지`

- **파일:** `src/main/resources/templates/nurse/chatbot.mustache`
- **위반 규칙:** `rule_javascript.md` §4`, `rule_css.md` §3 — "인라인 style 속성 사용 금지"
- **수정 방안:** fetch `async/await` + `response.ok` + `catch` 추가. 인라인 `style=""` → Tailwind 유틸리티 클래스로 교체

### M6. NursePatientRepository / DoctorReservationRepository — 중복 엔티티 Repository (M1과 연동)

- M1 항목 참고. 별도 이슈로 기록하여 작업 추적 용도.

### M7. admin/dashboard.mustache — fetch 오류 처리 누락 + 인라인 style — `강상민`

- **파일:** `src/main/resources/templates/admin/dashboard.mustache`
- **위반 규칙:** `rule_javascript.md` §4`, `rule_css.md` §3`
- **내용:** 대시보드 통계 API 호출 fetch에 `async/await` / `response.ok` / `catch` 누락. 인라인 `style=""` 사용.
- **수정 방안:** M5와 동일 패턴 적용. 인라인 스타일 → Tailwind 클래스로 교체

### M8. application.properties — spring.profiles.active 하드코딩 — `김민구`

- **파일:** `src/main/resources/application.properties`
- **내용:** `spring.profiles.active=local` 등이 고정되어 있어 CI/배포 환경에서 환경변수 오버라이드 없이 의도치 않은 프로파일이 활성화될 수 있다.
- **수정 방안:** 해당 설정 제거 또는 `${SPRING_PROFILES_ACTIVE:local}` 환경변수 치환으로 교체

### M9. 테스트 — @DisplayName / given-when-then / UTF-8 인코딩 — `강상민`, `강태오`

- **파일:** `src/test/java/com/smartclinic/hms/` 하위 전체 테스트 파일
- **위반 규칙:** `rule_test.md` §3 — "@DisplayName 필수, given/when/then 구조, UTF-8 인코딩"
- **내용:** 일부 파일에 `@DisplayName` 누락, 인코딩 깨짐(`湲곕낯 ?뚮씪誘명꽣...`), given/when/then 주석 미사용.
- **수정 방안:**
  ```java
  @Test
  @DisplayName("예약 생성 - 정상 케이스")
  void createReservation_success() {
      // given
      // when
      // then
  }
  ```
  `build.gradle`에 UTF-8 설정 추가:
  ```groovy
  tasks.withType(Test) {
      systemProperty 'file.encoding', 'UTF-8'
  }
  ```

---

## 긍정적 사항

- `ReservationNumberGenerator`: `ConcurrentHashMap<LocalDate, AtomicLong>` 기반 스레드 안전한 예약번호 생성기 잘 구현됨
- `CustomException` 팩토리 패턴: `notFound()`, `conflict()`, `badRequest()` 등 일관된 예외 생성 구조 존재 (활용률만 높이면 됨)
- `Reservation.create()` 정적 팩토리: 도메인 객체 생성 책임이 명확히 도메인 레이어에 있음
- `ReceptionService`: `JOIN FETCH` 쿼리로 N+1 문제 해결 (이전 리뷰 H5 반영)
- `WalkinService`: C5, C6 수정 완료로 예외 메시지 및 중복 없는 예약번호 생성 정상 동작

---

## 최종 판정: **REQUEST CHANGES (변경 요청)**

CRITICAL 6건은 이번 리뷰 세션에서 즉시 수정 완료. HIGH 14건 수정 후 재리뷰 요청.

### 작업자별 수정 우선순위 요약

| 담당자        | HIGH | MEDIUM | 합계 | 최우선 항목             |
| ------------- | ---- | ------ | ---- | ----------------------- |
| 김민구 (Lead) | 4    | 1      | 5    | H1 NPE, H2 sample 경로 |
| 강태오 (A)    | 4    | 2      | 6    | H6 TOCTOU, H8 예외처리 |
| 조유지 (B)    | 6    | 2      | 8+   | H9~H11 Mypage 서비스화 |
| 강상민 (C)    | 4    | 2      | 6    | H18 POST 핸들러 구현   |
