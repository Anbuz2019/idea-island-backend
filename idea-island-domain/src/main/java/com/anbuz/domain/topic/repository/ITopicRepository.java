package com.anbuz.domain.topic.repository;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;

import java.util.List;
import java.util.Optional;

public interface ITopicRepository {

    void saveTopic(Topic topic);

    Optional<Topic> findTopicById(Long id);

    List<Topic> findTopicsByUserId(Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

    void updateTopic(Topic topic);

    void deleteTopic(Long id);

    int countTagGroupsByTopicId(Long topicId);

    void saveTagGroup(UserTagGroup group);

    Optional<UserTagGroup> findTagGroupById(Long groupId);

    List<UserTagGroup> findTagGroupsByTopicId(Long topicId);

    void updateTagGroup(UserTagGroup group);

    void deleteTagGroup(Long groupId);

    int countTagValuesByGroupId(Long groupId);

    void saveTagValue(UserTagValue value);

    Optional<UserTagValue> findTagValueById(Long valueId);

    List<UserTagValue> findTagValuesByGroupId(Long groupId);

    void updateTagValue(UserTagValue value);

    void deleteTagValue(Long valueId);

    boolean existsTagGroupByTopicIdAndName(Long topicId, String name);

    boolean existsTagValueByGroupIdAndValue(Long groupId, String value);

}
