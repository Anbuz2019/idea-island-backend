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
