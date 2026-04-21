package com.anbuz.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MaterialType {

    ARTICLE("article", "文章"),
    SOCIAL("social", "社交内容"),
    MEDIA("media", "视频/音频"),
    IMAGE("image", "图片截图"),
    EXCERPT("excerpt", "摘录片段"),
    INPUT("input", "主动输入");

    private final String code;
    private final String desc;

    public static MaterialType of(String code) {
        for (MaterialType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown material type: " + code);
    }

}
