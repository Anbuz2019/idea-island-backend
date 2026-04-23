package com.anbuz.domain.topic.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 主题自动失效规则实体，负责表达资料在主题内自动归档或失效的触发条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicAutoInvalidRule {

    private Long id;
    private Long topicId;
    private String ruleType;
    private Integer thresholdDays;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
