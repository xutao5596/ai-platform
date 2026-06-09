package com.aiplatform.common.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常。
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;
    private final String detail;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.detail = null;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.detail = null;
    }

    public BusinessException(int code, String message, String detail) {
        super(message);
        this.code = code;
        this.detail = detail;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.detail = null;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.detail = detail;
    }
}
