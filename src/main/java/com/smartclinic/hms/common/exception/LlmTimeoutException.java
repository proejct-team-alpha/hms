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
