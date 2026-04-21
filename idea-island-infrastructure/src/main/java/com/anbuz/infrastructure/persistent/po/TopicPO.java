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
public class TopicPO {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Integer status;
    private Integer materialCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
