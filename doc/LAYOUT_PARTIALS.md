# 공통 레이아웃 Mustache Partials

> **참조 문서:** [proejct-team-alpha/documents](https://github.com/proejct-team-alpha/documents) — 전체 화면 목록 및 화면별 기능 정의서 v5.1 §2

## 1. 파일 경로

모든 partials는 `src/main/resources/templates/common/` 에 위치합니다.

| 파일 | 문서 코드 | 설명 |
|------|----------|------|
| `header-public.mustache` | H1 | 비회원 헤더 (L1 레이아웃) |
| `header-staff.mustache` | H2 | 내부 직원 헤더 (L2 레이아웃) |
| `header-login.mustache` | H3 | 로그인 전용 헤더 (L3 레이아웃) |
| `footer-public.mustache` | F1 | 비회원 푸터 (L1 레이아웃) |
| `footer-staff.mustache` | F2 | 내부 직원 푸터 (L2 레이아웃) |
| `sidebar-staff.mustache` | S1 | STAFF 사이드 메뉴 |
| `sidebar-doctor.mustache` | S2 | DOCTOR 사이드 메뉴 |
| `sidebar-nurse.mustache` | S3 | NURSE 사이드 메뉴 |
| `sidebar-admin.mustache` | S4 | ADMIN 사이드 메뉴 |
| `sidebar-item-manager.mustache` | S5 | ITEM_MANAGER 사이드 메뉴 |
| `sidebar-patient.mustache` | — | 환자용 (L1은 사이드바 없음) |

## 2. 화면별 적용 조합

### L1 — 비회원 공개 (화면 00~04)
```mustache
{{> common/header-public}}
<!-- 본문 -->
{{> common/footer-public}}
```
- **적용 화면:** home/index, reservation/*

### L2 — 내부 직원 (화면 06~42)
```mustache
{{> common/sidebar-{role}}}
<div class="main-content flex-1 flex flex-col ml-64">
  {{> common/header-staff}}
  <main class="content-wrapper flex-1 p-6">
    <!-- 본문 -->
  </main>
  {{> common/footer-staff}}
</div>
```
- **sidebar-{role}:** staff | doctor | nurse | admin | item-manager
- **적용 화면:** admin/*, staff/*, doctor/*, nurse/*, item-manager/*

### L3 — 로그인 (화면 05)
```mustache
{{> common/header-login}}
<!-- 본문 (로그인 폼) -->
```
- **적용 화면:** auth/login

## 3. 모델 변수 (LayoutModelInterceptor 주입)

| 변수 | 설명 |
|------|------|
| `pageTitle` | 현재 화면 제목 (헤더 H2에 표시) |
| `loginName` | 로그인 사용자명 |
| `roleLabel` | ROLE (STAFF, DOCTOR, NURSE, ADMIN, ITEM_MANAGER) |
| `dashboardUrl` | 역할별 대시보드 URL |
| `currentPath` | 현재 요청 경로 |
| `_csrf` | CSRF 토큰 (로그아웃 폼 등) |
| `isStaffDashboard`, `isAdminDashboard` 등 | 사이드바 메뉴 활성화 플래그 |

## 4. Import 예시

```mustache
<!DOCTYPE html>
<html lang="ko">
<head>...</head>
<body class="min-h-screen flex">
  {{> common/sidebar-admin}}
  <div class="main-content flex-1 flex flex-col ml-64">
    {{> common/header-staff}}
    <main class="content-wrapper flex-1 p-6">
      <h1>관리자 대시보드</h1>
      <p>본문 내용</p>
    </main>
    {{> common/footer-staff}}
  </div>
  <script src="/js/feather.min.js"></script>
  <script>feather.replace();</script>
</body>
</html>
```
