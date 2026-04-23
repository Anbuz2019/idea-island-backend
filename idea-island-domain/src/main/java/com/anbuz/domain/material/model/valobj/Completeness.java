package com.anbuz.domain.material.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 完整度值对象，负责把资料补充信息映射为系统完整度标签。
 */
@Getter
@AllArgsConstructor
public enum Completeness {

    COMPLETE("完整"),
    MISSING_TAGS("缺标签"),
    MISSING_REVIEW("缺评价"),
    INBOX_ONLY("仅入库");

    private final String label;

    /**
     * 根据资料的标签、评语、评分状态判断完整度。
     *
     * 规则（优先级从高到低）：
     * 1. 无用户标签 → MISSING_TAGS（无论评语评分是否存在）
     * 2. 有用户标签，但缺评语或缺评分 → MISSING_REVIEW
     * 3. 有用户标签 + 有评语 + 有评分 → COMPLETE
     * 特殊：无标签 + 无评语 + 无评分 → INBOX_ONLY（和 MISSING_TAGS 合并至无标签时优先判 MISSING_TAGS，
     *       但为了展示语义更准确，当三者都无时返回 INBOX_ONLY）
     */
    public static Completeness of(List<String> userTagValues, String comment, Object score) {
        boolean hasUserTags = userTagValues != null && !userTagValues.isEmpty();
        boolean hasComment = comment != null && !comment.isBlank();
        boolean hasScore = score != null;

        if (!hasUserTags && !hasComment && !hasScore) return INBOX_ONLY;
        if (!hasUserTags) return MISSING_TAGS;
        if (!hasComment || !hasScore) return MISSING_REVIEW;
        return COMPLETE;
    }

}
