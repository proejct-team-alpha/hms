# HMS 프로젝트 코딩 규칙 요약

> **기준**: 2025~2026년 · Spring Boot 4.0.x + Mustache SSR

본 문서는 `doc/rules/` 하위 상세 규칙의 요약이다. 구체적인 코드 예시·패턴은 각 규칙 문서를 참조한다.

---

## 규칙 문서 목록

| 문서 | 대상 | 핵심 내용 |
|------|------|-----------|
| [rule_spring.md](rules/rule_spring.md) | 백엔드 (Spring Boot) | 보안, API 설계, 에러 핸들링, Security |
| [rule_test.md](rules/rule_test.md) | 테스트 | Given-When-Then, AssertJ, Mockito, 결정성 보장 |
| [rule_javascript.md](rules/rule_javascript.md) | 프론트엔드 (JavaScript) | 변수/함수, 비동기, 에러 처리, 금지 패턴 |
| [rule_css.md](rules/rule_css.md) | 스타일 (CSS) | BEM, 레이아웃, 파일 구조, Mustache 연동 |

---

## Spring (Back-end) 요약

- **환경 변수**: DB 비밀번호 등 민감 정보는 환경 변수로 주입, `.gitignore` 필수
- **에러 핸들러**: `ErrorResponse` 공통 포맷, `GlobalExceptionHandler`로 일괄 처리
- **URI 설계**: 계층형 자원 (`/api/orders/{orderId}/items`), RESTful
- **서비스 레이어**: `@Transactional(readOnly = true)` 기본, 쓰기 메서드만 개별 선언
- **DTO**: Java Record 우선, `@Valid` / `@Validated` 검증
- **Security**: `SecurityFilterChain` Bean, CORS `CorsConfigurationSource`, 환경별 origins

---

## 테스트 요약

- **코드 작성**: 메서드당 단일 책임, 30~40줄 기준, 매직 넘버/문자열 상수화
- **최소 기준**: 핵심 로직 테스트 필수, 테스트 없는 핵심 로직 배포 금지
- **외부 의존**: DB·API 의존 금지. H2 또는 Testcontainers만 사용
- **결정성**: `System.currentTimeMillis()`, `LocalDateTime.now()` 직접 사용 금지 → Clock 추상화. 랜덤값은 고정 시드/Stub
- **Given-When-Then**: `// given`, `// when`, `// then` 주석 필수
- **도구**: AssertJ, BDDMockito (`given().willReturn()`), `@DisplayName` 필수
- **유형**: 단위 `@ExtendWith(MockitoExtension)`, Repository `@DataJpaTest`, Controller `@WebMvcTest`, 통합 `@SpringBootTest` 최소화

---

## JavaScript 요약

- **변수**: `const` 우선, `var` 금지, 전역 변수 금지, TDZ 이해
- **함수**: 화살표 함수 기본, 암시적 return, 단일 책임 원칙
- **비동기**: async/await 중심, `Promise.all` / `Promise.allSettled`, AbortController
- **에러**: try-catch, Error Cause 체이닝, fetch `response.ok` 검사
- **금지**: var, then/catch 남용, fire-and-forget, `==`, `parseInt` radix 누락

---

## CSS 요약

- **철학**: 클래스명에 의미 담기, 셀렉터 3단계 이하, 재사용 단위로 분리
- **네이밍**: BEM (`컴포넌트__요소--상태`), 접두어 `l-`/`c-`/`u-`/`is-`/`js-`
- **레이아웃**: CSS Grid 우선, Flexbox 보조, Grid+Flex 조합
- **파일 구조**: `base/` `components/` `layouts/` `pages/` `utilities/`
- **변수**: `:root`에 색상·간격·타이포그래피, rem/em 활용
- **금지**: id 셀렉터, !important, 인라인 스타일, float 레이아웃
- **Mustache**: 상태 클래스 활용 (`card--{{#isImportant}}highlighted{{/isImportant}}`)

---

## 공통 원칙

- **일관성**: 팀 전체가 동일한 패턴을 따르는 것이 가장 중요
- **점진적 적용**: 규칙은 필요에 따라 강화·완화 가능
- **상세 참조**: 세부 규칙·코드 예시는 각 `rules/*.md` 문서 참조
