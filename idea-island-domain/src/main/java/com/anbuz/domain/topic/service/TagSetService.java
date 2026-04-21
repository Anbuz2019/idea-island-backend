package com.anbuz.domain.topic.service;

import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagSetService {

    private static final int MAX_TAG_GROUPS = 20;
    private static final int MAX_TAG_VALUES = 50;

    private final ITopicRepository topicRepository;

    public UserTagGroup createTagGroup(Long topicId, String name, boolean exclusive,
                                       boolean required, int sortOrder) {
        if (topicRepository.existsTagGroupByTopicIdAndName(topicId, name)) {
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
        return group;
    }

    public UserTagGroup updateTagGroup(Long groupId, String name, String color,
                                       Boolean exclusive, Boolean required, Integer sortOrder) {
        UserTagGroup group = topicRepository.findTagGroupById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "标签组不存在"));
        if (name != null && !name.equals(group.getName())) {
            if (topicRepository.existsTagGroupByTopicIdAndName(group.getTopicId(), name)) {
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签组名称已存在");
            }
            group.setName(name);
        }
        if (color != null) group.setColor(color);
        if (exclusive != null) group.setExclusive(exclusive);
        if (required != null) group.setRequired(required);
        if (sortOrder != null) group.setSortOrder(sortOrder);
        group.setUpdatedAt(LocalDateTime.now());
        topicRepository.updateTagGroup(group);
        return group;
    }

    public void deleteTagGroup(Long groupId) {
        topicRepository.findTagGroupById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "标签组不存在"));
        topicRepository.deleteTagGroup(groupId);
    }

    public UserTagValue addTagValue(Long groupId, String value, String color, int sortOrder) {
        if (topicRepository.existsTagValueByGroupIdAndValue(groupId, value)) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签值已存在：" + value);
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
        return tagValue;
    }

    public void deleteTagValue(Long valueId) {
        topicRepository.findTagValueById(valueId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "标签值不存在"));
        topicRepository.deleteTagValue(valueId);
    }

    public List<UserTagGroup> listTagGroups(Long topicId) {
        return topicRepository.findTagGroupsByTopicId(topicId);
    }

    public List<UserTagValue> listTagValues(Long groupId) {
        return topicRepository.findTagValuesByGroupId(groupId);
    }

}
