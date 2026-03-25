# Toast 메시지 전환 기획서

## 개요

현재 Mustache 템플릿의 오류/에러/경고/성공 메시지가 각 페이지에 고정된 인라인 div로 표시되고 있음.
이를 통일된 Toast UI 컴포넌트로 전환하여 UX 일관성을 확보한다.

---

## 현재 메시지 표시 방식 분류

| 유형                  | 현재 방식                                      | 파일 수                   |
| --------------------- | ---------------------------------------------- | ------------------------- |
| 페이지 레벨 에러/성공 | 인라인 div (`{{errorMessage}}`, `{{message}}`) | 10개                      |
| 폼 필드 에러          | 입력 필드 아래 `<p>` 텍스트                    | 1개 (staff-form)          |
| JS 동적 메시지        | JS로 show/hide                                 | 2개                       |
| Toast (이미 적용)     | fixed bottom toast                             | 1개 (symptom-reservation) |

---

## Toast 전환 대상 파일 (10~12개)

### A. 페이지 레벨 에러/성공 메시지 → Toast 전환 (10개)

| #   | 파일                                      | 변수명                                   | 메시지 종류                |
| --- | ----------------------------------------- | ---------------------------------------- | -------------------------- |
| 1   | `auth/login.mustache`                     | `{{error}}`, `{{logout}}`                | 로그인 실패, 로그아웃 성공 |
| 2   | `reservation/direct-reservation.mustache` | `{{errorMessage}}`                       | 예약 검증 오류             |
| 3   | `reservation/reservation-lookup.mustache` | `{{errorMessage}}`                       | 조회 실패                  |
| 4   | `reservation/reservation-cancel.mustache` | `{{errorMessage}}`                       | 취소 오류                  |
| 5   | `reservation/reservation-modify.mustache` | `{{errorMessage}}`                       | 변경 오류                  |
| 6   | `admin/staff-form.mustache`               | `{{errorMessage}}`                       | 직원 등록/수정 오류        |
| 7   | `admin/item-form.mustache`                | `{{errorMessage}}`, `{{message}}`        | 물품 오류/성공             |
| 8   | `admin/rule-new.mustache`                 | `{{errorMessage}}`                       | 규칙 등록 오류             |
| 9   | `admin/rule-detail.mustache`              | `{{errorMessage}}`, `{{successMessage}}` | 규칙 오류/성공             |
| 10  | `item-manager/item-form.mustache`         | `{{errorMessage}}`                       | 물품 관리 오류             |

### B. JS 동적 메시지 → Toast 통합 (2개)

| #   | 파일                                       | 현재 방식                                      |
| --- | ------------------------------------------ | ---------------------------------------------- |
| 11  | `reservation/symptom-reservation.mustache` | error-toast (이미 toast) + form-error (인라인) |
| 12  | `admin/rule-detail.mustache`               | JS feedback div                                |

### C. 제외 — 전환하지 않는 항목

| 파일                                                             | 이유                                                    |
| ---------------------------------------------------------------- | ------------------------------------------------------- |
| `admin/staff-form.mustache` 필드별 에러 (`{{usernameError}}` 등) | 어느 필드가 잘못되었는지 바로 보여야 하므로 인라인 유지 |
| `error/500.mustache`                                             | 전용 에러 페이지이므로 toast 불필요                     |

---

## 작업 항목

### 1. 공통 Toast 컴포넌트 생성 (신규)

- `templates/common/toast.mustache` — partial로 모든 페이지에서 `{{> common/toast}}`로 include
- 타입: 성공(green), 에러(red), 경고(yellow) 3종
- 자동 소멸 (3~5초) + 닫기 버튼
- `showToast(type, message)` JS 헬퍼 함수 포함
- 위치: fixed bottom-center (현재 symptom-reservation과 동일 스타일)

### 2. 템플릿 파일 수정 (10~12개)

각 파일에 대해:

- 기존 인라인 에러/성공 div 제거
- `{{> common/toast}}` partial 추가
- 서버 렌더링 메시지(`{{errorMessage}}` 등)를 toast로 표시하는 초기화 스크립트 추가:

  ```html
  {{#errorMessage}}
  <script>
    showToast("error", "{{errorMessage}}");
  </script>
  {{/errorMessage}}
  ```

### 3. 변경 없는 항목

- **컨트롤러(Java)** — `request.setAttribute("errorMessage", ...)` 그대로 유지
- **변수명** — 기존 Mustache 변수명 변경 없음
- **폼 필드 에러** — 인라인 유지 (toast와 별개)

---

## Toast UI 스펙

```
┌──────────────────────────────────────────────┐
│  [icon]  메시지 텍스트                    [X] │
└──────────────────────────────────────────────┘
  ↑ fixed bottom-6 left-1/2 -translate-x-1/2
```

| 타입    | 배경색                    | 아이콘           | 자동 소멸 |
| ------- | ------------------------- | ---------------- | --------- |
| error   | `bg-red-600 text-white`   | `alert-circle`   | 5초       |
| success | `bg-green-600 text-white` | `check-circle`   | 3초       |
| warning | `bg-amber-500 text-white` | `alert-triangle` | 4초       |

---

## 예상 작업량
    | 예상                         |
| ----------------------- | ---------------------------- |
| toast.mustache + JS     | 1건                          |
| 템플릿 수정             | 10~12건                      |
| 테스트 st.mustache + JS | 1건                          |
| 템플릿 수정             | 10~12건                      |
| 테스트                  수정 | 10~12건 |
| 테스트 | 각 페이지 에러 시나리오 확인 |

---

## 비고

- 기존 `symptom-reservation.mustache`의 error-toast를 공통 컴포넌트로 교체하여 중복 제거
- 추후 AJAX 전환하는 페이지가 늘어나면 JS에서 `showToast()`를 직접 호출하는 패턴으로 통일
