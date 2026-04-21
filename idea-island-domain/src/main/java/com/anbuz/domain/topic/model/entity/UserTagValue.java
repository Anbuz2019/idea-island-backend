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
public class UserTagValue {

    private Long id;
    private Long groupId;
    private String value;
    private String color;
    private Integer sortOrder;
    private LocalDateTime createdAt;

}
