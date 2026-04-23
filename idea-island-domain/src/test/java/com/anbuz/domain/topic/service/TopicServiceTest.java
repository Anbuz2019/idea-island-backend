package com.anbuz.domain.topic.service;

import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.TopicAutoInvalidRule;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.valobj.TopicStats;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.domain.topic.service.impl.TopicService;
import com.anbuz.types.enums.AutoInvalidRuleType;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopicService 主题领域服务")
class TopicServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TOPIC_ID = 10L;

    @Mock
    private ITopicRepository topicRepository;

    @Mock
    private IMaterialRepository materialRepository;

    @InjectMocks
    private TopicService topicService;

    @Nested
    @DisplayName("创建主题")
    class CreateTopic {

        @Test
        @DisplayName("主题名称不重复且未达上限，创建主题并初始化自动失效规则")
        void givenUniqueNameAndBelowLimit_whenCreateTopic_thenCreatesTopicAndDefaultRules() {
            when(topicRepository.existsByUserIdAndName(USER_ID, "后端")).thenReturn(false);
            when(topicRepository.countTopicsByUserId(USER_ID)).thenReturn(3L);
            doAnswer(invocation -> {
                Topic topic = invocation.getArgument(0);
                topic.setId(TOPIC_ID);
                return null;
            }).when(topicRepository).saveTopic(any(Topic.class));

            Topic result = topicService.createTopic(USER_ID, "后端", "后端相关资料");

            assertThat(result)
                    .returns(TOPIC_ID, Topic::getId)
                    .returns(USER_ID, Topic::getUserId)
                    .returns("后端", Topic::getName)
                    .returns(1, Topic::getStatus)
                    .returns(0, Topic::getMaterialCount)
                    .satisfies(topic -> assertThat(topic.getCreatedAt()).isNotNull())
                    .satisfies(topic -> assertThat(topic.getUpdatedAt()).isNotNull());

            ArgumentCaptor<TopicAutoInvalidRule> ruleCaptor = ArgumentCaptor.forClass(TopicAutoInvalidRule.class);
            verify(topicRepository).saveTopic(any(Topic.class));
            verify(topicRepository, org.mockito.Mockito.times(2)).saveAutoInvalidRule(ruleCaptor.capture());
            assertThat(ruleCaptor.getAllValues())
                    .extracting(TopicAutoInvalidRule::getRuleType)
                    .containsExactly(AutoInvalidRuleType.INBOX_TIMEOUT.getCode(),
                            AutoInvalidRuleType.PENDING_REVIEW_TIMEOUT.getCode());
            assertThat(ruleCaptor.getAllValues())
                    .extracting(TopicAutoInvalidRule::getThresholdDays)
                    .containsExactly(90, 60);
        }

        @Test
        @DisplayName("主题名称已存在时，拒绝创建")
        void givenDuplicateName_whenCreateTopic_thenThrowsBusinessConflict() {
            when(topicRepository.existsByUserIdAndName(USER_ID, "后端")).thenReturn(true);

            assertThatThrownBy(() -> topicService.createTopic(USER_ID, "后端", null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).countTopicsByUserId(USER_ID);
            verify(topicRepository, never()).saveTopic(any());
        }

        @Test
        @DisplayName("用户主题数量达到 50 个时，拒绝继续创建")
        void givenTopicLimitReached_whenCreateTopic_thenThrowsBusinessConflict() {
            when(topicRepository.existsByUserIdAndName(USER_ID, "新主题")).thenReturn(false);
            when(topicRepository.countTopicsByUserId(USER_ID)).thenReturn(50L);

            assertThatThrownBy(() -> topicService.createTopic(USER_ID, "新主题", null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).saveTopic(any());
            verify(topicRepository, never()).saveAutoInvalidRule(any());
        }
    }

    @Nested
    @DisplayName("编辑主题")
    class UpdateTopic {

        @Test
        @DisplayName("传入新名称和描述时，更新主题")
        void givenOwnedTopic_whenUpdateTopic_thenUpdatesMutableFields() {
            Topic topic = ownedTopic();
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(topicRepository.existsByUserIdAndName(USER_ID, "架构")).thenReturn(false);

            Topic result = topicService.updateTopic(TOPIC_ID, USER_ID, "架构", "架构资料");

            assertThat(result)
                    .returns("架构", Topic::getName)
                    .returns("架构资料", Topic::getDescription)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
            verify(topicRepository).updateTopic(topic);
        }

        @Test
        @DisplayName("改名命中同用户重复名称时，拒绝更新")
        void givenDuplicateNewName_whenUpdateTopic_thenThrowsBusinessConflict() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.existsByUserIdAndName(USER_ID, "架构")).thenReturn(true);

            assertThatThrownBy(() -> topicService.updateTopic(TOPIC_ID, USER_ID, "架构", null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).updateTopic(any());
        }
    }

    @Nested
    @DisplayName("启停主题")
    class ToggleTopic {

        @Test
        @DisplayName("停用主题时，将状态置为 0")
        void givenOwnedTopic_whenDisableTopic_thenStatusBecomesDisabled() {
            Topic topic = ownedTopic();
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));

            topicService.disableTopic(TOPIC_ID, USER_ID);

            assertThat(topic)
                    .returns(0, Topic::getStatus)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
            verify(topicRepository).updateTopic(topic);
        }

        @Test
        @DisplayName("启用主题时，将状态置为 1")
        void givenOwnedTopic_whenEnableTopic_thenStatusBecomesEnabled() {
            Topic topic = ownedTopic();
            topic.setStatus(0);
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));

            topicService.enableTopic(TOPIC_ID, USER_ID);

            assertThat(topic)
                    .returns(1, Topic::getStatus)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
            verify(topicRepository).updateTopic(topic);
        }
    }

    @Nested
    @DisplayName("删除主题")
    class DeleteTopic {

        @Test
        @DisplayName("主题下有资料时，不允许删除")
        void givenTopicWithMaterials_whenDeleteTopic_thenThrowsBusinessConflict() {
            Topic topic = ownedTopic();
            topic.setMaterialCount(5);
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));

            assertThatThrownBy(() -> topicService.deleteTopic(TOPIC_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).deleteTopic(any());
        }

        @Test
        @DisplayName("主题无资料时，删除主题、标签集和自动失效规则")
        void givenEmptyTopicWithTagGroups_whenDeleteTopic_thenDeletesTopicAndChildren() {
            UserTagGroup group = UserTagGroup.builder().id(20L).topicId(TOPIC_ID).name("阶段").build();
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(topicRepository.findTagGroupsByTopicId(TOPIC_ID)).thenReturn(List.of(group));

            topicService.deleteTopic(TOPIC_ID, USER_ID);

            verify(topicRepository).deleteTagValuesByGroupId(20L);
            verify(topicRepository).deleteTagGroup(20L);
            verify(topicRepository).deleteAutoInvalidRulesByTopicId(TOPIC_ID);
            verify(topicRepository).deleteTopic(TOPIC_ID);
        }

        @Test
        @DisplayName("操作他人主题时，抛出无权限异常")
        void givenOtherUserTopic_whenDeleteTopic_thenThrowsForbidden() {
            Topic topic = ownedTopic();
            topic.setUserId(999L);
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));

            assertThatThrownBy(() -> topicService.deleteTopic(TOPIC_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        }
    }

    @Nested
    @DisplayName("查询与统计")
    class QueryTopic {

        @Test
        @DisplayName("查询主题列表时，返回当前用户主题")
        void givenUserId_whenListTopics_thenReturnsUserTopics() {
            when(topicRepository.findTopicsByUserId(USER_ID)).thenReturn(List.of(ownedTopic()));

            List<Topic> result = topicService.listTopics(USER_ID);

            assertThat(result)
                    .hasSize(1)
                    .first()
                    .returns(TOPIC_ID, Topic::getId);
        }

        @Test
        @DisplayName("查询主题统计时，聚合资料数量、状态、类型、周新增、均分和待处理数")
        void givenOwnedTopic_whenGetTopicStats_thenAggregatesMaterialStats() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(ownedTopic()));
            when(materialRepository.countMaterials(any(MaterialListQuery.class))).thenReturn(12L, 3L, 4L);
            when(materialRepository.countByStatus(USER_ID, TOPIC_ID)).thenReturn(Map.of(MaterialStatus.INBOX.getCode(), 4L));
            when(materialRepository.countByMaterialType(TOPIC_ID)).thenReturn(Map.of("article", 6L));
            when(materialRepository.averageScoreByTopicId(TOPIC_ID)).thenReturn(new BigDecimal("7.5"));

            TopicStats result = topicService.getTopicStats(TOPIC_ID, USER_ID);

            assertThat(result)
                    .returns(12L, TopicStats::getTotalMaterials)
                    .returns(3L, TopicStats::getWeeklyNew)
                    .returns(4L, TopicStats::getPendingCount)
                    .returns(new BigDecimal("7.5"), TopicStats::getAverageScore)
                    .satisfies(stats -> assertThat(stats.getStatusCounts()).containsEntry(MaterialStatus.INBOX.getCode(), 4L))
                    .satisfies(stats -> assertThat(stats.getTypeCounts()).containsEntry("article", 6L));
        }

        @Test
        @DisplayName("主题不存在时，抛出资源不存在异常")
        void givenMissingTopic_whenGetTopic_thenThrowsNotFound() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> topicService.getTopic(TOPIC_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOT_FOUND.getCode());
        }
    }

    private Topic ownedTopic() {
        return Topic.builder()
                .id(TOPIC_ID)
                .userId(USER_ID)
                .name("后端")
                .description("后端相关资料")
                .status(1)
                .materialCount(0)
                .build();
    }
}
