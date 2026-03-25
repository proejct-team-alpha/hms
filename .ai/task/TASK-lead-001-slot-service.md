# HMS Task Template

> This template is used together with `.ai/workflows/workflow.md`.
> Tasks created from this template must follow the workflow execution order.

## AI Summary

- SlotService와 ReservationValidationService를 구현하여 예약 중복 방지 및 슬롯 관리 기능 제공
- 30분 단위 시간 슬롯 생성, 의사 진료요일 검증, 중복 예약 검증 로직 구현
- 공통 서비스 레이어(`common/service`)에 배치하여 reservation/staff 모듈에서 호출 가능하도록 설계
- Mockito 기반 단위 테스트로 핵심 로직 검증 완료 필요

## 1) Task Meta

- Task ID: TASK-lead-001-slot-service
- Task Name: SlotService & ReservationValidationService 구현
- ACTIVE_ROLE: `LEAD`
- Scope (URL): /reservation/**, /staff/** (간접 영향)
- Scope (Module): common/service
- Status: `TODO`

## 2) Goal

- Problem: 예약 시 시간 슬롯 가용성 확인 및 중복 예약 방지 로직이 미구현 상태. 개발자 A(예약), 개발자 B(접수)가 W2부터 병렬 개발하려면 이 공통 서비스가 필수.
- Expected Outcome:
  - SlotService: 30분 단위 슬롯 생성, 의사 진료요일 검증, 특정 날짜/의사 가용 슬롯 조회
  - ReservationValidationService: 동일 환자 동일 날짜 중복 예약 검증, 시간 슬롯 유효성 검증
  - 두 서비스 모두 인터페이스 + 구현체 분리
- Out of Scope: 예약 폼 UI, 예약 CRUD Controller/Service (개발자 A 영역)

## 3) Context Loading Checklist

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/SKILL_LEAD.md` 확인
- [ ] 외부 documents 저장소 확인 (API 명세서 §1.8, §3, §5)

### 3.1 Core Rules Summary 핵심 규칙 요약

#### AGENTS.md

1. `@Transactional(readOnly = true)` 기본, 쓰기 메서드만 별도 트랜잭션
2. DTO는 Record 우선, 입력은 `@Valid`/`@Validated` 검증
3. 예외는 `CustomException` + `GlobalExceptionHandler` 포맷 유지
4. 핵심 비즈니스 로직 테스트 없이 배포 금지
5. 모듈 경계를 넘는 임의 패키지/레이어 추가 금지

#### .ai/memory.md

1. 서비스 레이어가 트랜잭션 경계를 소유
2. 공통 서비스(`common/service`)는 책임개발자 소유/검토 영역
3. `common/service` 핵심 검증/슬롯 로직 우회 금지
4. 인터페이스 계약 변경은 관련 모듈 동시 검토 전제
5. 보안/공통/엔티티 레이어는 보호 영역

#### doc/PROJECT_STRUCTURE.md

1. common/service 경로에 SlotService, ReservationValidationService 배치
2. 루트 패키지: `com.smartclinic.hms`
3. Reservation 엔티티: status(RESERVED→RECEIVED→COMPLETED/CANCELLED), timeSlot, date
4. Doctor 엔티티: available_days, specialty
5. 에러코드: DUPLICATE_RESERVATION, INVALID_TIME_SLOT, DOCTOR_NOT_AVAILABLE

#### doc/RULE.md

1. 서비스 레이어 중심 설계, 컨트롤러는 입출력 처리에 집중
2. Given-When-Then 구조 테스트 필수
3. 외부 의존 금지, Mock 처리
4. 시간/랜덤 결정성 보장 (Clock 추상화)
5. 단위 테스트는 Mockito 기반 격리 테스트 우선

#### doc/SKILL\_LEAD.md (optional)

1. SlotService는 W1 후반 작업 (Day 3~5)
2. 중복 슬롯 예약 시 예외 발생, 테스트 통과가 완료 기준
3. SlotService 수정 금지 (다른 개발자) — 인터페이스 호출만 허용
4. W2에서 개발자 A PR 리뷰 시 SlotService 호출 패턴 검증
5. 테스트 커버리지 목표: SlotService 80%+

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론: 구조는 `PROJECT_STRUCTURE.md`의 `common/service` 경로, 기능 스펙은 외부 API 명세서 §1.8을 따르되, 코딩 규칙은 `RULE.md`를 우선한다.

## 4) Plan (Step A)

### 4.1 Candidate Files

Create:

- Service: `common/service/SlotService.java` (인터페이스)
- Service: `common/service/SlotServiceImpl.java` (구현체)
- Service: `common/service/ReservationValidationService.java` (인터페이스)
- Service: `common/service/ReservationValidationServiceImpl.java` (구현체)
- Repository: (없음 — DoctorRepository, ReservationRepository는 각 모듈에서 생성 예정, 의존성 주입으로 사용)

Modify:

- 없음 (신규 생성만)

Test:

- ServiceTest: `SlotServiceTest.java`
- ServiceTest: `ReservationValidationServiceTest.java`

### 4.2 Interface Type

- Delivery Type: `JSON API` (내부 서비스, 직접 HTTP 노출 없음)
- SSR/PRG 적용 여부: 해당 없음 (서비스 레이어)
- 표준 응답 포맷 적용 여부(JSON): 해당 없음 (내부 호출)

### 4.3 Requirements

Functional

1. 30분 단위 시간 슬롯 목록 생성 (예: 09:00, 09:30, ..., 17:30)
2. 특정 날짜 + 의사 기준 가용 슬롯 조회 (이미 예약된 슬롯 제외)
3. 의사 진료요일 검증 (Doctor.availableDays와 요청 날짜 요일 비교)
4. 동일 환자 동일 날짜 중복 예약 검증
5. 시간 슬롯 유효성 검증 (범위 내 존재 여부)

Validation

1. 과거 날짜 예약 요청 차단
2. 의사 비진료일 예약 요청 시 `DOCTOR_NOT_AVAILABLE` 예외

Authorization

1. 서비스 레이어이므로 직접 권한 검증 없음 (호출하는 Controller에서 처리)

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부: 인터페이스 + 구현체 분리, `common/service` 패키지 배치
- 트랜잭션 정책: `@Transactional(readOnly = true)` 기본 (조회 중심 서비스)
- Validation 적용: `CustomException.conflict("DUPLICATE_RESERVATION", ...)`, `CustomException.badRequest("INVALID_TIME_SLOT", ...)`
- 권한 정책: 서비스 레이어 자체 권한 없음, 호출부에서 제어
- 시간 의존성: `Clock` 주입으로 결정성 보장 (과거 날짜 검증 시)

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [ ] SlotService 테스트 (Mockito)
  - [ ] 30분 단위 전체 슬롯 목록 생성 검증
  - [ ] 가용 슬롯 조회 — 예약된 슬롯 제외 확인
  - [ ] 의사 비진료일 요청 시 예외 발생
  - [ ] 과거 날짜 요청 시 예외 발생
- [ ] ReservationValidationService 테스트 (Mockito)
  - [ ] 동일 환자 동일 날짜 중복 예약 시 예외 발생
  - [ ] 유효하지 않은 시간 슬롯 시 예외 발생
  - [ ] 정상 케이스 통과
- [ ] 실패 케이스 최소 1개 (각 서비스당)
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
  - API 명세서 / v5.2 권장
  - 아키텍처 정의서 / v2.0

### 7.4 Verification Result

- 실행한 테스트: (구현 후 기록)
- 수동 검증: (구현 후 기록)

### 7.5 TODO / Risk / Escalation

- TODO: DoctorRepository, ReservationRepository 인터페이스가 다른 모듈에서 먼저 생성되어야 DI 가능
- Risk: Repository 미생성 시 컴파일 불가 — 임시 인터페이스 정의 필요할 수 있음
- Escalation: 없음

## 8) Safety & Ownership Gates

### 8.1 Forbidden Check

- [x] URL prefix 임의 변경 없음
- [x] 임의 패키지/모듈 생성 없음 (기존 `common/service` 사용)
- [x] 민감정보 하드코딩 없음

### 8.2 Ownership Check

- [x] ACTIVE_ROLE 범위 내 수정만 수행 (common/service는 LEAD 소유)
- [x] 범위 밖 변경 필요 시 에스컬레이션 메모 작성

## 9) Done Definition

- [ ] 기능 동작 확인
- [ ] 테스트 통과 (SlotService 80%+)
- [ ] Report 작성 완료
- [ ] 금지사항 위반 없음
