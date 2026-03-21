<!-- Parent: ../AI-CONTEXT.md -->

# templates/admin

## 목적

관리자 전용 대시보드 및 CRUD 관리 기능을 위한 Mustache 템플릿.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| dashboard.mustache | 관리자 대시보드 (통계 카드, 차트 포함) |
| department-list.mustache | 진료과 목록 및 관리 |
| item-list.mustache | 물품 목록 조회 및 검색 |
| item-form.mustache | 물품 등록/수정 폼 |
| staff-list.mustache | 직원 목록 조회 및 관리 |
| staff-form.mustache | 직원 등록/수정 폼 |
| rule-list.mustache | 운영 규칙 목록 및 관리 |
| rule-new.mustache | 운영 규칙 등록/수정 폼 |
| reservation-list.mustache | 예약 목록 및 취소 관리 |

## AI 작업 지침

- 모든 템플릿은 `layouts/`의 레이아웃을 확장하여 사용한다 (보통 `{{> layouts/header-admin}}` 등).
- 폼 데이터 전송 시 Spring Security의 CSRF 토큰을 포함해야 한다.
- 대시보드 차트 렌더링을 위해 `admin-style.css` 및 `footer-admin.js` 등을 활용한다.

## 의존성

- CSS: `static/css/admin-style.css`
- JS: `static/js/sidebar-admin.js`, `static/js/footer-admin.js`
