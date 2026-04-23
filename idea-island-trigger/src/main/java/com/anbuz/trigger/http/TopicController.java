package com.anbuz.trigger.http;

import com.anbuz.api.http.ITopicController;
import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.valobj.TopicStats;
import com.anbuz.domain.topic.service.ITopicService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 主题 HTTP 适配器，负责把主题管理请求转换为主题域服务调用。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TopicController implements ITopicController {

    private final ITopicService topicService;

    @Override
    public Result<List<TopicResponse>> list() {
        Long userId = UserContext.currentUserId();
        log.debug("List topics userId={}", userId);
        return Result.ok(topicService.listTopics(userId).stream().map(this::toResponse).toList());
    }

    @Override
    public Result<TopicResponse> create(@Valid @RequestBody CreateTopicRequest req) {
        Long userId = UserContext.currentUserId();
        log.info("Create topic requested userId={} name={}", userId, req.getName());
        Topic topic = topicService.createTopic(userId, req.getName(), req.getDescription());
        log.info("Create topic succeeded userId={} topicId={} name={}", userId, topic.getId(), topic.getName());
        return Result.ok(toResponse(topic));
    }

    @Override
    public Result<TopicResponse> detail(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        log.debug("Load topic detail userId={} topicId={}", userId, id);
        return Result.ok(toResponse(topicService.getTopic(id, userId)));
    }

    @Override
    public Result<TopicResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateTopicRequest req) {
        Long userId = UserContext.currentUserId();
        log.info("Update topic requested userId={} topicId={} nameChanged={} descriptionChanged={}",
                userId, id, req.getName() != null, req.getDescription() != null);
        Topic topic = topicService.updateTopic(id, userId, req.getName(), req.getDescription());
        log.info("Update topic succeeded userId={} topicId={}", userId, id);
        return Result.ok(toResponse(topic));
    }

    @Override
    public Result<Void> disable(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        topicService.disableTopic(id, userId);
        log.info("Disable topic succeeded userId={} topicId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<Void> enable(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        topicService.enableTopic(id, userId);
        log.info("Enable topic succeeded userId={} topicId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        topicService.deleteTopic(id, userId);
        log.info("Delete topic succeeded userId={} topicId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<TopicStatsResponse> stats(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        log.debug("Load topic stats userId={} topicId={}", userId, id);
        TopicStats stats = topicService.getTopicStats(id, userId);
        return Result.ok(new TopicStatsResponse(stats.getTotalMaterials(), stats.getStatusCounts(), stats.getTypeCounts(),
                stats.getWeeklyNew(), stats.getAverageScore(), stats.getPendingCount()));
    }

    private TopicResponse toResponse(Topic topic) {
        return new TopicResponse(topic.getId(), topic.getUserId(), topic.getName(), topic.getDescription(),
                topic.getStatus(), topic.getMaterialCount(), topic.getCreatedAt(), topic.getUpdatedAt());
    }
}
