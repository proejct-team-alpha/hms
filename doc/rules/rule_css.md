# CSS 작성 규칙 (Spring + Mustache SSR 프로젝트)

> **기준**: 2025~2026년. 유지보수성과 확장성을 최우선으로 하는 CSS 작성 가이드.

---

## 1. 기본 철학

- **"클래스명은 의미(의도)를 담는다"**
  → 무엇을 하는 컴포넌트/요소인지 한눈에 알 수 있어야 함
- **"셀렉터 중첩은 3단계 이하 권장"** (BEM + 약간의 예외 허용)
- **"재사용 가능한 단위로 분리"**
  → 한 페이지에만 쓰이는 스타일은 최대한 피한다
- **"디자인 시스템의 일부"**라는 관점 유지

---

## 2. 클래스 네이밍 규칙 (BEM + 일부 변형)

### 기본 패턴

```text
컴포넌트명__요소--상태/변형
```

### 예시

```text
.card
.card__header
.card__title
.card__content
.card--highlighted
.card--error
.user-profile__avatar--large
```

### 추천 접두어 (선택적)

| 접두어 | 의미                        | 예시                       |
| ------ | --------------------------- | -------------------------- |
| `l-`   | layout (레이아웃 전용)      | `l-container`, `l-grid`    |
| `c-`   | component (재사용 컴포넌트) | `c-button`, `c-modal`      |
| `t-`   | typography                  | `t-heading-1`, `t-body-sm` |
| `u-`   | utility (단일 목적 유틸)    | `u-text-center`, `u-mt-16` |
| `is-`  | 상태 (JS로 토글되는 상태)   | `is-active`, `is-loading`  |
| `js-`  | JS에서만 사용하는 훅 클래스 | `js-tab-trigger`           |

### 권장하지 않는 네이밍

- **색상/크기 직접 포함**: `.blue-button-large` → `.c-button--primary--lg`
- **너무 긴 이름**: `.user-management-page-main-content-wrapper`
- **페이지명 포함**: `.notice-page-title` → `.c-notice-title`

---

## 3. 레이아웃 방식 우선순위 (2025~2026 기준)

- **CSS Grid** → 대부분의 2차원 레이아웃에 우선 사용
- **Flexbox** → 1차원 정렬, 콘텐츠 중심 정렬에 사용
- **Grid + Flex 조합** → 가장 현실적인 패턴

```css
/* 권장 */
.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

/* 덜 권장 (필요할 때만) */
.flex-row {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
}
```

### 절대 피해야 할 패턴

```css
/* ❌ 매우 취약한 코드 */
.wrapper > div > div > .content { ... }
.page .box .inner .title { ... }
```

---

## 4. 파일 구조 & 분리 기준 (추천)

```text
assets/
  css/
    base/               # reset, typography, root 변수 등
    ├── _reset.css
    ├── _typography.css
    ├── _variables.css
    └── _normalize-custom.css

    components/         # 재사용 가능한 단위 컴포넌트
    ├── _button.css
    ├── _card.css
    ├── _form.css
    ├── _modal.css
    └── _table.css

    layouts/            # 페이지 전체 레이아웃 관련
    ├── _header.css
    ├── _footer.css
    ├── _sidebar.css
    └── _container.css

    pages/              # 페이지별 특이사항만 (최소화!)
    ├── _dashboard.css
    └── _user-detail.css

    utilities/          # u- 접두어 유틸리티 클래스
    ├── _spacing.css
    ├── _text.css
    └── _visibility.css

    main.css            # 모든 파일 @import
```

### main.css 예시

```css
@import "base/variables";
@import "base/reset";
@import "base/typography";

@import "components/button";
@import "components/card";
@import "components/form";

@import "layouts/header";
@import "layouts/footer";

@import "utilities/spacing";
@import "utilities/text";
```

---

## 5. 변수 사용 원칙 (2025~2026)

```css
:root {
  /* 색상 */
  --primary-500: #3b82f6;
  --primary-600: #2563eb;
  --gray-100: #f3f4f6;
  --gray-900: #111827;

  /* 타이포그래피 */
  --font-base: "Pretendard", system-ui, sans-serif;

  /* 간격·크기 */
  --radius-md: 0.75rem; /* 12px */
  --space-4: 1rem;
  --space-5: 1.25rem;
  --space-6: 1.5rem;

  /* 그림자 */
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.1);
}
```

- **rem, em** 적극 활용 (px만 사용 금지)
- **마법의 숫자** 남발 금지 → 변수화 또는 `calc()` 사용

---

## 6. 금지/강력 지양 패턴

- ❌ **id 셀렉터** 사용 금지 (`#header`, `#content` 등)
- ❌ **!important** 최대한 사용 금지 (필요 시 주석 필수)
- ❌ **인라인 스타일** 사용 금지 (Mustache 안에서 `style=""` 쓰지 않기)
- ❌ **float** 기반 레이아웃
- ❌ **px 단위만** 사용하는 것 (rem, em 적극 활용)
- ❌ **마법의 숫자** (magic number) 남발 → 변수화 또는 계산식 사용

---

## 7. Mustache 환경 특화 팁

### 상태 클래스 활용

```html
<!-- 좋음 -->
<div class="card card--{{#isImportant}}highlighted{{/isImportant}}">
  <h3 class="card__title">{{title}}</h3>
  <div class="card__content">...</div>
</div>

<!-- 좋지 않음 -->
<div class="card" style="border-color: {{color}}; background: {{bg}}"></div>
```

### 폼 에러 상태

```html
<!-- 상태 클래스 활용 추천 -->
<div class="form-group form-group--{{#hasError}}error{{/hasError}}">
  <label class="form-label">이름</label>
  <input class="form-input" ... />
  {{#hasError}}
  <span class="form-error-message">{{errorMessage}}</span>
  {{/hasError}}
</div>
```

---

## 8. 요약 체크리스트

- [ ] 클래스명에 페이지명 들어갔는가?
- [ ] 셀렉터 중첩 4단계 이상인가?
- [ ] !important 썼는가?
- [ ] 재사용 가능성이 있는데 페이지 css에 넣었는가?
- [ ] float / position:absolute 남용했는가?
- [ ] 변수 대신 하드코딩된 색상/크기 썼는가?

---

> 이 규칙은 필요에 따라 점진적으로 강화/완화 가능합니다.
> **가장 중요한 것은 "팀 전체가 일관된 패턴을 따르는 것"입니다.**
