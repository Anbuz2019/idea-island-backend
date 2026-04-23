package com.anbuz.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 标签类型枚举，负责区分用户手动标签和系统生成标签。
 */
@Getter
@AllArgsConstructor
public enum TagType {

    SYSTEM("system"),
    USER("user");

    private final String code;

}
