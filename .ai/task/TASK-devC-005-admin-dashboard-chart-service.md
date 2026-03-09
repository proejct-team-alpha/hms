# TASK-devC-005-admin-dashboard-chart-service

## AI Summary

- 관리자 차트 API 응답(`AdminDashboardChartResponse`)을 기존 `AdminDashboardStatsService`에서 조립하는 작업을 정의한다.
- `AdminDashboardStatsService`에 `getDashboardChart()` 메서드를 추가해 Repository 조회 결과를 차트 응답 DTO로 조립한다.
- 차트 데이터는 카테고리별 총 개수와 현재 날짜 기준 앞뒤 7일 일별 환자 수를 포함한다.
- 일별 환자 수는 누락 날짜를 0으로 보정하는 규칙을 기존 서비스 내부 조립 로직에 반영한다.
- 카드 통계 DTO(`AdminDashboardStatsResponse`)와 차트 DTO(`AdminDashboardChartResponse`)는 분리된 책임으로 유지한다.

## 1) Task Meta

- Task ID: `TASK-devC-005-admin-dashboard-chart-service`
- Task Name: `AdminDashboardStatsService 차트 응답 조립 메서드 추가`
- ACTIVE_ROLE: `DEV_C`
- Scope (URL): `/admin/dashboard/stats`
- Scope (Module): `admin/dashboard`
- Status: `TODO`

## 2) Goal

- Problem: 차트 응답 조립 책임이 Controller 또는 다른 통계 로직과 섞이면 유지보수성과 테스트 용이성이 낮아진다.
- Expected Outcome: `AdminDashboardStatsService`에 `AdminDashboardChartResponse` 조립 메서드를 추가해 차트 API 응답 책임을 서비스 계층으로 유지한다.
- Out of Scope: `admin-dashboard.js` 수정, SSR `/admin/dashboard` 뷰/모델 수정, 카드 통계(`AdminDashboardStatsResponse`) 로직 변경.

## 3) Context Loading Checklist

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [ ] `doc/SKILL_{ACTIVE_ROLE}.md` 확인 (있을 때)
- [ ] 외부 documents 저장소 확인 (필요할 때)

### 3.1 Core Rules Summary 핵심 규칙 요약 (필요 시)

#### AGENTS.md

1. 로컬 구조/코딩 규칙 문서를 우선 적용한다.
2. `/admin` URL prefix와 모듈 경계를 임의 변경하지 않는다.
3. Service는 비즈니스 조립 책임, Controller는 연결 책임으로 분리한다.
4. DTO는 Record 우선 원칙을 따른다.
5. 작업 결과에 변경 파일/검증/리스크를 명시한다.

#### .ai/memory.md

1. `admin` 모듈은 DEV_C 소유 범위다.
2. `/admin/**` 경로는 `ROLE_ADMIN` 정책을 따른다.
3. Service 계층이 트랜잭션 경계를 소유한다.
4. Controller는 입출력/위임 중심으로 유지한다.
5. 테스트는 Given-When-Then 구조를 따른다.

#### doc/PROJECT_STRUCTURE.md

1. 루트 패키지는 `com.smartclinic.hms`를 유지한다.
2. 대상 모듈은 `admin/dashboard`로 제한한다.
3. DTO는 `admin/dashboard/dto` 경로를 유지한다.
4. 타 역할 모듈(`staff`, `doctor`, `nurse`) 변경을 피한다.
5. 기존 URL 구조(`/admin/**`)를 유지한다.

#### doc/RULE.md

1. Service 클래스는 `@Transactional(readOnly = true)` 기본 정책을 적용한다.
2. DTO/응답 계약은 일관된 필드명과 구조를 유지한다.
3. Controller는 서비스 호출 연결 수준으로 최소 변경한다.
4. Service 테스트는 Mockito 중심으로 작성한다.
5. 카드 통계 DTO와 차트 DTO 책임은 분리하되 서비스 클래스는 하나로 유지할 수 있다.

#### doc/SKILL\_{ACTIVE_ROLE}.md (optional)

1. DEV_C는 `/admin/**`, `/api/**` 영역을 담당한다.
2. 관리자 대시보드 API 설계/조립 로직은 DEV_C 책임 범위다.
3. SSR과 JSON API 책임을 분리한다.
4. 대시보드 통계 기능은 서비스 계층 중심으로 구성한다.
5. 범위 밖 변경은 에스컬레이션 메모를 남긴다.

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론: 구조/패키지 배치는 `doc/PROJECT_STRUCTURE.md`, 서비스/테스트 설계 원칙은 `doc/RULE.md`를 우선 적용한다.

## 4) Plan (Step A)

### 4.1 Candidate Files

Create:

- Controller:
- Service:
- Repository:
- DTO:
- Template:

Modify:

- Controller: `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardApiController.java` (`/admin/dashboard/stats`에서 `AdminDashboardStatsService#getDashboardChart()` 호출 연결)
- Service: `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsService.java` (`getDashboardChart()` 메서드 추가 및 `AdminDashboardChartResponse` 조립 책임 포함)
- Repository: `src/main/java/com/smartclinic/hms/admin/item/ItemRepository.java`, `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationRepository.java` (차트 조립용 조회 메서드 추가 후보 계획만 포함)
- DTO: `src/main/java/com/smartclinic/hms/admin/dashboard/dto/AdminDashboardChartResponse.java` (기존 중첩 record `CategoryCount`, `DailyPatientCount` 사용 전제)

Test:

- ServiceTest: `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardStatsServiceTest.java`
- ControllerTest: `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardApiControllerTest.java`

### 4.2 Interface Type

- Delivery Type: `JSON API`
- SSR/PRG 적용 여부: 차트 응답 API는 JSON 전용으로 SSR/PRG 미적용
- 표준 응답 포맷 적용 여부(JSON): `Resp.ok(...)` 기반으로 차트 DTO 반환

### 4.3 Requirements

Functional

1. `AdminDashboardStatsService#getDashboardChart()` 메서드는 Repository 조회 결과를 `AdminDashboardChartResponse`로 조립한다.
2. 차트 데이터 1: 카테고리명 + 카테고리별 총 개수(`CategoryCount`)를 포함한다.
3. 차트 데이터 2: 현재 날짜 기준 앞뒤 합계 7일(`D-3 ~ D+3`)의 날짜 + 날짜별 환자 수(`DailyPatientCount`)를 포함한다.

Validation

1. 날짜별 환자 수에서 조회 결과에 없는 날짜는 0으로 보정한다.
2. 카드 통계 DTO(`AdminDashboardStatsResponse`)와 차트 DTO(`AdminDashboardChartResponse`)를 분리된 책임으로 유지한다.

Authorization

1. 차트 API 경로(`/admin/**`)는 `ROLE_ADMIN` 정책을 따른다.

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부: `AdminDashboardChartResponse` 조립 책임은 기존 `AdminDashboardStatsService` 내부 `getDashboardChart()` 메서드에 두고 Controller는 위임만 수행한다.
- 트랜잭션 정책: 조회 중심 기존 서비스이므로 `@Transactional(readOnly = true)`를 유지한다.
- Validation 적용: 조회 결과 매핑 시 7일 범위 날짜 누락분을 0으로 보정하는 조립 규칙을 적용한다.
- 권한 정책: 차트 API는 기존 `/admin/**` 보안 정책(ROLE_ADMIN)을 그대로 사용한다.

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [ ] Service 테스트 (Mockito)
- [ ] Controller 테스트 (MockMvc + ROLE)
- [ ] 실패 케이스 최소 1개
- [ ] Given-When-Then 주석 적용

### 6.2 Executed Tests

- 테스트 클래스: 미실행 (계획: `AdminDashboardStatsServiceTest`, `AdminDashboardApiControllerTest`)
- 결과: 미실행

## 7) Report (Step D)

### 7.1 Changed Files

- `.ai/task/TASK-devC-005-admin-dashboard-chart-service.md`

### 7.2 Implementation Summary

- 기존 `AdminDashboardStatsService`에서 `AdminDashboardChartResponse`를 조립하도록 작업 범위를 정의했다.
- 서비스가 카테고리별 집계 + 7일 일별 환자 수를 조립하고 누락 날짜를 0으로 보정하도록 명시했다.
- Controller는 `getDashboardChart()` 호출 연결 수준으로만 수정하고, Repository는 조회 메서드 후보 계획만 포함했다.
- 카드 통계 DTO(`AdminDashboardStatsResponse`)와 차트 DTO(`AdminDashboardChartResponse`) 책임 분리를 유지하도록 명문화했다.

### 7.3 References + Versions

- Local:
  - `doc/RULE.md`
  - `doc/PROJECT_STRUCTURE.md`
- External(documents):
  - 문서명/ 버전: `04_API_명세서 v5.2` (필요 시 확인)
  - 문서명/ 버전: `06_화면_기능_정의서 v5.1` (필요 시 확인)

### 7.4 Verification Result

- 실행한 테스트: 미실행
- 수동 검증: 템플릿 구조 준수, 서비스 책임 분리, 범위(Out of Scope) 반영 확인

### 7.5 TODO / Risk / Escalation

- TODO: `AdminDashboardStatsService#getDashboardChart()` 실제 구현 및 Repository 조회 메서드 확정
- Risk: 날짜 기준(LocalDate.now, timezone)과 7일 범위 정의(`D-3~D+3`) 합의 필요
- Escalation: 없음

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
- [x] 금지사항 위반 없음
