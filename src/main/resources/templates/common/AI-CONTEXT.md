<!-- Parent: ../AI-CONTEXT.md -->

# templates/common

## 목적

모든 역할(Public, Staff, Doctor, Nurse, Admin)에서 공용으로 사용되는 레이아웃 컴포넌트(Header, Footer, Sidebar).

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| header-public.mustache | 비로그인/환자용 헤더 |
| header-staff.mustache | 직원용 헤더 (로그아웃 버튼 등 포함) |
| sidebar-admin.mustache | 관리자용 사이드바 메뉴 |
| sidebar-doctor.mustache | 의사용 사이드바 메뉴 |
| sidebar-nurse.mustache | 간호사용 사이드바 메뉴 |
| sidebar-staff.mustache | 스태프용 사이드바 메뉴 |
| footer-public.mustache | 공용 푸터 |

## AI 작업 지침

- 사이드바 메뉴 수정 시 `LayoutModelInterceptor`에서 전달되는 모델 데이터와 일치하는지 확인한다.
- 활성화된 메뉴 표시를 위한 로직(active class)은 서버 측 모델 데이터를 기반으로 처리한다.
