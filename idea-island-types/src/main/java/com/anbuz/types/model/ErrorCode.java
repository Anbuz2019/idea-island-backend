package com.anbuz.types.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_INVALID(1001, "参数校验失败"),
    NOT_FOUND(1002, "资源不存在"),
    FORBIDDEN(1003, "无权限"),
    INVALID_STATUS_TRANSITION(1004, "状态流转不合法"),
    BUSINESS_CONFLICT(1005, "业务约束冲突"),
    INTERNAL_ERROR(5000, "系统内部错误");

    private final int code;
    private final String message;

}
