<!-- Parent: ../../../../../../AI-CONTEXT.md -->

# templates — Mustache 뷰 템플릿

## 목적

SSR 뷰 전체. Mustache 엔진 사용. L1(비회원)/L2(직원로그인)/L3(로그인+사이드바) 레이아웃으로 구성.

## 레이아웃 규칙

| 레이아웃 | 구성 | 사용 화면 |
|---------|------|----------|
| L1 | header-public + footer-public | 홈, 예약 (비회원) |
| L2 | header-login + footer-staff | 로그인 화면 |
| L3 | header-staff + sidebar-* + footer-staff | 모든 직원 화면 |

## 디렉토리 구조

```
templates/
├── common/          ← 공통 파셜 (책임개발자 소유, 수정 금지)
├── home/            ← 홈 메인 (개발자 A)
├── reservation/     ← 비회원 예약 S00~S04 (개발자 A)
├── auth/            ← 로그인 (책임개발자)
├── staff/           ← 스태프 화면 (개발자 B)
├── doctor/          ← 의사 화면 (개발자 B)
├── nurse/           ← 간호사 화면 (개발자 B)
├── admin/           ← 관리자 화면 (개발자 C)
└── item-manager/    ← 물품 관리자 화면
```

## common/ 파셜 목록

| 파일 | L1/L2/L3 | 설명 |
|------|----------|------|
| header-public.mustache | L1 | 비회원 헤더 (MediCare+ 로고 + 직원로그인 링크) |
| header-staff.mustache | L3 | 직원 헤더 |
| header-login.mustache | L2 | 로그인 화면 헤더 |
| footer-public.mustache | L1 | 비회원 푸터 |
| footer-staff.mustache | L2/L3 | 직원 푸터 |
| sidebar-staff.mustache | L3 | STAFF 사이드바 |
| sidebar-doctor.mustache | L3 | DOCTOR 사이드바 |
| sidebar-nurse.mustache | L3 | NURSE 사이드바 |
| sidebar-admin.mustache | L3 | ADMIN 사이드바 |
| sidebar-item-manager.mustache | L3 | 물품관리 사이드바 |
| sidebar-patient.mustache | L3 | 환자 사이드바 |

## reservation/ 파일

| 파일 | 화면 |
|------|------|
| patient-choice.mustache | S00 예약방법 선택 |
| symptom-reservation.mustache | S01 AI 증상 입력 |
| direct-reservation.mustache | S03 직접 예약 폼 (Flatpickr 포함) |
| reservation-complete.mustache | S04 예약 완료 |

## AI 작업 지침

- `common/**` 파셜 수정 금지 (책임개발자 소유)
- 비회원 화면에서 `header-staff`, `sidebar-*` 사용 금지
- Mustache 변수: `{{변수명}}`, 섹션: `{{#조건}}...{{/조건}}`
- CSRF: `<input type="hidden" name="{{_csrf.parameterName}}" value="{{_csrf.token}}">`
- LayoutModelInterceptor 자동 주입 변수 활용 (`{{pageTitle}}`, `{{loginName}}` 등)
- `feather.replace()` 모든 페이지 하단에 필수 (아이콘 초기화)
