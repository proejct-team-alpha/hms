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
