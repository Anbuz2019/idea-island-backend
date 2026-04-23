package com.anbuz.domain.topic.service.impl;

import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.domain.topic.service.ITagSetService;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签集领域服务，负责维护主题内用户自定义标签组和值。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagSetService implements ITagSetService {

    private static final int MAX_TAG_GROUPS = 20;
    private static final int MAX_TAG_VALUES = 50;

    private final ITopicRepository topicRepository;

    @Override
    @Transactional
    public UserTagGroup createTagGroup(Long userId, Long topicId, String name, boolean exclusive,
                                       boolean required, int sortOrder) {
        assertOwnedTopic(topicId, userId);
        if (topicRepository.existsTagGroupByTopicIdAndName(topicId, name)) {
            log.warn("Create tag group rejected due to duplicate name userId={} topicId={} name={}", userId, topicId, name);
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签组名称已存在");
        }
        if (topicRepository.countTagGroupsByTopicId(topicId) >= MAX_TAG_GROUPS) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "每个主题最多创建 " + MAX_TAG_GROUPS + " 个标签组");
        }
        LocalDateTime now = LocalDateTime.now();
        UserTagGroup group = UserTagGroup.builder()
                .topicId(topicId)
                .name(name)
                .exclusive(exclusive)
                .required(required)
                .sortOrder(sortOrder)
                .createdAt(now)
                .updatedAt(now)
                .build();
        topicRepository.saveTagGroup(group);
        log.info("Create tag group succeeded userId={} topicId={} groupId={} name={}", userId, topicId, group.getId(), name);
        return group;
    }

    @Override
    @Transactional
    public UserTagGroup updateTagGroup(Long userId, Long groupId, String name, String color,
                                       Boolean exclusive, Boolean required, Integer sortOrder) {
        UserTagGroup group = getOwnedGroup(groupId, userId);
        if (name != null && !name.equals(group.getName())) {
            if (topicRepository.existsTagGroupByTopicIdAndName(group.getTopicId(), name)) {
                log.warn("Update tag group rejected due to duplicate name userId={} groupId={} name={}", userId, groupId, name);
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签组名称已存在");
            }
            group.setName(name);
        }
        if (color != null) {
            group.setColor(color);
        }
        if (exclusive != null && !exclusive.equals(group.getExclusive()) && Boolean.TRUE.equals(exclusive)
                && topicRepository.existsMultiValueUsageInGroup(groupId)) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "该标签组已有资料使用多个标签值，无法切换为互斥");
        }
        if (exclusive != null) {
            group.setExclusive(exclusive);
        }
        if (required != null) {
            group.setRequired(required);
        }
        if (sortOrder != null) {
            group.setSortOrder(sortOrder);
        }
        group.setUpdatedAt(LocalDateTime.now());
        topicRepository.updateTagGroup(group);
        log.info("Update tag group succeeded userId={} groupId={} topicId={}", userId, groupId, group.getTopicId());
        return group;
    }

    @Override
    @Transactional
    public void deleteTagGroup(Long userId, Long groupId) {
        getOwnedGroup(groupId, userId);
        if (topicRepository.countMaterialReferencesByGroupId(groupId) > 0) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签组已被资料引用，无法删除");
        }
        topicRepository.deleteTagValuesByGroupId(groupId);
        topicRepository.deleteTagGroup(groupId);
        log.info("Delete tag group succeeded userId={} groupId={}", userId, groupId);
    }

    @Override
    @Transactional
    public UserTagValue addTagValue(Long userId, Long groupId, String value, String color, int sortOrder) {
        getOwnedGroup(groupId, userId);
        if (topicRepository.existsTagValueByGroupIdAndValue(groupId, value)) {
            log.warn("Add tag value rejected due to duplicate value userId={} groupId={} value={}", userId, groupId, value);
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签值已存在: " + value);
        }
        if (topicRepository.countTagValuesByGroupId(groupId) >= MAX_TAG_VALUES) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "每个标签组最多 " + MAX_TAG_VALUES + " 个标签值");
        }
        UserTagValue tagValue = UserTagValue.builder()
                .groupId(groupId)
                .value(value)
                .color(color)
                .sortOrder(sortOrder)
                .createdAt(LocalDateTime.now())
                .build();
        topicRepository.saveTagValue(tagValue);
        log.info("Add tag value succeeded userId={} groupId={} valueId={} value={}", userId, groupId, tagValue.getId(), value);
        return tagValue;
    }

    @Override
    @Transactional
    public UserTagValue updateTagValue(Long userId, Long valueId, String value, String color, Integer sortOrder) {
        UserTagValue tagValue = getOwnedValue(valueId, userId);
        if (value != null && !value.equals(tagValue.getValue())) {
            if (topicRepository.existsTagValueByGroupIdAndValue(tagValue.getGroupId(), value)) {
                log.warn("Update tag value rejected due to duplicate value userId={} valueId={} value={}", userId, valueId, value);
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签值已存在: " + value);
            }
            topicRepository.updateMaterialTagValue(tagValue.getGroupId(), tagValue.getValue(), value);
            tagValue.setValue(value);
        }
        if (color != null) {
            tagValue.setColor(color);
        }
        if (sortOrder != null) {
            tagValue.setSortOrder(sortOrder);
        }
        topicRepository.updateTagValue(tagValue);
        log.info("Update tag value succeeded userId={} valueId={} groupId={}", userId, valueId, tagValue.getGroupId());
        return tagValue;
    }

    @Override
    @Transactional
    public void deleteTagValue(Long userId, Long valueId) {
        UserTagValue tagValue = getOwnedValue(valueId, userId);
        if (topicRepository.countMaterialReferencesByValue(tagValue.getGroupId(), tagValue.getValue()) > 0) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签值已被资料引用，无法删除");
        }
        topicRepository.deleteTagValue(valueId);
        log.info("Delete tag value succeeded userId={} valueId={} groupId={}", userId, valueId, tagValue.getGroupId());
    }

    @Override
    public List<UserTagGroup> listTagGroups(Long userId, Long topicId) {
        assertOwnedTopic(topicId, userId);
        return topicRepository.findTagGroupsByTopicId(topicId);
    }

    @Override
    public List<UserTagValue> listTagValues(Long userId, Long groupId) {
        getOwnedGroup(groupId, userId);
        return topicRepository.findTagValuesByGroupId(groupId);
    }

    private void assertOwnedTopic(Long topicId, Long userId) {
        var topic = topicRepository.findTopicById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "主题不存在"));
        if (!userId.equals(topic.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private UserTagGroup getOwnedGroup(Long groupId, Long userId) {
        UserTagGroup group = topicRepository.findTagGroupById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "标签组不存在"));
        assertOwnedTopic(group.getTopicId(), userId);
        return group;
    }

    private UserTagValue getOwnedValue(Long valueId, Long userId) {
        UserTagValue value = topicRepository.findTagValueById(valueId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "标签值不存在"));
        UserTagGroup group = getOwnedGroup(value.getGroupId(), userId);
        value.setGroupId(group.getId());
        return value;
    }

}
