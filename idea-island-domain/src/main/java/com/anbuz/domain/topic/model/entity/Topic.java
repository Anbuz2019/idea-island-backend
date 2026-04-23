package com.anbuz.domain.topic.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 主题实体，负责表达用户资料集合的配置、状态和默认管理规则。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Integer status;
    private Integer materialCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }

}
