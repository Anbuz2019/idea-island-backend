package com.anbuz.trigger.http;

import com.anbuz.api.http.ITagGroupController;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.service.ITagSetService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.enums.TagType;
import com.anbuz.types.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagGroupController scenarios")
class TagGroupControllerTest {

    @Mock
    private ITagSetService tagSetService;

    @InjectMocks
    private TagGroupController tagGroupController;

    @Nested
    @DisplayName("list tag groups")
    class ListTagGroups {

        @Test
        @DisplayName("returns system and user tag groups")
        void givenTopicId_whenList_thenReturnsSystemAndUserTagGroups() {
            UserContext.set(1L);
            try {
                when(tagSetService.listTagGroups(1L, 10L)).thenReturn(List.of(group()));
                when(tagSetService.listTagValues(1L, 20L)).thenReturn(List.of(value()));

                Result<List<ITagGroupController.TagGroupDetailResponse>> result = tagGroupController.list(10L);

                assertThat(result).returns(0, Result::getCode);
                assertThat(result.getData())
                        .hasSize(3)
                        .extracting(detail -> detail.group().tagGroupKey())
                        .containsExactly("sys_score_range", "sys_completeness", "20");
                assertThat(result.getData().get(0).group())
                        .returns(TagType.SYSTEM.getCode(), ITagGroupController.TagGroupResponse::tagType)
                        .returns("评分区间", ITagGroupController.TagGroupResponse::name);
                assertThat(result.getData().get(2).group())
                        .returns(TagType.USER.getCode(), ITagGroupController.TagGroupResponse::tagType)
                        .returns("phase", ITagGroupController.TagGroupResponse::name);
                assertThat(result.getData().get(2).values())
                        .singleElement()
                        .returns("analysis", ITagGroupController.TagValueResponse::value);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("mutate tag group")
    class MutateTagGroup {

        @Test
        @DisplayName("delegates create to the domain service")
        void givenCreateRequest_whenCreate_thenDelegatesToDomainService() {
            ITagGroupController.CreateTagGroupRequest request = new ITagGroupController.CreateTagGroupRequest();
            request.setName("phase");
            request.setExclusive(true);
            request.setRequired(false);
            request.setSortOrder(3);

            UserContext.set(1L);
            try {
                when(tagSetService.createTagGroup(1L, 10L, "phase", true, false, 3)).thenReturn(group());

                Result<ITagGroupController.TagGroupResponse> result = tagGroupController.create(10L, request);

                assertThat(result.getData())
                        .returns(20L, ITagGroupController.TagGroupResponse::id)
                        .returns("20", ITagGroupController.TagGroupResponse::tagGroupKey)
                        .returns(TagType.USER.getCode(), ITagGroupController.TagGroupResponse::tagType);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("checks that the group belongs to the topic before update")
        void givenUpdateRequest_whenUpdate_thenChecksGroupBelongsToTopicAndDelegates() {
            ITagGroupController.UpdateTagGroupRequest request = new ITagGroupController.UpdateTagGroupRequest();
            request.setName("workflow");

            UserContext.set(1L);
            try {
                UserTagGroup updated = group();
                updated.setName("workflow");
                when(tagSetService.listTagGroups(1L, 10L)).thenReturn(List.of(group()));
                when(tagSetService.updateTagGroup(1L, 20L, "workflow", null, null, null, null)).thenReturn(updated);

                Result<ITagGroupController.TagGroupResponse> result = tagGroupController.update(10L, 20L, request);

                assertThat(result.getData()).returns("workflow", ITagGroupController.TagGroupResponse::name);
                verify(tagSetService).listTagGroups(1L, 10L);
                verify(tagSetService).updateTagGroup(1L, 20L, "workflow", null, null, null, null);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("checks that the group belongs to the topic before delete")
        void givenGroupId_whenDelete_thenChecksGroupBelongsToTopicAndDelegates() {
            UserContext.set(1L);
            try {
                when(tagSetService.listTagGroups(1L, 10L)).thenReturn(List.of(group()));

                Result<Void> result = tagGroupController.delete(10L, 20L);

                assertThat(result).returns(0, Result::getCode);
                verify(tagSetService).listTagGroups(1L, 10L);
                verify(tagSetService).deleteTagGroup(1L, 20L);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("mutate tag value")
    class MutateTagValue {

        @Test
        @DisplayName("delegates add value to the domain service")
        void givenCreateValueRequest_whenAddValue_thenDelegatesToDomainService() {
            ITagGroupController.CreateTagValueRequest request = new ITagGroupController.CreateTagValueRequest();
            request.setValue("analysis");
            request.setColor("#00AAFF");
            request.setSortOrder(1);

            UserContext.set(1L);
            try {
                when(tagSetService.addTagValue(1L, 20L, "analysis", "#00AAFF", 1)).thenReturn(value());

                Result<ITagGroupController.TagValueResponse> result = tagGroupController.addValue(20L, request);

                assertThat(result.getData())
                        .returns(30L, ITagGroupController.TagValueResponse::id)
                        .returns(20L, ITagGroupController.TagValueResponse::groupId)
                        .returns("analysis", ITagGroupController.TagValueResponse::value);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("checks that the value belongs to the group before update")
        void givenUpdateValueRequest_whenUpdateValue_thenChecksValueBelongsToGroupAndDelegates() {
            ITagGroupController.UpdateTagValueRequest request = new ITagGroupController.UpdateTagValueRequest();
            request.setValue("architecture");

            UserContext.set(1L);
            try {
                UserTagValue updated = value();
                updated.setValue("architecture");
                when(tagSetService.listTagValues(1L, 20L)).thenReturn(List.of(value()));
                when(tagSetService.updateTagValue(1L, 30L, "architecture", null, null)).thenReturn(updated);

                Result<ITagGroupController.TagValueResponse> result = tagGroupController.updateValue(20L, 30L, request);

                assertThat(result.getData()).returns("architecture", ITagGroupController.TagValueResponse::value);
                verify(tagSetService).listTagValues(1L, 20L);
                verify(tagSetService).updateTagValue(1L, 30L, "architecture", null, null);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("checks that the value belongs to the group before delete")
        void givenValueId_whenDeleteValue_thenChecksValueBelongsToGroupAndDelegates() {
            UserContext.set(1L);
            try {
                when(tagSetService.listTagValues(1L, 20L)).thenReturn(List.of(value()));

                Result<Void> result = tagGroupController.deleteValue(20L, 30L);

                assertThat(result).returns(0, Result::getCode);
                verify(tagSetService).listTagValues(1L, 20L);
                verify(tagSetService).deleteTagValue(1L, 30L);
            } finally {
                UserContext.clear();
            }
        }
    }

    private UserTagGroup group() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 10, 0);
        return UserTagGroup.builder()
                .id(20L)
                .topicId(10L)
                .name("phase")
                .color("#FFAA00")
                .exclusive(true)
                .required(false)
                .sortOrder(3)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private UserTagValue value() {
        return UserTagValue.builder()
                .id(30L)
                .groupId(20L)
                .value("analysis")
                .color("#00AAFF")
                .sortOrder(1)
                .createdAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();
    }
}
