package com.anbuz.trigger.http;

import com.anbuz.api.http.ITopicController;
import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.valobj.TopicStats;
import com.anbuz.domain.topic.service.ITopicService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopicController scenarios")
class TopicControllerTest {

    @Mock
    private ITopicService topicService;

    @InjectMocks
    private TopicController topicController;

    @Nested
    @DisplayName("topic management")
    class TopicManagement {

        @Test
        @DisplayName("delegates create with the current user")
        void givenCreateRequest_whenCreate_thenDelegatesWithCurrentUser() {
            ITopicController.CreateTopicRequest request = new ITopicController.CreateTopicRequest();
            request.setName("backend");
            request.setDescription("backend knowledge");

            UserContext.set(1L);
            try {
                when(topicService.createTopic(1L, "backend", "backend knowledge")).thenReturn(topic());

                Result<ITopicController.TopicResponse> result = topicController.create(request);

                assertThat(result).returns(0, Result::getCode);
                assertThat(result.getData())
                        .extracting(ITopicController.TopicResponse::id,
                                ITopicController.TopicResponse::userId,
                                ITopicController.TopicResponse::name)
                        .containsExactly(10L, 1L, "backend");
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("returns the current user's topic list")
        void givenCurrentUser_whenList_thenReturnsTopics() {
            UserContext.set(1L);
            try {
                when(topicService.listTopics(1L)).thenReturn(List.of(topic()));

                Result<List<ITopicController.TopicResponse>> result = topicController.list();

                assertThat(result.getData())
                        .singleElement()
                        .returns("backend", ITopicController.TopicResponse::name);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("returns the topic detail")
        void givenTopicId_whenDetail_thenReturnsTopicDetail() {
            UserContext.set(1L);
            try {
                when(topicService.getTopic(10L, 1L)).thenReturn(topic());

                Result<ITopicController.TopicResponse> result = topicController.detail(10L);

                assertThat(result.getData())
                        .returns(10L, ITopicController.TopicResponse::id)
                        .returns("backend", ITopicController.TopicResponse::name);
                verify(topicService).getTopic(10L, 1L);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("delegates update with path id and current user")
        void givenUpdateRequest_whenUpdate_thenDelegatesWithCurrentUser() {
            ITopicController.UpdateTopicRequest request = new ITopicController.UpdateTopicRequest();
            request.setName("architecture");
            request.setDescription("system design");

            Topic updated = topic();
            updated.setName("architecture");
            updated.setDescription("system design");

            UserContext.set(1L);
            try {
                when(topicService.updateTopic(10L, 1L, "architecture", "system design")).thenReturn(updated);

                Result<ITopicController.TopicResponse> result = topicController.update(10L, request);

                assertThat(result.getData())
                        .returns("architecture", ITopicController.TopicResponse::name)
                        .returns("system design", ITopicController.TopicResponse::description);
                verify(topicService).updateTopic(10L, 1L, "architecture", "system design");
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("delegates disable with topic id and current user")
        void givenTopicId_whenDisable_thenDelegates() {
            UserContext.set(2L);
            try {
                Result<Void> result = topicController.disable(10L);

                assertThat(result).returns(0, Result::getCode);
                verify(topicService).disableTopic(10L, 2L);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("delegates enable with topic id and current user")
        void givenTopicId_whenEnable_thenDelegates() {
            UserContext.set(2L);
            try {
                Result<Void> result = topicController.enable(10L);

                assertThat(result).returns(0, Result::getCode);
                verify(topicService).enableTopic(10L, 2L);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("delegates delete with topic id and current user")
        void givenTopicId_whenDelete_thenDelegates() {
            UserContext.set(2L);
            try {
                Result<Void> result = topicController.delete(10L);

                assertThat(result).returns(0, Result::getCode);
                verify(topicService).deleteTopic(10L, 2L);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("topic stats")
    class TopicStatsQuery {

        @Test
        @DisplayName("maps the domain stats into the response")
        void givenTopicId_whenStats_thenReturnsStatsResponse() {
            UserContext.set(1L);
            try {
                TopicStats stats = TopicStats.builder()
                        .totalMaterials(12L)
                        .statusCounts(Map.of(MaterialStatus.INBOX.getCode(), 4L))
                        .typeCounts(Map.of("article", 6L))
                        .weeklyNew(3L)
                        .averageScore(new BigDecimal("7.5"))
                        .pendingCount(5L)
                        .build();
                when(topicService.getTopicStats(10L, 1L)).thenReturn(stats);

                Result<ITopicController.TopicStatsResponse> result = topicController.stats(10L);

                assertThat(result.getData())
                        .returns(12L, ITopicController.TopicStatsResponse::totalMaterials)
                        .returns(3L, ITopicController.TopicStatsResponse::weeklyNew)
                        .returns(new BigDecimal("7.5"), ITopicController.TopicStatsResponse::averageScore)
                        .returns(5L, ITopicController.TopicStatsResponse::pendingCount)
                        .satisfies(response -> assertThat(response.statusCounts()).containsEntry(MaterialStatus.INBOX.getCode(), 4L))
                        .satisfies(response -> assertThat(response.typeCounts()).containsEntry("article", 6L));
            } finally {
                UserContext.clear();
            }
        }
    }

    private Topic topic() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 10, 0);
        return Topic.builder()
                .id(10L)
                .userId(1L)
                .name("backend")
                .description("backend knowledge")
                .status(1)
                .materialCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
