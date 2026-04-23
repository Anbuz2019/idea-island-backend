package com.anbuz.domain.topic.service;

import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;

import java.util.List;

/**
 * 标签集领域服务接口，定义主题下标签组和值的维护能力。
 */
public interface ITagSetService {

    UserTagGroup createTagGroup(Long userId, Long topicId, String name, boolean exclusive, boolean required, int sortOrder);

    UserTagGroup updateTagGroup(Long userId, Long groupId, String name, String color,
                                Boolean exclusive, Boolean required, Integer sortOrder);

    void deleteTagGroup(Long userId, Long groupId);

    UserTagValue addTagValue(Long userId, Long groupId, String value, String color, int sortOrder);

    UserTagValue updateTagValue(Long userId, Long valueId, String value, String color, Integer sortOrder);

    void deleteTagValue(Long userId, Long valueId);

    List<UserTagGroup> listTagGroups(Long userId, Long topicId);

    List<UserTagValue> listTagValues(Long userId, Long groupId);
}
