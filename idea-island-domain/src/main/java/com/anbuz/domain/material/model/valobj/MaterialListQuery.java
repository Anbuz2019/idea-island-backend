package com.anbuz.domain.material.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资料列表查询对象，负责承载资料列表和搜索入口的领域查询条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialListQuery {

    private Long userId;
    private Long topicId;
    private List<String> statuses;
    private List<String> materialTypes;
    private BigDecimal scoreMin;
    private BigDecimal scoreMax;
    private LocalDateTime createdStart;
    private LocalDateTime createdEnd;
    private String keyword;
    @Builder.Default
    private boolean includeComment = false;
    @Builder.Default
    private String sortBy = "createdAt";
    @Builder.Default
    private String sortDirection = "DESC";
    @Builder.Default
    private int page = 1;
    @Builder.Default
    private int pageSize = 20;
    private List<TagFilter> tagFilters;
    @Builder.Default
    private boolean unreadOnly = false;

    public int getOffset() {
        return Math.max(page - 1, 0) * Math.max(pageSize, 1);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagFilter {
        private String tagGroupKey;
        private List<String> tagValues;
    }

}
