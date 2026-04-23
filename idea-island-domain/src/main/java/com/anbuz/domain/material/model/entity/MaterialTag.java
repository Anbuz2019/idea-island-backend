package com.anbuz.domain.material.model.entity;

import com.anbuz.types.enums.TagType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资料标签实体，负责表达资料与用户标签或系统标签之间的绑定关系。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialTag {

    public static final String UNGROUPED_USER_TAG_GROUP_KEY = "__ungrouped__";

    private Long id;
    private Long materialId;
    private TagType tagType;
    private String tagGroupKey;
    private String tagValue;
    private LocalDateTime createdAt;

    public boolean isUngroupedUserTag() {
        return tagType == TagType.USER && UNGROUPED_USER_TAG_GROUP_KEY.equals(tagGroupKey);
    }

    public String getApiTagGroupKey() {
        return isUngroupedUserTag() ? null : tagGroupKey;
    }

}
