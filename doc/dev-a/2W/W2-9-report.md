# W2-#9 Report: 단위 테스트 작성

## 작업 개요

`feature/reservation` 브랜치의 예약 기능에 대한 단위 테스트를 작성했다.
`ReservationService`, `ReservationRepository`, `PatientRepository`, `ReservationController` 4개 클래스를 대상으로
총 13개 테스트를 작성했으며, 전체 통과했다.

---

## 생성 파일

| 파일 | 유형 | 테스트 수 |
|------|------|-----------|
| `test/.../ReservationServiceTest.java` | Mockito 단위 테스트 | 3 |
| `test/.../PatientRepositoryTest.java` | @DataJpaTest | 3 |
| `test/.../ReservationRepositoryTest.java` | @DataJpaTest | 4 |
| `test/.../ReservationControllerTest.java` | @WebMvcTest | 3 |

---

## 구현 상세

### 1. ReservationServiceTest (Mockito)

의존성을 모두 Mock 처리하여 서비스 비즈니스 로직만 검증.

```java
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock DoctorRepository doctorRepository;
    @Mock PatientRepository patientRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock DepartmentRepository departmentRepository;
    @InjectMocks ReservationService reservationService;
    ...
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 실제 DB 없이 서비스 로직만 독립적으로 테스트하는 클래스입니다. 의존 객체들을 모두 가짜(Mock)로 대체합니다.
> - **왜 이렇게 썼는지**: `@Mock`은 가짜 객체를 만들어 실제 DB 없이 동작을 흉내 낼 수 있게 합니다. `@InjectMocks`는 `@Mock`으로 만든 가짜 객체들을 `ReservationService`의 의존성으로 자동 주입합니다. 이를 통해 빠르고 독립적인 단위 테스트가 가능합니다.
> - **쉽게 말하면**: 진짜 창고(DB) 없이 가짜 창고를 만들어서 예약 서비스 직원의 업무 처리 능력만 따로 시험하는 것입니다.

| 테스트 | 검증 내용 |
|--------|-----------|
| `createReservation_success_existingPatient` | 기존 환자 재사용 시 `patientRepository.save()` 미호출, 예약번호 RES- 형식, DTO 필드 정확성 |
| `createReservation_success_newPatient` | 신규 환자 생성 시 `patientRepository.save()` 호출 확인 |
| `createReservation_duplicateSlot_throwsException` | 중복 슬롯 존재 시 `IllegalStateException("이미 예약된 시간대입니다.")` 발생 |

---

### 2. PatientRepositoryTest (@DataJpaTest)

H2 인메모리 DB로 실제 JPA 쿼리 검증.

| 테스트 | 검증 내용 |
|--------|-----------|
| `findByPhone_exists` | 저장된 전화번호로 조회 시 `Optional.isPresent()` |
| `findByPhone_notExists` | 없는 전화번호 조회 시 `Optional.isEmpty()` |
| `save_newPatient_assignsId` | 저장 후 ID 자동 생성(`assertThat(id).isNotNull()`) |

---

### 3. ReservationRepositoryTest (@DataJpaTest)

`@BeforeEach`에서 Department, Staff, Doctor, Patient를 EntityManager로 직접 영속화 후 테스트.

| 테스트 | 검증 내용 |
|--------|-----------|
| `findByReservationNumber_found` | 예약번호 조회 성공 + 상태 RESERVED 확인 |
| `findByReservationNumber_notFound` | 없는 예약번호 → `Optional.isEmpty()` |
| `existsByDoctor_duplicateReserved_detected` | RESERVED 예약 존재 시 중복 감지 true |
| `existsByDoctor_cancelledExcluded` | 취소 후 동일 슬롯 → 중복 감지 false (CANCELLED 제외 확인) |

---

### 4. ReservationControllerTest (@WebMvcTest)

CSRF 비활성화 + 모든 요청 허용 TestSecurityConfig 적용.

```java
@TestConfiguration
@EnableWebSecurity
static class TestSecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 컨트롤러 테스트에서만 사용하는 보안 설정입니다. 모든 요청을 허용하고 CSRF 검증을 비활성화합니다.
> - **왜 이렇게 썼는지**: `@WebMvcTest`는 Spring Security를 기본으로 활성화하는데, 테스트에서 인증/CSRF 처리를 신경 쓰지 않고 컨트롤러 로직만 검증하기 위해 별도 테스트용 보안 설정을 만들었습니다. `@TestConfiguration`은 테스트 컨텍스트에서만 적용되는 설정 클래스입니다.
> - **쉽게 말하면**: 테스트 중에는 보안 경비원을 잠시 해제하여 컨트롤러의 업무 처리 능력만 집중적으로 테스트하는 것입니다.

| 테스트 | 검증 내용 |
|--------|-----------|
| `createReservation_success_redirectsToComplete` | 유효한 폼 → 302 리다이렉트 → `/reservation/complete*` |
| `createReservation_blankName_returnsFormWithError` | 빈 이름 → 200 + `view("reservation/direct-reservation")` + `attribute("errorMessage", "이름을 입력해주세요.")` |
| `createReservation_blankPhone_returnsFormWithError` | 빈 전화번호 → 200 + 에러 메시지 확인 |

---

## 테스트 실행 결과

```
> Task :test
BUILD SUCCESSFUL in 20s

tests="3" failures="0" errors="0"  ← ReservationServiceTest
tests="3" failures="0" errors="0"  ← PatientRepositoryTest
tests="4" failures="0" errors="0"  ← ReservationRepositoryTest
tests="3" failures="0" errors="0"  ← ReservationControllerTest
```

**총 13개 테스트 전체 통과 (failures: 0, errors: 0)**

---

## 수용 기준 확인

- [x] `ReservationServiceTest` — 예약 생성 성공(기존 환자, 신규 환자), 중복 예약 예외
- [x] `ReservationRepositoryTest` — 예약번호 조회, UNIQUE 중복 체크, CANCELLED 제외 검증
- [x] `PatientRepositoryTest` — 전화번호 기반 조회(존재/미존재), 환자 저장
- [x] `ReservationControllerTest` — PRG 패턴(3xx redirect), @Valid 검증 실패(이름/전화번호)
- [x] 전체 테스트 통과 (`./gradlew test --tests "com.smartclinic.hms.reservation.reservation.*"`)
