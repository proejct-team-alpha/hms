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
