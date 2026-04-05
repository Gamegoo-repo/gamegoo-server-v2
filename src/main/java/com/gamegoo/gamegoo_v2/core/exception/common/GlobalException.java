package com.gamegoo.gamegoo_v2.core.exception.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GlobalException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String message;

    public GlobalException(ErrorCode errorCode) {
        this.status = errorCode.getStatus();
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public GlobalException(ErrorCode errorCode, String customMessage) {
        this.status = errorCode.getStatus();
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage() + ": " + customMessage;
    }
}
