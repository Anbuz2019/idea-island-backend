package com.anbuz.domain.topic.service;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.domain.topic.service.impl.TagSetService;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagSetService 标签集领域服务")
class TagSetServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TOPIC_ID = 10L;
    private static final Long GROUP_ID = 20L;
    private static final Long VALUE_ID = 30L;

    @Mock
    private ITopicRepository topicRepository;

    @InjectMocks
    private TagSetService tagSetService;

    @Nested
    @DisplayName("标签组")
    class TagGroups {

        @Test
        @DisplayName("主题归属正确且未达上限时，创建标签组")
        void givenOwnedTopicAndBelowLimit_whenCreateTagGroup_thenCreatesGroup() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsTagGroupByTopicIdAndName(TOPIC_ID, "阶段")).thenReturn(false);
            when(topicRepository.countTagGroupsByTopicId(TOPIC_ID)).thenReturn(2);
            doAnswer(invocation -> {
                UserTagGroup group = invocation.getArgument(0);
                group.setId(GROUP_ID);
                return null;
            }).when(topicRepository).saveTagGroup(any(UserTagGroup.class));

            UserTagGroup result = tagSetService.createTagGroup(USER_ID, TOPIC_ID, "阶段", true, true, 1);

            assertThat(result)
                    .returns(GROUP_ID, UserTagGroup::getId)
                    .returns(TOPIC_ID, UserTagGroup::getTopicId)
                    .returns("阶段", UserTagGroup::getName)
                    .returns(true, UserTagGroup::getExclusive)
                    .returns(true, UserTagGroup::getRequired)
                    .returns(1, UserTagGroup::getSortOrder)
                    .satisfies(group -> assertThat(group.getCreatedAt()).isNotNull())
                    .satisfies(group -> assertThat(group.getUpdatedAt()).isNotNull());
        }

        @Test
        @DisplayName("同主题标签组名称重复时，拒绝创建")
        void givenDuplicateGroupName_whenCreateTagGroup_thenThrowsBusinessConflict() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsTagGroupByTopicIdAndName(TOPIC_ID, "阶段")).thenReturn(true);

            assertThatThrownBy(() -> tagSetService.createTagGroup(USER_ID, TOPIC_ID, "阶段", true, false, 0))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).saveTagGroup(any());
        }

        @Test
        @DisplayName("主题标签组达到 20 个时，拒绝创建")
        void givenGroupLimitReached_whenCreateTagGroup_thenThrowsBusinessConflict() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsTagGroupByTopicIdAndName(TOPIC_ID, "阶段")).thenReturn(false);
            when(topicRepository.countTagGroupsByTopicId(TOPIC_ID)).thenReturn(20);

            assertThatThrownBy(() -> tagSetService.createTagGroup(USER_ID, TOPIC_ID, "阶段", true, false, 0))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).saveTagGroup(any());
        }

        @Test
        @DisplayName("非互斥标签组已有多值使用时，拒绝切换为互斥")
        void givenMultiValueUsage_whenUpdateGroupToExclusive_thenThrowsBusinessConflict() {
            UserTagGroup group = ownedGroup();
            group.setExclusive(false);
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(group));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsMultiValueUsageInGroup(GROUP_ID)).thenReturn(true);

            assertThatThrownBy(() -> tagSetService.updateTagGroup(USER_ID, GROUP_ID, null, null, true, null, null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).updateTagGroup(any());
        }

        @Test
        @DisplayName("更新标签组名称、颜色、必填和排序时，保存变更")
        void givenOwnedGroup_whenUpdateTagGroup_thenPersistsChanges() {
            UserTagGroup group = ownedGroup();
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(group));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsTagGroupByTopicIdAndName(TOPIC_ID, "流程")).thenReturn(false);

            UserTagGroup result = tagSetService.updateTagGroup(USER_ID, GROUP_ID, "流程", "#FFAA00", null, true, 5);

            assertThat(result)
                    .returns("流程", UserTagGroup::getName)
                    .returns("#FFAA00", UserTagGroup::getColor)
                    .returns(true, UserTagGroup::getRequired)
                    .returns(5, UserTagGroup::getSortOrder)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
            verify(topicRepository).updateTagGroup(group);
        }

        @Test
        @DisplayName("标签组已被资料引用时，拒绝删除")
        void givenReferencedGroup_whenDeleteTagGroup_thenThrowsBusinessConflict() {
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(ownedGroup()));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.countMaterialReferencesByGroupId(GROUP_ID)).thenReturn(1L);

            assertThatThrownBy(() -> tagSetService.deleteTagGroup(USER_ID, GROUP_ID))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).deleteTagGroup(any());
        }

        @Test
        @DisplayName("标签组未被引用时，删除标签值和标签组")
        void givenUnreferencedGroup_whenDeleteTagGroup_thenDeletesGroupAndValues() {
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(ownedGroup()));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.countMaterialReferencesByGroupId(GROUP_ID)).thenReturn(0L);

            tagSetService.deleteTagGroup(USER_ID, GROUP_ID);

            verify(topicRepository).deleteTagValuesByGroupId(GROUP_ID);
            verify(topicRepository).deleteTagGroup(GROUP_ID);
        }
    }

    @Nested
    @DisplayName("标签值")
    class TagValues {

        @Test
        @DisplayName("标签组归属正确且未达上限时，新增标签值")
        void givenOwnedGroupAndBelowLimit_whenAddTagValue_thenCreatesValue() {
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(ownedGroup()));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsTagValueByGroupIdAndValue(GROUP_ID, "需求分析")).thenReturn(false);
            when(topicRepository.countTagValuesByGroupId(GROUP_ID)).thenReturn(3);
            doAnswer(invocation -> {
                UserTagValue value = invocation.getArgument(0);
                value.setId(VALUE_ID);
                return null;
            }).when(topicRepository).saveTagValue(any(UserTagValue.class));

            UserTagValue result = tagSetService.addTagValue(USER_ID, GROUP_ID, "需求分析", "#00AAFF", 1);

            assertThat(result)
                    .returns(VALUE_ID, UserTagValue::getId)
                    .returns(GROUP_ID, UserTagValue::getGroupId)
                    .returns("需求分析", UserTagValue::getValue)
                    .returns("#00AAFF", UserTagValue::getColor)
                    .returns(1, UserTagValue::getSortOrder)
                    .satisfies(value -> assertThat(value.getCreatedAt()).isNotNull());
        }

        @Test
        @DisplayName("标签值名称重复时，拒绝新增")
        void givenDuplicateTagValue_whenAddTagValue_thenThrowsBusinessConflict() {
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(ownedGroup()));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsTagValueByGroupIdAndValue(GROUP_ID, "需求分析")).thenReturn(true);

            assertThatThrownBy(() -> tagSetService.addTagValue(USER_ID, GROUP_ID, "需求分析", null, 0))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).saveTagValue(any());
        }

        @Test
        @DisplayName("标签值达到 50 个时，拒绝新增")
        void givenValueLimitReached_whenAddTagValue_thenThrowsBusinessConflict() {
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(ownedGroup()));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsTagValueByGroupIdAndValue(GROUP_ID, "需求分析")).thenReturn(false);
            when(topicRepository.countTagValuesByGroupId(GROUP_ID)).thenReturn(50);

            assertThatThrownBy(() -> tagSetService.addTagValue(USER_ID, GROUP_ID, "需求分析", null, 0))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).saveTagValue(any());
        }

        @Test
        @DisplayName("修改标签值文本时，同步级联更新资料标签展示值")
        void givenNewValueText_whenUpdateTagValue_thenUpdatesMaterialTagValue() {
            when(topicRepository.findTagValueById(VALUE_ID)).thenReturn(Optional.of(ownedValue()));
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(ownedGroup()));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsTagValueByGroupIdAndValue(GROUP_ID, "方案设计")).thenReturn(false);

            UserTagValue result = tagSetService.updateTagValue(USER_ID, VALUE_ID, "方案设计", "#FFFFFF", 9);

            assertThat(result)
                    .returns("方案设计", UserTagValue::getValue)
                    .returns("#FFFFFF", UserTagValue::getColor)
                    .returns(9, UserTagValue::getSortOrder);
            verify(topicRepository).updateMaterialTagValue(GROUP_ID, "需求分析", "方案设计");
            verify(topicRepository).updateTagValue(result);
        }

        @Test
        @DisplayName("标签值已被资料引用时，拒绝删除")
        void givenReferencedValue_whenDeleteTagValue_thenThrowsBusinessConflict() {
            when(topicRepository.findTagValueById(VALUE_ID)).thenReturn(Optional.of(ownedValue()));
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(ownedGroup()));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.countMaterialReferencesByValue(GROUP_ID, "需求分析")).thenReturn(2L);

            assertThatThrownBy(() -> tagSetService.deleteTagValue(USER_ID, VALUE_ID))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).deleteTagValue(any());
        }

        @Test
        @DisplayName("查询标签值时，仅返回归属标签组下的值")
        void givenOwnedGroup_whenListTagValues_thenReturnsValues() {
            when(topicRepository.findTagGroupById(GROUP_ID)).thenReturn(Optional.of(ownedGroup()));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.findTagValuesByGroupId(GROUP_ID)).thenReturn(List.of(ownedValue()));

            List<UserTagValue> result = tagSetService.listTagValues(USER_ID, GROUP_ID);

            assertThat(result)
                    .hasSize(1)
                    .first()
                    .returns("需求分析", UserTagValue::getValue);
        }
    }

    @Nested
    @DisplayName("归属校验")
    class Ownership {

        @Test
        @DisplayName("操作他人主题时，抛出无权限异常")
        void givenOtherUserTopic_whenCreateTagGroup_thenThrowsForbidden() {
            Topic topic = ownedTopic();
            topic.setUserId(999L);
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));

            assertThatThrownBy(() -> tagSetService.createTagGroup(USER_ID, TOPIC_ID, "阶段", true, false, 0))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        }
    }

    private Topic ownedTopic() {
        return Topic.builder()
                .id(TOPIC_ID)
                .userId(USER_ID)
                .name("后端")
                .status(1)
                .build();
    }

    private UserTagGroup ownedGroup() {
        return UserTagGroup.builder()
                .id(GROUP_ID)
                .topicId(TOPIC_ID)
                .name("阶段")
                .exclusive(true)
                .required(false)
                .sortOrder(0)
                .build();
    }

    private UserTagValue ownedValue() {
        return UserTagValue.builder()
                .id(VALUE_ID)
                .groupId(GROUP_ID)
                .value("需求分析")
                .sortOrder(0)
                .build();
    }
}
