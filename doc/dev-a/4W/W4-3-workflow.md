# W4-3 Workflow — LLM 예외 클래스 이식

> **작성일**: 4W
> **브랜치**: `feature/Llm`
> **목표**: `LlmServiceUnavailableException`, `LlmTimeoutException` HMS `common/exception/`에 이식

---

## 전체 흐름

```
CustomException 상속으로 LLM 예외 2개 생성
  → GlobalExceptionHandler 수정 불필요 (CustomException 포괄 처리)
  → ./gradlew build 검증
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 기존 구조 | `CustomException`에 `LLM_SERVICE_UNAVAILABLE(503)` 코드 정의됨 |
| GlobalExceptionHandler | `CustomException` 하위 클래스 포괄 처리 → 별도 핸들러 불필요 |
| 예외 종류 | `LlmServiceUnavailableException(503)`, `LlmTimeoutException(504)` |

---

## 실행 흐름

```
[1] LlmServiceUnavailableException — CustomException 상속 (503)
[2] LlmTimeoutException — CustomException 상속 (504)
[3] ./gradlew build — BUILD SUCCESSFUL 검증
```

---

## UI Mockup

```
[예외 클래스 작업 — UI 없음]
```

---

## 작업 목록

1. `LlmServiceUnavailableException.java` 신규 — `CustomException` 상속 (503)
2. `LlmTimeoutException.java` 신규 — `CustomException` 상속 (504)
3. `./gradlew build` — 오류 없음 검증

---

## 작업 진행내용

- [x] LlmServiceUnavailableException 작성
- [x] LlmTimeoutException 작성
- [x] 빌드 확인 — BUILD SUCCESSFUL

---

## 실행 흐름에 대한 코드

### LlmServiceUnavailableException (503)

```java
/**
 * Python LLM 서버 연결 실패 시 발생하는 예외 (503 Service Unavailable)
 */
public class LlmServiceUnavailableException extends CustomException {
    public LlmServiceUnavailableException(String message, Throwable cause) {
        super("LLM_SERVICE_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
        initCause(cause);
    }
    public LlmServiceUnavailableException(String message) {
        super("LLM_SERVICE_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Python LLM 서버에 연결할 수 없을 때 던지는(throw) 예외 클래스를 정의합니다. `extends CustomException`으로 기존 HMS 예외 체계를 그대로 이어받고, HTTP 상태 코드 503(서비스 사용 불가)을 응답으로 보냅니다.
> - **왜 이렇게 썼는지**: `CustomException`을 상속하면 `GlobalExceptionHandler`가 이미 이 예외를 처리하도록 되어 있어서 별도 핸들러를 추가할 필요가 없습니다. 생성자를 두 가지로 만든 이유는, 원인 예외(`cause`)를 함께 전달할 때와 메시지만 전달할 때 모두 대응하기 위해서입니다.
> - **쉽게 말하면**: "LLM 서버가 꺼져 있어요"라는 전용 에러 카드를 만들어, 문제 발생 시 이 카드를 꺼내 보여주는 것과 같습니다.

### LlmTimeoutException (504)

```java
/**
 * Python LLM 서버 응답 타임아웃 시 발생하는 예외 (504 Gateway Timeout)
 */
public class LlmTimeoutException extends CustomException {
    public LlmTimeoutException(String message, Throwable cause) {
        super("LLM_TIMEOUT", message, HttpStatus.GATEWAY_TIMEOUT);
        initCause(cause);
    }
    public LlmTimeoutException(String message) {
        super("LLM_TIMEOUT", message, HttpStatus.GATEWAY_TIMEOUT);
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Python LLM 서버가 응답하는 데 너무 오래 걸릴 때 던지는 예외 클래스를 정의합니다. HTTP 상태 코드 504(게이트웨이 시간 초과)를 응답으로 보냅니다.
> - **왜 이렇게 썼는지**: 서버가 꺼진 것(503)과 응답이 느린 것(504)을 다른 예외 클래스로 분리하면, 문제의 원인을 더 명확하게 구분할 수 있습니다. `LlmServiceUnavailableException`과 구조가 동일하지만 에러 코드와 HTTP 상태가 다릅니다.
> - **쉽게 말하면**: "LLM 서버가 너무 오래 걸려요"라는 전용 에러 카드로, 연결 실패와 시간 초과를 구별해서 사용자에게 다른 메시지를 줄 수 있습니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 빌드 | ./gradlew build | BUILD SUCCESSFUL |
| import 확인 | 다른 클래스에서 import | 정상 인식 |

---

## 완료 기준

- [x] `./gradlew build` 오류 없음
- [x] `LlmServiceUnavailableException`, `LlmTimeoutException` 정상 import 가능
