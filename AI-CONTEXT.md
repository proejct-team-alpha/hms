# HMS — Hospital Management System

## 목적

스마트 클리닉 병원 예약·관리 시스템. 비회원 환자 예약(AI 증상 분석 포함)부터
직원(STAFF/DOCTOR/NURSE/ADMIN) 업무 처리까지 전 기능을 SSR로 제공한다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| View | Mustache (SSR) |
| CSS | Tailwind CSS 4.x + BEM |
| Security | Spring Security (세션 기반) |
| DB | H2 (dev) / MySQL (prod) |
| ORM | JPA + Hibernate |
| LLM | Claude API (Anthropic) |
| Build | Gradle |

## 프로젝트 구조

```
hms/
├── src/main/java/com/smartclinic/hms/   ← Java 소스 (핵심)
├── src/main/resources/
│   ├── templates/                        ← Mustache 뷰
│   └── static/                           ← CSS, JS, 이미지
├── src/test/                             ← 테스트
├── doc/                                  ← 프로젝트 문서 및 규칙
└── .ai/                                  ← AI 에이전트 작업 파일
```

## 모듈 소유권

| 모듈 | 담당자 | 경로 |
|------|--------|------|
| config / domain / common / llm | 책임개발자(김민구) | 수정 시 이슈 등록 |
| reservation / home | 개발자 A(강태오) | `/reservation/**` |
| staff / doctor / nurse | 개발자 B(조유지) | `/staff/**` `/doctor/**` `/nurse/**` |
| admin | 개발자 C(강상민) | `/admin/**` |

## 역할별 URL

| 역할 | 진입점 |
|------|--------|
| 비회원(환자) | `/reservation/**` |
| STAFF | `/staff/**` |
| DOCTOR | `/doctor/**` |
| NURSE | `/nurse/**` |
| ADMIN | `/admin/**` |

## 핵심 규칙 요약

- GET → `request.setAttribute()` 후 뷰 반환
- POST 성공 → `redirect:/` (PRG 패턴)
- POST 실패 → 폼 뷰 재반환 (에러 포함)
- AJAX → `@ResponseBody` JSON (`Resp.ok()`)
- DTO → Java Record + `@Valid`
- 예외 → `CustomException` 팩토리 메서드
- JS → `const/let` 우선, `var` 금지, `async/await`

## 하위 디렉토리

- `src/main/java/com/smartclinic/hms/` — [AI-CONTEXT.md](src/main/java/com/smartclinic/hms/AI-CONTEXT.md)
- `src/main/resources/templates/` — [AI-CONTEXT.md](src/main/resources/templates/AI-CONTEXT.md)
- `src/main/resources/static/` — [AI-CONTEXT.md](src/main/resources/static/AI-CONTEXT.md)
- `doc/` — 규칙 및 설계 문서 (PRD, API, RULE 등)
- `.ai/` — 태스크, 워크플로우, 스킬

## 참고 문서

- 전체 요구사항: `doc/PRD.md`
- API 명세: `doc/API.md`
- 코딩 규칙: `doc/RULE.md` + `doc/rules/`
- 규칙 준수 현황: `doc/RULE_COMPLIANCE_REPORT.md`
