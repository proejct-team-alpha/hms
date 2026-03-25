<!-- Parent: ../AI-CONTEXT.md -->

# static/js/pages — 페이지별 JavaScript

## 목적

각 Mustache 뷰 페이지에 특화된 DOM 조작, 이벤트 핸들링, 차트 렌더링 및 AJAX 요청 로직.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| admin-dashboard.js | 관리자 대시보드 (Chart.js 연동, 통계 업데이트) |
| staff-dashboard.js | 스태프 대시보드 (환자 목록, 상태 변경 AJAX) |
| doctor-treatment-detail.js | 진료 상세 (처방 입력, 진료 완료 처리) |
| nurse-patient-detail.js | 환자 정보 관리 (활력 징후 입력 등) |
| staff-reception-detail.js | 접수 상세 처리 (상태 전환 로직) |

## 하위 디렉토리

- 해당 없음

## AI 작업 지침

- `static/js/app.js`에서 제공하는 공용 유틸리티를 최대한 활용한다.
- `const/let`을 사용하고, 전역 변수 오염을 방지하기 위해 즉시 실행 함수(IIFE)나 모듈 패턴을 권장한다.
- `feather.replace()`가 호출된 후 아이콘이 정상적으로 렌더링되는지 확인한다.
- AJAX 요청 시 `CSRF 토큰`을 헤더나 파라미터에 포함시켜야 한다.
- 모든 API 응답은 `Resp` 객체(성공 여부, 데이터, 메시지) 형식에 맞추어 처리한다.

## 테스트

- 개발자 도구 콘솔에서 JS 에러가 없는지 확인한다.
- API 호출 시 네트워크 탭을 통해 페이로드와 응답을 검증한다.

## 의존성

- 외부: Chart.js, Flatpickr, Feather Icons, Lucide (모든 라이브러리는 로컬 서빙됨)
- 내부: `common.js`, `app.js` (공용 로직)
