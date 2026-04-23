package com.anbuz.test.web;

import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.service.ITagSetService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.trigger.http.GlobalExceptionHandler;
import com.anbuz.trigger.http.TagGroupController;
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

import java.time.LocalDateTime;
import java.util.List;

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
@DisplayName("TagGroupController MockMvc scenarios")
class TagGroupControllerWebMvcTest {

    private static final String TEST_USER_HEADER = "X-Test-UserId";

    @Mock
    private ITagSetService tagSetService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TagGroupController tagGroupController = new TagGroupController(tagSetService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(tagGroupController)
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
    @DisplayName("tag group query")
    class TagGroupQuery {

        @Test
        @DisplayName("returns system and user tag groups")
        void givenTopicId_whenList_thenReturnsSystemAndUserGroups() throws Exception {
            when(tagSetService.listTagGroups(1L, 10L)).thenReturn(List.of(group()));
            when(tagSetService.listTagValues(1L, 20L)).thenReturn(List.of(value()));

            mockMvc.perform(get("/api/v1/topics/10/tag-groups")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data[0].group.tagGroupKey").value("sys_score_range"))
                    .andExpect(jsonPath("$.data[2].group.tagGroupKey").value("20"))
                    .andExpect(jsonPath("$.data[2].values[0].value").value("analysis"));
        }
    }

    @Nested
    @DisplayName("tag group validation")
    class TagGroupValidation {

        @Test
        @DisplayName("returns the created group when the request is valid")
        void givenValidRequest_whenCreateTagGroup_thenReturnsGroupPayload() throws Exception {
            when(tagSetService.createTagGroup(1L, 10L, "phase", true, false, 3)).thenReturn(group());

            mockMvc.perform(post("/api/v1/topics/10/tag-groups")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"phase","exclusive":true,"required":false,"sortOrder":3}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(20))
                    .andExpect(jsonPath("$.data.tagGroupKey").value("20"));
        }

        @Test
        @DisplayName("returns param invalid when required exclusive flag is missing")
        void givenMissingExclusive_whenCreateTagGroup_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/topics/10/tag-groups")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"phase","required":false}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(tagSetService);
        }

        @Test
        @DisplayName("returns param invalid when group color is not HEX")
        void givenInvalidColor_whenUpdateTagGroup_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(put("/api/v1/topics/10/tag-groups/20")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"color":"red"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(tagSetService);
        }

        @Test
        @DisplayName("returns the updated group when the request is valid")
        void givenValidRequest_whenUpdateTagGroup_thenReturnsGroupPayload() throws Exception {
            UserTagGroup updated = group();
            updated.setName("workflow");
            when(tagSetService.listTagGroups(1L, 10L)).thenReturn(List.of(group()));
            when(tagSetService.updateTagGroup(1L, 20L, "workflow", null, null, null, null)).thenReturn(updated);

            mockMvc.perform(put("/api/v1/topics/10/tag-groups/20")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"workflow"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("workflow"));
        }

        @Test
        @DisplayName("returns success when deleting a tag group")
        void givenGroupId_whenDeleteTagGroup_thenReturnsSuccessPayload() throws Exception {
            when(tagSetService.listTagGroups(1L, 10L)).thenReturn(List.of(group()));

            mockMvc.perform(delete("/api/v1/topics/10/tag-groups/20")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(tagSetService).deleteTagGroup(1L, 20L);
        }
    }

    @Nested
    @DisplayName("tag value validation")
    class TagValueValidation {

        @Test
        @DisplayName("returns the created value when the request is valid")
        void givenValidRequest_whenCreateTagValue_thenReturnsValuePayload() throws Exception {
            when(tagSetService.addTagValue(1L, 20L, "analysis", "#00AAFF", 1)).thenReturn(value());

            mockMvc.perform(post("/api/v1/tag-groups/20/values")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"value":"analysis","color":"#00AAFF","sortOrder":1}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(30))
                    .andExpect(jsonPath("$.data.value").value("analysis"));
        }

        @Test
        @DisplayName("returns param invalid when tag value is blank")
        void givenBlankValue_whenCreateTagValue_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/tag-groups/20/values")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"value":""}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(tagSetService);
        }

        @Test
        @DisplayName("returns the updated value when the request is valid")
        void givenValidRequest_whenUpdateTagValue_thenReturnsValuePayload() throws Exception {
            UserTagValue updated = value();
            updated.setValue("architecture");
            when(tagSetService.listTagValues(1L, 20L)).thenReturn(List.of(value()));
            when(tagSetService.updateTagValue(1L, 30L, "architecture", null, null)).thenReturn(updated);

            mockMvc.perform(put("/api/v1/tag-groups/20/values/30")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"value":"architecture"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.value").value("architecture"));
        }

        @Test
        @DisplayName("returns success when deleting a tag value")
        void givenValueId_whenDeleteTagValue_thenReturnsSuccessPayload() throws Exception {
            when(tagSetService.listTagValues(1L, 20L)).thenReturn(List.of(value()));

            mockMvc.perform(delete("/api/v1/tag-groups/20/values/30")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(tagSetService).deleteTagValue(1L, 30L);
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
