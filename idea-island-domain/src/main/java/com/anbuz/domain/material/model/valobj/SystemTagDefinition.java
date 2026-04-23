package com.anbuz.domain.material.model.valobj;

import java.util.Arrays;
import java.util.List;

/**
 * 系统标签定义，负责统一系统标签组和值的展示与刷新口径。
 */
public record SystemTagDefinition(String groupKey, String groupName, boolean exclusive,
                                  boolean required, int sortOrder, List<String> values) {

    public static final String SYS_SCORE_RANGE = "sys_score_range";
    public static final String SYS_COMPLETENESS = "sys_completeness";

    private static final List<SystemTagDefinition> DEFINITIONS = List.of(
            new SystemTagDefinition(SYS_SCORE_RANGE, "评分区间", true, false, -200,
                    Arrays.stream(ScoreRange.values()).map(ScoreRange::getLabel).toList()),
            new SystemTagDefinition(SYS_COMPLETENESS, "完整度", true, false, -100,
                    Arrays.stream(Completeness.values()).map(Completeness::getLabel).toList())
    );

    public static List<SystemTagDefinition> definitions() {
        return DEFINITIONS;
    }
}
