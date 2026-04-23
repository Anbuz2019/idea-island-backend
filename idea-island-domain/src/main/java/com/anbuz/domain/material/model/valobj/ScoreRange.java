package com.anbuz.domain.material.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 评分区间值对象，负责把用户评分映射为系统评分区间标签。
 */
@Getter
@AllArgsConstructor
public enum ScoreRange {

    NONE("无分", null, null),
    LOW("低", BigDecimal.ZERO, new BigDecimal("4.0")),
    MEDIUM("中", new BigDecimal("4.0"), new BigDecimal("7.0")),
    HIGH("高", new BigDecimal("7.0"), new BigDecimal("10.0"));

    private final String label;
    private final BigDecimal min;
    private final BigDecimal max;

    public static ScoreRange of(BigDecimal score) {
        if (score == null) return NONE;
        if (score.compareTo(new BigDecimal("4.0")) < 0) return LOW;
        if (score.compareTo(new BigDecimal("7.0")) < 0) return MEDIUM;
        return HIGH;
    }

}
