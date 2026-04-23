package com.anbuz.domain.topic.service;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.valobj.TopicStats;

import java.util.List;

/**
 * 主题领域服务接口，定义主题生命周期、统计和规则配置能力。
 */
public interface ITopicService {

    Topic createTopic(Long userId, String name, String description);

    Topic updateTopic(Long topicId, Long userId, String name, String description);

    void disableTopic(Long topicId, Long userId);

    void enableTopic(Long topicId, Long userId);

    void deleteTopic(Long topicId, Long userId);

    List<Topic> listTopics(Long userId);

    Topic getTopic(Long topicId, Long userId);

    TopicStats getTopicStats(Long topicId, Long userId);
}
