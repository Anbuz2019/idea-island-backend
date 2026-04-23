package com.anbuz.trigger.http;

import com.anbuz.api.http.ITagGroupController;
import com.anbuz.domain.material.model.valobj.SystemTagDefinition;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.service.ITagSetService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.enums.TagType;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签组 HTTP 适配器，负责把标签组和值维护请求转换为主题域服务调用。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TagGroupController implements ITagGroupController {

    private final ITagSetService tagSetService;

    @Override
    public Result<List<TagGroupDetailResponse>> list(@PathVariable Long topicId) {
        Long userId = UserContext.currentUserId();
        log.debug("List tag groups userId={} topicId={}", userId, topicId);
        List<TagGroupDetailResponse> systemGroups = SystemTagDefinition.definitions().stream()
                .map(definition -> toSystemGroupDetail(topicId, definition))
                .toList();
        List<TagGroupDetailResponse> userGroups = tagSetService.listTagGroups(userId, topicId).stream()
                .map(group -> new TagGroupDetailResponse(toGroupResponse(group),
                        tagSetService.listTagValues(userId, group.getId()).stream().map(this::toValueResponse).toList()))
                .toList();
        List<TagGroupDetailResponse> result = new java.util.ArrayList<>(systemGroups);
        result.addAll(userGroups);
        return Result.ok(result);
    }

    @Override
    public Result<TagGroupResponse> create(@PathVariable Long topicId, @Valid @RequestBody CreateTagGroupRequest request) {
        Long userId = UserContext.currentUserId();
        log.info("Create tag group requested userId={} topicId={} name={}", userId, topicId, request.getName());
        UserTagGroup group = tagSetService.createTagGroup(userId, topicId, request.getName(),
                request.getExclusive(), request.getRequired(), request.getSortOrder() == null ? 0 : request.getSortOrder());
        log.info("Create tag group succeeded userId={} topicId={} groupId={}", userId, topicId, group.getId());
        return Result.ok(toGroupResponse(group));
    }

    @Override
    public Result<TagGroupResponse> update(@PathVariable Long topicId, @PathVariable Long groupId,
                                           @Valid @RequestBody UpdateTagGroupRequest request) {
        Long userId = UserContext.currentUserId();
        assertGroupInTopic(userId, topicId, groupId);
        log.info("Update tag group requested userId={} topicId={} groupId={} nameChanged={} colorChanged={} exclusiveChanged={} requiredChanged={} sortOrderChanged={}",
                userId, topicId, groupId, request.getName() != null, request.getColor() != null,
                request.getExclusive() != null, request.getRequired() != null, request.getSortOrder() != null);
        UserTagGroup group = tagSetService.updateTagGroup(userId, groupId, request.getName(),
                request.getColor(), request.getExclusive(), request.getRequired(), request.getSortOrder());
        log.info("Update tag group succeeded userId={} topicId={} groupId={}", userId, topicId, groupId);
        return Result.ok(toGroupResponse(group));
    }

    @Override
    public Result<Void> delete(@PathVariable Long topicId, @PathVariable Long groupId) {
        Long userId = UserContext.currentUserId();
        assertGroupInTopic(userId, topicId, groupId);
        tagSetService.deleteTagGroup(userId, groupId);
        log.info("Delete tag group succeeded userId={} topicId={} groupId={}", userId, topicId, groupId);
        return Result.ok();
    }

    @Override
    public Result<TagValueResponse> addValue(@PathVariable Long groupId, @Valid @RequestBody CreateTagValueRequest request) {
        Long userId = UserContext.currentUserId();
        log.info("Add tag value requested userId={} groupId={} value={}", userId, groupId, request.getValue());
        UserTagValue value = tagSetService.addTagValue(userId, groupId, request.getValue(),
                request.getColor(), request.getSortOrder() == null ? 0 : request.getSortOrder());
        log.info("Add tag value succeeded userId={} groupId={} valueId={}", userId, groupId, value.getId());
        return Result.ok(toValueResponse(value));
    }

    @Override
    public Result<TagValueResponse> updateValue(@PathVariable Long groupId, @PathVariable Long valueId,
                                                @Valid @RequestBody UpdateTagValueRequest request) {
        Long userId = UserContext.currentUserId();
        assertValueInGroup(userId, groupId, valueId);
        log.info("Update tag value requested userId={} groupId={} valueId={} valueChanged={} colorChanged={} sortOrderChanged={}",
                userId, groupId, valueId, request.getValue() != null, request.getColor() != null, request.getSortOrder() != null);
        UserTagValue value = tagSetService.updateTagValue(userId, valueId,
                request.getValue(), request.getColor(), request.getSortOrder());
        log.info("Update tag value succeeded userId={} groupId={} valueId={}", userId, groupId, valueId);
        return Result.ok(toValueResponse(value));
    }

    @Override
    public Result<Void> deleteValue(@PathVariable Long groupId, @PathVariable Long valueId) {
        Long userId = UserContext.currentUserId();
        assertValueInGroup(userId, groupId, valueId);
        tagSetService.deleteTagValue(userId, valueId);
        log.info("Delete tag value succeeded userId={} groupId={} valueId={}", userId, groupId, valueId);
        return Result.ok();
    }

    private void assertGroupInTopic(Long userId, Long topicId, Long groupId) {
        boolean matched = tagSetService.listTagGroups(userId, topicId).stream()
                .anyMatch(group -> groupId.equals(group.getId()));
        if (!matched) {
            throw new AppException(ErrorCode.NOT_FOUND, "标签组不存在或不属于当前主题");
        }
    }

    private void assertValueInGroup(Long userId, Long groupId, Long valueId) {
        boolean matched = tagSetService.listTagValues(userId, groupId).stream()
                .anyMatch(value -> valueId.equals(value.getId()));
        if (!matched) {
            throw new AppException(ErrorCode.NOT_FOUND, "标签值不存在或不属于当前标签组");
        }
    }

    private TagGroupResponse toGroupResponse(UserTagGroup group) {
        return new TagGroupResponse(group.getId(), group.getTopicId(), TagType.USER.getCode(), String.valueOf(group.getId()),
                group.getName(), group.getColor(), group.getExclusive(), group.getRequired(), group.getSortOrder(),
                group.getCreatedAt(), group.getUpdatedAt());
    }

    private TagGroupDetailResponse toSystemGroupDetail(Long topicId, SystemTagDefinition definition) {
        TagGroupResponse group = new TagGroupResponse(null, topicId, TagType.SYSTEM.getCode(), definition.groupKey(),
                definition.groupName(), null, definition.exclusive(), definition.required(), definition.sortOrder(), null, null);
        List<TagValueResponse> values = definition.values().stream()
                .map(value -> new TagValueResponse(null, null, value, null, 0, null))
                .toList();
        return new TagGroupDetailResponse(group, values);
    }

    private TagValueResponse toValueResponse(UserTagValue value) {
        return new TagValueResponse(value.getId(), value.getGroupId(), value.getValue(), value.getColor(),
                value.getSortOrder(), value.getCreatedAt());
    }
}
