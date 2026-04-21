package com.anbuz.domain.material.model.entity;

import com.anbuz.types.enums.TagType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialTag {

    private Long id;
    private Long materialId;
    private TagType tagType;
    private String tagGroupKey;
    private String tagValue;
    private LocalDateTime createdAt;

}
