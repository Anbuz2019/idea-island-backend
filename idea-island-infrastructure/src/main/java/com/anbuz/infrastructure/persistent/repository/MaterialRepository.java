package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.infrastructure.persistent.dao.IMaterialDao;
import com.anbuz.infrastructure.persistent.dao.IMaterialMetaDao;
import com.anbuz.infrastructure.persistent.dao.IMaterialTagDao;
import com.anbuz.infrastructure.persistent.po.MaterialMetaPO;
import com.anbuz.infrastructure.persistent.po.MaterialPO;
import com.anbuz.infrastructure.persistent.po.MaterialTagPO;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import com.anbuz.types.enums.TagType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资料仓储实现，负责把资料聚合读写转换为 MyBatis 持久化操作。
 */
@Repository
@RequiredArgsConstructor
public class MaterialRepository implements IMaterialRepository {

    private final IMaterialDao materialDao;
    private final IMaterialMetaDao materialMetaDao;
    private final IMaterialTagDao materialTagDao;

    @Override
    public Long saveMaterial(Material material) {
        MaterialPO po = toPO(material);
        materialDao.insert(po);
        return po.getId();
    }

    @Override
    public Optional<Material> findById(Long id) {
        return materialDao.selectById(id).map(this::toDomain);
    }

    @Override
    public void updateMaterial(Material material) {
        materialDao.update(toPO(material));
    }

    @Override
    public void clearInvalidation(Long materialId, LocalDateTime updatedAt) {
        materialDao.clearInvalidation(materialId, updatedAt);
    }

    @Override
    public void clearArchivedAt(Long materialId, LocalDateTime updatedAt) {
        materialDao.clearArchivedAt(materialId, updatedAt);
    }

    @Override
    public void saveMeta(MaterialMeta meta) {
        materialMetaDao.insert(toMetaPO(meta));
    }

    @Override
    public Optional<MaterialMeta> findMetaByMaterialId(Long materialId) {
        return materialMetaDao.selectByMaterialId(materialId).map(this::toMetaDomain);
    }

    @Override
    public void updateMeta(MaterialMeta meta) {
        materialMetaDao.update(toMetaPO(meta));
    }

    @Override
    public void deleteUserTags(Long materialId) {
        materialTagDao.deleteByMaterialIdAndTagType(materialId, TagType.USER.getCode());
    }

    @Override
    public void deleteTags(Long materialId) {
        materialTagDao.deleteByMaterialId(materialId);
    }

    @Override
    public void saveTags(List<MaterialTag> tags) {
        if (tags == null || tags.isEmpty()) return;
        try {
            materialTagDao.insertBatch(tags.stream().map(this::toTagPO).collect(Collectors.toList()));
        } catch (DuplicateKeyException e) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "duplicate material tags");
        }
    }

    @Override
    public List<MaterialTag> findTagsByMaterialId(Long materialId) {
        return materialTagDao.selectByMaterialId(materialId)
                .stream().map(this::toTagDomain).collect(Collectors.toList());
    }

    @Override
    public List<MaterialTag> findTagsByMaterialIdAndType(Long materialId, TagType tagType) {
        return materialTagDao.selectByMaterialIdAndTagType(materialId, tagType.getCode())
                .stream().map(this::toTagDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteTagByMaterialIdAndGroupKey(Long materialId, String tagGroupKey) {
        materialTagDao.deleteByMaterialIdAndGroupKey(materialId, tagGroupKey);
    }

    @Override
    public List<Material> queryMaterials(MaterialListQuery query) {
        return materialDao.selectByQuery(query).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countMaterials(MaterialListQuery query) {
        return materialDao.countByQuery(query);
    }

    @Override
    public Map<String, Long> countByStatus(Long userId, Long topicId) {
        Map<String, Long> result = new LinkedHashMap<>();
        materialDao.countByStatus(userId, topicId).forEach(item -> result.put(item.getName(), item.getCount()));
        return result;
    }

    @Override
    public Map<String, Long> countByMaterialType(Long topicId) {
        Map<String, Long> result = new LinkedHashMap<>();
        materialDao.countByMaterialType(topicId).forEach(item -> result.put(item.getName(), item.getCount()));
        return result;
    }

    @Override
    public BigDecimal averageScoreByTopicId(Long topicId) {
        return materialDao.averageScoreByTopicId(topicId);
    }

    @Override
    public void updateLastRetrievedAt(List<Long> materialIds, LocalDateTime retrievedAt) {
        if (materialIds == null || materialIds.isEmpty()) {
            return;
        }
        materialDao.updateLastRetrievedAt(materialIds, retrievedAt);
    }

    @Override
    public List<Material> findByTopicIdAndStatusAndInboxAtBefore(
            Long topicId, MaterialStatus status, LocalDateTime threshold) {
        return materialDao.selectByStatusAndInboxAtBefore(topicId, status.getCode(), threshold)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Material> findByTopicIdAndStatusAndUpdatedAtBefore(
            Long topicId, MaterialStatus status, LocalDateTime threshold) {
        return materialDao.selectByStatusAndUpdatedAtBefore(topicId, status.getCode(), threshold)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    private MaterialPO toPO(Material m) {
        return MaterialPO.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .topicId(m.getTopicId())
                .materialType(m.getMaterialType() != null ? m.getMaterialType().getCode() : null)
                .status(m.getStatus() != null ? m.getStatus().getCode() : null)
                .title(m.getTitle())
                .description(m.getDescription())
                .rawContent(m.getRawContent())
                .sourceUrl(m.getSourceUrl())
                .fileKey(m.getFileKey())
                .comment(m.getComment())
                .score(m.getScore())
                .invalidReason(m.getInvalidReason())
                .inboxAt(m.getInboxAt())
                .collectedAt(m.getCollectedAt())
                .archivedAt(m.getArchivedAt())
                .invalidAt(m.getInvalidAt())
                .lastRetrievedAt(m.getLastRetrievedAt())
                .isDeleted(m.getDeleted())
                .deletedAt(m.getDeletedAt())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private Material toDomain(MaterialPO po) {
        return Material.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .topicId(po.getTopicId())
                .materialType(po.getMaterialType() != null ? MaterialType.of(po.getMaterialType()) : null)
                .status(po.getStatus() != null ? MaterialStatus.of(po.getStatus()) : null)
                .title(po.getTitle())
                .description(po.getDescription())
                .rawContent(po.getRawContent())
                .sourceUrl(po.getSourceUrl())
                .fileKey(po.getFileKey())
                .comment(po.getComment())
                .score(po.getScore())
                .invalidReason(po.getInvalidReason())
                .inboxAt(po.getInboxAt())
                .collectedAt(po.getCollectedAt())
                .archivedAt(po.getArchivedAt())
                .invalidAt(po.getInvalidAt())
                .lastRetrievedAt(po.getLastRetrievedAt())
                .deleted(po.getIsDeleted())
                .deletedAt(po.getDeletedAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private MaterialMetaPO toMetaPO(MaterialMeta m) {
        return MaterialMetaPO.builder()
                .id(m.getId())
                .materialId(m.getMaterialId())
                .author(m.getAuthor())
                .sourcePlatform(m.getSourcePlatform())
                .publishTime(m.getPublishTime())
                .wordCount(m.getWordCount())
                .durationSeconds(m.getDurationSeconds())
                .thumbnailKey(m.getThumbnailKey())
                .extraJson(m.getExtraJson())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private MaterialMeta toMetaDomain(MaterialMetaPO po) {
        return MaterialMeta.builder()
                .id(po.getId())
                .materialId(po.getMaterialId())
                .author(po.getAuthor())
                .sourcePlatform(po.getSourcePlatform())
                .publishTime(po.getPublishTime())
                .wordCount(po.getWordCount())
                .durationSeconds(po.getDurationSeconds())
                .thumbnailKey(po.getThumbnailKey())
                .extraJson(po.getExtraJson())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private MaterialTagPO toTagPO(MaterialTag t) {
        return MaterialTagPO.builder()
                .id(t.getId())
                .materialId(t.getMaterialId())
                .tagType(t.getTagType() != null ? t.getTagType().getCode() : null)
                .tagGroupKey(t.getTagGroupKey())
                .tagValue(t.getTagValue())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private MaterialTag toTagDomain(MaterialTagPO po) {
        return MaterialTag.builder()
                .id(po.getId())
                .materialId(po.getMaterialId())
                .tagType(po.getTagType() != null ? TagType.valueOf(po.getTagType().toUpperCase()) : null)
                .tagGroupKey(po.getTagGroupKey())
                .tagValue(po.getTagValue())
                .createdAt(po.getCreatedAt())
                .build();
    }

}
