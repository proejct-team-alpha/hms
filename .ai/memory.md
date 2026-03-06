# Project Memory

## Architecture

- 최우선 규칙 문서는 `AGENTS.md`이며, 작업 시작 시 항상 먼저 확인한다.
- 문서 우선순위: 로컬 `doc/PROJECT_STRUCTURE.md`, `doc/RULE.md` > 외부 documents 저장소.
- 충돌 시 구조/패키지는 `doc/PROJECT_STRUCTURE.md`, 코딩/테스트는 `doc/RULE.md`를 우선한다.
- 기본 아키텍처는 Spring Boot + Mustache SSR + Spring Security + JPA + Gradle이다.
- 인증은 세션 기반, 인가는 역할 기반으로 강제한다.
- 공통 예외 처리(`CustomException`, `GlobalExceptionHandler`) 패턴을 유지한다.
- 서비스 레이어가 트랜잭션 경계를 소유한다.
- 기본 트랜잭션 정책: 클래스 `@Transactional(readOnly = true)`, 쓰기 메서드만 별도 트랜잭션.
- DTO는 Record 우선, 입력 검증은 `@Valid`/`@Validated`를 적용한다.
- POST 처리 기본은 PRG(Post-Redirect-Get)이며, 실패 처리도 일관된 규칙을 유지한다.
- 민감정보는 환경변수로만 관리하고 `.env`/시크릿 커밋을 금지한다.
- 보안/공통/엔티티 레이어는 보호 영역으로 간주한다.

## Module Structure

- 루트 패키지는 `com.smartclinic.hms`로 고정한다.
- 핵심 모듈: `config`, `common`, `domain`, `auth`, `reservation`, `staff`, `doctor`, `nurse`, `admin`, `llm`.
- `config`는 보안/웹 설정, `common`은 예외/인터셉터/공통 서비스, `domain`은 엔티티를 담당한다.
- 업무 로직은 역할 모듈(`reservation`, `staff`, `doctor`, `nurse`, `admin`, `llm`)에 배치한다.
- 모듈 경계를 넘는 임의 패키지/레이어 추가를 금지한다.
- 컨트롤러/서비스/리포지토리는 같은 기능 모듈 경계에서 관리한다.
- 공통 인터셉터의 레이아웃 모델 주입 구조를 유지한다.
- 템플릿은 `templates/common` + 역할별 디렉터리 구조를 유지한다.
- 정적 리소스는 `static/css`, `static/js`, `static/images` 분리 원칙을 유지한다.
- 공통/설정 모듈에 업무 기능을 넣지 않는다.
- 엔티티 패키지에 프레젠테이션 로직을 넣지 않는다.

## URL Rules

- URL은 역할 기반 접두어를 유지한다: `/reservation/**`, `/staff/**`, `/doctor/**`, `/nurse/**`, `/admin/**`, `/api/**`, `/llm/**`.
- 공개 영역은 `/`와 `/reservation/**`를 기본으로 한다.
- 인증 필요 영역은 역할 접두어 기준으로 Security 정책을 적용한다.
- URL 패턴은 역할/자원/행위의 일관성을 유지한다.
- GET은 렌더링/조회, POST는 생성/변경/상태전이에 사용한다.
- POST 성공 후 redirect(PRG)를 기본으로 한다.
- JSON 응답은 AJAX/`/api/**` 등 필요한 경로에 한정한다.
- 로그인 성공 리다이렉트는 역할별 대시보드 규칙을 따른다.
- 정적 경로(`/css/**`, `/js/**`, `/images/**`)는 인터셉터 제외 경로로 유지한다.
- URL 의미 중복/혼용 네이밍을 금지한다.
- 모듈 경계를 넘는 prefix 재사용을 금지한다.

## Branch Strategy

- 개발 중심 브랜치는 `develop`이고, 배포 안정 브랜치는 `main`이다.
- `main` 직접 커밋/푸시를 금지한다.
- 모든 변경은 작업 브랜치(`feature/*`, `bugfix/*`, `hotfix/*`)에서 시작한다.
- 일반 기능/버그는 PR로 `develop`에 병합한다.
- PR 기반 머지만 허용하며 직접 merge를 지양한다.
- 최소 1명 승인 + 책임개발자 리뷰 원칙을 유지한다.
- 병합 방식은 Squash merge를 기본으로 한다.
- 작업 시작 전 `develop` 최신 동기화를 수행한다.
- 장기 생존 브랜치를 지양하고 작업 범위를 작게 유지한다.
- 커밋 메시지는 Conventional Commits를 사용한다.
- 강제 push는 `--force-with-lease`만 허용한다.

## Role & Permission Rules

- 비즈니스 역할: `ADMIN`, `STAFF`, `DOCTOR`, `NURSE`.
- Spring Security 역할: `ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_DOCTOR`, `ROLE_NURSE`.
- 역할 매핑은 1:1로 유지한다 (`ADMIN` <-> `ROLE_ADMIN` 등).
- `/admin/**`는 `ROLE_ADMIN` 전용이다.
- `/staff/**`는 `ROLE_STAFF` 중심, `/doctor/**`는 `ROLE_DOCTOR` 중심, `/nurse/**`는 `ROLE_NURSE` 중심이다.
- `/reservation/**`는 비회원 공개 흐름으로 유지한다.
- LLM 규칙 챗봇은 `DOCTOR`/`NURSE`(보안 역할 기준 `ROLE_DOCTOR`/`ROLE_NURSE`) 중심 접근 정책을 유지한다.
- 미인증 접근은 로그인 리다이렉트, 무권한 접근은 403 처리 원칙을 유지한다.
- 권한 검증은 Security 설정 + 도메인/서비스 레벨에서 방어적으로 유지한다.
- 역할 체계 임의 확장/변경은 금지한다.
- 상세 권한 예외는 `doc/API.md`가 아니라 보안 규칙 문서 합의 후 반영한다.

## Module Ownership

- 기본 원칙은 기능 단위 수직 소유(Controller/Service/Repository/Template 동일 영역 소유)다.
- 공유 레이어(`config`, `common`, `domain`)는 책임개발자 소유/검토 영역으로 다룬다.
- `reservation`은 외부 예약, `staff/doctor/nurse`는 내부업무, `admin`은 관리 기능 소유 영역이다.
- `llm`은 서비스 코어와 UI 연결부를 분리해 소유한다.
- `templates/common/**`, `templates/layouts/**`는 공유 소유 영역이다.
- 역할별 템플릿은 해당 역할 모듈 소유를 따른다.
- 타 소유 영역 직접 수정 대신 이슈/에스컬레이션을 우선한다.
- 읽기 접근 허용과 쓰기 변경 권한을 구분해 적용한다.
- 공통 모듈 변경 시 영향 모듈을 함께 검토한다.
- 소유권 불명확 파일은 공유 소유로 간주하고 임의 수정하지 않는다.
- 인터페이스 계약 변경은 관련 모듈 동시 검토를 전제로 한다.

## Forbidden Rules

- `domain/**` 엔티티 구조 임의 변경 금지.
- `SecurityConfig` 임의 변경 금지.
- `LayoutModelInterceptor` 임의 변경 금지.
- `common/service` 핵심 검증/슬롯 로직 우회 금지.
- `GlobalExceptionHandler` 공통 포맷 임의 변경 금지.
- 타 모듈 Service 내부 구현 직접 수정 금지.
- 상태 전이 규칙 중복 구현/다중 위치 변경 금지.
- Repository로 타 모듈 상태 직접 변경 금지.
- 시크릿 커밋 및 하드코딩(`.env`, API Key, 비밀번호) 금지.
- 테스트 없는 핵심 로직 반영 금지.
- PRG/CSRF 규칙을 깨는 요청 처리 패턴 도입 금지.
- 상세 예외 목록/기능별 금지사항은 `doc/RULE.md`와 보안/아키텍처 문서를 따른다.

## Coding Rules (요약)

- 백엔드 핵심: 트랜잭션/예외/검증/보안 규칙 일관성 유지.
- 서비스 레이어 중심 설계, 컨트롤러는 입출력 처리에 집중.
- DTO Record 우선, 입력 검증(`@Valid`, `@Validated`) 필수.
- JavaScript 핵심: `const` 우선, `var` 금지, async/await 중심, 전역 변수 금지.
- JS 오류 처리: try/catch + 명시적 실패 처리, fire-and-forget 금지.
- CSS 핵심: BEM, 낮은 셀렉터 깊이, 공통/컴포넌트 분리.
- CSS 금지: `id` 셀렉터 남용, `!important`, 인라인 스타일, float 레이아웃.
- Mustache는 상태 클래스 기반 스타일링을 우선한다.
- 매직 넘버/문자열 상수화, 네이밍 일관성, 단일 책임 원칙 유지.
- 공통 규칙 위반이 필요하면 문서 합의 후 반영한다.
- 상세 구현 규칙은 `doc/RULE.md`를 단일 기준으로 참조한다.

## Testing Minimum Rules

- 핵심 비즈니스 로직 테스트는 필수이며, 미작성 상태 반영을 금지한다.
- 테스트 구조는 Given-When-Then을 강제한다.
- 테스트 본문에 `// given`, `// when`, `// then` 주석을 명시한다.
- 테스트 이름/`@DisplayName`은 의도를 명확히 표현한다.
- 단위 테스트는 Mockito 기반 격리 테스트를 우선한다.
- Repository는 `@DataJpaTest`, Controller는 `@WebMvcTest` + MockMvc를 우선한다.
- 통합 테스트는 핵심 플로우 최소 세트만 유지한다.
- 외부 API 실호출 테스트를 금지하고 Mock으로 대체한다.
- 시간/랜덤/외부 상태 의존성을 제거해 결정성을 보장한다.
- 운영 DB 의존 테스트를 금지하고 H2/Testcontainers를 사용한다.
- 보안 테스트는 역할별 허용/차단 케이스를 모두 검증한다.
- 세부 테스트 패턴과 금지사항은 `doc/RULE.md`(및 `doc/rules/rule_test.md`)를 참조한다.
- 테스트 이름/`@DisplayName`은 의도를 명확히 표현한다.
- 단위 테스트는 Mockito 기반 격리 테스트를 우선한다.
- Repository는 `@DataJpaTest`, Controller는 `@WebMvcTest` + MockMvc를 우선한다.
- 통합 테스트는 핵심 플로우 최소 세트만 유지한다.
- 외부 API 실호출 테스트를 금지하고 Mock으로 대체한다.
- 시간/랜덤/외부 상태 의존성을 제거해 결정성을 보장한다.
- 운영 DB 의존 테스트를 금지하고 H2/Testcontainers를 사용한다.
- 보안 테스트는 역할별 허용/차단 케이스를 모두 검증한다.
- 세부 테스트 패턴과 금지사항은 `doc/RULE.md`(및 `doc/rules/rule_test.md`)를 참조한다.
