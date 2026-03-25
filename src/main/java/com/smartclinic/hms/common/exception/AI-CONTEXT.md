<!-- Parent: ../AI-CONTEXT.md -->

# common/exception

## 목적

애플리케이션 전역 예외 처리 및 커스텀 예외 정의를 담당한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| CustomException.java | 비즈니스 로직 예외 처리를 위한 런타임 예외 클래스 (ErrorType 기반) |
| GlobalExceptionHandler.java | `@RestControllerAdvice(annotations = RestController.class)` — **REST API** 전용. 실패 응답 본문은 `Resp`(status, msg, body) |
| SsrExceptionHandler.java | `@ControllerAdvice(annotations = Controller.class)` — **SSR(Mustache)** 전용. `CustomException`의 HTTP 상태에 맞춰 403/404/500 뷰 반환 |

## 예외 처리 분리 (C-02 / 온보딩)

- **JSON API** (`@RestController`): `GlobalExceptionHandler` → `ResponseEntity<Resp<?>>`
- **서버 렌더링 화면** (`@Controller`): `SsrExceptionHandler` → 에러 템플릿 + `response.setStatus`

동일한 `CustomException`이라도 호출된 컨트롤러 종류에 따라 위 둘 중 하나만 적용된다.

## AI 작업 지침

- 새로운 비즈니스 예외 발생 시 `CustomException`을 사용하고, 상황에 맞는 `ErrorType`을 정의하거나 활용한다.
- REST 컨트롤러에서 던진 예외는 `GlobalExceptionHandler`가 JSON `Resp`로 변환한다.
- SSR 컨트롤러에서 던진 예외는 `SsrExceptionHandler`가 처리한다. **GlobalExceptionHandler만 수정해서 SSR을 커버하려 하지 말 것** (역할 중복·혼선).

## 의존성

- 내부: `common/util/Resp` (REST JSON 응답 시 사용)
