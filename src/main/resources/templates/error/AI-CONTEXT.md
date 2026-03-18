<!-- Parent: ../AI-CONTEXT.md -->

# templates/error — 에러 뷰

## 목적

Spring Boot 기본 에러 핸들링 메커니즘에 의해 렌더링되는 HTTP 상태 코드별 Mustache 페이지.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| 403.mustache | 접근 권한 없음 (Forbidden) |
| 404.mustache | 페이지를 찾을 수 없음 (Not Found) |
| 500.mustache | 서버 내부 오류 (Internal Server Error) |

## 하위 디렉토리

- 해당 없음

## AI 작업 지침

- 모든 에러 페이지는 `L1` (Public) 또는 `L2` (Staff용 간결한 헤더) 레이아웃을 사용한다.
- `GlobalExceptionHandler`에서 전달하는 `message`, `status`, `timestamp` 모델 데이터를 표시한다.
- 사용자 친화적인 메시지를 제공하고, 홈으로 돌아가는 링크를 포함한다.

## 테스트

- 존재하지 않는 URL 접속 시 404 페이지 확인.
- 권한 없는 메뉴 접근 시 403 페이지 확인.
- 의도적 예외 발생 시 500 페이지 확인.
