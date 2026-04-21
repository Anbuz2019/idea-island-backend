package com.anbuz.infrastructure.persistent.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTagGroupPO {

    private Long id;
    private Long topicId;
    private String name;
    private String color;
    private Boolean isExclusive;
    private Boolean isRequired;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
