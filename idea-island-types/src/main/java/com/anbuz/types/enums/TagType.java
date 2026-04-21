package com.anbuz.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TagType {

    SYSTEM("system"),
    USER("user");

    private final String code;

}
