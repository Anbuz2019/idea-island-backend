package com.anbuz.domain.topic.service;

import com.anbuz.domain.topic.model.entity.Topic;
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
public class TopicService {

    private static final int MAX_USER_TAG_GROUPS = 20;

    private final ITopicRepository topicRepository;

    public Topic createTopic(Long userId, String name, String description) {
        if (topicRepository.existsByUserIdAndName(userId, name)) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "主题名称已存在：" + name);
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
        return topic;
    }

    public Topic updateTopic(Long topicId, Long userId, String name, String description) {
        Topic topic = getOwnedTopic(topicId, userId);
        if (name != null && !name.equals(topic.getName())) {
            if (topicRepository.existsByUserIdAndName(userId, name)) {
                throw new AppException(ErrorCode.BUSINESS_CONFLICT, "主题名称已存在：" + name);
            }
            topic.setName(name);
        }
        if (description != null) topic.setDescription(description);
        topic.setUpdatedAt(LocalDateTime.now());
        topicRepository.updateTopic(topic);
        return topic;
    }

    public void disableTopic(Long topicId, Long userId) {
        Topic topic = getOwnedTopic(topicId, userId);
        topic.setStatus(0);
        topic.setUpdatedAt(LocalDateTime.now());
        topicRepository.updateTopic(topic);
    }

    public void enableTopic(Long topicId, Long userId) {
        Topic topic = getOwnedTopic(topicId, userId);
        topic.setStatus(1);
        topic.setUpdatedAt(LocalDateTime.now());
        topicRepository.updateTopic(topic);
    }

    public void deleteTopic(Long topicId, Long userId) {
        Topic topic = getOwnedTopic(topicId, userId);
        if (topic.getMaterialCount() > 0) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "请先清空主题下的资料再删除");
        }
        topicRepository.deleteTopic(topicId);
    }

    public List<Topic> listTopics(Long userId) {
        return topicRepository.findTopicsByUserId(userId);
    }

    public Topic getTopic(Long topicId, Long userId) {
        return getOwnedTopic(topicId, userId);
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
