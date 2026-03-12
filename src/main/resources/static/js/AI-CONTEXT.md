<!-- Parent: ../AI-CONTEXT.md -->

# static/js

## 목적

클라이언트 측 인터랙션 및 AJAX 처리를 위한 JavaScript 파일들.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| app.js | 전역 공용 스크립트 |
| sidebar-admin.js | 관리자 사이드바 제어 로직 |
| footer-admin.js | 관리자 페이지 차트 및 푸터 로직 |
| feather.min.js | Feather Icons 라이브러리 |
| flatpickr.min.js | 날짜 선택 라이브러리 |

## AI 작업 지침

- `var` 사용을 금지하고 `const` 및 `let`을 사용한다.
- 비동기 처리는 `async/await` 패턴을 권장한다.
- DOM 조작 시 가급적 현대적인 API(`querySelector` 등)를 사용한다.
