# W4-3 LLM 예외 클래스 이식

## 작업 목표
`LlmServiceUnavailableException`, `LlmTimeoutException`을 HMS `common/exception/`에 이식한다.
`CustomException`을 상속하는 방식으로 구현하며, `GlobalExceptionHandler`는 이미 `CustomException`을
포괄적으로 처리하므로 별도 핸들러 추가는 불필요하다.

## 작업 목록
<!-- TODO 1. LlmServiceUnavailableException — CustomException 상속으로 신규 작성 (503) -->
<!-- TODO 2. LlmTimeoutException — CustomException 상속으로 신규 작성 (504) -->
<!-- TODO 3. 빌드 확인 -->

## 진행 현황
- [x] 1. LlmServiceUnavailableException 작성
- [x] 2. LlmTimeoutException 작성
- [x] 3. 빌드 확인 — BUILD SUCCESSFUL

## 수정/추가 파일
- `src/main/java/com/smartclinic/hms/common/exception/LlmServiceUnavailableException.java` (신규)
- `src/main/java/com/smartclinic/hms/common/exception/LlmTimeoutException.java` (신규)

## 상세 내용

### 사전 분석
- `CustomException`에 이미 `LLM_SERVICE_UNAVAILABLE (503)` 에러코드 정의됨
- `GlobalExceptionHandler`가 `CustomException` 및 **하위 클래스 모두 포괄** (`handleBusiness` 핸들러)
- → LLM 예외가 `CustomException` 상속 시 `GlobalExceptionHandler` 수정 불필요

### 1. LlmServiceUnavailableException (503)
```java
package com.smartclinic.hms.common.exception;

import org.springframework.http.HttpStatus;

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

### 2. LlmTimeoutException (504)
```java
package com.smartclinic.hms.common.exception;

import org.springframework.http.HttpStatus;

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

## 수용 기준
- [ ] `./gradlew build` 오류 없음
- [ ] `LlmServiceUnavailableException`, `LlmTimeoutException` 정상 import 가능
