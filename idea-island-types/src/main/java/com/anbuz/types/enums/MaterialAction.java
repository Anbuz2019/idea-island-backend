package com.anbuz.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资料动作枚举，负责定义驱动资料状态流转的用户或系统动作。
 */
@Getter
@AllArgsConstructor
public enum MaterialAction {

    MARK_READ("标记已读"),
    COLLECT("完成评价"),
    ARCHIVE("主动归档"),
    INVALIDATE("标记失效"),
    RESTORE("恢复至收件箱"),
    RESTORE_COLLECTED("恢复收录");

    private final String desc;

}
