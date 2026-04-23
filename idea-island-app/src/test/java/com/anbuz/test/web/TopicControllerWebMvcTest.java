package com.anbuz.test.web;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.valobj.TopicStats;
import com.anbuz.domain.topic.service.ITopicService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.trigger.http.GlobalExceptionHandler;
import com.anbuz.trigger.http.TopicController;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.model.ErrorCode;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopicController MockMvc scenarios")
class TopicControllerWebMvcTest {

    private static final String TEST_USER_HEADER = "X-Test-UserId";

    @Mock
    private ITopicService topicService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TopicController topicController = new TopicController(topicService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(topicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addFilters(testUserContextFilter())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested
    @DisplayName("list")
    class ListTopics {

        @Test
        @DisplayName("returns the current user's topics")
        void givenCurrentUser_whenList_thenReturnsTopicPayload() throws Exception {
            when(topicService.listTopics(1L)).thenReturn(List.of(topic()));

            mockMvc.perform(get("/api/v1/topics")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data[0].id").value(10))
                    .andExpect(jsonPath("$.data[0].name").value("backend"));
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTopic {

        @Test
        @DisplayName("returns success payload when request is valid")
        void givenValidRequest_whenCreate_thenReturnsTopicPayload() throws Exception {
            when(topicService.createTopic(1L, "backend", "backend knowledge")).thenReturn(topic());

            mockMvc.perform(post("/api/v1/topics")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"backend","description":"backend knowledge"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.name").value("backend"));
        }

        @Test
        @DisplayName("returns param invalid when topic name exceeds the limit")
        void givenOversizedName_whenCreate_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/topics")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(topicService);
        }
    }

    @Nested
    @DisplayName("detail")
    class DetailTopic {

        @Test
        @DisplayName("returns the topic detail when the topic exists")
        void givenTopicId_whenDetail_thenReturnsTopicPayload() throws Exception {
            when(topicService.getTopic(10L, 1L)).thenReturn(topic());

            mockMvc.perform(get("/api/v1/topics/10")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.name").value("backend"));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTopic {

        @Test
        @DisplayName("returns the updated topic when the request is valid")
        void givenValidRequest_whenUpdate_thenReturnsTopicPayload() throws Exception {
            Topic updated = topic();
            updated.setName("architecture");
            updated.setDescription("system design");
            when(topicService.updateTopic(10L, 1L, "architecture", "system design")).thenReturn(updated);

            mockMvc.perform(put("/api/v1/topics/10")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"architecture","description":"system design"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("architecture"))
                    .andExpect(jsonPath("$.data.description").value("system design"));
        }
    }

    @Nested
    @DisplayName("status flow")
    class StatusFlow {

        @Test
        @DisplayName("returns success when disabling a topic")
        void givenTopicId_whenDisable_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(post("/api/v1/topics/10/disable")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(topicService).disableTopic(10L, 1L);
        }

        @Test
        @DisplayName("returns success when enabling a topic")
        void givenTopicId_whenEnable_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(post("/api/v1/topics/10/enable")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(topicService).enableTopic(10L, 1L);
        }

        @Test
        @DisplayName("returns success when deleting a topic")
        void givenTopicId_whenDelete_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(delete("/api/v1/topics/10")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(topicService).deleteTopic(10L, 1L);
        }
    }

    @Nested
    @DisplayName("stats")
    class TopicStatsQuery {

        @Test
        @DisplayName("returns the topic stats payload")
        void givenTopicId_whenStats_thenReturnsStatsPayload() throws Exception {
            when(topicService.getTopicStats(10L, 1L)).thenReturn(TopicStats.builder()
                    .totalMaterials(12L)
                    .statusCounts(Map.of(MaterialStatus.INBOX.getCode(), 4L))
                    .typeCounts(Map.of("article", 6L))
                    .weeklyNew(3L)
                    .averageScore(new BigDecimal("7.5"))
                    .pendingCount(5L)
                    .build());

            mockMvc.perform(get("/api/v1/topics/10/stats")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.totalMaterials").value(12))
                    .andExpect(jsonPath("$.data.weeklyNew").value(3))
                    .andExpect(jsonPath("$.data.averageScore").value(7.5))
                    .andExpect(jsonPath("$.data.pendingCount").value(5))
                    .andExpect(jsonPath("$.data.statusCounts.INBOX").value(4))
                    .andExpect(jsonPath("$.data.typeCounts.article").value(6));
        }
    }

    private Filter testUserContextFilter() {
        return (request, response, chain) -> {
            String userId = request instanceof jakarta.servlet.http.HttpServletRequest httpServletRequest
                    ? httpServletRequest.getHeader(TEST_USER_HEADER)
                    : null;
            if (userId != null && !userId.isBlank()) {
                UserContext.set(Long.parseLong(userId));
            }
            try {
                chain.doFilter(request, response);
            } finally {
                UserContext.clear();
            }
        };
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
