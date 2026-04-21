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
public class MaterialTagPO {

    private Long id;
    private Long materialId;
    private String tagType;
    private String tagGroupKey;
    private String tagValue;
    private LocalDateTime createdAt;

}
