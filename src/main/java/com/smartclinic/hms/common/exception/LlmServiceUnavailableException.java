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
