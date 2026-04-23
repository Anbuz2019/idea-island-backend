package com.anbuz.domain.topic.service.impl;

import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.TopicAutoInvalidRule;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.valobj.TopicStats;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.domain.topic.service.ITopicService;
import com.anbuz.types.enums.AutoInvalidRuleType;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 主题领域服务，负责主题创建、配置更新、启停删除、统计和自动失效规则维护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicService implements ITopicService {

    private static final int MAX_TOPICS_PER_USER = 50;

    private final ITopicRepository topicRepository;
    private final IMaterialRepository materialRepository;

    @Override
    @Transactional
    public Topic createTopic(Long userId, String name, String description) {
        if (topicRepository.existsByUserIdAndName(userId, name)) {
            log.warn("Create topic rejected due to duplicate name userId={} name={}", userId, name);
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "主题名称已存在: " + name);
        }
        if (topicRepository.countTopicsByUserId(userId) >= MAX_TOPICS_PER_USER) {
            log.warn("Create topic rejected because user reached topic limit userId={} limit={}",
                    userId, MAX_TOPICS_PER_USER);
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "每个用户最多创建 " + MAX_TOPICS_PER_USER + " 个主题");
        }
        LocalDateTime now = LocalDateTime.now();
        Topic topic = Topic.builder()
                .userId(userId)
                .name(name)
                .description(description)
                .status(1)
                .materialCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        topicRepository.saveTopic(topic);
        topicRepository.saveAutoInvalidRule(TopicAutoInvalidRule.builder()
                .topicId(topic.getId())
                .ruleType(AutoInvalidRuleType.INBOX_TIMEOUT.getCode())
                .thresholdDays(90)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
        topicRepository.saveAutoInvalidRule(TopicAutoInvalidRule.builder()
                .topicId(topic.getId())
                .ruleType(AutoInvalidRuleType.PENDING_REVIEW_TIMEOUT.getCode())
                .thresholdDays(60)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
        log.info("Create topic succeeded userId={} topicId={} name={}", userId, topic.getId(), name);
        return topic;
    }

    @Override
    @Transactional
    public Topic updateTopic(Long topicId, Long userId, String name, String description) {
        Topic topic = getOwnedTopic(topicId, userId);
        if (name != null && !name.equals(topic.getName())) {
            if (topicRepository.existsByUserIdAndName(userId, name)) {
                log.warn("Update topic rejected due to duplicate name userId={} topicId={} name={}", userId, topicId, name);
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "主题名称已存在: " + name);
            }
            topic.setName(name);
        }
        if (description != null) {
            topic.setDescription(description);
        }
        topic.setUpdatedAt(LocalDateTime.now());
        topicRepository.updateTopic(topic);
        log.info("Update topic succeeded userId={} topicId={} name={}", userId, topicId, topic.getName());
        return topic;
    }

    @Override
    @Transactional
    public void disableTopic(Long topicId, Long userId) {
        Topic topic = getOwnedTopic(topicId, userId);
        topic.setStatus(0);
        topic.setUpdatedAt(LocalDateTime.now());
        topicRepository.updateTopic(topic);
        log.info("Disable topic succeeded userId={} topicId={}", userId, topicId);
    }

    @Override
    @Transactional
    public void enableTopic(Long topicId, Long userId) {
        Topic topic = getOwnedTopic(topicId, userId);
        topic.setStatus(1);
        topic.setUpdatedAt(LocalDateTime.now());
        topicRepository.updateTopic(topic);
        log.info("Enable topic succeeded userId={} topicId={}", userId, topicId);
    }

    @Override
    @Transactional
    public void deleteTopic(Long topicId, Long userId) {
        Topic topic = getOwnedTopic(topicId, userId);
        if (topic.getMaterialCount() > 0) {
            log.warn("Delete topic rejected due to remaining materials userId={} topicId={} materialCount={}",
                    userId, topicId, topic.getMaterialCount());
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "请先清空主题下的资料再删除");
        }
        for (UserTagGroup group : topicRepository.findTagGroupsByTopicId(topicId)) {
            topicRepository.deleteTagValuesByGroupId(group.getId());
            topicRepository.deleteTagGroup(group.getId());
        }
        topicRepository.deleteAutoInvalidRulesByTopicId(topicId);
        topicRepository.deleteTopic(topicId);
        log.info("Delete topic succeeded userId={} topicId={}", userId, topicId);
    }

    @Override
    public List<Topic> listTopics(Long userId) {
        return topicRepository.findTopicsByUserId(userId);
    }

    @Override
    public Topic getTopic(Long topicId, Long userId) {
        return getOwnedTopic(topicId, userId);
    }

    @Override
    public TopicStats getTopicStats(Long topicId, Long userId) {
        getOwnedTopic(topicId, userId);
        MaterialListQuery baseQuery = MaterialListQuery.builder()
                .userId(userId)
                .topicId(topicId)
                .page(1)
                .pageSize(1)
                .build();
        long totalMaterials = materialRepository.countMaterials(baseQuery);
        long weeklyNew = materialRepository.countMaterials(MaterialListQuery.builder()
                .userId(userId)
                .topicId(topicId)
                .createdStart(LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay())
                .page(1)
                .pageSize(1)
                .build());
        long pendingCount = materialRepository.countMaterials(MaterialListQuery.builder()
                .userId(userId)
                .topicId(topicId)
                .statuses(List.of(MaterialStatus.INBOX.getCode(), MaterialStatus.PENDING_REVIEW.getCode()))
                .page(1)
                .pageSize(1)
                .build());
        BigDecimal averageScore = materialRepository.averageScoreByTopicId(topicId);
        if (averageScore == null) {
            averageScore = BigDecimal.ZERO;
        }
        return TopicStats.builder()
                .totalMaterials(totalMaterials)
                .statusCounts(materialRepository.countByStatus(userId, topicId))
                .typeCounts(materialRepository.countByMaterialType(topicId))
                .weeklyNew(weeklyNew)
                .averageScore(averageScore)
                .pendingCount(pendingCount)
                .build();
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
