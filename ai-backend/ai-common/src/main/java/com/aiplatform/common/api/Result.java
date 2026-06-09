package com.aiplatform.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一 API 响应包装。
 *
 * @param <T> 数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 200;
    public static final int CODE_FAIL = 500;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_BAD_REQUEST = 400;

    private int code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(CODE_SUCCESS, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(CODE_SUCCESS, message, data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(CODE_FAIL, message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(CODE_UNAUTHORIZED, message, null);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(CODE_FORBIDDEN, message, null);
    }

    public boolean isSuccess() {
        return this.code == CODE_SUCCESS;
    }
}
