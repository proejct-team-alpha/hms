# HMS Task Template

> This template is used together with `.ai/workflows/workflow.md`.
> Tasks created from this template must follow the workflow execution order.

## AI Summary

- Linux 서버 배포 환경 구성: MySQL 연결, 환경변수 설정, 배포 스크립트 작성
- application-prod.properties 운영 설정 확정 및 프로필 분리 검증
- v1.0 태그 생성 및 최종 MVP 동작 확인
- API Key, DB 비밀번호 등 민감정보 환경변수 관리 최종 점검

## 1) Task Meta

- Task ID: TASK-lead-005-deployment
- Task Name: 운영 서버 배포 & v1.0 릴리스
- ACTIVE_ROLE: `LEAD`
- Scope (URL): 전체
- Scope (Module): config, 배포 스크립트
- Status: `TODO`

## 2) Goal

- Problem: 개발 환경(H2 인메모리)에서만 동작하는 상태이며, Linux 운영 서버 배포 환경이 구성되지 않음. 최종 MVP를 운영에 배포하고 v1.0 태그를 생성해야 함.
- Expected Outcome:
  - Linux 서버에서 MySQL 기반으로 HMS 운영 가능
  - 배포 스크립트(빌드 → 실행) 완성
  - 환경변수 기반 설정 관리 (API Key, DB 비밀번호 등)
  - v1.0 태그 생성 및 전체 MVP 동작 확인
- Out of Scope: CI/CD 파이프라인 구축, Docker 컨테이너화, 모니터링 시스템

## 3) Context Loading Checklist

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/SKILL_LEAD.md` 확인
- [ ] 외부 documents 저장소 확인 (배포 가이드)

### 3.1 Core Rules Summary 핵심 규칙 요약

#### AGENTS.md

1. 민감정보 환경변수 관리, `.gitignore` 필수
2. 시크릿 커밋 및 하드코딩 금지
3. 환경 변수: DB 비밀번호 등 민감 정보는 환경 변수로 주입
4. 변경 요약 시 "참조 문서"와 "적용한 버전" 명시
5. main 직접 커밋/푸시 금지

#### .ai/memory.md

1. 배포 안정 브랜치는 `main`
2. main 직접 커밋/푸시 금지
3. 민감정보는 환경변수로만 관리
4. 커밋 메시지는 Conventional Commits 사용
5. PR 기반 머지만 허용

#### doc/PROJECT_STRUCTURE.md

1. `application-prod.properties.example` 운영 템플릿 존재
2. `.env.example` 환경 변수 템플릿 존재
3. `run-dev.ps1` 개발 실행 스크립트 존재
4. DB: H2 (dev) / MySQL (prod)
5. 빌드 도구: Gradle

#### doc/RULE.md

1. 환경 변수로 민감 정보 주입
2. `.gitignore` 필수
3. 프로필별 설정 분리 (dev/prod)
4. Security 환경별 origins 설정
5. 핵심 로직 테스트 없이 배포 금지

#### doc/SKILL\_LEAD.md (optional)

1. W4 후반(Day 3~5): Linux 서버 배포
2. 배포 스크립트, 환경 설정 → 운영 서버 동작이 완료 기준
3. 최종 배포 완료: v1.0 태그
4. API Key 환경변수 관리 확인 (.gitignore 등록)
5. 전체 MVP 동작 확인

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론: 배포 절차는 외부 배포 가이드를 따르되, 보안/환경변수 규칙은 `RULE.md`와 `memory.md`를 우선한다.

## 4) Plan (Step A)

### 4.1 Candidate Files

Create:

- Script: `deploy.sh` (Linux 배포 스크립트)
- Config: `application-prod.properties` (`application-prod.properties.example` 기반)

Modify:

- Config: `build.gradle` (bootJar 설정 확인)
- Config: `.gitignore` (`.env`, `application-prod.properties` 등록 확인)

Test:

- 수동 검증 (배포 후 전체 흐름 동작 확인)

### 4.2 Interface Type

- Delivery Type: 해당 없음 (배포/인프라 태스크)
- SSR/PRG 적용 여부: 해당 없음
- 표준 응답 포맷 적용 여부(JSON): 해당 없음

### 4.3 Requirements

Functional

1. `./gradlew bootJar`로 실행 가능한 JAR 빌드
2. `deploy.sh`: 빌드 → 기존 프로세스 종료 → 새 프로세스 시작 → 헬스체크
3. MySQL 연결 설정 (application-prod.properties)
4. 환경변수 로딩 (.env 파일 또는 시스템 환경변수)
5. 프로필 활성화: `--spring.profiles.active=prod`

Validation

1. MySQL 연결 성공 확인
2. 모든 엔드포인트 접근 가능 확인 (역할별 로그인 → 대시보드)
3. LLM API 연동 확인 (Claude API Key 환경변수 주입)

Authorization

1. 운영 환경에서도 동일한 SecurityConfig URL 패턴 적용 확인
2. CSRF 동작 확인

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부: 기존 프로필 분리 구조 유지 (dev/prod)
- 트랜잭션 정책: 운영 DDL은 `validate` 또는 `update` (create-drop 금지)
- Validation 적용: 해당 없음
- 권한 정책: SecurityConfig 동일 적용
- 배포 스크립트:
  - 빌드: `./gradlew clean bootJar`
  - 실행: `nohup java -jar build/libs/hms-*.jar --spring.profiles.active=prod &`
  - 헬스체크: `curl -f http://localhost:8080/login` 또는 Spring Actuator
- 환경변수 관리:
  - `CLAUDE_API_KEY`: Claude API 키
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: MySQL 접속 정보
  - `SERVER_PORT`: 서버 포트 (기본 8080)

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [ ] 배포 전 전체 테스트 스위트 통과 확인 (`./gradlew test`)
- [ ] 배포 스크립트 실행 → 서버 기동 확인
- [ ] 역할별 로그인 성공 확인 (admin01, staff01, doctor01, nurse01)
- [ ] 예약 → 접수 → 진료 E2E 수동 검증
- [ ] LLM 증상 분석 동작 확인
- [ ] CSRF 토큰 동작 확인 (POST 요청)
- [ ] 403/404 에러 페이지 렌더링 확인

### 6.2 Executed Tests

- 테스트 클래스: (배포 후 기록)
- 결과: (배포 후 기록)

## 7) Report (Step D)

### 7.1 Changed Files

- (배포 후 기록)

### 7.2 Implementation Summary

- (배포 후 기록)

### 7.3 References + Versions

- Local:
  - `doc/RULE.md`
  - `doc/PROJECT_STRUCTURE.md`
- External(documents):
  - 배포 가이드 / v1.0
  - 아키텍처 정의서 / v2.0

### 7.4 Verification Result

- 실행한 테스트: (배포 후 기록)
- 수동 검증: (배포 후 기록)

### 7.5 TODO / Risk / Escalation

- TODO: 모든 통합 테스트(TASK-lead-004) 통과 후 배포 진행
- TODO: MySQL 스키마 마이그레이션 전략 확정 (Flyway 도입 고려)
- Risk: H2 → MySQL 전환 시 SQL 호환성 이슈 (MODE=MySQL로 완화하나 완전하지 않음)
- Risk: Claude API Key 노출 방지 — 서버 환경변수 설정 필수
- Escalation: 서버 인프라 접근 권한 필요 시 팀장에게 요청

## 8) Safety & Ownership Gates

### 8.1 Forbidden Check

- [x] URL prefix 임의 변경 없음
- [x] 임의 패키지/모듈 생성 없음
- [x] 민감정보 하드코딩 없음 (환경변수 사용)

### 8.2 Ownership Check

- [x] ACTIVE_ROLE 범위 내 수정만 수행 (배포는 LEAD 책임)
- [x] 범위 밖 변경 필요 시 에스컬레이션 메모 작성

## 9) Done Definition

- [ ] 운영 서버 기동 및 전체 MVP 동작 확인
- [ ] 역할별 로그인 + 대시보드 접근 확인
- [ ] v1.0 태그 생성
- [ ] Report 작성 완료
- [ ] 금지사항 위반 없음 (특히 민감정보 관리)
