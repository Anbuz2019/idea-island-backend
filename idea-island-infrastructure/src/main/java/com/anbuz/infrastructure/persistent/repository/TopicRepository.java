package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.TopicAutoInvalidRule;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.infrastructure.persistent.dao.ITagDao;
import com.anbuz.infrastructure.persistent.dao.ITopicDao;
import com.anbuz.infrastructure.persistent.dao.ITopicAutoInvalidRuleDao;
import com.anbuz.infrastructure.persistent.po.TopicPO;
import com.anbuz.infrastructure.persistent.po.TopicAutoInvalidRulePO;
import com.anbuz.infrastructure.persistent.po.UserTagGroupPO;
import com.anbuz.infrastructure.persistent.po.UserTagValuePO;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 主题仓储实现，负责把主题和标签配置读写转换为 MyBatis 持久化操作。
 */
@Repository
@RequiredArgsConstructor
public class TopicRepository implements ITopicRepository {

    private final ITopicDao topicDao;
    private final ITagDao tagDao;
    private final ITopicAutoInvalidRuleDao topicAutoInvalidRuleDao;

    @Override
    public void saveTopic(Topic topic) {
        TopicPO po = toPO(topic);
        try {
            topicDao.insert(po);
            topic.setId(po.getId());
        } catch (DuplicateKeyException e) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "主题名称已存在");
        }
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
    public long countTopicsByUserId(Long userId) {
        return topicDao.countByUserId(userId);
    }

    @Override
    public int countTagGroupsByTopicId(Long topicId) {
        return tagDao.countGroupsByTopicId(topicId);
    }

    @Override
    public void saveTagGroup(UserTagGroup group) {
        UserTagGroupPO po = toGroupPO(group);
        try {
            tagDao.insertGroup(po);
            group.setId(po.getId());
        } catch (DuplicateKeyException e) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签组名称已存在");
        }
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
        UserTagValuePO po = toValuePO(value);
        try {
            tagDao.insertValue(po);
            value.setId(po.getId());
        } catch (DuplicateKeyException e) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "标签值已存在");
        }
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

    @Override
    public long countMaterialReferencesByGroupId(Long groupId) {
        return tagDao.countMaterialReferencesByGroupKey(String.valueOf(groupId));
    }

    @Override
    public long countMaterialReferencesByValue(Long groupId, String value) {
        return tagDao.countMaterialReferencesByGroupKeyAndValue(String.valueOf(groupId), value);
    }

    @Override
    public boolean existsMultiValueUsageInGroup(Long groupId) {
        return tagDao.countMaterialsWithMultipleValuesInGroup(String.valueOf(groupId)) > 0;
    }

    @Override
    public void updateMaterialTagValue(Long groupId, String oldValue, String newValue) {
        tagDao.updateMaterialTagValue(String.valueOf(groupId), oldValue, newValue);
    }

    @Override
    public List<Long> findRequiredTagGroupIdsByTopicId(Long topicId) {
        return tagDao.selectRequiredGroupIdsByTopicId(topicId);
    }

    @Override
    public void deleteTagValuesByGroupId(Long groupId) {
        tagDao.deleteValuesByGroupId(groupId);
    }

    @Override
    public List<TopicAutoInvalidRule> findEnabledAutoInvalidRules() {
        return topicAutoInvalidRuleDao.selectEnabledRules()
                .stream()
                .map(this::toRuleDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAutoInvalidRule(TopicAutoInvalidRule rule) {
        try {
            topicAutoInvalidRuleDao.insert(toRulePO(rule));
        } catch (DuplicateKeyException e) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "自动失效规则已存在");
        }
    }

    @Override
    public void deleteAutoInvalidRulesByTopicId(Long topicId) {
        topicAutoInvalidRuleDao.deleteByTopicId(topicId);
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

    private TopicAutoInvalidRulePO toRulePO(TopicAutoInvalidRule rule) {
        return TopicAutoInvalidRulePO.builder()
                .id(rule.getId())
                .topicId(rule.getTopicId())
                .ruleType(rule.getRuleType())
                .thresholdDays(rule.getThresholdDays())
                .isEnabled(rule.getEnabled())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private TopicAutoInvalidRule toRuleDomain(TopicAutoInvalidRulePO po) {
        return TopicAutoInvalidRule.builder()
                .id(po.getId())
                .topicId(po.getTopicId())
                .ruleType(po.getRuleType())
                .thresholdDays(po.getThresholdDays())
                .enabled(po.getIsEnabled())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

}
