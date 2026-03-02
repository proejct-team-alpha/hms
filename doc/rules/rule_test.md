# 테스트 작성 규칙

> **기준**: 2025~2026년 · JUnit 5 · Mockito · AssertJ · Spring Boot 4.0.x

백엔드 테스트 코드의 일관성, 유지보수성, 결정성을 보장하기 위한 규칙이다.

---

## 1. 코드 작성 (테스트 대상 코드)

| #   | Rule                         | 비고                |
| --- | ---------------------------- | ------------------- |
| 1   | **메서드 하나당 책임 하나**  | 단일 책임 원칙 준수 |
| 2   | **메서드 길이 과도 금지**    | 보통 30~40줄 기준   |
| 3   | **매직 넘버, 문자열 상수화** | 하드코딩 지양       |

---

## 2. 테스트 최소 기준

### 2.1 테스트 원칙

- **핵심 로직에 대한 테스트 존재**
  핵심 로직: 비즈니스 규칙·도메인 정책·결정을 내리는 서비스/유틸 코드. 팀에서 "핵심" 범위를 정의해 두는 것을 권장.
- **테스트 없는 핵심 로직 배포 금지**
- **커버리지 목표(선택)**: 라인 70% 이상, 브랜치 60% 이상 등 팀 합의. RULE 레벨로 강제하지 않을 수 있으며, 프로젝트 규모에 따라 조정.
- **테스트는 외부 환경(DB, API)에 의존하지 않는다.**
- **슬라이스/통합 테스트 시 DB**: In-memory(H2) 또는 Testcontainers만 사용. 실제 운영/개발 DB 연결 금지.

### 2.2 테스트 결정성 보장

> CI에서만 깨지는 테스트 원인 1순위

| #   | Rule                                                                   | 비고                              |
| --- | ---------------------------------------------------------------------- | --------------------------------- |
| 1   | **System.currentTimeMillis()**, **LocalDateTime.now()** 직접 사용 금지 | Clock, TimeProvider 등으로 추상화 |
| 2   | **랜덤값**은 고정 시드 또는 Stub 사용                                  |                                   |

---

## 3. 테스트 코드 작성 규칙 (2025~2026 베스트 프랙티스)

모든 단위 테스트·슬라이스 테스트는 아래 규칙을 준수한다. 예외가 필요한 경우 기술 리더 승인 및 문서화를 거친다.

### 3.1 기본 원칙

| #   | Rule                                                           | 비고                          |
| --- | -------------------------------------------------------------- | ----------------------------- |
| 1   | **Given-When-Then 패턴 필수**                                  | 모든 테스트는 3단계 구조 준수 |
| 2   | **주석 구분**: `// given`, `// when`, `// then` 명시           | 가독성 극대화                 |
| 3   | **AssertJ 사용**                                               | JUnit 기본 Assertions 대신    |
| 4   | **Mock 라이브러리**: Java → Mockito, Kotlin → MockK            |                               |
| 5   | **테스트 메서드 이름**: 행위 중심 + 결과 중심, BDD 스타일 권장 |                               |

### 3.2 테스트 유형별 적용 범위

| 테스트 유형                         | 사용 어노테이션 조합                          | 목 객체 사용      | Given-When-Then | 추천 Assert       | 비고                     |
| ----------------------------------- | --------------------------------------------- | ----------------- | --------------- | ----------------- | ------------------------ |
| 순수 단위 테스트 (Service, Util 등) | `@ExtendWith(MockitoExtension.class)`         | Mockito / MockK   | 필수            | AssertJ           | Spring 컨텍스트 로드 X   |
| Repository 슬라이스                 | `@DataJpaTest` + `@AutoConfigureTestDatabase` | 필요 시 Mock      | 필수            | AssertJ           | H2 또는 Testcontainers만 |
| Controller 슬라이스                 | `@WebMvcTest` + `@MockBean`                   | 필수 (Service 등) | 필수            | AssertJ + MockMvc |                          |
| 전체 통합 테스트                    | `@SpringBootTest` + `@AutoConfigureMockMvc`   | 최소화            | 권장            | AssertJ           | 느리므로 최소화          |

### 3.3 테스트 메서드 이름 규칙 (강력 추천)

- **BDD 스타일**: `[메서드명]_[상황설명]_should[기대결과]` 또는 `should[기대결과]_when[상황]`
- **한글 메서드명 예시**: `findById_존재하는ID_주면_해당회원을반환한다`, `register_중복이메일이면_예외를던진다`
- **대안(CI·빌드 도구 호환)**: 메서드명은 영문, `@DisplayName`에 한글 사용
  예: `@DisplayName("존재하는 ID를 주면 해당 회원을 반환한다")` + `findById_withValidId_returnsMember`
- **Kotlin**: snake_case 또는 자연어 스타일 허용 (팀 결정)

### 3.4 코드 구조 템플릿 (Java + JUnit 5 + Mockito + AssertJ)

```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 서비스 단위 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("findById - 존재하는 ID로 조회하면 해당 회원을 반환한다")
    void findById_존재하는ID_주면_해당회원을반환한다() {
        // given
        Long memberId = 1L;
        Member expected = Member.builder()
                .id(memberId)
                .email("test@example.com")
                .nickname("테스트유저")
                .build();

        given(memberRepository.findById(anyLong()))
                .willReturn(Optional.of(expected));

        // when
        Member actual = memberService.findById(memberId);

        // then
        assertThat(actual)
                .isNotNull()
                .extracting("id", "email", "nickname")
                .containsExactly(memberId, "test@example.com", "테스트유저");
    }

    @Test
    @DisplayName("register - 이미 존재하는 이메일이면 예외를 던진다")
    void register_중복이메일이면_예외를던진다() {
        // given
        MemberCreateRequest request = MemberCreateRequest.builder()
                .email("duplicate@example.com")
                .build();

        given(memberRepository.existsByEmail(request.getEmail()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 사용중인 이메일입니다.");
    }
}
```

### 3.5 Kotlin + MockK 템플릿 (선택)

```kotlin
import io.mockk.every
import io.mockk.mockk
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@DisplayName("회원 서비스")
class MemberServiceTest {

    private val memberRepository: MemberRepository = mockk()
    private val memberService = MemberService(memberRepository)

    @Test
    fun `이메일로 회원 조회 - 존재하면 회원 반환`() {
        // given
        val email = "test@example.com"
        val expected = Member(id = 1L, email = email, nickname = "테스트")

        every { memberRepository.findByEmail(email) } returns expected

        // when
        val actual = memberService.findByEmail(email)

        // then
        assertThat(actual).isEqualTo(expected)
    }
}
```

### 3.6 추가 강제 규칙

| #   | Rule                                                                                            | 비고                             |
| --- | ----------------------------------------------------------------------------------------------- | -------------------------------- |
| 1   | **주석 강제**: 모든 테스트에 `// given`, `// when`, `// then` 3줄 주석 필수                     | 가독성 극대화                    |
| 2   | **BDDMockito 권장**: `given(...).willReturn(...)` 형식 사용                                     | `when(...).thenReturn(...)` 대신 |
| 3   | **AssertJ 체이닝**: `extracting()`, `hasFieldOrPropertyWithValue()`, `satisfies()` 등 적극 활용 |                                  |
| 4   | **@DisplayName 필수**: 테스트 클래스와 메서드 모두에 의미 있는 한글 설명 작성                   |                                  |
| 5   | **예외 테스트**: `assertThatThrownBy()` 사용                                                    |                                  |
| 6   | **단일 책임**: 한 테스트 메서드 = 한 시나리오                                                   |                                  |
