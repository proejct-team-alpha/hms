# TASK-devC-003-admin-dashboard-stats-4metrics

## AI Summary

- 관리자 대시보드 통계 4종(오늘 예약, 총 예약, 직원 수, 재고 부족)을 DB 집계 쿼리 기반으로 제공한다.
- `GET /admin/dashboard` SSR 모델과 `GET /admin/dashboard/stats` JSON 응답이 동일한 집계 소스를 사용하도록 일원화한다.
- 집계 로직은 `admin/dashboard` 모듈 내 Repository/Service 계층으로 분리해 모듈 책임을 유지한다.
- 접근 권한은 기존 보안 정책(`/admin/**` = `ROLE_ADMIN`)을 그대로 따른다.
- Repository/Service/Controller 테스트로 집계 정확도와 권한 동작을 함께 검증한다.

## 1) Task Meta

- Task ID: `TASK-devC-003-admin-dashboard-stats-4metrics`
- Task Name: `관리자 대시보드 통계 4종 + Repository 집계 쿼리 구현`
- ACTIVE_ROLE: `DEV_C`
- Scope (URL): `/admin/dashboard`, `/admin/dashboard/stats`
- Scope (Module): `admin/dashboard`, `admin/item`
- Status: `TODO`

## 2) Goal

- Problem: 관리자 대시보드 통계가 현재 기본값(하드코딩) 중심이라 실제 운영 데이터 모니터링이 어렵다.
- Expected Outcome: 오늘 예약 수, 총 예약 수, 직원 수, 재고 부족 수를 DB에서 집계해 SSR/AJAX 모두 동일하게 제공한다.
- Out of Scope: 진료과 분포 차트 고도화, 관리자 전체 화면 리뉴얼, 타 역할 대시보드 통계 변경.

## 3) Context Loading Checklist

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/SKILL_{ACTIVE_ROLE}.md` 확인 (있을 때)
- [ ] 외부 documents 저장소 확인 (필요 시)

### 3.1 Core Rules Summary (적용 규칙 요약)

#### AGENTS.md

1. 로컬 구조/코딩 규칙 문서를 우선 적용한다.
2. URL prefix(`/admin`)를 임의 변경하지 않는다.
3. 테스트 없이 핵심 로직 반영 금지.
4. 모듈 경계를 지키고 임의 패키지/레이어 생성 금지.
5. 작업 결과에 변경 파일/검증/리스크를 명시한다.

#### .ai/memory.md

1. `/admin/**`는 `ROLE_ADMIN` 전용 경로다.
2. admin 모듈은 DEV_C 소유 범위다.
3. 컨트롤러는 입출력, 서비스는 비즈니스 로직 중심으로 분리한다.
4. 테스트는 Given-When-Then 구조를 따른다.
5. 공통 보안/예외 포맷을 임의로 변경하지 않는다.

#### doc/PROJECT_STRUCTURE.md

1. 루트 패키지 `com.smartclinic.hms`를 유지한다.
2. 관리자 기능은 `admin/**` 경로에서 구현한다.
3. 공통/타 역할 모듈 경계를 침범하지 않는다.
4. 템플릿 구조는 `templates/admin/**`를 따른다.
5. 기존 URL 구조(`/admin/**`)를 유지한다.

#### doc/RULE.md

1. 서비스는 `@Transactional(readOnly = true)` 기본 정책을 따른다.
2. DTO/응답 포맷을 일관되게 유지한다.
3. Controller 테스트는 `@WebMvcTest` + MockMvc + 권한 케이스를 포함한다.
4. Repository 검증은 `@DataJpaTest`를 우선한다.
5. 테스트 본문에 Given-When-Then 주석을 명시한다.

#### doc/SKILL_{ACTIVE_ROLE}.md (optional)

1. DEV_C는 `/admin/**`, `/api/**` 범위를 담당한다.
2. 관리자 대시보드/통계 구현은 DEV_C 핵심 작업이다.
3. 통계는 SSR + AJAX 갱신 시나리오를 지원해야 한다.
4. 타 모듈 소유 Repository 직접 수정은 지양한다.
5. 관리자 모듈 내 읽기 집계 중심으로 설계한다.

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론: 구조/경로는 `doc/PROJECT_STRUCTURE.md`, 구현/테스트 방식은 `doc/RULE.md`를 우선 적용한다.

## 4) Plan (Step A)

### 4.1 Candidate Files

Create:

- Controller: 없음 (기존 AdminDashboardController 확장)
- Service: `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsService.java`
- Repository: `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsRepository.java`
- DTO: `src/main/java/com/smartclinic/hms/admin/dashboard/dto/AdminDashboardStatsResponse.java`
- Template: 없음 (기존 대시보드 모델 키 재사용)

Modify:

- Controller: `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardController.java` (SSR 통계 모델 주입 + JSON stats 엔드포인트)
- Service: 없음 (신규 생성 우선)
- Repository: 없음 (신규 생성 우선)
- DTO: 없음 (신규 생성 우선)

Test:

- ServiceTest: `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsServiceTest.java`
- ControllerTest: `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardControllerTest.java`
- RepositoryTest: `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsRepositoryTest.java`

### 4.2 Interface Type

- Delivery Type: `Mixed`
- SSR/PRG 적용 여부: SSR GET 적용 (`/admin/dashboard`)
- 표준 응답 포맷 적용 여부(JSON): 적용 (`/admin/dashboard/stats`)

### 4.3 Requirements

Functional

1. 오늘 예약 수를 `Reservation` 기준 `reservationDate = today` 집계로 제공한다.
2. 총 예약 수를 `Reservation` 전체 건수 집계로 제공한다.
3. 직원 수를 `Staff(active=true)` 기준 집계로 제공한다.
4. 재고 부족 수를 `Item(quantity < minQuantity)` 기준 집계로 제공한다.

Validation

1. 집계 결과 null 안전 처리(값이 없으면 0).
2. 날짜 기준은 서버 로컬 날짜(`LocalDate.now()`) 단일 기준으로 통일한다.

Authorization

1. `/admin/dashboard` 및 `/admin/dashboard/stats`는 `ROLE_ADMIN`만 허용한다.

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부: `admin/dashboard` 모듈 내부 서비스/리포지토리/DTO로 구성한다.
- 트랜잭션 정책: 통계 조회 서비스에 `@Transactional(readOnly = true)`를 적용한다.
- Validation 적용: 입력값 검증 대신 결과 null/타입 안전성 검증을 적용한다.
- 권한 정책: 기존 SecurityConfig의 `/admin/**` 정책을 재사용한다.

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [ ] Service 테스트 (Mockito)
- [ ] Controller 테스트 (MockMvc + ROLE)
- [ ] 실패 케이스 최소 1개 (비관리자 접근 403)
- [ ] Given-When-Then 주석 적용

### 6.2 Executed Tests

- 테스트 클래스: 미실행 (Task 문서 작성 단계)
- 결과: 미실행

## 7) Report (Step D)

### 7.1 Changed Files

- `.ai/task/TASK-devC-003-admin-dashboard-stats-4metrics.md`

### 7.2 Implementation Summary

- 관리자 대시보드 통계 4종을 DB 집계 쿼리 기반으로 구현하는 작업 계획을 정의했다.
- SSR과 JSON 통계 API가 동일 집계 결과를 사용하도록 서비스/리포지토리 일원화 전략을 정리했다.
- Repository/DataJpaTest + Service/Mockito + Controller/MockMvc 권한 검증 계획을 포함했다.

### 7.3 References + Versions

- Local:
  - `doc/RULE.md`
  - `doc/PROJECT_STRUCTURE.md`
- External(documents):
  - 문서명/ 버전: `04_API_명세서 v5.2` (권장)
  - 문서명/ 버전: `06_화면_기능_정의서 v5.1` (권장)

### 7.4 Verification Result

- 실행한 테스트: 없음 (Task 문서 작성만 수행)
- 수동 검증: 템플릿 형식/Task ID/DEV_C 범위 점검 완료

### 7.5 TODO / Risk / Escalation

- TODO: 구현 시 집계 쿼리 인덱스/성능 확인.
- Risk: `Reservation` 날짜 컬럼명/타입이 다르면 쿼리 조건을 조정해야 한다.
- Escalation: 타 모듈 Repository 수정 필요 시 Lead 승인 후 진행.

## 8) Safety & Ownership Gates

### 8.1 Forbidden Check

- [x] URL prefix 임의 변경 없음
- [x] 임의 패키지/모듈 생성 없음
- [x] 민감정보 하드코딩 없음

### 8.2 Ownership Check

- [x] ACTIVE_ROLE 범위 내 수정만 수행
- [x] 범위 밖 변경 필요 시 에스컬레이션 메모 작성

## 9) Done Definition

- [ ] 기능 동작 확인
- [ ] 테스트 통과
- [x] Report 작성 완료
- [x] 금지 사항 위반 없음
