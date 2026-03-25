<!-- Parent: ../../../../AI-CONTEXT.md -->

# test — 테스트

## 목적

HMS 전 기능의 자동화 테스트. Given-When-Then 패턴 필수.

## 현재 테스트 파일

```
src/test/java/com/smartclinic/hms/admin/dashboard/
├── AdminDashboardControllerTest.java      ← @WebMvcTest
├── AdminDashboardApiControllerTest.java   ← @WebMvcTest
├── AdminDashboardStatsServiceTest.java    ← @ExtendWith(MockitoExtension)
└── AdminDashboardStatsRepositoryTest.java ← @DataJpaTest
```

## 테스트 유형별 어노테이션

| 유형 | 어노테이션 | 용도 |
|------|-----------|------|
| 단위 (Service) | `@ExtendWith(MockitoExtension.class)` | Mock 의존성 |
| 슬라이스 (Repository) | `@DataJpaTest` | H2 인메모리 DB |
| 슬라이스 (Controller) | `@WebMvcTest` | MockMvc |
| 통합 | `@SpringBootTest` | 전체 컨텍스트 |

## 필수 규칙

```java
// Given-When-Then 주석 필수
@Test
@DisplayName("오늘 예약 수 조회 - 정상")
void countTodayReservations() {
    // given
    given(reservationRepository.countByDate(any())).willReturn(5L);

    // when
    long result = statsService.getTodayReservationCount();

    // then
    assertThat(result).isEqualTo(5L);
}
```

## 금지 사항

- `LocalDate.now()` / `LocalDateTime.now()` 직접 사용 → `Clock` 추상화
- `Mockito.when()` → `BDDMockito.given()` 사용
- `@DisplayName` 누락 금지
- `AssertJ` (`assertThat`) 사용, JUnit `assertEquals` 지양

## 커버리지 목표

- Service: 80% 이상
- Repository: 70% 이상
- (JaCoCo 측정)
