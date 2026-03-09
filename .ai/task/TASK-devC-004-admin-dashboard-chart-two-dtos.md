# TASK-devC-004-admin-dashboard-chart-two-dtos

## AI Summary

- 관리자 대시보드 차트 데이터용 DTO 2종(카테고리별 총 개수, 오늘 기준 앞뒤 7일 일별 환자 수) 설계 작업을 정의한다.
- DTO는 `admin/dashboard/dto` 패키지에 Java record 기반으로 생성한다.
- `admin-dashboard.js`에서 fetch 후 바로 소비 가능한 JSON 구조를 명시한다.
- 서비스/Repository 집계 구현은 제외하고 API 응답 계약(Controller 응답 타입 정렬 포함)만 범위로 제한한다.

## 1) Task Meta

- Task ID: `TASK-devC-004-admin-dashboard-chart-two-dtos`
- Task Name: `관리자 대시보드 차트 DTO 2종 설계`
- ACTIVE_ROLE: `{DEV_C}`
- Scope (URL): `/admin/dashboard/stats`
- Scope (Module): `admin/dashboard`
- Status: `{TODO}`

## 2) Goal

- Problem: 차트 응답 구조가 명확하지 않아 프론트 fetch/Chart.js 바인딩 시 필드 불일치와 추가 가공 비용이 발생할 수 있다.
- Expected Outcome: 카테고리명 + 카테고리별 총 개수 DTO와 오늘 기준 앞뒤 7일 날짜 + 일별 환자 수 DTO를 record로 설계한다.
- Out of Scope: Service 집계 로직 구현, Repository 쿼리 구현, SSR(`/admin/dashboard`) 모델 변경.

## 3) Context Loading Checklist

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [ ] `doc/SKILL_{ACTIVE_ROLE}.md` 확인 (있을 때)
- [ ] 외부 documents 저장소 확인 (필요할 때)

### 3.1 Core Rules Summary 핵심 규칙 요약 (필요 시)

#### AGENTS.md

1. 로컬 문서(`doc/PROJECT_STRUCTURE.md`, `doc/RULE.md`)를 우선 적용한다.
2. URL prefix(`/admin`)와 모듈 경계를 임의 변경하지 않는다.
3. DTO는 역할 모듈 경계(`admin`) 내에서 설계한다.
4. 서비스 로직 제외 범위를 문서에서 명확히 구분한다.
5. 결과 보고에 변경 파일/검증/리스크를 포함한다.

#### .ai/memory.md

1. `admin` 모듈은 DEV_C 소유 범위다.
2. `/admin/**`는 `ROLE_ADMIN` 정책을 따른다.
3. Controller는 입출력 계약, Service는 비즈니스 로직 책임을 가진다.
4. DTO는 Record 우선 원칙을 적용한다.
5. 공통 보안/예외 구조는 임의 변경하지 않는다.

#### doc/PROJECT_STRUCTURE.md

1. 루트 패키지 `com.smartclinic.hms`를 유지한다.
2. DTO 위치는 `admin/dashboard/dto`를 사용한다.
3. `/admin/dashboard`(SSR)와 `/admin/dashboard/stats`(JSON API) 경계를 유지한다.
4. 신규 모듈/패키지 추가 없이 기존 구조 내에서 작업한다.
5. 타 역할 모듈 경계를 침범하지 않는다.

#### doc/RULE.md

1. DTO는 Java record 기반으로 설계한다.
2. API 응답은 프론트 소비 관점에서 키 이름을 명확히 고정한다.
3. Controller는 필요 시 응답 타입 정렬 수준으로 최소 변경한다.
4. 테스트 계획은 MockMvc 기반 JSON 구조 검증 중심으로 작성한다.
5. 서비스/Repository 구현 제외 범위를 명시한다.

#### doc/SKILL\_{ACTIVE_ROLE}.md (optional)

1. DEV_C는 `/admin/**`, `/api/**` 범위를 담당한다.
2. 관리자 대시보드 통계/차트 응답 계약 정의는 DEV_C 책임 범위다.
3. SSR과 JSON API 응답 책임을 분리한다.
4. DTO/Controller 중심의 변경을 우선한다.
5. 범위 밖 구현은 에스컬레이션 대상으로 기록한다.

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론: DTO 위치/모듈 경계는 `doc/PROJECT_STRUCTURE.md`, DTO 형식/테스트 원칙은 `doc/RULE.md`를 우선 적용한다.

## 4) Plan (Step A)

### 4.1 Candidate Files

Create:

- Controller:
- Service:
- Repository:
- DTO: `src/main/java/com/smartclinic/hms/admin/dashboard/dto/AdminDashboardCategoryCountChartResponse.java`
- DTO: `src/main/java/com/smartclinic/hms/admin/dashboard/dto/AdminDashboardDailyPatientChartResponse.java`
- DTO: `src/main/java/com/smartclinic/hms/admin/dashboard/dto/AdminDashboardChartDataset.java`
- Template:

Modify:

- Controller: `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardApiController.java` (`/admin/dashboard/stats` 응답 타입을 신규 DTO 구조로 정렬, 필요 시)
- Service: (범위 제외)
- Repository: (범위 제외)
- DTO: 기존 stats DTO와 중복/충돌 없는 네이밍 정리

Test:

- ServiceTest:
- ControllerTest: `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardApiControllerTest.java`

### 4.2 Interface Type

- Delivery Type: `{JSON API}`
- SSR/PRG 적용 여부: `/admin/dashboard/stats`는 JSON API이므로 SSR/PRG 미적용
- 표준 응답 포맷 적용 여부(JSON): fetch + Chart.js에서 바로 소비 가능한 응답 구조 적용

### 4.3 Requirements

Functional

1. 카테고리 차트 DTO는 `labels(카테고리명 목록)` + `datasets[].data(카테고리별 총 개수)` 구조를 제공한다.
2. 일별 환자 차트 DTO는 오늘 날짜 기준 앞뒤 합계 7일(`D-3 ~ D+3`)의 `labels(날짜)` + `datasets[].data(일별 환자 수)` 구조를 제공한다.
3. 두 DTO 모두 `datasets[].label` 필드를 포함해 Chart.js 기본 형식과 호환되도록 한다.

Validation

1. `labels`와 `datasets[].data` 길이 일치 규칙을 DTO 계약으로 명시한다.
2. 날짜 라벨 포맷(예: `yyyy-MM-dd`)을 고정해 프론트 파싱 불일치를 방지한다.

Authorization

1. `/admin/dashboard/stats`는 `/admin/**` 정책에 따라 `ROLE_ADMIN`만 접근 가능해야 한다.

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부: DTO는 `admin/dashboard/dto` 패키지에만 생성하고 타 모듈 수정은 하지 않는다.
- 트랜잭션 정책: DTO 설계 작업이므로 트랜잭션 적용 대상이 없다.
- Validation 적용: 입력 검증이 아닌 응답 계약(JSON 필드명/배열 길이/날짜 라벨 형식) 기준을 정의한다.
- 권한 정책: 기존 `ROLE_ADMIN` 접근 정책을 재사용한다.

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [ ] Service 테스트 (Mockito)
- [x] Controller 테스트 (MockMvc + ROLE)
- [x] 실패 케이스 최소 1개
- [x] Given-When-Then 주석 적용

### 6.2 Executed Tests

- 테스트 클래스: `AdminDashboardApiControllerTest` (계획)
- 결과: 미실행

## 7) Report (Step D)

### 7.1 Changed Files

- `.ai/task/TASK-devC-004-admin-dashboard-chart-two-dtos.md`

### 7.2 Implementation Summary

- 카테고리별 총 개수 차트 DTO와 오늘 기준 앞뒤 7일 일별 환자 수 차트 DTO 설계 범위를 정의했다.
- 두 DTO 모두 Chart.js 호환 구조(`labels`, `datasets[].label`, `datasets[].data`)를 기준으로 명시했다.
- `admin-dashboard.js` fetch 연동 시 추가 변환 없이 사용할 JSON 응답 계약을 문서화했다.
- 서비스/Repository 구현은 제외하고 Controller 응답 타입 정렬만 허용 범위로 제한했다.

### 7.3 References + Versions

- Local:
  - `doc/RULE.md`
  - `doc/PROJECT_STRUCTURE.md`
- External(documents):
  - 문서명/ 버전: `04_API_명세서 v5.2` (권장, 필요 시 확인)
  - 문서명/ 버전: `06_화면_기능_정의서 v5.1` (권장, 필요 시 확인)

### 7.4 Verification Result

- 실행한 테스트: 미실행
- 수동 검증: 템플릿 섹션 구조/Task ID 규칙/요구 DTO 2종 반영 여부 확인

### 7.5 TODO / Risk / Escalation

- TODO: DTO 실제 생성 및 `/admin/dashboard/stats` 응답 모델 연결
- Risk: 일별 라벨 기준 시간대/기준일 계산 방식(LocalDate.now 기준) 합의 필요
- Escalation: 차트 API를 단일/복수 엔드포인트로 분리할지 결정 필요 시 LEAD 확인

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

