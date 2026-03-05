# HMS 프로젝트 코딩 규칙 요약

> **기준**: 2025~2026년, Spring Boot 4.0.x + Mustache SSR

본 문서는 `doc/rules/` 하위 상세 규칙 문서를 빠르게 참조하기 위한 요약 문서다.
실제 구현 시에는 각 규칙 문서를 우선 확인한다.

---

## 규칙 문서 목록

| 문서 | 대상 | 핵심 내용 |
|------|------|-----------|
| [rule_spring.md](rules/rule_spring.md) | 백엔드 (Spring Boot) | 보안, API 설계, 예외 처리, 트랜잭션, DTO 검증 |
| [rule_test.md](rules/rule_test.md) | 테스트 | Given-When-Then, Mockito, AssertJ, 결정성 보장 |
| [rule_javascript.md](rules/rule_javascript.md) | 프론트엔드 (JavaScript) | 변수/함수 규칙, 비동기 처리, 에러 처리, 금지 패턴 |
| [rule_css.md](rules/rule_css.md) | 스타일 (CSS) | BEM, 레이아웃 원칙, 파일 구조, Mustache 연동 |
| [rule-controller.md](rules/rule-controller.md) | Controller (SSR/API) | SSR/API 분리, `Resp.ok`, DTO 네이밍, 체크리스트 |

---

## Spring (Back-end) 요약

- **환경 변수**: DB 비밀번호, API Key 등 민감정보는 환경 변수로 주입하고 커밋 금지
- **예외 처리**: `GlobalExceptionHandler` 중심의 공통 포맷 유지
- **URI 설계**: RESTful 원칙과 계층형 자원 표현 유지
- **서비스 계층**: `@Transactional(readOnly = true)` 기본, 쓰기 메서드만 별도 트랜잭션
- **DTO/검증**: Record 우선, `@Valid` / `@Validated` 검증 필수
- **보안**: `SecurityFilterChain`, CORS, CSRF 정책을 일관되게 적용

---

## Controller Rules 요약

- **분리 원칙**: SSR Controller와 API Controller를 분리해 책임을 명확히 유지
- **응답 규격**: API 성공 응답은 `Resp.ok(...)` 사용, 실패는 `GlobalExceptionHandler` 포맷 유지
- **DTO 네이밍**: 요청 `...Request`, 응답 `...Response` 규칙 고정
- **체크리스트**: 서비스 위임, 권한 정책, 응답 형식 일관성을 점검
- **상세 참조**: [rule-controller.md](rules/rule-controller.md)

---

## 테스트 요약

- **핵심 원칙**: 핵심 비즈니스 로직은 테스트 없이 반영 금지
- **구조**: 테스트 본문에 `// given`, `// when`, `// then` 주석 필수
- **도구**: AssertJ, BDDMockito (`given().willReturn()`), `@DisplayName` 사용
- **유형**:
  - 단위: `@ExtendWith(MockitoExtension.class)`
  - Repository: `@DataJpaTest`
  - Controller: `@WebMvcTest`
  - 통합: `@SpringBootTest` (최소 범위)
- **결정성**: 시간/랜덤/외부 의존성은 고정 또는 Mock 처리

---

## JavaScript 요약

- **변수**: `const` 우선, `let` 보조, `var` 금지
- **함수**: 단일 책임, 명확한 반환, 사이드이펙트 최소화
- **비동기**: async/await 중심, `Promise.all` / `Promise.allSettled` 활용
- **에러 처리**: try/catch, `response.ok` 체크, 실패 경로 명시
- **금지 패턴**: 무분별한 then/catch 체인, fire-and-forget, 느슨한 동등 비교(`==`)

---

## CSS 요약

- **방법론**: BEM 네이밍, 셀렉터 깊이 최소화
- **레이아웃**: Grid 우선, Flex 보조
- **파일 구조**: base/components/layouts/pages/utilities 분리
- **토큰화**: `:root` 기반 색상/간격/타이포 변수화
- **금지 패턴**: id 셀렉터 남용, `!important`, 인라인 스타일, float 레이아웃

---

## 공통 원칙

- **일관성**: 팀 전체가 동일한 구현/리뷰 기준을 따른다.
- **점진적 강화**: 규칙은 프로젝트 단계에 맞게 강화한다.
- **상세 참조**: 실제 구현 전 `doc/rules/*.md`를 반드시 확인한다.