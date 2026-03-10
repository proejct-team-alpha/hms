<!-- Parent: ../../../../../../AI-CONTEXT.md -->

# static — 정적 리소스

## 목적

CSS, JavaScript, 이미지 등 정적 파일. Tailwind CSS 4.x + BEM 방법론.

## 디렉토리 구조

```
static/
├── css/             ← 스타일시트
├── js/              ← JavaScript (라이브러리 + 페이지별)
│   └── pages/       ← 페이지 전용 JS
└── images/          ← 이미지 (.gitkeep)
```

## CSS 파일

| 파일 | 설명 |
|------|------|
| input.css | Tailwind CSS 입력 파일 |
| tailwind.min.css | Tailwind CSS 빌드 결과물 |
| style.css | 공통 스타일 |
| common.css | 공통 컴포넌트 스타일 |
| home-style.css | 홈·예약 화면 스타일 |
| admin-style.css | 관리자 화면 스타일 |
| doctor-style.css | 의사 화면 스타일 |
| style-staff.css | 스태프 화면 스타일 |
| style-nurse.css | 간호사 화면 스타일 |
| style-patient.css | 환자 화면 스타일 |
| style-item-manager.css | 물품관리 스타일 |
| flatpickr.min.css | Flatpickr 캘린더 (로컬 서빙) |

## JavaScript — 라이브러리

| 파일 | 설명 |
|------|------|
| feather.min.js | Feather 아이콘 (모든 페이지 필수) |
| flatpickr.min.js | Flatpickr 날짜 선택기 (예약 폼) |
| chart.min.js | Chart.js (관리자 대시보드 차트) |
| lucide.min.js | Lucide 아이콘 |
| app.js | 공통 앱 스크립트 |

## JavaScript — 페이지별 (js/pages/)

| 파일 | 설명 |
|------|------|
| admin-dashboard.js | 관리자 대시보드 차트/통계 |
| staff-dashboard.js | 스태프 대시보드 |
| staff-reception-detail.js | 접수 상세 처리 |
| staff-walkin-reception.js | 방문 접수 |
| staff-phone-reservation.js | 전화 예약 |
| doctor-treatment-detail.js | 진료 완료 처리 |
| nurse-patient-detail.js | 환자 상세 |
| item-manager-dashboard.js | 물품관리 대시보드 |

## CSS 규칙 요약

- BEM 방법론: `block__element--modifier`
- 접두어: `l-`(레이아웃) / `c-`(컴포넌트) / `u-`(유틸) / `is-`(상태) / `js-`(JS훅)
- 단위: `rem/em` 우선, `px` 금지 (경계값 예외)
- 변수: `:root { --color-* }` CSS Custom Properties
- 금지: `!important`, id 셀렉터, 인라인 스타일, `float`

## AI 작업 지침

- CDN 사용 금지 (CSP unsafe-inline/외부 URL 차단) — 모든 라이브러리 로컬 서빙
- Tailwind 빌드: `npm run build` (tailwind CSS CLI)
- JS `var` 금지 → `const/let`
- `innerHTML` 직접 사용 금지 (XSS) → `textContent` 또는 `createElement`
- `feather.replace()` 모든 페이지 최하단 필수
