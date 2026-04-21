package com.anbuz.domain.topic.service;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopicService 主题领域服务")
class TopicServiceTest {

    @Mock
    private ITopicRepository topicRepository;

    @InjectMocks
    private TopicService topicService;

    private static final Long USER_ID = 1L;
    private static final Long TOPIC_ID = 10L;

    @Nested
    @DisplayName("创建主题")
    class CreateTopic {

        @Test
        @DisplayName("主题名称不重复，创建成功并返回新主题")
        void givenUniqueName_whenCreate_thenTopicCreated() {
            when(topicRepository.existsByUserIdAndName(USER_ID, "后端")).thenReturn(false);

            Topic result = topicService.createTopic(USER_ID, "后端", "后端相关资料");

            assertThat(result.getName()).isEqualTo("后端");
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getStatus()).isEqualTo(1);
            verify(topicRepository).saveTopic(any());
        }

        @Test
        @DisplayName("主题名称已存在，应抛出业务冲突异常")
        void givenDuplicateName_whenCreate_thenThrowsBusinessConflict() {
            when(topicRepository.existsByUserIdAndName(USER_ID, "后端")).thenReturn(true);

            assertThatThrownBy(() -> topicService.createTopic(USER_ID, "后端", null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(topicRepository, never()).saveTopic(any());
        }
    }

    @Nested
    @DisplayName("删除主题")
    class DeleteTopic {

        @Test
        @DisplayName("主题下有资料时，不允许删除")
        void givenTopicWithMaterials_whenDelete_thenThrowsBusinessConflict() {
            Topic topic = Topic.builder()
                    .id(TOPIC_ID).userId(USER_ID).name("测试").materialCount(5).build();
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));

            assertThatThrownBy(() -> topicService.deleteTopic(TOPIC_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());
        }

        @Test
        @DisplayName("主题下无资料时，允许删除")
        void givenEmptyTopic_whenDelete_thenDeleteSucceeds() {
            Topic topic = Topic.builder()
                    .id(TOPIC_ID).userId(USER_ID).name("测试").materialCount(0).build();
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));

            topicService.deleteTopic(TOPIC_ID, USER_ID);

            verify(topicRepository).deleteTopic(TOPIC_ID);
        }

        @Test
        @DisplayName("操作他人主题，应抛出无权限异常")
        void givenOtherUserTopic_whenDelete_thenThrowsForbidden() {
            Topic topic = Topic.builder()
                    .id(TOPIC_ID).userId(999L).name("他人主题").materialCount(0).build();
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));

            assertThatThrownBy(() -> topicService.deleteTopic(TOPIC_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        }
    }

}
