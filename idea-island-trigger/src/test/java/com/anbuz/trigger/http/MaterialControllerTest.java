package com.anbuz.trigger.http;

import com.anbuz.api.http.IMaterialController;
import com.anbuz.domain.material.model.aggregate.MaterialAggregate;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.model.valobj.MaterialPageResult;
import com.anbuz.domain.material.model.valobj.MaterialStatusRecord;
import com.anbuz.domain.material.service.IMaterialService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import com.anbuz.types.enums.TagType;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialController scenarios")
class MaterialControllerTest {

    @Mock
    private IMaterialService materialService;

    @InjectMocks
    private MaterialController materialController;

    @Nested
    @DisplayName("list materials")
    class ListMaterials {

        @Test
        @DisplayName("parses tag filters and maps the query into the domain service")
        void givenValidRequest_whenList_thenMapsQueryAndReturnsPage() {
            IMaterialController.ListMaterialsRequest request = new IMaterialController.ListMaterialsRequest();
            request.setTopicId(10L);
            request.setStatus(List.of("INBOX"));
            request.setMaterialType(List.of("article"));
            request.setKeyword("redis");
            request.setSortBy("score");
            request.setSortDirection("ASC");
            request.setTagFilters("""
                    [{"tagGroupKey":"11","tagValues":["backend","redis"]}]
                    """);
            request.setPage(2);
            request.setPageSize(30);

            when(materialService.listMaterials(eq(1L), any()))
                    .thenReturn(MaterialPageResult.builder()
                            .items(List.of(MaterialAggregate.builder().material(buildMaterial()).build()))
                            .total(1)
                            .page(2)
                            .pageSize(30)
                            .build());

            UserContext.set(1L);
            try {
                Result<IMaterialController.MaterialPageResponse> result = materialController.list(request);

                ArgumentCaptor<MaterialListQuery> captor = ArgumentCaptor.forClass(MaterialListQuery.class);
                verify(materialService).listMaterials(eq(1L), captor.capture());

                assertThat(captor.getValue())
                        .returns(10L, MaterialListQuery::getTopicId)
                        .returns("redis", MaterialListQuery::getKeyword)
                        .returns("score", MaterialListQuery::getSortBy)
                        .returns("ASC", MaterialListQuery::getSortDirection)
                        .returns(2, MaterialListQuery::getPage)
                        .returns(30, MaterialListQuery::getPageSize);
                assertThat(captor.getValue().getTagFilters())
                        .singleElement()
                        .satisfies(tagFilter -> assertThat(tagFilter)
                                .returns("11", MaterialListQuery.TagFilter::getTagGroupKey)
                                .extracting(MaterialListQuery.TagFilter::getTagValues)
                                .asList()
                                .containsExactly("backend", "redis"));
                assertThat(result)
                        .returns(0, Result::getCode)
                        .extracting(Result::getData)
                        .extracting(IMaterialController.MaterialPageResponse::total,
                                IMaterialController.MaterialPageResponse::page,
                                IMaterialController.MaterialPageResponse::pageSize)
                        .containsExactly(1L, 2, 30);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("throws param invalid when tagFilters is malformed")
        void givenMalformedTagFilters_whenList_thenThrowsParamInvalid() {
            IMaterialController.ListMaterialsRequest request = new IMaterialController.ListMaterialsRequest();
            request.setTopicId(10L);
            request.setTagFilters("[");

            UserContext.set(1L);
            try {
                assertThatThrownBy(() -> materialController.list(request))
                        .isInstanceOf(AppException.class)
                        .extracting("code")
                        .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("submit material")
    class Submit {

        @Test
        @DisplayName("maps the request into a submit command and returns the material id")
        void givenValidRequest_whenSubmit_thenDelegatesWithSubmitCommand() {
            IMaterialController.SubmitRequest request = new IMaterialController.SubmitRequest();
            request.setTopicId(10L);
            request.setMaterialType("article");
            request.setTitle("Redis notes");
            request.setDescription("cache");
            request.setSourceUrl("https://example.com");
            request.setAuthor("tester");
            IMaterialController.UpdateTagsRequest.TagItem tag = new IMaterialController.UpdateTagsRequest.TagItem();
            tag.setTagGroupKey("20");
            tag.setTagValue("analysis");
            request.setTags(List.of(tag));

            when(materialService.submit(eq(1L), any())).thenReturn(88L);

            UserContext.set(1L);
            try {
                Result<Long> result = materialController.submit(request);

                ArgumentCaptor<IMaterialService.SubmitCommand> captor =
                        ArgumentCaptor.forClass(IMaterialService.SubmitCommand.class);
                verify(materialService).submit(eq(1L), captor.capture());

                assertThat(captor.getValue())
                        .returns(10L, IMaterialService.SubmitCommand::getTopicId)
                        .returns("article", IMaterialService.SubmitCommand::getMaterialType)
                        .returns("Redis notes", IMaterialService.SubmitCommand::getTitle)
                        .returns("cache", IMaterialService.SubmitCommand::getDescription)
                        .returns("https://example.com", IMaterialService.SubmitCommand::getSourceUrl)
                        .returns("tester", IMaterialService.SubmitCommand::getAuthor);
                assertThat(captor.getValue().getTags())
                        .extracting(IMaterialService.TagInput::getTagGroupKey, IMaterialService.TagInput::getTagValue)
                        .containsExactly(org.assertj.core.groups.Tuple.tuple("20", "analysis"));
                assertThat(result)
                        .returns(0, Result::getCode)
                        .returns(88L, Result::getData);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("accepts ungrouped user tags when submitting a material")
        void givenUngroupedTag_whenSubmit_thenDelegatesWithNullGroupKey() {
            IMaterialController.SubmitRequest request = new IMaterialController.SubmitRequest();
            request.setTopicId(10L);
            request.setMaterialType("article");
            request.setSourceUrl("https://example.com");
            IMaterialController.UpdateTagsRequest.TagItem tag = new IMaterialController.UpdateTagsRequest.TagItem();
            tag.setTagValue("quick-note");
            request.setTags(List.of(tag));

            when(materialService.submit(eq(1L), any())).thenReturn(88L);

            UserContext.set(1L);
            try {
                materialController.submit(request);

                ArgumentCaptor<IMaterialService.SubmitCommand> captor =
                        ArgumentCaptor.forClass(IMaterialService.SubmitCommand.class);
                verify(materialService).submit(eq(1L), captor.capture());
                assertThat(captor.getValue().getTags())
                        .extracting(IMaterialService.TagInput::getTagGroupKey, IMaterialService.TagInput::getTagValue)
                        .containsExactly(org.assertj.core.groups.Tuple.tuple(null, "quick-note"));
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("detail")
    class Detail {

        @Test
        @DisplayName("loads the material aggregate and maps the detail response")
        void givenMaterialId_whenDetail_thenReturnsAggregateDetail() {
            when(materialService.getDetail(1L, 100L)).thenReturn(buildAggregate("Redis notes", "tester"));

            UserContext.set(1L);
            try {
                Result<IMaterialController.MaterialDetailResponse> result = materialController.detail(100L);

                assertThat(result).returns(0, Result::getCode);
                assertThat(result.getData().material())
                        .returns(100L, IMaterialController.MaterialResponse::id)
                        .returns("Redis notes", IMaterialController.MaterialResponse::title)
                        .returns("https://example.com", IMaterialController.MaterialResponse::sourceUrl);
                assertThat(result.getData().meta())
                        .returns("tester", IMaterialController.MaterialMetaResponse::author)
                        .returns("wechat", IMaterialController.MaterialMetaResponse::sourcePlatform);
                assertThat(result.getData().tags())
                        .singleElement()
                        .returns("11", IMaterialController.MaterialTagResponse::tagGroupKey)
                        .returns("backend", IMaterialController.MaterialTagResponse::tagValue);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("maps reserved ungrouped user tag key back to a null api tagGroupKey")
        void givenUngroupedUserTag_whenDetail_thenReturnsNullTagGroupKey() {
            MaterialAggregate aggregate = buildAggregate("Redis notes", "tester");
            aggregate.setTags(List.of(MaterialTag.builder()
                    .id(301L)
                    .materialId(100L)
                    .tagType(TagType.USER)
                    .tagGroupKey(MaterialTag.UNGROUPED_USER_TAG_GROUP_KEY)
                    .tagValue("quick-note")
                    .build()));
            when(materialService.getDetail(1L, 100L)).thenReturn(aggregate);

            UserContext.set(1L);
            try {
                Result<IMaterialController.MaterialDetailResponse> result = materialController.detail(100L);

                assertThat(result.getData().tags())
                        .singleElement()
                        .returns(null, IMaterialController.MaterialTagResponse::tagGroupKey)
                        .returns("quick-note", IMaterialController.MaterialTagResponse::tagValue);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("update basic")
    class UpdateBasic {

        @Test
        @DisplayName("maps the request into an update command and returns the updated detail")
        void givenBasicFields_whenUpdateBasic_thenDelegatesWithUpdateCommand() {
            IMaterialController.UpdateBasicRequest request = new IMaterialController.UpdateBasicRequest();
            request.setTitle("Updated Redis notes");
            request.setRawContent("new content");
            request.setSourceUrl("https://example.com/updated");

            when(materialService.updateBasic(eq(1L), eq(100L), any()))
                    .thenReturn(buildAggregate("Updated Redis notes", "tester"));

            UserContext.set(1L);
            try {
                Result<IMaterialController.MaterialDetailResponse> result = materialController.updateBasic(100L, request);

                ArgumentCaptor<IMaterialService.UpdateBasicCommand> captor =
                        ArgumentCaptor.forClass(IMaterialService.UpdateBasicCommand.class);
                verify(materialService).updateBasic(eq(1L), eq(100L), captor.capture());

                assertThat(captor.getValue())
                        .returns("Updated Redis notes", IMaterialService.UpdateBasicCommand::getTitle)
                        .returns("new content", IMaterialService.UpdateBasicCommand::getRawContent)
                        .returns("https://example.com/updated", IMaterialService.UpdateBasicCommand::getSourceUrl);
                assertThat(result.getData().material())
                        .returns("Updated Redis notes", IMaterialController.MaterialResponse::title);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("update meta")
    class UpdateMeta {

        @Test
        @DisplayName("maps the request into a meta update command and returns the updated detail")
        void givenMetaFields_whenUpdateMeta_thenDelegatesWithUpdateCommand() {
            IMaterialController.UpdateMetaRequest request = new IMaterialController.UpdateMetaRequest();
            request.setAuthor("updated-author");
            request.setSourcePlatform("newsletter");
            request.setWordCount(1200);
            request.setDurationSeconds(360);
            request.setThumbnailKey("covers/material-100.png");
            request.setExtraJson("{\"source\":\"manual\"}");

            when(materialService.updateMeta(eq(1L), eq(100L), any()))
                    .thenReturn(buildAggregate("Redis notes", "updated-author"));

            UserContext.set(1L);
            try {
                Result<IMaterialController.MaterialDetailResponse> result = materialController.updateMeta(100L, request);

                ArgumentCaptor<IMaterialService.UpdateMetaCommand> captor =
                        ArgumentCaptor.forClass(IMaterialService.UpdateMetaCommand.class);
                verify(materialService).updateMeta(eq(1L), eq(100L), captor.capture());

                assertThat(captor.getValue())
                        .returns("updated-author", IMaterialService.UpdateMetaCommand::getAuthor)
                        .returns("newsletter", IMaterialService.UpdateMetaCommand::getSourcePlatform)
                        .returns(1200, IMaterialService.UpdateMetaCommand::getWordCount)
                        .returns(360, IMaterialService.UpdateMetaCommand::getDurationSeconds)
                        .returns("covers/material-100.png", IMaterialService.UpdateMetaCommand::getThumbnailKey)
                        .returns("{\"source\":\"manual\"}", IMaterialService.UpdateMetaCommand::getExtraJson);
                assertThat(result.getData().meta())
                        .returns("updated-author", IMaterialController.MaterialMetaResponse::author);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("delete material")
    class DeleteMaterial {

        @Test
        @DisplayName("delegates delete to the domain service")
        void givenMaterialId_whenDelete_thenDelegatesToDomainService() {
            UserContext.set(1L);
            try {
                Result<Void> result = materialController.delete(100L);

                assertThat(result).returns(0, Result::getCode);
                verify(materialService).deleteMaterial(1L, 100L);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("mark read")
    class MarkRead {

        @Test
        @DisplayName("delegates mark read to the domain service")
        void givenMaterialId_whenMarkRead_thenDelegatesToDomainService() {
            UserContext.set(1L);
            try {
                Result<Void> result = materialController.markRead(100L);

                assertThat(result).returns(0, Result::getCode);
                verify(materialService).markRead(1L, 100L);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("collect material")
    class CollectMaterial {

        @Test
        @DisplayName("delegates comment and score to the domain service")
        void givenCommentAndScore_whenCollect_thenDelegatesToDomainService() {
            IMaterialController.CollectRequest request = new IMaterialController.CollectRequest();
            request.setComment("great material");
            request.setScore(new BigDecimal("8.5"));

            UserContext.set(1L);
            try {
                Result<Void> result = materialController.collect(100L, request);

                assertThat(result).returns(0, Result::getCode);
                verify(materialService).collect(1L, 100L, "great material", new BigDecimal("8.5"));
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("archive material")
    class ArchiveMaterial {

        @Test
        @DisplayName("delegates archive to the domain service")
        void givenMaterialId_whenArchive_thenDelegatesToDomainService() {
            UserContext.set(1L);
            try {
                Result<Void> result = materialController.archive(100L);

                assertThat(result).returns(0, Result::getCode);
                verify(materialService).archive(1L, 100L);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("invalidate material")
    class InvalidateMaterial {

        @Test
        @DisplayName("delegates invalid reason to the domain service")
        void givenInvalidReason_whenInvalidate_thenDelegatesToDomainService() {
            IMaterialController.InvalidateRequest request = new IMaterialController.InvalidateRequest();
            request.setInvalidReason("outdated");

            UserContext.set(1L);
            try {
                Result<Void> result = materialController.invalidate(100L, request);

                assertThat(result).returns(0, Result::getCode);
                verify(materialService).invalidate(1L, 100L, "outdated");
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("restore material")
    class RestoreMaterial {

        @Test
        @DisplayName("delegates restore to inbox to the domain service")
        void givenMaterialId_whenRestore_thenDelegatesToDomainService() {
            UserContext.set(1L);
            try {
                Result<Void> result = materialController.restore(100L);

                assertThat(result).returns(0, Result::getCode);
                verify(materialService).restore(1L, 100L);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("delegates restore to collected to the domain service")
        void givenMaterialId_whenRestoreCollected_thenDelegatesToDomainService() {
            UserContext.set(1L);
            try {
                Result<Void> result = materialController.restoreCollected(100L);

                assertThat(result).returns(0, Result::getCode);
                verify(materialService).restoreCollected(1L, 100L);
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("update tags")
    class UpdateTags {

        @Test
        @DisplayName("maps tag items into domain tag inputs")
        void givenTagItems_whenUpdateTags_thenDelegatesWithTagInputs() {
            IMaterialController.UpdateTagsRequest request = new IMaterialController.UpdateTagsRequest();
            IMaterialController.UpdateTagsRequest.TagItem first = new IMaterialController.UpdateTagsRequest.TagItem();
            first.setTagGroupKey("11");
            first.setTagValue("backend");
            IMaterialController.UpdateTagsRequest.TagItem second = new IMaterialController.UpdateTagsRequest.TagItem();
            second.setTagGroupKey("12");
            second.setTagValue("redis");
            request.setTags(List.of(first, second));

            UserContext.set(1L);
            try {
                Result<Void> result = materialController.updateTags(100L, request);

                ArgumentCaptor<List<IMaterialService.TagInput>> captor = ArgumentCaptor.forClass(List.class);
                verify(materialService).updateTags(eq(1L), eq(100L), captor.capture());

                assertThat(captor.getValue())
                        .extracting(IMaterialService.TagInput::getTagGroupKey, IMaterialService.TagInput::getTagValue)
                        .containsExactly(
                                org.assertj.core.groups.Tuple.tuple("11", "backend"),
                                org.assertj.core.groups.Tuple.tuple("12", "redis")
                        );
                assertThat(result).returns(0, Result::getCode);
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("accepts ungrouped user tags when replacing material tags")
        void givenUngroupedTag_whenUpdateTags_thenDelegatesWithNullGroupKey() {
            IMaterialController.UpdateTagsRequest request = new IMaterialController.UpdateTagsRequest();
            IMaterialController.UpdateTagsRequest.TagItem tag = new IMaterialController.UpdateTagsRequest.TagItem();
            tag.setTagValue("quick-note");
            request.setTags(List.of(tag));

            UserContext.set(1L);
            try {
                materialController.updateTags(100L, request);

                ArgumentCaptor<List<IMaterialService.TagInput>> captor = ArgumentCaptor.forClass(List.class);
                verify(materialService).updateTags(eq(1L), eq(100L), captor.capture());
                assertThat(captor.getValue())
                        .extracting(IMaterialService.TagInput::getTagGroupKey, IMaterialService.TagInput::getTagValue)
                        .containsExactly(org.assertj.core.groups.Tuple.tuple(null, "quick-note"));
            } finally {
                UserContext.clear();
            }
        }
    }

    private Material buildMaterial() {
        return Material.builder()
                .id(100L)
                .userId(1L)
                .topicId(10L)
                .materialType(MaterialType.ARTICLE)
                .status(MaterialStatus.INBOX)
                .title("Redis notes")
                .rawContent("redis raw content")
                .sourceUrl("https://example.com")
                .build();
    }

    private MaterialAggregate buildAggregate(String title, String author) {
        Material material = buildMaterial();
        material.setTitle(title);
        MaterialMeta meta = MaterialMeta.builder()
                .id(200L)
                .materialId(100L)
                .author(author)
                .sourcePlatform("wechat")
                .wordCount(1000)
                .durationSeconds(120)
                .thumbnailKey("covers/material-100.png")
                .extraJson("{\"quality\":\"high\"}")
                .build();
        return MaterialAggregate.builder()
                .material(material)
                .meta(meta)
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
