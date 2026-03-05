# TASK-devC-002-admin-dashboard-render

## AI Summary

- 관리자 대시보드 진입점 `GET /admin/dashboard`를 `AdminDashboardController`에서 SSR로 렌더링한다.
- 컨트롤러는 렌더링 책임만 갖고, 기본 페이지 메타(`pageTitle`)를 모델에 주입한다.
- 반환 뷰는 `templates/admin/dashboard.mustache`를 사용한다.
- 접근 권한은 기존 보안 정책(`/admin/**` = `ROLE_ADMIN`)을 그대로 따른다.

## 1) Task Meta

- Task ID: `TASK-devC-002-admin-dashboard-render`
- Task Name: `AdminDashboardController 구현 (GET /admin/dashboard)`
- ACTIVE_ROLE: `DEV_C`
- Scope (URL): `/admin/dashboard`
- Scope (Module): `admin/dashboard`
- Status: `DONE`

## 2) Goal

- Problem: 관리자 대시보드 진입 컨트롤러 명세가 명확하지 않아 렌더링/권한/뷰 경로 기준이 흔들릴 수 있다.
- Expected Outcome: `GET /admin/dashboard` 요청 시 `AdminDashboardController`가 `admin/dashboard` 뷰를 반환한다.
- Expected Outcome: 모델에 `pageTitle`을 주입해 레이아웃 타이틀 규칙을 맞춘다.
- Out of Scope: 대시보드 통계 집계 로직(서비스/리포지토리), AJAX 통계 API, 대시보드 상세 기능 CRUD.

## 3) Context Loading Checklist

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/SKILL_{ACTIVE_ROLE}.md` 확인
- [ ] 외부 documents 저장소 확인 (필요 시)

### 3.1 Core Rules Summary (적용 규칙 요약)

#### AGENTS.md

1. 작업 시작 전 필수 컨텍스트 문서를 먼저 읽는다.
2. URL prefix(`/admin`)를 임의 변경하지 않는다.
3. 로컬 구조/코딩 규칙 문서를 최우선으로 따른다.
4. 모듈 경계를 넘는 임의 패키지/레이어 생성 금지.
5. 작업 결과는 변경 파일/검증 결과까지 보고한다.

#### .ai/memory.md

1. `/admin/**`는 `ROLE_ADMIN` 전용 경로다.
2. `admin` 모듈은 DEV_C 소유 영역이다.
3. 컨트롤러는 입출력/렌더링 책임 중심으로 유지한다.
4. 보안 설정(`SecurityConfig`) 직접 변경은 지양한다.
5. 핵심 로직 변경 시 테스트를 함께 반영한다.

#### doc/PROJECT_STRUCTURE.md

1. 루트 패키지는 `com.smartclinic.hms`를 유지한다.
2. 대상 경로는 `admin/dashboard/AdminDashboardController.java`다.
3. 뷰 템플릿은 `templates/admin/dashboard.mustache`를 사용한다.
4. 역할 기반 레이아웃 구조(L2) 패턴을 유지한다.
5. 구조 문서와 다른 신규 모듈 생성은 금지한다.

#### doc/RULE.md

1. SSR GET 요청은 뷰 렌더링 책임을 명확히 한다.
2. 컨트롤러는 비즈니스 로직을 최소화한다.
3. 테스트는 Given-When-Then 구조를 따른다.
4. 컨트롤러 테스트는 MockMvc + 권한 케이스 포함이 권장된다.
5. 예외/보안 공통 규칙 포맷을 유지한다.

#### doc/SKILL_DEV_C.md (optional)

1. DEV_C의 W1 초기 작업에 `AdminDashboardController` 구현이 포함된다.
2. 경로는 `GET /admin/dashboard`로 고정한다.
3. 관리자 대시보드 화면(S17) 렌더링이 목표다.
4. 관리자 모듈 범위 내 변경만 수행한다.
5. 대시보드 통계 구현은 후속 단계로 분리 가능하다.

### 3.2 Conflict Resolution (문서 우선순위 적용 결론 1줄)

- 결론: 구조/경로는 `doc/PROJECT_STRUCTURE.md`, 코딩/테스트 규칙은 `doc/RULE.md`를 우선 적용한다.

## 4) Plan (Step A)

### 4.1 File Plan

Create:

- 없음

Modify:

- Controller: `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardController.java` (`GET /admin/dashboard` 핸들러 및 모델 바인딩)
- Template: `src/main/resources/templates/admin/dashboard.mustache` (필요 시 타이틀 바인딩 확인)

Test:

- ControllerTest: `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardControllerTest.java` (신규 또는 기존 확장)

### 4.2 Interface Type

- Delivery Type: `SSR`
- SSR/PRG 적용 여부: GET 렌더링(해당 없음)
- 표준 응답 포맷 적용 여부(JSON): 미적용

### 4.3 Requirements

Functional

1. `GET /admin/dashboard` 요청 시 `admin/dashboard` 뷰를 반환한다.
2. 모델에 `pageTitle` 값을 주입한다.
3. 기존 `/admin/**` 접근 정책과 충돌 없이 동작한다.

Validation

1. 입력 파라미터 없음 (검증 대상 없음).
2. 뷰 경로 오타/누락이 없도록 고정 문자열로 반환한다.

Authorization

1. `ROLE_ADMIN` 접근만 허용되어야 한다.

## 5) Implementation Notes (Step B)

- 구조 규칙 적용 여부: `admin/dashboard` 모듈 내 컨트롤러 수정으로 제한.
- 트랜잭션 정책: 컨트롤러 단 작업으로 서비스 트랜잭션 미적용.
- Validation 적용: 입력 없음.
- 권한 정책: SecurityConfig의 기존 `/admin/**` 정책을 그대로 사용.

## 6) Test Plan & Result (Step C)

### 6.1 Test Plan

- [x] Controller 테스트 (MockMvc + ROLE_ADMIN 접근 성공)
- [x] Controller 테스트 (권한 없음/미인증 접근 차단)
- [x] Given-When-Then 주석 적용

### 6.2 Executed Tests

- 테스트 클래스: `AdminDashboardControllerTest`
- 결과: 통과

## 7) Report (Step D)

### 7.1 Changed Files

- `src/main/java/com/smartclinic/hms/admin/dashboard/AdminDashboardController.java`
- `src/test/java/com/smartclinic/hms/admin/dashboard/AdminDashboardControllerTest.java`
- `build.gradle`

### 7.2 Implementation Summary

- `GET /admin/dashboard` 핸들러가 `admin/dashboard` 뷰를 반환하도록 유지하고 `pageTitle`을 `Admin Dashboard`로 주입하도록 명시했다.
- 템플릿 렌더링 실패를 방지하기 위해 대시보드 기본 모델 값(통계/차트)을 0, `[]` 기반으로 초기화했다.
- `@WebMvcTest` 기반 컨트롤러 테스트를 추가해 `ROLE_ADMIN` 접근 성공(200), 미인증 리다이렉트(302), 비관리자 권한 거부(403)를 검증하도록 작성했다.
- Spring Boot 4 테스트 슬라이스 분리에 맞춰 `spring-boot-starter-webmvc-test` 의존성을 추가했다.

### 7.3 References + Versions

- Local:
  - `doc/RULE.md`
  - `doc/PROJECT_STRUCTURE.md`
- External(documents):
  - 필요 시 확인

### 7.4 Verification Result

- 실행한 테스트: `./gradlew test --tests "com.smartclinic.hms.admin.dashboard.AdminDashboardControllerTest"` (통과)
- 수동 검증: 코드/매핑/모델 바인딩 정적 검토 완료

### 7.5 TODO / Risk / Escalation

- TODO: 대시보드 통계 서비스 연동 시 기본값(0/빈 배열) 주입 로직을 서비스 결과로 교체.
- Risk: 현재 `pageTitle`은 영문 고정값으로 설정되어 다국어 정책과 다를 수 있다.
- Escalation: 없음.

## 8) Safety & Ownership Gates

### 8.1 Forbidden Check

- [x] URL prefix 임의 변경 없음
- [x] 임의 패키지/모듈 생성 없음
- [x] 민감정보 하드코딩 없음

### 8.2 Ownership Check

- [x] ACTIVE_ROLE 범위 내 수정만 수행
- [x] 범위 밖 변경 필요 시 에스컬레이션 메모 작성

## 9) Done Definition

- [x] 기능 동작 확인
- [x] 테스트 통과
- [x] Report 작성 완료
- [x] 금지 사항 위반 없음
