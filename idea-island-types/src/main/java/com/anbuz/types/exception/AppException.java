package com.anbuz.types.exception;

import com.anbuz.types.model.ErrorCode;
import lombok.Getter;

/**
 * 应用业务异常，负责在领域和触发层携带统一错误码。
 */
@Getter
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public AppException(ErrorCode errorCode, String detail) {
        super(detail);
        this.code = errorCode.getCode();
    }

    public AppException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

}
