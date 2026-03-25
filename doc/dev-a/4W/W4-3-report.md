# W4-3 리포트 - LLM 예외 클래스 이식

## 작업 개요
- **작업명**: LlmServiceUnavailableException, LlmTimeoutException을 HMS common/exception에 이식
- **추가 파일**:
  - `src/main/java/com/smartclinic/hms/common/exception/LlmServiceUnavailableException.java` (신규)
  - `src/main/java/com/smartclinic/hms/common/exception/LlmTimeoutException.java` (신규)

## 작업 내용

### 1. 사전 분석 결과
원본 예외 클래스(spring-python-llm)는 `RuntimeException`을 직접 상속하고 있었다.
HMS의 `CustomException`을 상속하는 방식으로 전환하면 `GlobalExceptionHandler`의
`handleBusiness(CustomException)` 핸들러가 하위 클래스까지 자동으로 포괄하므로
**GlobalExceptionHandler 수정 없이** 503/504 응답이 처리된다.

또한 `CustomException`에 이미 `LLM_SERVICE_UNAVAILABLE` 에러코드와
`serviceUnavailable()` 팩토리 메서드가 정의되어 있어 일관성을 유지할 수 있었다.

### 2. LlmServiceUnavailableException (503)

| 항목 | 값 |
|---|---|
| errorCode | `LLM_SERVICE_UNAVAILABLE` |
| httpStatus | `503 Service Unavailable` |
| 상속 | `CustomException` |
| 생성자 | `(String message, Throwable cause)`, `(String message)` |

### 3. LlmTimeoutException (504)

| 항목 | 값 |
|---|---|
| errorCode | `LLM_TIMEOUT` |
| httpStatus | `504 Gateway Timeout` |
| 상속 | `CustomException` |
| 생성자 | `(String message, Throwable cause)`, `(String message)` |

## 빌드 결과

```
BUILD SUCCESSFUL in 2s
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 두 개의 새 예외 클래스를 추가한 후 프로젝트 전체가 오류 없이 빌드되었음을 나타냅니다.
> - **왜 이렇게 썼는지**: 새로운 클래스를 추가했을 때 기존 코드와 이름 충돌이나 의존성 문제가 없는지 빌드로 확인합니다.
> - **쉽게 말하면**: 새 직원이 입사했을 때 기존 팀과 문제 없이 잘 어울리는지 확인하는 과정입니다.

## 특이사항
- `GlobalExceptionHandler` 수정 없음 — `handleBusiness(CustomException)` 핸들러가 하위 클래스 자동 처리
- `LLM_TIMEOUT` 에러코드는 기존 `CustomException` 주석에 없던 코드 — Task 5 Service 이식 시 활용 예정
- 원본 대비 `cause` 전달 방식: 원본은 `super(message, cause)` 사용, HMS는 `CustomException`이 `cause` 파라미터를 받지 않으므로 `initCause(cause)`로 처리
