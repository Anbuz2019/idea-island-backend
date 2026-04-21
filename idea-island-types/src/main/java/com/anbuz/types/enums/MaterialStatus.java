package com.anbuz.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MaterialStatus {

    INBOX("INBOX", "收件箱"),
    PENDING_REVIEW("PENDING_REVIEW", "待评价"),
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
