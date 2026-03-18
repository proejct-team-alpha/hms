<!-- Parent: ../AI-CONTEXT.md -->

# doc — 프로젝트 문서 및 규칙

## 목적

시스템 설계(PRD), API 명세, 코딩 규칙(RULE), 기술 스택 및 작업 가이드라인을 보관하는 중앙 지식 저장소.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| PRD.md | 제품 요구사항 정의서 (기능, 역할, 프로세스) |
| API.md | 백엔드 API 명세서 (Request/Response, 에러 코드) |
| RULE.md | 공통 코딩 규칙 및 아키텍처 원칙 |
| PROJECT_STRUCTURE.md | 디렉토리 구조 및 모듈 소유권 상세 설명 |
| RULE_COMPLIANCE_REPORT.md | 현재 코드의 규칙 준수 여부 및 기술 부채 보고서 |
| CSS_RULE_COMPLIANCE.md | CSS/Tailwind BEM 준수 가이드 |
| LAYOUT_PARTIALS.md | Mustache 레이아웃(L1, L2, L3) 및 파셜 사용 가이드 |

## 하위 디렉토리

- `rules/` - 언어/프레임워크별 세부 코딩 규칙 (`rule_spring.md`, `rule_css.md` 등)
- `dev-a/` - 개발자 A(강태오)의 작업 로그 및 설계 문서
- `dev-c/` - 개발자 C(강상민)의 작업 로그 및 설계 문서

## AI 작업 지침

- 새로운 기능을 개발하기 전 반드시 `PRD.md`와 `API.md`를 검토하여 요구사항을 숙지한다.
- 코드 작성 시 `RULE.md`와 `rules/` 하위의 구체적인 규칙을 엄격히 준수한다.
- 아키텍처 변경이나 중요한 규칙 변경 시 관련 문서를 먼저 업데이트하고 보고한다.
- `RULE_COMPLIANCE_REPORT.md`를 참고하여 기존 코드의 스타일을 유지한다.

## 의존성

- 내부: `src/main/java/`, `src/main/resources/` (코드 구현의 기준)
- 외부: Anthropic Claude (LLM 가이드라인 적용)
