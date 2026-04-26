package com.anbuz.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资料状态枚举，负责定义资料从收件箱到归档、删除等生命周期状态。
 */
@Getter
@AllArgsConstructor
public enum MaterialStatus {

    INBOX("INBOX", "待阅读"),
    PENDING_REVIEW("PENDING_REVIEW", "待总结"),
    COLLECTED("COLLECTED", "已收录"),
    ARCHIVED("ARCHIVED", "已归档"),
    INVALID("INVALID", "已失效");

    private final String code;
    private final String desc;

    public static MaterialStatus of(String code) {
        for (MaterialStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown material status: " + code);
    }

}
