package com.anbuz.domain.material.service.impl;

import com.anbuz.domain.material.adapter.MaterialEventPublisher;
import com.anbuz.domain.material.model.aggregate.MaterialAggregate;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.model.valobj.MaterialPageResult;
import com.anbuz.domain.material.model.valobj.MaterialStatusRecord;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.material.service.IMaterialService;
import com.anbuz.domain.material.service.IStatusTransitionService;
import com.anbuz.domain.material.service.ISystemTagService;
import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.types.enums.MaterialAction;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import com.anbuz.types.enums.TagType;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资料领域服务，负责执行资料提交、列表查询、状态流转、标签维护和事件发布规则。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialService implements IMaterialService {

    private static final List<String> DEFAULT_VISIBLE_STATUSES = List.of(
            MaterialStatus.INBOX.getCode(),
            MaterialStatus.PENDING_REVIEW.getCode(),
            MaterialStatus.COLLECTED.getCode(),
            MaterialStatus.ARCHIVED.getCode());

    private final IMaterialRepository materialRepository;
    private final ITopicRepository topicRepository;
    private final IStatusTransitionService statusTransitionService;
    private final ISystemTagService systemTagService;
    private final MaterialEventPublisher materialEventPublisher;

    @Override
    @Transactional
    public Long submit(Long userId, SubmitCommand command) {
        Topic topic = getOwnedTopic(command.getTopicId(), userId);
        if (!topic.isEnabled()) {
            log.warn("Submit material rejected due to disabled topic userId={} topicId={}", userId, command.getTopicId());
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "主题已停用，无法提交资料");
        }
        MaterialType materialType = MaterialType.of(command.getMaterialType());
        validateSubmitCommand(materialType, command);

        LocalDateTime now = LocalDateTime.now();
        Material material = Material.builder()
                .userId(userId)
                .topicId(topic.getId())
                .materialType(materialType)
                .status(MaterialStatus.INBOX)
                .title(command.getTitle())
                .description(command.getDescription())
                .rawContent(command.getRawContent())
                .sourceUrl(command.getSourceUrl())
                .fileKey(command.getFileKey())
                .deleted(false)
                .inboxAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        Long materialId = materialRepository.saveMaterial(material);
        materialRepository.saveMeta(MaterialMeta.builder()
                .materialId(materialId)
                .author(command.getAuthor())
                .sourcePlatform(command.getSourcePlatform())
                .publishTime(command.getPublishTime())
                .wordCount(command.getRawContent() == null ? null : countContentCharacters(command.getRawContent()))
                .durationSeconds(command.getDurationSeconds())
                .thumbnailKey(command.getThumbnailKey())
                .createdAt(now)
                .updatedAt(now)
                .build());

        topic.setMaterialCount((topic.getMaterialCount() == null ? 0 : topic.getMaterialCount()) + 1);
        topic.setUpdatedAt(now);
        topicRepository.updateTopic(topic);
        if (command.getTags() != null && !command.getTags().isEmpty()) {
            saveUserTags(material, materialId, command.getTags(), false);
        }
        systemTagService.refreshSystemTags(materialId, material.getScore(), material.getComment());
        publishAfterCommit(() -> materialEventPublisher.publishMaterialSubmitted(materialId));
        log.info("Submit material succeeded userId={} topicId={} materialId={} materialType={}",
                userId, topic.getId(), materialId, materialType.getCode());
        return materialId;
    }

    @Override
    public MaterialPageResult listMaterials(Long userId, MaterialListQuery query) {
        MaterialListQuery normalized = normalizeQuery(userId, query, false);
        List<MaterialAggregate> items = materialRepository.queryMaterials(normalized)
                .stream()
                .map(this::buildAggregate)
                .collect(Collectors.toList());
        long total = materialRepository.countMaterials(normalized);
        return MaterialPageResult.builder()
                .items(items)
                .total(total)
                .page(normalized.getPage())
                .pageSize(normalized.getPageSize())
                .build();
    }

    @Override
    @Transactional
    public MaterialPageResult searchMaterials(Long userId, MaterialListQuery query) {
        MaterialListQuery normalized = normalizeQuery(userId, query, true);
        List<Material> materials = materialRepository.queryMaterials(normalized);
        if (!materials.isEmpty()) {
            materialRepository.updateLastRetrievedAt(materials.stream().map(Material::getId).toList(), LocalDateTime.now());
        }
        return MaterialPageResult.builder()
                .items(materials.stream().map(this::buildAggregate).toList())
                .total(materialRepository.countMaterials(normalized))
                .page(normalized.getPage())
                .pageSize(normalized.getPageSize())
                .build();
    }

    @Override
    public MaterialPageResult inbox(Long userId, Long topicId, int page, int pageSize) {
        if (topicId != null) {
            getOwnedTopic(topicId, userId);
        }
        return listMaterials(userId, MaterialListQuery.builder()
                .topicId(topicId)
                .statuses(List.of(MaterialStatus.INBOX.getCode()))
                .sortBy("createdAt")
                .sortDirection("DESC")
                .page(page)
                .pageSize(pageSize)
                .build());
    }

    @Override
    public MaterialAggregate getDetail(Long userId, Long materialId) {
        return buildAggregate(getOwnedMaterial(materialId, userId));
    }

    @Override
    @Transactional
    public MaterialAggregate updateBasic(Long userId, Long materialId, UpdateBasicCommand command) {
        Material material = getOwnedMaterial(materialId, userId);
        if (material.getStatus() == MaterialStatus.INVALID) {
            log.warn("Update material basic rejected due to invalid status userId={} materialId={}", userId, materialId);
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "已失效资料不可编辑");
        }
        if (command.getTitle() != null) {
            material.setTitle(command.getTitle());
        }
        if (command.getRawContent() != null) {
            material.setRawContent(command.getRawContent());
        }
        if (command.getSourceUrl() != null) {
            material.setSourceUrl(command.getSourceUrl());
        }
        LocalDateTime now = LocalDateTime.now();
        material.setUpdatedAt(now);
        materialRepository.updateMaterial(material);
        if (command.getRawContent() != null) {
            syncWordCount(materialId, command.getRawContent(), now);
        }
        log.info("Update material basic succeeded userId={} materialId={}", userId, materialId);
        return buildAggregate(material);
    }

    @Override
    @Transactional
    public MaterialAggregate updateMeta(Long userId, Long materialId, UpdateMetaCommand command) {
        Material material = getOwnedMaterial(materialId, userId);
        LocalDateTime now = LocalDateTime.now();
        MaterialMeta meta = materialRepository.findMetaByMaterialId(materialId)
                .orElse(MaterialMeta.builder()
                        .materialId(materialId)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
        boolean creating = meta.getId() == null;
        if (command.getAuthor() != null) {
            meta.setAuthor(command.getAuthor());
        }
        if (command.getSourcePlatform() != null) {
            meta.setSourcePlatform(command.getSourcePlatform());
        }
        if (command.getPublishTime() != null) {
            meta.setPublishTime(command.getPublishTime());
        }
        if (command.getWordCount() != null) {
            meta.setWordCount(command.getWordCount());
        }
        if (command.getDurationSeconds() != null) {
            meta.setDurationSeconds(command.getDurationSeconds());
        }
        if (command.getThumbnailKey() != null) {
            meta.setThumbnailKey(command.getThumbnailKey());
        }
        if (command.getExtraJson() != null) {
            meta.setExtraJson(command.getExtraJson());
        }
        meta.setUpdatedAt(now);
        if (creating) {
            materialRepository.saveMeta(meta);
        } else {
            materialRepository.updateMeta(meta);
        }
        log.info("Update material meta succeeded userId={} materialId={} metaCreated={}", userId, materialId, creating);
        return buildAggregate(material);
    }

    @Override
    @Transactional
    public void deleteMaterial(Long userId, Long materialId) {
        Material material = getOwnedMaterial(materialId, userId);
        if (material.getStatus() != MaterialStatus.INVALID) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "仅失效状态资料允许删除");
        }
        LocalDateTime now = LocalDateTime.now();
        material.setDeleted(true);
        material.setDeletedAt(now);
        material.setUpdatedAt(now);
        materialRepository.updateMaterial(material);

        Topic topic = getOwnedTopic(material.getTopicId(), userId);
        int currentCount = topic.getMaterialCount() == null ? 0 : topic.getMaterialCount();
        topic.setMaterialCount(Math.max(currentCount - 1, 0));
        topic.setUpdatedAt(now);
        topicRepository.updateTopic(topic);
        log.info("Delete material succeeded userId={} materialId={} topicId={}", userId, materialId, material.getTopicId());
    }

    @Override
    @Transactional
    public void updateTags(Long userId, Long materialId, List<TagInput> tags) {
        Material material = getOwnedMaterial(materialId, userId);
        saveUserTags(material, materialId, tags, true);
        systemTagService.refreshSystemTags(materialId, material.getScore(), material.getComment());
        log.info("Update material tags succeeded userId={} materialId={} tagCount={}", userId, materialId, tags == null ? 0 : tags.size());
    }

    private void saveUserTags(Material material, Long materialId, List<TagInput> tags, boolean replaceExisting) {
        List<UserTagGroup> groups = topicRepository.findTagGroupsByTopicId(material.getTopicId());
        Map<Long, UserTagGroup> groupMap = groups.stream()
                .collect(Collectors.toMap(UserTagGroup::getId, g -> g));
        Map<Long, Set<String>> allowedValues = new HashMap<>();
        for (UserTagGroup group : groups) {
            allowedValues.put(group.getId(), topicRepository.findTagValuesByGroupId(group.getId())
                    .stream().map(UserTagValue::getValue).collect(Collectors.toSet()));
        }

        List<TagInput> safeTags = tags == null ? List.of() : tags;
        Map<Long, Integer> tagCountByGroup = new HashMap<>();
        Map<String, Set<String>> selectedValuesByGroup = new HashMap<>();
        Set<Long> filledGroups = new HashSet<>();
        List<TagInput> normalizedTags = new ArrayList<>(safeTags.size());
        for (TagInput tag : safeTags) {
            if (tag == null) {
                throw new AppException(ErrorCode.PARAM_INVALID, "tag item 不能为空");
            }
            if (isBlank(tag.getTagValue())) {
                throw new AppException(ErrorCode.PARAM_INVALID, "tag_value 不能为空");
            }

            String storedTagGroupKey = normalizeUserTagGroupKey(tag.getTagGroupKey());
            if (MaterialTag.UNGROUPED_USER_TAG_GROUP_KEY.equals(storedTagGroupKey)) {
                if (!selectedValuesByGroup.computeIfAbsent(storedTagGroupKey, key -> new HashSet<>()).add(tag.getTagValue())) {
                    throw new AppException(ErrorCode.BUSINESS_CONFLICT, "duplicate tag value: " + tag.getTagValue());
                }
                normalizedTags.add(new TagInput(storedTagGroupKey, tag.getTagValue()));
                continue;
            }
            Long groupId;
            try {
                groupId = Long.valueOf(storedTagGroupKey);
            } catch (NumberFormatException e) {
                throw new AppException(ErrorCode.PARAM_INVALID, "tag_group_key 非法: " + tag.getTagGroupKey());
            }
            UserTagGroup group = groupMap.get(groupId);
            if (group == null) {
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签组不存在或不属于该主题: " + tag.getTagGroupKey());
            }
            if (!allowedValues.getOrDefault(groupId, Set.of()).contains(tag.getTagValue())) {
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签值不合法: " + tag.getTagValue());
            }
            if (!selectedValuesByGroup.computeIfAbsent(storedTagGroupKey, key -> new HashSet<>()).add(tag.getTagValue())) {
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "duplicate tag value: " + tag.getTagValue());
            }
            tagCountByGroup.merge(groupId, 1, Integer::sum);
            if (Boolean.TRUE.equals(group.getExclusive()) && tagCountByGroup.get(groupId) > 1) {
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "互斥标签组只允许选择一个值: " + group.getName());
            }
            filledGroups.add(groupId);
            normalizedTags.add(new TagInput(storedTagGroupKey, tag.getTagValue()));
        }

        for (UserTagGroup group : groups) {
            if (Boolean.TRUE.equals(group.getRequired()) && !filledGroups.contains(group.getId())) {
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "必填标签组未填写: " + group.getName());
            }
        }

        if (replaceExisting) {
            materialRepository.deleteUserTags(materialId);
        }
        if (!normalizedTags.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<MaterialTag> materialTags = normalizedTags.stream()
                    .map(tag -> MaterialTag.builder()
                            .materialId(materialId)
                            .tagType(TagType.USER)
                            .tagGroupKey(tag.getTagGroupKey())
                            .tagValue(tag.getTagValue())
                            .createdAt(now)
                            .build())
                    .toList();
            materialRepository.saveTags(materialTags);
        }
    }

    private String normalizeUserTagGroupKey(String tagGroupKey) {
        return isBlank(tagGroupKey) ? MaterialTag.UNGROUPED_USER_TAG_GROUP_KEY : tagGroupKey.trim();
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long materialId) {
        statusTransitionService.transit(materialId, userId, MaterialAction.MARK_READ, null, null, null);
    }

    @Override
    @Transactional
    public void collect(Long userId, Long materialId, String comment, BigDecimal score) {
        Material material = getOwnedMaterial(materialId, userId);
        statusTransitionService.assertRequiredTagsFilled(materialId,
                topicRepository.findRequiredTagGroupIdsByTopicId(material.getTopicId()));
        Material updated = statusTransitionService.transit(materialId, userId, MaterialAction.COLLECT, comment, score, null);
        systemTagService.refreshSystemTags(materialId, updated.getScore(), updated.getComment());
    }

    @Override
    @Transactional
    public void archive(Long userId, Long materialId) {
        statusTransitionService.transit(materialId, userId, MaterialAction.ARCHIVE, null, null, null);
    }

    @Override
    @Transactional
    public void invalidate(Long userId, Long materialId, String invalidReason) {
        statusTransitionService.transit(materialId, userId, MaterialAction.INVALIDATE, null, null, invalidReason);
        materialRepository.deleteTags(materialId);
    }

    @Override
    @Transactional
    public void restore(Long userId, Long materialId) {
        statusTransitionService.transit(materialId, userId, MaterialAction.RESTORE, null, null, null);
    }

    @Override
    @Transactional
    public void restoreCollected(Long userId, Long materialId) {
        statusTransitionService.transit(materialId, userId, MaterialAction.RESTORE_COLLECTED, null, null, null);
    }

    private void publishAfterCommit(Runnable publisher) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.run();
            }
        });
    }

    private void syncWordCount(Long materialId, String rawContent, LocalDateTime now) {
        MaterialMeta meta = materialRepository.findMetaByMaterialId(materialId)
                .orElse(MaterialMeta.builder()
                        .materialId(materialId)
                        .createdAt(now)
                        .build());
        boolean creating = meta.getId() == null;
        meta.setWordCount(countContentCharacters(rawContent));
        meta.setUpdatedAt(now);
        if (creating) {
            materialRepository.saveMeta(meta);
        } else {
            materialRepository.updateMeta(meta);
        }
    }

    private int countContentCharacters(String rawContent) {
        long count = rawContent.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .count();
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private MaterialListQuery normalizeQuery(Long userId, MaterialListQuery query, boolean includeComment) {
        MaterialListQuery normalized = query == null ? new MaterialListQuery() : query;
        normalized.setUserId(userId);
        normalized.setIncludeComment(includeComment || normalized.isIncludeComment());
        if (normalized.getStatuses() == null || normalized.getStatuses().isEmpty()) {
            normalized.setStatuses(DEFAULT_VISIBLE_STATUSES);
        }
        if (normalized.getPage() <= 0) {
            normalized.setPage(1);
        }
        if (normalized.getPageSize() <= 0) {
            normalized.setPageSize(20);
        }
        if (normalized.getPageSize() > 100) {
            normalized.setPageSize(100);
        }
        if (null==normalized.getSortBy() || !List.of("createdAt", "score", "status").contains(normalized.getSortBy())) {
            normalized.setSortBy("createdAt");
        }
        if (!"ASC".equalsIgnoreCase(normalized.getSortDirection())) {
            normalized.setSortDirection("DESC");
        } else {
            normalized.setSortDirection("ASC");
        }
        if (normalized.getTopicId() != null) {
            getOwnedTopic(normalized.getTopicId(), userId);
        }
        return normalized;
    }

    private void validateSubmitCommand(MaterialType materialType, SubmitCommand command) {
        switch (materialType) {
            case ARTICLE -> require(command.getSourceUrl(), "article 类型必须提供 sourceUrl");
            case SOCIAL -> {
                if (isBlank(command.getRawContent()) && isBlank(command.getSourceUrl())) {
                    throw new AppException(ErrorCode.PARAM_INVALID, "social 类型必须提供 rawContent 或 sourceUrl");
                }
            }
            case MEDIA -> {
                if (isBlank(command.getSourceUrl()) && isBlank(command.getFileKey())) {
                    throw new AppException(ErrorCode.PARAM_INVALID, "media 类型必须提供 sourceUrl 或 fileKey");
                }
            }
            case IMAGE -> require(command.getFileKey(), "image 类型必须提供 fileKey");
            case EXCERPT, INPUT -> require(command.getRawContent(), materialType.getCode() + " 类型必须提供 rawContent");
        }
    }

    private void require(String value, String message) {
        if (isBlank(value)) {
            throw new AppException(ErrorCode.PARAM_INVALID, message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private MaterialAggregate buildAggregate(Material material) {
        return MaterialAggregate.builder()
                .material(material)
                .meta(materialRepository.findMetaByMaterialId(material.getId()).orElse(null))
                .tags(materialRepository.findTagsByMaterialId(material.getId()))
                .statusHistory(buildStatusHistory(material))
                .build();
    }

    private List<MaterialStatusRecord> buildStatusHistory(Material material) {
        List<MaterialStatusRecord> history = new ArrayList<>();
        if (material.getInboxAt() != null || material.getCreatedAt() != null) {
            history.add(MaterialStatusRecord.builder()
                    .status(MaterialStatus.INBOX.getCode())
                    .label(MaterialStatus.INBOX.getDesc())
                    .occurredAt(material.getInboxAt() != null ? material.getInboxAt() : material.getCreatedAt())
                    .build());
        }
        if (material.getStatus() == MaterialStatus.PENDING_REVIEW) {
            history.add(MaterialStatusRecord.builder()
                    .status(MaterialStatus.PENDING_REVIEW.getCode())
                    .label(MaterialStatus.PENDING_REVIEW.getDesc())
                    .occurredAt(material.getUpdatedAt())
                    .build());
        }
        if (material.getCollectedAt() != null) {
            history.add(MaterialStatusRecord.builder()
                    .status(MaterialStatus.COLLECTED.getCode())
                    .label(MaterialStatus.COLLECTED.getDesc())
                    .occurredAt(material.getCollectedAt())
                    .build());
        }
        if (material.getArchivedAt() != null) {
            history.add(MaterialStatusRecord.builder()
                    .status(MaterialStatus.ARCHIVED.getCode())
                    .label(MaterialStatus.ARCHIVED.getDesc())
                    .occurredAt(material.getArchivedAt())
                    .build());
        }
        if (material.getInvalidAt() != null) {
            history.add(MaterialStatusRecord.builder()
                    .status(MaterialStatus.INVALID.getCode())
                    .label(MaterialStatus.INVALID.getDesc())
                    .occurredAt(material.getInvalidAt())
                    .build());
        }
        return history.stream()
                .sorted(Comparator.comparing(MaterialStatusRecord::getOccurredAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();
    }

    private Material getOwnedMaterial(Long materialId, Long userId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "资料不存在"));
        if (!userId.equals(material.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        return material;
    }

    private Topic getOwnedTopic(Long topicId, Long userId) {
        Topic topic = topicRepository.findTopicById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "主题不存在"));
        if (!userId.equals(topic.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        return topic;
    }

}
