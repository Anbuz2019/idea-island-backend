package com.anbuz.domain.topic.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 主题统计值对象，负责承载主题下资料数量和状态分布。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicStats {

    private long totalMaterials;
    private Map<String, Long> statusCounts;
    private Map<String, Long> typeCounts;
    private long weeklyNew;
    private BigDecimal averageScore;
    private long pendingCount;

}
