package com.anbuz.domain.material.repository;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.TagType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IMaterialRepository {

    Long saveMaterial(Material material);

    Optional<Material> findById(Long id);

    void updateMaterial(Material material);

    void saveMeta(MaterialMeta meta);

    Optional<MaterialMeta> findMetaByMaterialId(Long materialId);

    void updateMeta(MaterialMeta meta);

    void deleteUserTags(Long materialId);

    void saveTags(List<MaterialTag> tags);

    List<MaterialTag> findTagsByMaterialId(Long materialId);

    List<MaterialTag> findTagsByMaterialIdAndType(Long materialId, TagType tagType);

    void deleteTagByMaterialIdAndGroupKey(Long materialId, String tagGroupKey);

    /** 查询某主题下，某状态超过指定时间未操作的资料（用于自动失效） */
    List<Material> findByTopicIdAndStatusAndInboxAtBefore(Long topicId, MaterialStatus status, LocalDateTime threshold);

    List<Material> findByTopicIdAndStatusAndUpdatedAtBefore(Long topicId, MaterialStatus status, LocalDateTime threshold);

}
