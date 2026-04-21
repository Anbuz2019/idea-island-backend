package com.anbuz.domain.topic.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTagGroup {

    private Long id;
    private Long topicId;
    private String name;
    private String color;
    private Boolean exclusive;
    private Boolean required;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
