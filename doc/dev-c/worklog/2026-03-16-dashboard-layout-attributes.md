# 관리자 대시보드 공통 레이아웃 데이터 정리

## 배경
- `AdminDashboardController.java`와 `dashboard.mustache`를 과거 브랜치 버전으로 덮어쓴 뒤
- 최신 `common/header-staff.mustache`, `common/sidebar-admin.mustache`, `common/footer-staff.mustache`와 결합되면서
- 공통 partial이 기대하는 값과 대시보드 화면 구조가 어긋나는 상황이 생김

## 확인한 구조
### 1. 대시보드 본문 데이터
`AdminDashboardController`는 대시보드 통계 DTO를 `model`로 내려줌.

- `model.todayReservations`
- `model.totalReservations`
- `model.totalStaff`
- `model.lowStockItems`

이 값들은 대시보드 본문에서 사용하므로 유지 대상이다.

### 2. 공통 partial이 기대하는 값
#### `common/header-staff.mustache`
- `dashboardUrl`
- `pageTitle`
- `loginName`
- `roleLabel`
- `_csrf`

#### `common/sidebar-admin.mustache`
- `isAdminDashboard`
- `_csrf`

## 실제 반영 내용
`AdminDashboardController`에 아래 request attribute를 추가했다.

- `pageTitle = "관리자 대시보드"`
- `dashboardUrl = "/admin/dashboard"`
- `loginName = authentication.getName()`
- `roleLabel = ADMIN 역할 문자열`
- `isAdminDashboard = true`

기존 `model` attribute는 그대로 유지했다.

## 왜 추가했고 왜 제거하지 않았는가
- 현재 문제는 불필요한 데이터를 많이 내려서가 아니라, 공통 partial이 기대하는 값이 부족한 쪽에 가까웠다.
- 따라서 `model`은 유지하고, 공통 레이아웃에서 직접 쓰는 값만 보강하는 방식이 더 안전하다.
- `common/*`를 과거 구조로 되돌리는 것보다 컨트롤러가 최신 공통 구조를 맞추는 편이 영향 범위가 작다.

## 인터셉터와의 관계
프로젝트에는 `LayoutModelInterceptor`가 존재하며, 아래 공통 값을 자동 주입한다.

- `pageTitle`
- `loginName`
- `roleLabel`
- `dashboardUrl`
- `isAdminDashboard` 등 사이드바 활성화 플래그

즉 런타임 기준으로는 인터셉터가 이미 많은 공통 값을 보강한다.
다만 대시보드 컨트롤러를 과거 구조로 되돌린 뒤에도 화면이 최소한 필요한 값을 직접 갖도록 컨트롤러에서 명시적으로 보강했다.

## 테스트에서 추가로 맞춘 점
`AdminDashboardControllerTest`는 Mustache가 request attribute를 읽을 수 있도록 아래 설정을 명시했다.

- `spring.mustache.servlet.expose-request-attributes=true`
- `spring.mustache.servlet.allow-request-override=true`

이 설정이 없으면 `request.setAttribute("model", stats)` 방식이 테스트 렌더링에서 보이지 않을 수 있다.

## 검증 결과
실행 명령:
- `./gradlew test --tests 'com.smartclinic.hms.admin.dashboard.AdminDashboardControllerTest'`

결과:
- `BUILD SUCCESSFUL`

## 남은 메모
- 이번 작업은 공통 partial용 데이터 보강에 집중했다.
- `dashboard.mustache` 자체의 한글 깨짐, 깨진 태그, 아이콘 혼용 문제는 별도 정리 대상이다.