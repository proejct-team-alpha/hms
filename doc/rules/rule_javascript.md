# JavaScript 코딩 규칙

> **기준**: 2025~2026 현대 JavaScript (ES2022+). Vanilla JS, 라이브러리·프레임워크와 무관하게 적용 가능한 공통 규칙이다.

---

## 1. 변수 & 상수 선언 규칙 (Variables & Constants)

| #   | Rule                                                                                                                                                                          | 비고                                            |
| --- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- |
| 1   | **var는 절대 사용하지 않는다** (ES6 이후 완전 금지)                                                                                                                           |                                                 |
| 2   | 재할당이 필요 없는 모든 변수는 **const**로 선언한다. (기본 원칙: const > let). const 재할당 시도 시 런타임 에러 → 실수로 값을 바꾸는 버그를 조기에 발견 가능                  |                                                 |
| 3   | 재할당이 반드시 필요한 경우에만 **let**을 사용한다. (for 루프의 i, j 등은 let 허용, for...of/forEach 내부에서는 const 추천)                                                   |                                                 |
| 4   | const와 let은 **사용 직전에 선언**한다. let/const는 TDZ(Temporal Dead Zone)로 호이스팅되나 선언 전 접근 시 ReferenceError → hoisting 혼란 최소화                              |                                                 |
| 5   | 같은 스코프 내에서 const를 let보다 위에 선언한다. (가독성)                                                                                                                    |                                                 |
| 6   | **전역 변수는 절대 사용하지 않는다.** (window.xxx, globalThis.xxx 직접 할당 금지. 필요 시 모듈·클로저 활용)                                                                   |                                                 |
| 7   | **네이밍**: 변수/함수 → camelCase, 상수(불변) → UPPER*SNAKE_CASE, 클래스/생성자 → PascalCase. **private 필드**: **`#privateField`** 권장. 빌드 환경 미지원 시 `*` 접두사 허용 | 예: `const MAX_UPLOAD_SIZE = 10 * 1024 * 1024;` |

---

## 2. 함수 선언 및 사용 규칙 (Functions)

| #   | Rule                                                                                                                                                                         | 비고                         |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------- |
| 1   | **네이밍**: camelCase, 동사 + 목적어 형태 권장. (좋음: fetchUserData, calculateTotalPrice, handleSubmit / 나쁨: fn, doIt, x)                                                 |                              |
| 2   | **화살표 함수**를 기본으로 사용 (특히 콜백, 짧은 함수). 한 줄 표현식에서는 **암시적 return** 적극 권장. `function name() {}` 은 생성자, this 바인딩 필요, 재귀 함수에만 사용 | `const double = x => x * 2;` |
| 3   | **async 함수**는 반드시 async 키워드 명시                                                                                                                                    |                              |
| 4   | 함수는 **한 가지 역할만** 수행. 단일 책임 원칙 우선. 맥락에 따라 30~40줄까지 허용하는 팀도 있으나, 길어지면 분리·리팩토링 검토                                               |                              |
| 5   | 매개변수 기본값 적극 활용: `function greet(name = 'Guest') {}`                                                                                                               |                              |
| 6   | 반환은 early return 또는 단일 return 중 팀 컨벤션 따름                                                                                                                       |                              |
| 7   | 익명 함수는 거의 사용하지 않는다. (map, filter 등 짧은 화살표 함수는 예외 허용)                                                                                              |                              |
| 8   | **IIFE**는 특별한 경우에만 사용. 모듈 스코프 + `import`로 대부분 대체 가능                                                                                                   |                              |

---

## 3. 비동기 처리 규칙 (Async/Await 중심)

| #   | Rule                                                                                                                                                       | 비고                                             |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| 1   | **비동기 처리 기본 방식은 async/await**. `.then().catch()` 체인은 최대한 피한다. 콜백 스타일(callback hell)은 절대 사용하지 않는다.                        |                                                  |
| 2   | 모든 비동기 함수는 `async`로 선언한다.                                                                                                                     | `async function fetchUser(id) { ... }`           |
| 3   | Promise를 반환하는 함수는 **await 없이 호출하지 않는다**. (fire-and-forget 금지)                                                                           |                                                  |
| 4   | 병렬 비동기 처리 시 **Promise.all** 적극 활용. 단, 하나라도 reject 시 전체 reject 되므로, 일부 실패해도 나머지 결과가 필요하면 **Promise.allSettled** 고려 | `const [user, posts] = await Promise.all([...])` |
| 5   | **AbortController**는 직접 fetch 사용 시 적극 활용. 타임아웃·취소 가능 wrapper 권장.                                                                       |                                                  |
| 6   | **Promise.any**, **Promise.withResolvers** (ES2024) 등 상황에 맞게 활용                                                                                    |                                                  |
| 7   | **Top-level await**: 모듈 최상위에서 `await` 사용 가능한 환경이라면 적극 활용                                                                              |                                                  |

---

## 4. 예외 및 오류 처리 규칙 (Error Handling)

| #   | Rule                                                                                                                                                         | 비고                                                                                              |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------- |
| 1   | **async 함수 내에서는 반드시 try-catch 사용**                                                                                                                |                                                                                                   |
| 2   | **커스텀 에러 클래스** 적극 활용 (도메인별 에러 구분. 예: AuthError, NetworkError)                                                                           |                                                                                                   |
| 3   | **Error Cause 체이닝** 적극 활용 (ES2022+). 원인 에러를 `cause`로 전달하여 스택 추적 용이하게                                                                | `throw new Error("Failed", { cause: originalError });`                                            |
| 4   | **fetch 응답 에러** 처리 시 `response.ok` 검사 후 `cause`에 응답 본문 포함 권장                                                                              | `if (!response.ok) throw new Error('HTTP ' + response.status, { cause: await response.text() });` |
| 5   | **전역 에러 핸들링**: `window.onerror`, `unhandledrejection` 이벤트를 모니터링 도구에 연결                                                                   |                                                                                                   |
| 6   | **console.\*** 메서드는 개발 환경에서만 사용. 프로덕션에서는 eslint-plugin-no-console + 빌드 시 strip 조합 권장. (많은 팀이 일부 허용하나, 이상적은 제거)    |                                                                                                   |
| 7   | **Promise rejection은 반드시 catch 처리**. unhandledrejection 이벤트는 모니터링 도구에 연결                                                                  |                                                                                                   |
| 8   | 예상 가능한 에러(404, 401, 403 등) → 사용자 친화적 메시지로 변환. 예상치 못한 에러(500, 네트워크 오류) → "알 수 없는 오류가 발생했습니다" + 재시도 버튼 제공 |                                                                                                   |

---

## 5. 네이밍 컨벤션 전체 요약

| 대상          | 규칙                                                          | 예시                              |
| ------------- | ------------------------------------------------------------- | --------------------------------- |
| 변수·함수     | camelCase                                                     | `fetchUserData`, `handleSubmit`   |
| 상수(불변)    | UPPER_SNAKE_CASE                                              | `MAX_UPLOAD_SIZE`, `API_BASE_URL` |
| 클래스·생성자 | PascalCase                                                    | `UserService`, `DatePicker`       |
| private 필드  | **`#privateField`** 권장. 빌드 환경 미지원 시 `_` 접두사 허용 | `#internalCache`, `_legacyField`  |

---

## 6. 금지 패턴 (Do Not)

- ❌ **var** 사용
- ❌ **.then().catch()** 체인 남용 (async/await 우선)
- ❌ **async 함수 안에서 await 없이 then() 섞어 쓰기**
- ❌ **전역 변수** (window.xxx, globalThis.xxx 직접 할당)
- ❌ **fire-and-forget** (Promise 반환 함수를 await 없이 호출)
- ❌ **콜백 지옥** (callback hell)
- ❌ **new Promise((resolve) => ...)** 불필요한 래핑 (이미 Promise 반환하는 API는 그대로 사용)
- ❌ **Object.assign({}, obj)** → `{ ...obj }` 구조 분해 할당 선호
- ❌ **for-in** (객체 순회) → `Object.keys` / `Object.entries` / `Object.values` + `for-of` 선호
- ❌ **==** 사용 → **===** 강제 (eslint eqeqeq)
- ❌ **parseInt(str)** radix 누락 → `Number(str)` 또는 `+str` 선호. parseInt 사용 시 `parseInt(str, 10)` 명시
- ❌ **innerHTML** 직접 사용 (XSS 위험). **필요 시 DOMPurify 등으로 sanitization 필수** 후 제한적 사용.

---

## 7. 추천 라이브러리 & 패턴

| 용도            | 추천                                                                |
| --------------- | ------------------------------------------------------------------- |
| HTTP 클라이언트 | **fetch** 기반 ky, ofetch 권장. 직접 fetch 시 AbortController 활용. |
| 폼·입력 검증    | Zod, Yup (데이터 검증 스키마)                                       |
| 에러 모니터링   | Sentry, Datadog, LogRocket                                          |
| XSS 방지        | DOMPurify (HTML sanitization)                                       |
| 린트·포맷       | **ESLint + Prettier** 필수. ESLint 9+ **flat config** 사용 권장.    |
