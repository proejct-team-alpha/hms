# HMS Task Template

> This template is used together with `.ai/workflows/workflow.md`.
> Tasks created from this template must follow the workflow execution order.

## AI Summary

- 전체 MVP 흐름(예약→접수→진료→완료) E2E 통합 테스트 및 보안 회귀 테스트 실행
- SecurityConfig URL 패턴별 ROLE 접근 통제 회귀 테스트로 보안 정책 무결성 확인
- JaCoCo 커버리지 점검: Service 80%+, Repository 70%+ 달성 여부 확인
- API Key 환경변수 관리, CSRF 토큰, 세션 보안 등 보안 체크리스트 전체 통과

## 1) Task Meta

- Task ID: TASK-lead-004-integration-security
- Task Name: 통합 테스트 & 보안 체크포인트 & 커버리지 점검
- ACTIVE_ROLE: `LEAD`
- Scope (URL): 전체 URL 패턴 (/, /reservation/**, /staff/**, /doctor/**, /nurse/**, /admin/**, /llm/**)
- Scope (Module): 전체 (통합 검증)
- Status: `TODO`

## 2) Goal

- Problem: 개별 모듈 개발 완료 후 전체 흐름 통합 검증과 보안 회귀 테스트가 필요. JaCoCo 커버리지 목표 달성 여부 확인 필요.
- Expected Outcome:
  - 예약 → 접수 → 진료 → 완료 E2E 시나리오 통과
  - LLM 추천 예약 → 폴백 시나리오 통과
  - 모든 URL 패턴 ROLE 접근 통제 회귀 테스트 통과
  - CSRF, 세션 보안, API Key 환경변수 관리 체크리스트 통과
  - JaCoCo 커버리지: Service 80%+, Repository 70%+
- Out of Scope: 개별 모듈 단위 테스트 (각 개발자 영역), 성능 테스트

## 3) Context Loading Checklist

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/SKILL_LEAD.md` 확인
- [ ] 외부 documents 저장소 확인 (테스트 전략서, 코드 리뷰 규칙 v2)

### 3.1 Core Rules Summary 핵심 규칙 요약

#### AGENTS.md

1. 핵심 비즈니스 로직 테스트 없이 배포 금지
2. 보안 테스트는 역할별 허용/차단 케이스 모두 검증
3. 통합 테스트는 핵심 플로우 최소 세트만 유지
4. 민감정보 환경변수 관리, `.gitignore` 필수
5. CSRF 규칙을 깨는 요청 처리 패턴 도입 금지

#### .ai/memory.md

1. 인증은 세션 기반, 인가는 역할 기반 강제
2. SecurityConfig URL 패턴별 ROLE 접근 제어
3. 미인증 접근은 로그인 리다이렉트, 무권한 접근은 403 처리
4. API Key 환경변수 관리
5. 보안/공통/엔티티 레이어는 보호 영역

#### doc/PROJECT_STRUCTURE.md

1. URL 패턴별 접근 권한 매핑 (§5.2)
2. 테스트 계정 4개 (admin01, staff01, doctor01, nurse01)
3. CSRF 토큰 사용법 정의
4. SecurityConfig, WebMvcConfig 구조
5. 에러 페이지 (403, 404) 템플릿 존재

#### doc/RULE.md

1. Controller 테스트: `@WebMvcTest` + MockMvc
2. 통합 테스트: `@SpringBootTest` 최소화
3. 보안 테스트: spring-security-test 활용
4. 외부 DB 의존 금지, H2 사용
5. Given-When-Then 구조 필수

#### doc/SKILL\_LEAD.md (optional)

1. W4 후반: 통합 테스트, 보안 체크포인트, JaCoCo 커버리지
2. SecurityConfig URL 패턴 테스트(MockMvc): 모든 URL 패턴별 ROLE 접근 제어 검증
3. 커버리지 목표: SecurityConfig 100%, Service 80%+, Repository 70%+
4. W4 체크포인트: ROLE URL 접근 통제 회귀 테스트 통과
5. API Key 환경변수 관리 확인 (.gitignore 등록)

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론: 보안 정책은 SecurityConfig + `memory.md` 역할 규칙을 기준으로, 테스트 패턴은 `RULE.md`를 따른다.

## 4) Plan (Step A)

### 4.1 Candidate Files

Create:

- Test: `SecurityConfigIntegrationTest.java` (URL 패턴별 ROLE 접근 통제)
- Test: `ReservationFlowIntegrationTest.java` (예약→접수→진료 E2E)
- Test: `LlmFlowIntegrationTest.java` (LLM 추천 + 폴백 E2E)

Modify:

- Config: `build.gradle` (JaCoCo 플러그인 설정 확인/추가)

Test:

- ControllerTest: `SecurityConfigIntegrationTest.java`
- IntegrationTest: `ReservationFlowIntegrationTest.java`
- IntegrationTest: `LlmFlowIntegrationTest.java`

### 4.2 Interface Type

- Delivery Type: 해당 없음 (테스트 + 점검 태스크)
- SSR/PRG 적용 여부: 해당 없음
- 표준 응답 포맷 적용 여부(JSON): 해당 없음

### 4.3 Requirements

Functional

1. SecurityConfig URL 패턴 회귀 테스트: 모든 역할(ADMIN, STAFF, DOCTOR, NURSE) × 모든 URL 패턴 매트릭스 검증
2. 예약 → 접수 → 진료 → 완료 E2E 통합 시나리오
3. LLM 증상 분석 → 추천 예약 → 폴백(API 실패 → 직접 선택) 시나리오
4. CSRF 토큰 검증 (POST 요청 시 토큰 없으면 403)
5. JaCoCo 커버리지 리포트 생성 및 목표 달성 확인

Validation

1. 미인증 사용자 → 인증 필요 URL 접근 시 로그인 리다이렉트
2. 권한 없는 역할 → 타 역할 URL 접근 시 403 응답

Authorization

1. 테스트에서 `@WithMockUser(roles = "...")` 사용하여 역할별 시뮬레이션

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부: 테스트 코드만 생성, build.gradle JaCoCo 설정 확인
- 트랜잭션 정책: 통합 테스트에서 `@Transactional` + 롤백으로 테스트 격리
- Validation 적용: 해당 없음
- 권한 정책: spring-security-test의 `@WithMockUser`, `SecurityMockMvcRequestPostProcessors.csrf()` 활용
- 보안 체크리스트:
  - [ ] API Key가 환경변수로만 관리되는지 확인
  - [ ] `.env`가 `.gitignore`에 포함되어 있는지 확인
  - [ ] CSRF 토큰이 모든 POST 폼에 포함되어 있는지 확인
  - [ ] 세션 고정 공격 방어 설정 확인

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [ ] SecurityConfigIntegrationTest (MockMvc + ROLE)
  - [ ] 비인증 → /admin/** 접근 시 로그인 리다이렉트
  - [ ] ROLE_STAFF → /admin/** 접근 시 403
  - [ ] ROLE_ADMIN → /admin/** 접근 시 200
  - [ ] ROLE_DOCTOR → /doctor/** 접근 시 200
  - [ ] ROLE_NURSE → /nurse/** 접근 시 200
  - [ ] 비인증 → /reservation/** 접근 시 200 (공개)
  - [ ] 비인증 → /llm/symptom/** 접근 시 200 (공개)
  - [ ] ROLE_STAFF → /llm/rules/** 접근 시 403
  - [ ] ROLE_DOCTOR → /llm/rules/** 접근 시 200
  - [ ] CSRF 미포함 POST 요청 시 403
- [ ] ReservationFlowIntegrationTest
  - [ ] 예약 생성 → 접수 처리 → 진료 완료 순차 통과
  - [ ] 예약 생성 → 취소 시나리오 통과
- [ ] LlmFlowIntegrationTest
  - [ ] 증상 분석 → 추천 결과 반환 시나리오
  - [ ] API 실패 → 폴백(직접 선택) 시나리오
- [ ] JaCoCo 커버리지 확인
  - [ ] Service 레이어 80%+
  - [ ] Repository 레이어 70%+
- [ ] Given-When-Then 주석 적용

### 6.2 Executed Tests

- 테스트 클래스: (구현 후 기록)
- 결과: (구현 후 기록)

## 7) Report (Step D)

### 7.1 Changed Files

- (구현 후 기록)

### 7.2 Implementation Summary

- (구현 후 기록)

### 7.3 References + Versions

- Local:
  - `doc/RULE.md`
  - `doc/PROJECT_STRUCTURE.md`
- External(documents):
  - 테스트 전략서 / v1.0
  - 코드 리뷰 규칙 / v2.0
  - 아키텍처 정의서 / v2.0

### 7.4 Verification Result

- 실행한 테스트: (구현 후 기록)
- 수동 검증: (구현 후 기록)

### 7.5 TODO / Risk / Escalation

- TODO: 모든 모듈 개발 완료 후(W4 후반) 실행 가능
- TODO: JaCoCo 미달 시 부족 모듈 테스트 보완 요청
- Risk: 통합 테스트 환경에서 H2와 MySQL 동작 차이
- Escalation: 커버리지 미달 모듈은 해당 개발자에게 테스트 보완 요청

## 8) Safety & Ownership Gates

### 8.1 Forbidden Check

- [x] URL prefix 임의 변경 없음
- [x] 임의 패키지/모듈 생성 없음
- [x] 민감정보 하드코딩 없음

### 8.2 Ownership Check

- [x] ACTIVE_ROLE 범위 내 수정만 수행 (통합 테스트는 LEAD 책임)
- [x] 범위 밖 변경 필요 시 에스컬레이션 메모 작성

## 9) Done Definition

- [ ] SecurityConfig 회귀 테스트 전체 통과
- [ ] E2E 통합 시나리오 통과 (예약→접수→진료, LLM 폴백)
- [ ] 보안 체크리스트 전체 통과
- [ ] JaCoCo 커버리지 목표 달성 (Service 80%+, Repository 70%+)
- [ ] Report 작성 완료
- [ ] 금지사항 위반 없음
