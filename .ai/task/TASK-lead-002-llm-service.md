# HMS Task Template

> This template is used together with `.ai/workflows/workflow.md`.
> Tasks created from this template must follow the workflow execution order.

## AI Summary

- Claude API 연동 LlmService 구현: 증상 분석(`analyzeSymptom`)과 규칙 챗봇(`askChatbot`) 두 가지 핵심 기능
- SymptomRequest/Response, ChatbotRequest/Response DTO 설계 및 팀 공유
- API 실패 시 폴백 처리(직접 선택 전환) 구현으로 서비스 안정성 확보
- Mock 기반 테스트(정상 응답 파싱, 타임아웃, 잘못된 JSON, 폴백) 80%+ 커버리지 목표

## 1) Task Meta

- Task ID: TASK-lead-002-llm-service
- Task Name: LlmService 구현 (증상 분석 + 규칙 챗봇 + DTO + 폴백)
- ACTIVE_ROLE: `LEAD`
- Scope (URL): /llm/symptom/**, /llm/rules/**
- Scope (Module): llm
- Status: `TODO`

## 2) Goal

- Problem: LLM 기반 증상 분석(F06)과 규칙 챗봇(F06) 기능이 미구현 상태. 개발자 A(증상 UI)와 개발자 B(챗봇 UI)가 W4에서 UI를 연동하려면 W3까지 LlmService와 DTO가 확정되어야 함.
- Expected Outcome:
  - `LlmService.analyzeSymptom()`: 증상 텍스트 → 추천 진료과/의사/시간 JSON 파싱 반환
  - `LlmService.askChatbot()`: 병원 규칙 기반 Q&A 대화 기능
  - DTO: SymptomRequest/Response, ChatbotRequest/Response Record 확정
  - 폴백: API 타임아웃/에러 시 직접 선택 전환 처리
- Out of Scope: 증상 추천 UI (개발자 A), 챗봇 대화 UI (개발자 B), LlmRecommendation/ChatbotHistory 저장 로직 (연동부에서 처리)

## 3) Context Loading Checklist

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/SKILL_LEAD.md` 확인
- [ ] 외부 documents 저장소 확인 (API 명세서 §4, §8)

### 3.1 Core Rules Summary 핵심 규칙 요약

#### AGENTS.md

1. 예외는 `CustomException` + `GlobalExceptionHandler` 포맷 유지
2. DTO는 Record 우선, 입력은 `@Valid`/`@Validated` 검증
3. 외부 API 실호출 테스트 금지, Mock 대체
4. 민감정보(API Key) 환경변수로만 관리
5. JSON 응답은 AJAX/`/api/**` 등 필요한 경로에 한정

#### .ai/memory.md

1. LlmService 수정 금지 (다른 개발자) — 인터페이스 호출만 허용
2. LLM 규칙 챗봇은 DOCTOR/NURSE 중심 접근 정책
3. `/llm/symptom/**`는 전체 공개
4. `/llm/rules/**`는 DOCTOR, NURSE 접근
5. 서비스 레이어가 트랜잭션 경계를 소유

#### doc/PROJECT_STRUCTURE.md

1. ClaudeApiConfig.java 이미 존재 (RestClient 빈 설정)
2. LlmRecommendation 엔티티: symptomText, recommendedDept/Doctor/Time, isUsed
3. ChatbotHistory 엔티티: sessionId, Staff(FK), question, answer
4. 에러코드: LLM_SERVICE_UNAVAILABLE, LLM_PARSE_ERROR
5. application-dev.properties: claude-sonnet-4-6, 5초 타임아웃

#### doc/RULE.md

1. 서비스 레이어 중심 설계
2. 외부 API Mock 테스트 필수
3. 시간/랜덤 결정성 보장
4. async/await 중심 (프론트 연동 시)
5. 에러 처리: try-catch + 명시적 실패 처리

#### doc/SKILL\_LEAD.md (optional)

1. W3 후반(Day 4~5)에 Claude API 연동 + LlmService 구현
2. W4 전반(Day 1~2)에 LlmService 완성 + 폴백 처리 + develop 머지
3. LLM DTO 확정 후 금요일 미팅에서 팀 공유
4. 리스크 대응: LlmService 미완성 시 개발자 A/B는 더미 데이터로 진행
5. 테스트 커버리지 목표: LlmService 80%+ (정상/타임아웃/파싱에러/폴백)

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론: LLM 기능 스펙은 외부 API 명세서 §4, §8을 따르고, ClaudeApiConfig 구조는 기존 코드를 유지하며, 코딩 규칙은 `RULE.md`를 우선한다.

## 4) Plan (Step A)

### 4.1 Candidate Files

Create:

- Service: `llm/LlmService.java`
- DTO: `llm/dto/SymptomAnalysisRequest.java` (Record)
- DTO: `llm/dto/SymptomAnalysisResponse.java` (Record)
- DTO: `llm/dto/ChatbotRequest.java` (Record)
- DTO: `llm/dto/ChatbotResponse.java` (Record)

Modify:

- Config: `config/ClaudeApiConfig.java` (필요 시 RestClient 설정 보완)

Test:

- ServiceTest: `LlmServiceTest.java`

### 4.2 Interface Type

- Delivery Type: `Mixed` (서비스 자체는 내부, 향후 Controller에서 SSR + AJAX 모두 사용)
- SSR/PRG 적용 여부: 해당 없음 (서비스 레이어)
- 표준 응답 포맷 적용 여부(JSON): DTO Record로 표준화

### 4.3 Requirements

Functional

1. `analyzeSymptom(SymptomAnalysisRequest)`: 증상 텍스트를 Claude API로 전송, 추천 진료과/의사/시간 JSON 파싱 반환
2. `askChatbot(ChatbotRequest)`: 병원 규칙 컨텍스트 + 사용자 질문을 Claude API로 전송, 답변 반환
3. Claude API 호출 시 시스템 프롬프트(병원 컨텍스트) 자동 포함
4. API 타임아웃(5초) 초과 시 `LLM_SERVICE_UNAVAILABLE` 예외 → 폴백 처리
5. 응답 JSON 파싱 실패 시 `LLM_PARSE_ERROR` 예외 → 폴백 처리

Validation

1. 증상 텍스트 빈 값/null 검증
2. 챗봇 질문 빈 값/null 검증

Authorization

1. 서비스 레이어 자체 권한 없음 (Controller에서 URL 기반 접근 제어)
2. `/llm/symptom/**`: 전체 공개
3. `/llm/rules/**`: DOCTOR, NURSE만 접근 (SecurityConfig에서 처리)

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부: `llm` 패키지에 서비스 + DTO 배치, 인터페이스 분리 불필요 (단일 구현체)
- 트랜잭션 정책: 외부 API 호출 서비스이므로 `@Transactional` 불필요 (DB 쓰기 없음)
- Validation 적용: DTO에 `@NotBlank` 등 Bean Validation, 서비스에서 CustomException 변환
- 권한 정책: SecurityConfig URL 패턴으로 이미 제어됨
- API Key 관리: `${CLAUDE_API_KEY}` 환경변수, 절대 하드코딩 금지
- 폴백 전략: try-catch로 API 호출 감싸고, 실패 시 CustomException.serviceUnavailable() 던져 호출부에서 직접 선택 전환 유도
- RestClient 사용: ClaudeApiConfig에서 생성한 RestClient 빈 주입

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [ ] LlmService 테스트 (Mockito — RestClient Mock)
  - [ ] analyzeSymptom 정상 응답 파싱 검증
  - [ ] analyzeSymptom 타임아웃 시 LLM_SERVICE_UNAVAILABLE 예외
  - [ ] analyzeSymptom 잘못된 JSON 응답 시 LLM_PARSE_ERROR 예외
  - [ ] askChatbot 정상 응답 검증
  - [ ] askChatbot API 실패 시 폴백 예외 발생
  - [ ] 빈 증상 텍스트 입력 시 검증 실패
- [ ] 실패 케이스 최소 3개 (타임아웃, 파싱에러, 폴백)
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
  - API 명세서 / v5.2 권장 (§4 증상 분석, §8 규칙 챗봇)
  - 아키텍처 정의서 / v2.0

### 7.4 Verification Result

- 실행한 테스트: (구현 후 기록)
- 수동 검증: (구현 후 기록)

### 7.5 TODO / Risk / Escalation

- TODO: LLM DTO 확정 후 개발자 A, B에게 인터페이스 공유 (W3 금요일 미팅)
- Risk: Claude API 응답 포맷 변경 시 파싱 로직 수정 필요
- Risk: 5초 타임아웃이 실제 운영에서 부족할 수 있음 (모니터링 필요)
- Escalation: 없음

## 8) Safety & Ownership Gates

### 8.1 Forbidden Check

- [x] URL prefix 임의 변경 없음 (`/llm/**` 기존 구조 유지)
- [x] 임의 패키지/모듈 생성 없음 (기존 `llm` 모듈 사용)
- [x] 민감정보 하드코딩 없음 (API Key 환경변수)

### 8.2 Ownership Check

- [x] ACTIVE_ROLE 범위 내 수정만 수행 (llm/LlmService는 LEAD 소유)
- [x] 범위 밖 변경 필요 시 에스컬레이션 메모 작성

## 9) Done Definition

- [ ] 기능 동작 확인 (analyzeSymptom, askChatbot 정상 호출)
- [ ] 테스트 통과 (LlmService 80%+, 정상/타임아웃/파싱에러/폴백)
- [ ] DTO 인터페이스 팀 공유 완료
- [ ] Report 작성 완료
- [ ] 금지사항 위반 없음
