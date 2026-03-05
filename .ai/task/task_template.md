# HMS Task Template

> This template is used together with `.ai/workflows/workflow.md`.
> Tasks created from this template must follow the workflow execution order.

## Task ID Convention

Task ID must follow this format:

`TASK-{role}-{number}-{feature}`

Examples:

- `TASK-devC-001-admin-dashboard`
- `TASK-devC-002-admin-reservation-list`
- `TASK-devA-001-reservation-create`
- `TASK-devB-001-reception-process`

Rules:

- `role`: `devA` | `devB` | `devC` | `lead` | `unknown`
- `number`: 3-digit incremental number per role
- `feature`: short kebab-case description

## AI Summary 작성 규칙

AI Summary는 작업 내용을 빠르게 이해할 수 있도록 **핵심 변경 사항만 3~6줄로 요약**한다.  
상세 구현 내용은 아래 섹션(Plan, Implementation, Report 등)에 작성한다.

작성 기준:

1. **무엇을 구현/수정했는지** (Feature 또는 작업 목적)
2. **핵심 변경 사항** (컨트롤러, 서비스, API, 화면 등)
3. **보안/권한 또는 주요 동작 변화**
4. **검증 결과** (테스트 또는 기본 동작 확인)

권장 형식:

- 작업 목적 또는 Feature 요약
- 주요 구현 내용 (엔드포인트, 화면, 로직 등)
- 권한 정책 또는 핵심 동작
- 테스트 또는 검증 결과

주의사항:

- 6줄을 넘기지 않는다.
- 코드 블록은 사용하지 않는다.
- 상세 설명은 아래 섹션에 작성한다.

## AI Summary

-
-
-

## 1) Task Meta

- Task ID:
- Task Name:
- ACTIVE_ROLE: `{DEV_A | DEV_B | DEV_C | LEAD | UNKNOWN}`
- Scope (URL): (예: /admin/**, /reservation/**)
- Scope (Module): (예: admin, reservation)
- Status: `{TODO | IN_PROGRESS | REVIEW | DONE}`

## 2) Goal

- Problem:
- Expected Outcome:
- Out of Scope:

## 3) Context Loading Checklist

- [ ] `AGENTS.md` 확인
- [ ] `.ai/memory.md` 확인
- [ ] `doc/PROJECT_STRUCTURE.md` 확인
- [ ] `doc/RULE.md` 확인
- [ ] `doc/SKILL_{ACTIVE_ROLE}.md` 확인 (있을 때)
- [ ] 외부 documents 저장소 확인 (필요할 때)

### 3.1 Core Rules Summary 핵심 규칙 요약 (필요 시)

#### AGENTS.md

1.
2.
3.
4.
5.

#### .ai/memory.md

1.
2.
3.
4.
5.

#### doc/PROJECT_STRUCTURE.md

1.
2.
3.
4.
5.

#### doc/RULE.md

1.
2.
3.
4.
5.

#### doc/SKILL\_{ACTIVE_ROLE}.md (optional)

1.
2.
3.
4.
5.

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론:

## 4) Plan (Step A)

### 4.1 Candidate Files

Create:

- Controller:
- Service:
- Repository:
- DTO:
- Template:

Modify:

- Controller: (이유)
- Service: (이유)
- Repository: (이유)
- DTO: (이유)

Test:

- ServiceTest:
- ControllerTest:

### 4.2 Interface Type

- Delivery Type: `{SSR | JSON API | Mixed}`
- SSR/PRG 적용 여부:
- 표준 응답 포맷 적용 여부(JSON):

### 4.3 Requirements

Functional

1.
2.
3.

Validation

1.
2.

Authorization

1.

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부
- 트랜잭션 정책
- Validation 적용
- 권한 정책

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [ ] Service 테스트 (Mockito)
- [ ] Controller 테스트 (MockMvc + ROLE)
- [ ] 실패 케이스 최소 1개
- [ ] Given-When-Then 주석 적용

### 6.2 Executed Tests

- 테스트 클래스
- 결과

## 7) Report (Step D)

### 7.1 Changed Files

-

### 7.2 Implementation Summary

-

### 7.3 References + Versions

- Local:
  - `doc/RULE.md`
  - `doc/PROJECT_STRUCTURE.md`
- External(documents):
  - 문서명 / 버전:
  - 문서명 / 버전:

### 7.4 Verification Result

- 실행한 테스트:
- 수동 검증:

### 7.5 TODO / Risk / Escalation

- TODO:
- Risk:
- Escalation:

## 8) Safety & Ownership Gates

### 8.1 Forbidden Check

- [ ] URL prefix 임의 변경 없음
- [ ] 임의 패키지/모듈 생성 없음
- [ ] 민감정보 하드코딩 없음

### 8.2 Ownership Check

- [ ] ACTIVE_ROLE 범위 내 수정만 수행
- [ ] 범위 밖 변경 필요 시 에스컬레이션 메모 작성

## 9) Done Definition

- [ ] 기능 동작 확인
- [ ] 테스트 통과
- [ ] Report 작성 완료
- [ ] 금지사항 위반 없음
