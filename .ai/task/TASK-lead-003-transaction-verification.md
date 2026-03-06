# HMS Task Template

> This template is used together with `.ai/workflows/workflow.md`.
> Tasks created from this template must follow the workflow execution order.

## AI Summary

- 예약 중복 방지 트랜잭션 동시성 테스트 및 전체 상태 흐름(RESERVED→RECEIVED→COMPLETED/CANCELLED) 검증
- 동시 예약 시 1건만 성공하는 비관적/낙관적 락 검증 테스트 구현
- 상태 전이 규칙 위반(역방향, 중복 전이 등) 시 예외 발생 확인
- W2~W3 기간 개발자 PR 리뷰와 병행하여 핵심 트랜잭션 안정성 확보

## 1) Task Meta

- Task ID: TASK-lead-003-transaction-verification
- Task Name: 예약 트랜잭션 동시성 & 상태 흐름 검증 테스트
- ACTIVE_ROLE: `LEAD`
- Scope (URL): (직접 URL 없음 — 서비스/도메인 레이어 검증)
- Scope (Module): domain, common/service, reservation (테스트만)
- Status: `TODO`

## 2) Goal

- Problem: 예약 중복 방지(SlotService)와 상태 전이(Reservation 엔티티)의 트랜잭션 안정성이 테스트로 검증되지 않은 상태. 동시 요청 시 데이터 정합성 보장 필요.
- Expected Outcome:
  - 동시 예약 시 1건만 성공하는 트랜잭션 테스트 통과
  - RESERVED → RECEIVED → COMPLETED 순방향 전이만 허용 확인
  - CANCELLED 상태에서 재전이 불가 확인
  - 역방향 전이(COMPLETED → RESERVED 등) 시 예외 발생 확인
- Out of Scope: 예약/접수 Controller 테스트 (각 개발자 영역), UI 테스트

## 3) Context Loading Checklist

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/SKILL_LEAD.md` 확인
- [ ] 외부 documents 저장소 확인 (아키텍처 정의서 v2.0 — 상태 전이 다이어그램)

### 3.1 Core Rules Summary 핵심 규칙 요약

#### AGENTS.md

1. 핵심 비즈니스 로직 테스트 없이 배포 금지
2. Given-When-Then 구조 필수
3. 상태 전이 규칙 중복 구현/다중 위치 변경 금지
4. 시간/랜덤/외부 의존성 제거해 결정성 보장
5. 통합 테스트는 핵심 플로우 최소 세트만 유지

#### .ai/memory.md

1. 상태 전이 규칙 중복 구현/다중 위치 변경 금지
2. Repository로 타 모듈 상태 직접 변경 금지
3. 서비스 레이어가 트랜잭션 경계를 소유
4. 보안 테스트는 역할별 허용/차단 케이스 모두 검증
5. 단위 테스트 Mockito 기반 격리 테스트 우선

#### doc/PROJECT_STRUCTURE.md

1. ReservationStatus: RESERVED → RECEIVED → COMPLETED / CANCELLED
2. Reservation 엔티티에 상태 전이 메서드 포함
3. 에러코드: INVALID_STATUS_TRANSITION, ALREADY_CANCELLED, ALREADY_COMPLETED
4. CustomException.invalidStatusTransition() — 409 상태
5. CustomException.conflict() — 409 중복/충돌

#### doc/RULE.md

1. 외부 의존 금지, H2/Testcontainers 사용
2. 통합 테스트 `@SpringBootTest` 최소화
3. AssertJ, BDDMockito 사용
4. `@DisplayName` 필수
5. 결정성 보장 (Clock 추상화)

#### doc/SKILL\_LEAD.md (optional)

1. W2 전반: 중복 예약 트랜잭션 검증 — 동시 예약 시 1건만 성공
2. W3 전반: 전체 상태 흐름 검증 — RESERVED→RECEIVED→COMPLETED 순방향만 허용
3. W2 체크포인트: 중복 예약 트랜잭션 테스트 통과
4. W3 체크포인트: 상태 전이 테스트 스위트 전체 통과
5. 상태 전이 테스트 커버리지 100% 경로 목표

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론: 상태 전이 경로는 Reservation 엔티티 도메인 메서드를 단일 기준으로 하고, 테스트 패턴은 `RULE.md`를 따른다.

## 4) Plan (Step A)

### 4.1 Candidate Files

Create:

- Test: `ReservationStatusTransitionTest.java` (상태 전이 단위 테스트)
- Test: `SlotServiceConcurrencyTest.java` (동시성 트랜잭션 테스트)

Modify:

- 없음 (테스트 전용 태스크)

Test:

- ServiceTest: `SlotServiceConcurrencyTest.java`
- DomainTest: `ReservationStatusTransitionTest.java`

### 4.2 Interface Type

- Delivery Type: 해당 없음 (테스트 전용)
- SSR/PRG 적용 여부: 해당 없음
- 표준 응답 포맷 적용 여부(JSON): 해당 없음

### 4.3 Requirements

Functional

1. 동시 예약 요청(2+ 스레드) 시 동일 슬롯에 1건만 예약 성공
2. RESERVED → RECEIVED 전이 성공
3. RECEIVED → COMPLETED 전이 성공
4. RESERVED → CANCELLED 전이 성공
5. 역방향/잘못된 전이 시 INVALID_STATUS_TRANSITION 예외 발생
6. CANCELLED/COMPLETED 상태에서 재전이 시 ALREADY_CANCELLED/ALREADY_COMPLETED 예외 발생

Validation

1. 모든 상태 전이 경로 테스트 (순방향 + 역방향 + 중복)
2. 동시성 테스트에서 데이터 정합성 검증

Authorization

1. 해당 없음 (도메인/서비스 레이어 테스트)

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부: 테스트 코드만 생성, 프로덕션 코드 변경 없음
- 트랜잭션 정책: 동시성 테스트에서 `@Transactional` + 비관적 락 동작 검증
- Validation 적용: 해당 없음 (테스트)
- 권한 정책: 해당 없음
- 동시성 테스트 도구: `ExecutorService` + `CountDownLatch`로 동시 요청 시뮬레이션
- 상태 전이 테스트: Reservation 엔티티 도메인 메서드 직접 호출, 순수 단위 테스트

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [ ] ReservationStatusTransitionTest
  - [ ] RESERVED → RECEIVED 성공
  - [ ] RECEIVED → COMPLETED 성공
  - [ ] RESERVED → CANCELLED 성공
  - [ ] COMPLETED → RESERVED 시 INVALID_STATUS_TRANSITION 예외
  - [ ] CANCELLED → RECEIVED 시 ALREADY_CANCELLED 예외
  - [ ] COMPLETED → CANCELLED 시 ALREADY_COMPLETED 예외
  - [ ] RECEIVED → RESERVED 역방향 시 예외
- [ ] SlotServiceConcurrencyTest
  - [ ] 2개 스레드 동시 예약 → 1건 성공, 1건 실패(DUPLICATE_RESERVATION)
  - [ ] 동시 예약 후 DB 슬롯 상태 정합성 확인
- [ ] Given-When-Then 주석 적용
- [ ] `@DisplayName` 의도 명확 표현

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
  - 아키텍처 정의서 / v2.0 (상태 전이 다이어그램)
  - 테스트 전략서 / v1.0

### 7.4 Verification Result

- 실행한 테스트: (구현 후 기록)
- 수동 검증: (구현 후 기록)

### 7.5 TODO / Risk / Escalation

- TODO: SlotService(TASK-lead-001) 완료 후 동시성 테스트 작성 가능
- TODO: 개발자 B 접수 구현 후 RESERVED→RECEIVED 통합 시나리오 검증 추가
- Risk: H2 인메모리 DB에서 비관적 락 동작이 MySQL과 다를 수 있음
- Escalation: 없음

## 8) Safety & Ownership Gates

### 8.1 Forbidden Check

- [x] URL prefix 임의 변경 없음
- [x] 임의 패키지/모듈 생성 없음
- [x] 민감정보 하드코딩 없음

### 8.2 Ownership Check

- [x] ACTIVE_ROLE 범위 내 수정만 수행 (domain, common/service 테스트는 LEAD 소유)
- [x] 범위 밖 변경 필요 시 에스컬레이션 메모 작성

## 9) Done Definition

- [ ] 상태 전이 테스트 100% 경로 통과
- [ ] 동시성 트랜잭션 테스트 통과 (1건만 성공)
- [ ] Report 작성 완료
- [ ] 금지사항 위반 없음
