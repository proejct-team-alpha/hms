<!-- Parent: ../AI-CONTEXT.md -->

# common/exception

## 목적

애플리케이션 전역 예외 처리 및 커스텀 예외 정의를 담당한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| CustomException.java | 비즈니스 로직 예외 처리를 위한 런타임 예외 클래스 (ErrorType 기반) |
| GlobalExceptionHandler.java | `@ControllerAdvice`를 이용한 전역 예외 처리기 (HTML 및 JSON 응답 분리 처리) |

## AI 작업 지침

- 새로운 비즈니스 예외 발생 시 `CustomException`을 사용하고, 상황에 맞는 `ErrorType`을 정의하거나 활용한다.
- 컨트롤러 레이어에서 예외 발생 시 `GlobalExceptionHandler`가 이를 가로채 적절한 에러 페이지나 JSON 응답을 반환한다.

## 의존성

- 내부: `common/Resp` (JSON 응답 시 활용)
