# HMS Workflow — Universal (Role & Tool Agnostic)

## 0) 목적

이 문서는 HMS 프로젝트에서 어떤 AI/자동화 에이전트(또는 사람)가 작업하더라도
동일한 품질/규칙/절차로 개발을 수행하게 하는 범용 워크플로우다.

---

## 1) Activation

작업 시작 시 역할을 선언한다.

`ACTIVE_ROLE: {DEV_A | DEV_B | DEV_C | LEAD | UNKNOWN}`

역할에 따라 수정 가능한 모듈 범위를 제한한다.

`ACTIVE_ROLE`이 `UNKNOWN`이면:

- 먼저 레포 구조/OWNERSHIP/skill 문서를 탐색해 역할을 확정하거나,
- 작업 범위를 안전한 문서/테스트/리팩토링으로 제한한다.

---

## 2) Context Loading (작업 시작마다 필수)

작업 시작 전 아래 문서를 읽고 시작한다.

### Core Context

1. `AGENTS.md` (프로젝트 기준 AGENTS 경로)
2. `.ai/memory.md`
3. `doc/PROJECT_STRUCTURE.md`
4. `doc/RULE.md`

### Role Context (가능하면)

5. `doc/SKILL_{ACTIVE_ROLE}.md` (예: `doc/SKILL_DEV_C.md`, 없으면 스킵)

### External Specs (필요할 때만)

6. documents 저장소(예: `project-team-alpha/documents`)의 최신 권장본을 참조한다.
   API 명세, 화면 기능 정의, 화면 시퀀스, 아키텍처 정의, 테스트 규칙, 리뷰 규칙을 확인하고,
   문서별 권장 버전을 Report에 명시한다.
   권장 버전 표기가 없으면 저장소 기본 브랜치의 최신 커밋 기준으로 적용한다.

### 2.1 읽기 산출물(강제)

- 각 문서에서 이번 작업에 적용되는 규칙 5줄 요약
- 충돌이 있으면 문서 우선순위로 결론 1줄

### 2.2 문서 우선순위

1. `doc/PROJECT_STRUCTURE.md`, `doc/RULE.md`
2. `.ai/AGENTS.md`, `.ai/memory.md`
3. 외부 documents 저장소 최신 권장 문서

---

## 3) Work Loop (Plan → Implement → Test → Report)

### Step A. Plan

- 변경/추가 파일 후보 목록 작성
- SSR vs JSON API 구분
- 입력/출력/검증/권한(ROLE) 요건을 5줄로 정리

### Step B. Implement

- 구조 규칙: `doc/PROJECT_STRUCTURE.md` 준수
- 코딩 규칙: `doc/RULE.md` 준수
- SSR이면 PRG 패턴(POST → redirect → GET)
- JSON이면 표준 응답 포맷 준수(프로젝트 규칙 기준)
- DTO/Validation/@Transactional 정책 준수

### Step C. Test

- 핵심 로직은 테스트 없이 반영 금지
- 최소 1개 이상 테스트 추가/수정
- Service 단위 테스트 우선(Mockito)
- Controller는 MockMvc + 권한(ROLE) 케이스 포함
- 실패 케이스 최소 1개 포함(유효성/권한/상태전이 등)

### Step D. Report (산출물 포맷 고정)

- 변경 파일 목록
- 구현 기능 요약
- 참조 문서(로컬/외부) + 적용 버전
- 검증 결과(실행한 테스트)
- TODO / 리스크 / 에스컬레이션

---

## 4) Safety & Ownership Gates (작업 전/후 체크)

### 4.1 금지

- URL prefix 임의 변경 금지
- 로컬 구조 문서와 다른 임의 패키지/모듈 생성 금지
- 민감정보 하드코딩 금지

### 4.2 소유권

- `ACTIVE_ROLE` 범위 밖 파일을 수정해야 하면 직접 수정하지 않는다.
- 대신 에스컬레이션 메모(왜 필요한지/어디를 바꿔야 하는지)만 남긴다.

---

## 5) Done Definition

- 기능 동작 확인
- 테스트 통과
- Report 작성 완료
- 금지 사항 위반 없음
