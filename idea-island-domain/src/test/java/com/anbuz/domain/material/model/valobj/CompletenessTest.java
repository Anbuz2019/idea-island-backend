package com.anbuz.domain.material.model.valobj;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Completeness 完整度值对象")
class CompletenessTest {

    @Test
    @DisplayName("无标签、无评语、无评分 → 仅入库")
    void givenNoTagsNoCommentNoScore_whenOf_thenInboxOnly() {
        assertThat(Completeness.of(Collections.emptyList(), null, null))
                .isEqualTo(Completeness.INBOX_ONLY);
    }

    @Test
    @DisplayName("有标签、有评语、有评分 → 完整")
    void givenAllPresent_whenOf_thenComplete() {
        assertThat(Completeness.of(List.of("需求分析"), "很好的文章", new BigDecimal("8.0")))
                .isEqualTo(Completeness.COMPLETE);
    }

    @Test
    @DisplayName("无标签、有评语 → 缺标签")
    void givenNoTagsButHasComment_whenOf_thenMissingTags() {
        assertThat(Completeness.of(Collections.emptyList(), "有评语", new BigDecimal("7.0")))
                .isEqualTo(Completeness.MISSING_TAGS);
    }

    @Test
    @DisplayName("有标签、无评分 → 缺评价")
    void givenHasTagsButMissingScore_whenOf_thenMissingReview() {
        assertThat(Completeness.of(List.of("方案设计"), "有评语", null))
                .isEqualTo(Completeness.MISSING_REVIEW);
    }

    @Test
    @DisplayName("有标签、无评语 → 缺评价")
    void givenHasTagsButMissingComment_whenOf_thenMissingReview() {
        assertThat(Completeness.of(List.of("方案设计"), null, new BigDecimal("5.0")))
                .isEqualTo(Completeness.MISSING_REVIEW);
    }

}
