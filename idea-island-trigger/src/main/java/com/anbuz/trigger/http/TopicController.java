package com.anbuz.trigger.http;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.service.TagSetService;
import com.anbuz.domain.topic.service.TopicService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final TagSetService tagSetService;

    @GetMapping
    public Result<List<Topic>> list() {
        return Result.ok(topicService.listTopics(UserContext.currentUserId()));
    }

    @PostMapping
    public Result<Topic> create(@Valid @RequestBody CreateTopicRequest req) {
        return Result.ok(topicService.createTopic(UserContext.currentUserId(), req.getName(), req.getDescription()));
    }

    @GetMapping("/{id}")
    public Result<Topic> detail(@PathVariable Long id) {
        return Result.ok(topicService.getTopic(id, UserContext.currentUserId()));
    }

    @PutMapping("/{id}")
    public Result<Topic> update(@PathVariable Long id, @Valid @RequestBody UpdateTopicRequest req) {
        return Result.ok(topicService.updateTopic(id, UserContext.currentUserId(), req.getName(), req.getDescription()));
    }

    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        topicService.disableTopic(id, UserContext.currentUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        topicService.enableTopic(id, UserContext.currentUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        topicService.deleteTopic(id, UserContext.currentUserId());
        return Result.ok();
    }

    @Data
    public static class CreateTopicRequest {
        @NotBlank @Size(max = 50) private String name;
        @Size(max = 500) private String description;
    }

    @Data
    public static class UpdateTopicRequest {
        @Size(max = 50) private String name;
        @Size(max = 500) private String description;
    }

}
