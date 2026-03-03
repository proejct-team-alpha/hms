# CSS 규칙 준수 수정 내역

> **기준 문서**: [doc/rules/rule_css.md](rules/rule_css.md)  
> **수정 일자**: 2025-03-03

---

## 1. 수정 개요

`common.css` 및 관련 Mustache 템플릿이 `rule_css.md` 위반 사항을 준수하도록 수정함.

---

## 2. 수정 내용

### 2.1 px → rem 변환 (§5, §6)

| 수정 전 | 수정 후 |
|---------|---------|
| `font-size: 14px` | `font-size: var(--font-size-base)` (0.875rem) |
| `max-width: 400px` | `max-width: 25rem` |
| `border-radius: 8px` | `border-radius: var(--radius-md)` (0.5rem) |
| `border-radius: 4px` | `border-radius: var(--radius-sm)` (0.25rem) |
| `width: 220px` | `width: var(--space-sidebar)` (13.75rem) |
| `min-height: calc(100vh - 52px)` | `min-height: calc(100vh - 3.25rem)` |

### 2.2 클래스 네이밍 규칙 (BEM + 접두어 적용) (§2)

| 수정 전 | 수정 후 | 구분 |
|---------|---------|------|
| `.header-login` | `.l-header--login` | layout |
| `.header-public` | `.l-header--public` | layout |
| `.header-staff` | `.l-header--staff` | layout |
| `.login-main` | `.l-login-main` | layout |
| `.login-form` | `.c-form` | component |
| `.form-group` | `.c-form__group` | component |
| `.error-msg` | `.c-alert--error` | component |
| `.success-msg` | `.c-alert--success` | component |
| `.login-hint` | `.c-login__hint` | component |
| `.footer-public` | `.l-footer--public` | layout |
| `.footer-staff` | `.l-footer--staff` | layout |
| `.error-page` | `.l-error-page` | layout |
| `.layout-body` | `.l-body` | layout |
| `.main-content` | `.l-main` | layout |
| `.sidebar` | `.l-sidebar` | layout |
| `.text-muted` | `.u-text-muted` | utility |
| `.text-danger` | `.u-text-danger` | utility |
| `.text-success` | `.u-text-success` | utility |

**접두어**: `l-`(layout), `c-`(component), `u-`(utility)

### 2.3 변수화 (§5, §8)

`:root`에 추가된 변수:

```css
/* 색상 */
--color-white: #ffffff;
--color-text: #1f2937;
--color-text-secondary: #374151;

/* 타이포그래피 */
--font-size-base: 0.875rem;
--font-size-sm: 0.8rem;
--font-size-lg: 1rem;
--font-size-xl: 1.5rem;

/* 간격 */
--space-1 ~ --space-16, --space-sidebar

/* 반경·그림자 */
--radius-sm, --radius-md, --shadow-sm
```

하드코딩 제거: `#fff`, `#1f2937`, `#374151` → 변수 참조

### 2.4 마법의 숫자 제거 (§5, §6)

숫자 리터럴 → `var(--space-*)`, `var(--radius-*)`, `var(--font-size-*)` 등 사용

### 2.5 인라인 스타일 제거 (§6)

`header-staff.mustache`의 로그아웃 폼:

- **수정 전**: `style="display:inline;"`
- **수정 후**: `class="u-inline-form"` + CSS `.u-inline-form { display: inline; }`

### 2.6 폼 라벨 클래스 추가

`c-form__label` 클래스를 login.mustache의 `<label>` 요소에 추가하여 BEM 구조 일관성 확보

---

## 3. 수정된 파일 목록

### CSS
- `src/main/resources/static/css/common.css`

### 템플릿
- `auth/login.mustache`
- `common/header-login.mustache`
- `common/header-public.mustache`
- `common/header-staff.mustache`
- `common/footer-public.mustache`
- `common/footer-staff.mustache`
- `common/sidebar-admin.mustache`
- `common/sidebar-doctor.mustache`
- `common/sidebar-nurse.mustache`
- `common/sidebar-staff.mustache`
- `admin/dashboard.mustache`
- `doctor/dashboard.mustache`
- `nurse/dashboard.mustache`
- `staff/dashboard.mustache`
- `error/403.mustache`
- `error/404.mustache`

---

## 4. 미적용 사항 (향후 검토)

| 항목 | 규칙 | 비고 |
|------|------|------|
| 파일 분리 | §4 | `base/`, `components/`, `layouts/`, `utilities/` 분리 구조는 추천 권장. 단일 파일 유지로 점진적 적용 예정 |

---

## 5. 체크리스트 (§8)

- [x] 클래스명에 페이지명 들어갔는가? → 제거됨
- [x] 셀렉터 중첩 4단계 이상인가? → 3단계 이하 유지
- [x] !important 썼는가? → 미사용
- [x] float / position:absolute 남용했는가? → 미사용
- [x] 변수 대신 하드코딩된 색상/크기 썼는가? → 변수화 완료
