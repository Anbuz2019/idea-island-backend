package com.anbuz.test.web;

import com.anbuz.domain.material.model.aggregate.MaterialAggregate;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.MaterialPageResult;
import com.anbuz.domain.material.model.valobj.MaterialStatusRecord;
import com.anbuz.domain.material.service.IMaterialService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.trigger.http.GlobalExceptionHandler;
import com.anbuz.trigger.http.MaterialController;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import com.anbuz.types.enums.TagType;
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

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialController MockMvc scenarios")
class MaterialControllerWebMvcTest {

    private static final String TEST_USER_HEADER = "X-Test-UserId";

    @Mock
    private IMaterialService materialService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MaterialController materialController = new MaterialController(materialService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(materialController)
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
    class ListMaterials {

        @Test
        @DisplayName("returns the material page when the request is valid")
        void givenValidRequest_whenList_thenReturnsMaterialPage() throws Exception {
            when(materialService.listMaterials(eq(1L), any()))
                    .thenReturn(MaterialPageResult.builder()
                            .items(List.of(buildAggregate("Redis notes", "tester")))
                            .total(1)
                            .page(1)
                            .pageSize(20)
                            .build());

            mockMvc.perform(get("/api/v1/materials")
                            .header(TEST_USER_HEADER, "1")
                            .param("topicId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.items[0].material.id").value(100))
                    .andExpect(jsonPath("$.data.items[0].material.title").value("Redis notes"))
                    .andExpect(jsonPath("$.data.items[0].meta.author").value("tester"));
        }

        @Test
        @DisplayName("returns param invalid when tagFilters is not valid json")
        void givenMalformedTagFilters_whenList_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/materials")
                            .header(TEST_USER_HEADER, "1")
                            .param("topicId", "1")
                            .param("tagFilters", "["))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(materialService);
        }
    }

    @Nested
    @DisplayName("submit")
    class Submit {

        @Test
        @DisplayName("returns the material id when the request is valid")
        void givenValidRequest_whenSubmit_thenReturnsMaterialId() throws Exception {
            when(materialService.submit(eq(1L), any())).thenReturn(88L);

            mockMvc.perform(post("/api/v1/materials")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "topicId": 1,
                                      "materialType": "article",
                                      "title": "Redis notes",
                                      "sourceUrl": "https://example.com"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value(88));
        }

        @Test
        @DisplayName("returns param invalid when rawContent exceeds the size limit")
        void givenOversizedRawContent_whenSubmit_thenReturnsParamInvalid() throws Exception {
            String rawContent = "a".repeat(50001);

            mockMvc.perform(post("/api/v1/materials")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "topicId": 1,
                                      "materialType": "INPUT",
                                      "rawContent": "%s"
                                    }
                                    """.formatted(rawContent)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(materialService);
        }

        @Test
        @DisplayName("accepts submitted user tags without tag groups")
        void givenUngroupedTag_whenSubmit_thenReturnsMaterialId() throws Exception {
            when(materialService.submit(eq(1L), any())).thenReturn(88L);

            mockMvc.perform(post("/api/v1/materials")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "topicId": 1,
                                      "materialType": "article",
                                      "sourceUrl": "https://example.com",
                                      "tags": [{"tagValue": "analysis"}]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value(88));

            verify(materialService).submit(eq(1L), argThat(command ->
                    command.getTags().size() == 1
                            && command.getTags().get(0).getTagGroupKey() == null
                            && "analysis".equals(command.getTags().get(0).getTagValue())));
        }
    }

    @Nested
    @DisplayName("detail")
    class Detail {

        @Test
        @DisplayName("returns the material detail when the material exists")
        void givenMaterialId_whenDetail_thenReturnsMaterialDetail() throws Exception {
            when(materialService.getDetail(1L, 100L)).thenReturn(buildAggregate("Redis notes", "tester"));

            mockMvc.perform(get("/api/v1/materials/100")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.material.id").value(100))
                    .andExpect(jsonPath("$.data.material.title").value("Redis notes"))
                    .andExpect(jsonPath("$.data.meta.author").value("tester"));
        }

        @Test
        @DisplayName("returns null tagGroupKey for ungrouped user tags")
        void givenUngroupedUserTag_whenDetail_thenReturnsNullTagGroupKey() throws Exception {
            MaterialAggregate aggregate = buildAggregate("Redis notes", "tester");
            aggregate.setTags(List.of(MaterialTag.builder()
                    .id(300L)
                    .materialId(100L)
                    .tagType(TagType.USER)
                    .tagGroupKey(MaterialTag.UNGROUPED_USER_TAG_GROUP_KEY)
                    .tagValue("quick-note")
                    .build()));
            when(materialService.getDetail(1L, 100L)).thenReturn(aggregate);

            mockMvc.perform(get("/api/v1/materials/100")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.tags[0].tagGroupKey").value(nullValue()))
                    .andExpect(jsonPath("$.data.tags[0].tagValue").value("quick-note"));
        }
    }

    @Nested
    @DisplayName("update basic")
    class UpdateBasic {

        @Test
        @DisplayName("returns the updated detail when the request is valid")
        void givenValidRequest_whenUpdateBasic_thenReturnsUpdatedDetail() throws Exception {
            when(materialService.updateBasic(eq(1L), eq(100L), any()))
                    .thenReturn(buildAggregate("Updated Redis notes", "tester"));

            mockMvc.perform(patch("/api/v1/materials/100")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"Updated Redis notes","sourceUrl":"https://example.com/updated"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.material.title").value("Updated Redis notes"));
        }
    }

    @Nested
    @DisplayName("update meta")
    class UpdateMeta {

        @Test
        @DisplayName("returns the updated meta when the request is valid")
        void givenValidRequest_whenUpdateMeta_thenReturnsUpdatedDetail() throws Exception {
            when(materialService.updateMeta(eq(1L), eq(100L), any()))
                    .thenReturn(buildAggregate("Redis notes", "updated-author"));

            mockMvc.perform(patch("/api/v1/materials/100/meta")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"author":"updated-author","sourcePlatform":"newsletter"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.meta.author").value("updated-author"));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteMaterial {

        @Test
        @DisplayName("returns success when delete completes")
        void givenMaterialId_whenDelete_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(delete("/api/v1/materials/100")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(materialService).deleteMaterial(1L, 100L);
        }
    }

    @Nested
    @DisplayName("mark read")
    class MarkRead {

        @Test
        @DisplayName("returns success when mark read completes")
        void givenMaterialId_whenMarkRead_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(post("/api/v1/materials/100/mark-read")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(materialService).markRead(1L, 100L);
        }
    }

    @Nested
    @DisplayName("collect")
    class Collect {

        @Test
        @DisplayName("returns success when the request is valid")
        void givenValidRequest_whenCollect_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(post("/api/v1/materials/100/collect")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"comment":"good material","score":8.5}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(materialService).collect(1L, 100L, "good material", new BigDecimal("8.5"));
        }

        @Test
        @DisplayName("returns param invalid when score has more than one decimal place")
        void givenScoreWithTwoDecimals_whenCollect_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/materials/100/collect")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"comment":"good material","score":8.55}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(materialService);
        }
    }

    @Nested
    @DisplayName("archive")
    class Archive {

        @Test
        @DisplayName("returns success when archive completes")
        void givenMaterialId_whenArchive_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(post("/api/v1/materials/100/archive")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(materialService).archive(1L, 100L);
        }
    }

    @Nested
    @DisplayName("invalidate")
    class Invalidate {

        @Test
        @DisplayName("returns success when the request is valid")
        void givenValidRequest_whenInvalidate_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(post("/api/v1/materials/100/invalidate")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"invalidReason":"outdated"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(materialService).invalidate(1L, 100L, "outdated");
        }

        @Test
        @DisplayName("returns param invalid when invalidReason exceeds the size limit")
        void givenOversizedInvalidReason_whenInvalidate_thenReturnsParamInvalid() throws Exception {
            String invalidReason = "r".repeat(501);

            mockMvc.perform(post("/api/v1/materials/100/invalidate")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"invalidReason":"%s"}
                                    """.formatted(invalidReason)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(materialService);
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        @DisplayName("returns success when restoring to inbox")
        void givenMaterialId_whenRestore_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(post("/api/v1/materials/100/restore")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(materialService).restore(1L, 100L);
        }

        @Test
        @DisplayName("returns success when restoring to collected")
        void givenMaterialId_whenRestoreCollected_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(post("/api/v1/materials/100/restore-collected")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(materialService).restoreCollected(1L, 100L);
        }
    }

    @Nested
    @DisplayName("update tags")
    class UpdateTags {

        @Test
        @DisplayName("returns success when tag items are valid")
        void givenValidTagItems_whenUpdateTags_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(put("/api/v1/materials/100/tags")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tags":[{"tagGroupKey":"11","tagValue":"analysis"}]}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(materialService).updateTags(eq(1L), eq(100L), argThat(tags ->
                    tags.size() == 1
                            && "11".equals(tags.get(0).getTagGroupKey())
                            && "analysis".equals(tags.get(0).getTagValue())));
        }

        @Test
        @DisplayName("accepts ungrouped user tags when replacing material tags")
        void givenUngroupedTag_whenUpdateTags_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(put("/api/v1/materials/100/tags")
                            .header(TEST_USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tags":[{"tagValue":"analysis"}]}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(materialService).updateTags(eq(1L), eq(100L), argThat(tags ->
                    tags.size() == 1
                            && tags.get(0).getTagGroupKey() == null
                            && "analysis".equals(tags.get(0).getTagValue())));
        }
    }

    private Filter testUserContextFilter() {
        return (request, response, chain) -> {
            String userId = request.getParameter(TEST_USER_HEADER);
            if (userId == null && request instanceof jakarta.servlet.http.HttpServletRequest httpServletRequest) {
                userId = httpServletRequest.getHeader(TEST_USER_HEADER);
            }

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

    private MaterialAggregate buildAggregate(String title, String author) {
        return MaterialAggregate.builder()
                .material(Material.builder()
                        .id(100L)
                        .userId(1L)
                        .topicId(10L)
                        .materialType(MaterialType.ARTICLE)
                        .status(MaterialStatus.INBOX)
                        .title(title)
                        .description("cache notes")
                        .rawContent("redis raw content")
                        .sourceUrl("https://example.com")
                        .build())
                .meta(MaterialMeta.builder()
                        .id(200L)
                        .materialId(100L)
                        .author(author)
                        .sourcePlatform("wechat")
                        .wordCount(1000)
                        .thumbnailKey("covers/material-100.png")
                        .build())
                .tags(List.of(MaterialTag.builder()
                        .id(300L)
                        .materialId(100L)
                        .tagType(TagType.USER)
                        .tagGroupKey("11")
                        .tagValue("backend")
                        .build()))
                .statusHistory(List.of(MaterialStatusRecord.builder()
                        .status(MaterialStatus.INBOX.getCode())
                        .label("Inbox")
                        .occurredAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                        .build()))
                .build();
    }
}
