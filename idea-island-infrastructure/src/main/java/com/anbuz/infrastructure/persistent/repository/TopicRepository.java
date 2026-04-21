package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.infrastructure.persistent.dao.ITagDao;
import com.anbuz.infrastructure.persistent.dao.ITopicDao;
import com.anbuz.infrastructure.persistent.po.TopicPO;
import com.anbuz.infrastructure.persistent.po.UserTagGroupPO;
import com.anbuz.infrastructure.persistent.po.UserTagValuePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TopicRepository implements ITopicRepository {

    private final ITopicDao topicDao;
    private final ITagDao tagDao;

    @Override
    public void saveTopic(Topic topic) {
        topicDao.insert(toPO(topic));
    }

    @Override
    public Optional<Topic> findTopicById(Long id) {
        return topicDao.selectById(id).map(this::toDomain);
    }

    @Override
    public List<Topic> findTopicsByUserId(Long userId) {
        return topicDao.selectByUserId(userId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndName(Long userId, String name) {
        return topicDao.countByUserIdAndName(userId, name) > 0;
    }

    @Override
    public void updateTopic(Topic topic) {
        topicDao.update(toPO(topic));
    }

    @Override
    public void deleteTopic(Long id) {
        topicDao.deleteById(id);
    }

    @Override
    public int countTagGroupsByTopicId(Long topicId) {
        return tagDao.countGroupsByTopicId(topicId);
    }

    @Override
    public void saveTagGroup(UserTagGroup group) {
        tagDao.insertGroup(toGroupPO(group));
    }

    @Override
    public Optional<UserTagGroup> findTagGroupById(Long groupId) {
        return tagDao.selectGroupById(groupId).map(this::toGroupDomain);
    }

    @Override
    public List<UserTagGroup> findTagGroupsByTopicId(Long topicId) {
        return tagDao.selectGroupsByTopicId(topicId)
                .stream().map(this::toGroupDomain).collect(Collectors.toList());
    }

    @Override
    public void updateTagGroup(UserTagGroup group) {
        tagDao.updateGroup(toGroupPO(group));
    }

    @Override
    public void deleteTagGroup(Long groupId) {
        tagDao.deleteGroupById(groupId);
    }

    @Override
    public int countTagValuesByGroupId(Long groupId) {
        return tagDao.countValuesByGroupId(groupId);
    }

    @Override
    public void saveTagValue(UserTagValue value) {
        tagDao.insertValue(toValuePO(value));
    }

    @Override
    public Optional<UserTagValue> findTagValueById(Long valueId) {
        return tagDao.selectValueById(valueId).map(this::toValueDomain);
    }

    @Override
    public List<UserTagValue> findTagValuesByGroupId(Long groupId) {
        return tagDao.selectValuesByGroupId(groupId)
                .stream().map(this::toValueDomain).collect(Collectors.toList());
    }

    @Override
    public void updateTagValue(UserTagValue value) {
        tagDao.updateValue(toValuePO(value));
    }

    @Override
    public void deleteTagValue(Long valueId) {
        tagDao.deleteValueById(valueId);
    }

    @Override
    public boolean existsTagGroupByTopicIdAndName(Long topicId, String name) {
        return tagDao.countGroupByTopicIdAndName(topicId, name) > 0;
    }

    @Override
    public boolean existsTagValueByGroupIdAndValue(Long groupId, String value) {
        return tagDao.countValueByGroupIdAndValue(groupId, value) > 0;
    }

    private TopicPO toPO(Topic t) {
        return TopicPO.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .name(t.getName())
                .description(t.getDescription())
                .status(t.getStatus())
                .materialCount(t.getMaterialCount())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private Topic toDomain(TopicPO po) {
        return Topic.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .name(po.getName())
                .description(po.getDescription())
                .status(po.getStatus())
                .materialCount(po.getMaterialCount())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private UserTagGroupPO toGroupPO(UserTagGroup g) {
        return UserTagGroupPO.builder()
                .id(g.getId())
                .topicId(g.getTopicId())
                .name(g.getName())
                .color(g.getColor())
                .isExclusive(g.getExclusive())
                .isRequired(g.getRequired())
                .sortOrder(g.getSortOrder())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    private UserTagGroup toGroupDomain(UserTagGroupPO po) {
        return UserTagGroup.builder()
                .id(po.getId())
                .topicId(po.getTopicId())
                .name(po.getName())
                .color(po.getColor())
                .exclusive(po.getIsExclusive())
                .required(po.getIsRequired())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private UserTagValuePO toValuePO(UserTagValue v) {
        return UserTagValuePO.builder()
                .id(v.getId())
                .groupId(v.getGroupId())
                .value(v.getValue())
                .color(v.getColor())
                .sortOrder(v.getSortOrder())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private UserTagValue toValueDomain(UserTagValuePO po) {
        return UserTagValue.builder()
                .id(po.getId())
                .groupId(po.getGroupId())
                .value(po.getValue())
                .color(po.getColor())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .build();
    }

}
