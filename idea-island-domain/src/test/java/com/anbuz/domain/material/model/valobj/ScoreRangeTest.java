package com.anbuz.domain.material.model.valobj;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScoreRange 评分区间值对象")
class ScoreRangeTest {

    @Test
    @DisplayName("评分为 null → 无分")
    void givenNullScore_whenOf_thenNone() {
        assertThat(ScoreRange.of(null)).isEqualTo(ScoreRange.NONE);
    }

    @Test
    @DisplayName("评分 0.0 → 低")
    void givenZeroScore_whenOf_thenLow() {
        assertThat(ScoreRange.of(BigDecimal.ZERO)).isEqualTo(ScoreRange.LOW);
    }

    @Test
    @DisplayName("评分 3.9 → 低")
    void givenScore39_whenOf_thenLow() {
        assertThat(ScoreRange.of(new BigDecimal("3.9"))).isEqualTo(ScoreRange.LOW);
    }

    @Test
    @DisplayName("评分 4.0 → 中")
    void givenScore40_whenOf_thenMedium() {
        assertThat(ScoreRange.of(new BigDecimal("4.0"))).isEqualTo(ScoreRange.MEDIUM);
    }

    @Test
    @DisplayName("评分 6.9 → 中")
    void givenScore69_whenOf_thenMedium() {
        assertThat(ScoreRange.of(new BigDecimal("6.9"))).isEqualTo(ScoreRange.MEDIUM);
    }

    @Test
    @DisplayName("评分 7.0 → 高")
    void givenScore70_whenOf_thenHigh() {
        assertThat(ScoreRange.of(new BigDecimal("7.0"))).isEqualTo(ScoreRange.HIGH);
    }

    @Test
    @DisplayName("评分 10.0 → 高")
    void givenScore100_whenOf_thenHigh() {
        assertThat(ScoreRange.of(new BigDecimal("10.0"))).isEqualTo(ScoreRange.HIGH);
    }

}
