package com.anbuz.domain.material.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资料元信息实体，负责承载作者、平台、封面、字数和扩展字段等补充信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialMeta {

    private Long id;
    private Long materialId;
    private String author;
    private String sourcePlatform;
    private LocalDateTime publishTime;
    private Integer wordCount;
    private Integer durationSeconds;
    private String thumbnailKey;
    private String extraJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
