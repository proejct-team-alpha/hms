# 선행 작업 체크리스트 (우선순위)

> 참고: [proejct-team-alpha/documents](https://github.com/proejct-team-alpha/documents) 기반

## Phase 1: 개발 환경 및 기동

### 작업 상태 비고

1 .env.example 생성 ✅ CLAUDE_API_KEY= 등 템플릿 제공
2 run-dev.ps1 실행 스크립트 ✅ .env 로드 후 ./gradlew bootRun
3 환경 변수 주입 확인 ⚠️ CLAUDE_API_KEY 없으면 Claude API 사용 불가

---

## Phase 2: 도메인 및 인증 기반

### Phase 2 작업 상태 비고

4 domain 엔티티 구현 ❌ Patient, Staff, Doctor, Department, Reservation, TreatmentRecord, Item, HospitalRule, LlmRecommendation
5 StaffUserDetailsService 구현 ❌ Staff 엔티티 기반 로그인, SecurityConfig 주석 참고
6 초기 데이터 (DataLoader) ❌ H2 in-memory용 테스트 계정 (admin01, staff01 등)

---

## Phase 3: 공통 인프라

### Phase 3 작업 상태 비고

7 LayoutModelInterceptor 구현 ❌ pageTitle, loginName, isAdmin/isDoctor/isNurse/isStaff, showChatbot, currentPath, dashboardUrl 주입 (documents §2)
8 WebMvcConfig 인터셉터 등록 ❌ LayoutModelInterceptor 등록, /css, /js, /images, /error, /h2-console 제외
9 SlotService / ReservationValidationService ❌ common/service 패키지 (30분 슬롯, 예약 검증)

---

## Phase 4: 뷰 및 정적 리소스

### Phase 4 작업 상태 비고

10 Mustache 템플릿 디렉터리 ✅ templates/ auth, error, common
11 로그인 화면 ✅ auth/login.mustache (L3, header-login)
12 403 / 404 에러 화면 ✅ error/403.mustache, error/404.mustache, ErrorPageController
13 공통 레이아웃 ✅ common/header-public, header-staff, header-login, footer-public, footer-staff, sidebar-staff/doctor/nurse/admin (documents §2)
14 정적 리소스 ✅ static/css/common.css, static/js/, static/images/

---

## Phase 5: 프로필 및 운영 설정

### Phase 5 작업 상태 비고

15 application-dev.properties ✅ H2 in-memory, 로깅, 에러 노출 (documents 10_환경_설정_템플릿)
16 application-prod.properties.example ✅ MySQL, Redis(주석), Graceful Shutdown, 로그 파일 (documents 10_환경_설정_템플릿)