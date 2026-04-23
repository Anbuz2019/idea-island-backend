package com.anbuz.domain.user.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 用户统计值对象，负责承载用户维度的主题数量和资料数量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {

    private long topicCount;
    private long materialCount;
    private Map<String, Long> statusCounts;

}
