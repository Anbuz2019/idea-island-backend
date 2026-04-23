package com.anbuz.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自动失效规则类型，负责定义主题规则按时间或周期触发的口径。
 */
@Getter
@AllArgsConstructor
public enum AutoInvalidRuleType {

    INBOX_TIMEOUT("INBOX_TIMEOUT", MaterialStatus.INBOX),
    PENDING_REVIEW_TIMEOUT("PENDING_REVIEW_TIMEOUT", MaterialStatus.PENDING_REVIEW);

    private final String code;
    private final MaterialStatus targetStatus;

    public static AutoInvalidRuleType of(String code) {
        for (AutoInvalidRuleType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown auto invalid rule type: " + code);
    }

}
