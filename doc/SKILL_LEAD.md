# 책임개발자 (김민구) — SKILL.md

> **역할:** Tech Lead
> **담당:** 전체 아키텍처, Security, 예약 핵심 로직, LlmService + Claude API 연동, 코드 리뷰, 배포
> **기준 문서:** PRD v1.0 / 프로젝트 계획서 v4.3 / 아키텍처 정의서 v2.0

---

## 1. 소유 영역

### 1.1 코드 소유권

| 영역 | 경로 | 설명 |
|------|------|------|
| **config** | `config/**` | SecurityConfig, WebMvcConfig, ClaudeApiConfig, ErrorPageController |
| **common** | `common/**` | GlobalExceptionHandler, CustomException, LayoutModelInterceptor, ReservationNumberGenerator |
| **domain** | `domain/**` | 전체 Entity 10개 + Enum 5개 |
| **common/service** | `common/service/**` | SlotService, ReservationValidationService |
| **llm** | `llm/LlmService.java` | Claude API 연동 서비스 |
| **설정 파일** | `application*.properties` | 환경 설정 |
| **공통 템플릿** | `templates/common/**` | header, footer, sidebar 파셜 |

### 1.2 Git 브랜치

```
feature/auth
feature/common-layout
feature/slot-service
feature/entities
feature/llm-service
feature/llm-chatbot-backend
```

---

## 2. 관련 PRD 기능

| PRD 기능 | 역할 |
|----------|------|
| F01 (비회원 예약) | SlotService, ReservationValidationService 제공 |
| F02 (인증) | SecurityConfig, StaffUserDetailsService |
| F06 (LLM 통합) | LlmService 전체 구현 |
| 전체 | Entity, 예외 처리, 인터셉터, 코드 리뷰 |

---

## 3. 주차별 작업 상세

### W1 — 기반 구축 (가장 중요한 주차)

**목표:** 개발자 3명이 W2부터 병렬 개발 가능하도록 모든 공유 레이어 완성

#### W1 전반 (Day 1~2)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | ERD 설계 확정 (10개 테이블) | `domain/*.java` Entity | JPA 컴파일 성공, 관계 매핑 완료 |
| 2 | Spring Boot 프로젝트 셋업 | `build.gradle`, 패키지 구조 | 빌드 성공, 서버 기동 확인 |
| 3 | Git 브랜치 전략 | main, develop 브랜치 생성 | 브랜치 보호 규칙 적용 |
| 4 | application.properties 설정 | DB, Redis, CSRF 설정 | 로컬 서버 구동, DB 연결 확인 |

#### W1 후반 (Day 3~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 5 | SecurityConfig (4 ROLE 접근 통제) | SecurityConfig.java, CustomUserDetails.java | 4개 역할 로그인·접근 통제 동작 |
| 6 | 공통 레이아웃 & 인터셉터 | LayoutModelInterceptor.java, `layouts/*.mustache` | 헤더/사이드바/푸터 자동 렌더링, 역할별 메뉴 분기 |
| 7 | SlotService (중복 예약 방지) | SlotService.java, SlotServiceImpl.java | 중복 슬롯 예약 시 예외 발생, 테스트 통과 |
| 8 | 예외 처리 | GlobalExceptionHandler.java, CustomException.java | 에러 페이지 렌더링, 적절한 HTTP 상태 코드 |
| 9 | 예약번호 생성기 | ReservationNumberGenerator.java | 고유 번호 생성 확인 |
| 10 | **develop 브랜치 머지** | 공유 레이어 머지 완료 | 3명 개발자 pull 후 빌드 성공 |

#### W1 테스트

- SecurityConfig URL 패턴 테스트 (MockMvc): 모든 URL 패턴별 ROLE 접근 제어 검증
- 테스트 환경 셋업: `application-test.properties`, H2, JaCoCo
- 테스트 유틸리티: `TestEntityFactory.java`

#### W1 체크포인트

- [ ] 10개 Entity JPA 매핑 완료
- [ ] 4 ROLE 로그인 성공 확인
- [ ] SlotService 중복 방지 테스트 통과
- [ ] 공통 레이아웃 역할별 렌더링 확인
- [ ] develop 머지 후 전 팀원 빌드 성공

---

### W2 — 예약 핵심 로직 검증 & 코드 리뷰

**목표:** 예약·접수 핵심 트랜잭션 안정성 확보, 개발자 PR 리뷰

#### W2 전반 (Day 1~2)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | 중복 예약 트랜잭션 검증 | 트랜잭션 테스트 케이스 | 동시 예약 시 1건만 성공 |
| 2 | 개발자 A (강태오) PR 리뷰 (예약 폼) | 리뷰 코멘트 | SlotService 호출 패턴 검증 |
| 3 | 접수 구조 설계 지원 | 상태 전이 다이어그램 | RESERVED → RECEIVED 경로 명확화 |

#### W2 후반 (Day 3~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 4 | 개발자 B (조유지) PR 리뷰 (접수/방문접수) | 리뷰 코멘트 | 상태 전이 로직 정확성 확인 |
| 5 | 개발자 C (강상민) PR 리뷰 (관리자 CRUD) | 리뷰 코멘트 | 페이징·검색 패턴 검증 |
| 6 | 공통 이슈 해결 | 버그 수정, 공유 코드 개선 | 개발자 블로커 제거 |
| 7 | **금요일 통합 미팅** | develop 머지 | 예약 → 접수 E2E 동작 확인 |

#### W2 체크포인트

- [ ] 예약 생성 → 접수 처리 E2E 시나리오 통과
- [ ] 중복 예약 트랜잭션 테스트 통과
- [ ] 개발자별 최소 1건 PR 리뷰 완료
- [ ] v0.5 태그 생성

---

### W3 — 상태 흐름 검증 & LlmService 예비 작업

**목표:** 전체 상태 흐름 안정성 확보, W3 후반 LlmService 구현 시작

#### W3 전반 (Day 1~3)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | 전체 상태 흐름 검증 | 상태 전이 테스트 스위트 | RESERVED→RECEIVED→COMPLETED 순방향만 허용 |
| 2 | 관리자 구조 리뷰 | 개발자 C PR 리뷰 | 물품/규칙 CRUD 패턴 확인 |
| 3 | 개발자 B PR 리뷰 (진료 기록) | 리뷰 코멘트 | TreatmentService 상태 전이 검증 |

#### W3 후반 (Day 4~5): LlmService 예비 단계

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 4 | Claude API 연동 설정 | ClaudeApiConfig.java, WebClient 설정 | API 기본 동작 확인 |
| 5 | LlmService 증상 분석 구현 | `LlmService.analyzeSymptom()` | 증상 텍스트 → 추천 JSON 파싱 성공 |
| 6 | LlmService 챗봇 구현 | `LlmService.askChatbot()` | 병원 규칙 기반 Q&A 동작 |
| 7 | LLM DTO 확정 | SymptomRequest/Response, ChatbotRequest/Response | DTO 인터페이스 팀 공유 |
| 8 | **금요일 LLM DTO 미팅** | DTO 문서 배포 | 개발자 A, B W4 작업 계획 확정 |

#### W3 테스트

- LlmService Mock 테스트: 정상 응답 파싱, 타임아웃, 잘못된 JSON, 폴백
- SlotService 테스트: 시간 슬롯 가용성 로직

#### W3 수요일 리스크 대응

```
LlmService 미완성 시:
→ 개발자 A: 직접 선택 예약 흐름 완성 (추천 UI 보류)
→ 개발자 B: 챗봇 더미 UI 스켈레톤 완성 (하드코딩 데이터)
→ 책임개발자: W4 초반까지 LlmService 완성 우선
```

#### W3 체크포인트

- [ ] 상태 전이 테스트 스위트 전체 통과
- [ ] LlmService 기본 동작 확인 (수요일까지)
- [ ] SymptomResponse/ChatbotResponse DTO 확정 및 팀 공유
- [ ] v0.8 태그 생성

---

### W4 — LLM 완성 & 통합 테스트 & 배포

**목표:** LLM 기능 완성, 전체 MVP 배포

#### W4 전반 (Day 1~2)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | LlmService 완성 & develop 머지 | 최종 LlmService 버전 | 개발자 A, B pull 후 UI 연동 시작 |
| 2 | 폴백 처리 구현 | API 실패 → 직접 선택 전환 | 타임아웃·에러 폴백 확인 |
| 3 | LLM 관련 PR 리뷰 | 개발자 A, B LLM UI 리뷰 | DTO 사용 패턴 검증 |

#### W4 후반 (Day 3~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 4 | 통합 테스트 실행 | 전체 흐름 통합 테스트 | 예약→접수→진료 E2E, LLM 추천 예약, 폴백 시나리오 |
| 5 | 보안 체크포인트 | ROLE URL, CSRF, API Key 환경변수 | 보안 체크리스트 전체 통과 |
| 6 | JaCoCo 커버리지 확인 | 커버리지 리포트 | Service 80%+, Repository 70%+ |
| 7 | Linux 서버 배포 | 배포 스크립트, 환경 설정 | 운영 서버 동작 |
| 8 | **최종 배포 완료** | v1.0 태그 | 전체 MVP 동작 확인 |

#### W4 병렬 작업 구조

```
책임개발자(김민구)          개발자A(강태오)     개발자B(조유지)    개발자C(강상민)
─────────────────         ───────────────   ───────────────   ───────────────
LlmService 완성            증상 UI           챗봇 UI           규칙 CRUD
  ↓ develop 머지            추천 연결         대화 이력          관리자 UI 마무리
폴백 처리                  면책 조항 추가     최종 UI 점검       버그 수정
통합 테스트                버그 수정          버그 수정          버그 수정
보안 리뷰                  ────────────── 전체 배포 지원 ──────────────
배포
```

#### W4 체크포인트

- [ ] LlmService develop 머지 완료 (Day 1)
- [ ] 전체 흐름 통합 테스트 통과
- [ ] ROLE URL 접근 통제 회귀 테스트 통과
- [ ] JaCoCo 커버리지 목표 달성
- [ ] API Key 환경변수 관리 확인 (.gitignore 등록)
- [ ] v1.0 태그 및 운영 배포 완료

---

## 4. 코드 리뷰 책임

| 주차 | 리뷰 대상 | 핵심 검증 항목 |
|------|----------|---------------|
| W2 | 개발자 A — 예약 폼 PR | SlotService 호출 패턴, @Valid 검증 |
| W2 | 개발자 B — 접수/방문접수 PR | 상태 전이 로직, 트랜잭션 원자성 |
| W2 | 개발자 C — 관리자 CRUD PR | 페이징, 검색, BCrypt 처리 |
| W3 | 개발자 B — 진료 기록 PR | TreatmentService 상태 전이, 권한 검증 |
| W3 | 개발자 C — 물품/규칙 PR | CRUD 패턴, 카테고리 관리 |
| W4 | 개발자 A — LLM 증상 UI PR | DTO 사용, CSRF, 폴백 처리 |
| W4 | 개발자 B — 챗봇 UI PR | DTO 사용, AJAX, 접근 권한 |

---

## 5. 테스트 커버리지 목표

| 대상 | 목표 | 테스트 유형 |
|------|------|------------|
| SecurityConfig | 100% URL 패턴 | MockMvc |
| SlotService | 80%+ | 단위 (Mockito) |
| LlmService | 80%+ | Mock (정상/타임아웃/파싱에러/폴백) |
| 상태 전이 | 100% 경로 | 단위 |
| 통합 테스트 | E2E 시나리오 | 통합 |

---

## 6. 절대 보호 원칙 (다른 개발자 대상)

| 규칙 | 설명 |
|------|------|
| Entity 수정 금지 | `domain/*.java`는 책임개발자만 수정. 변경 필요 시 GitHub Issue 등록 |
| SecurityConfig 수정 금지 | 읽기 전용. URL 패턴 추가 필요 시 책임개발자에게 요청 |
| SlotService 수정 금지 | 인터페이스 호출만 허용 |
| LlmService 수정 금지 | 인터페이스 호출만 허용 |
| 공통 템플릿 수정 금지 | `templates/common/**`은 책임개발자만 수정 |
