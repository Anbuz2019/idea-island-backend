package com.anbuz.domain.topic.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户标签值实体，负责表达标签组下可选标签值的配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTagValue {

    private Long id;
    private Long groupId;
    private String value;
    private String color;
    private Integer sortOrder;
    private LocalDateTime createdAt;

}
