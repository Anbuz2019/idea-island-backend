package com.anbuz.domain.material.service;

import com.anbuz.domain.material.adapter.MaterialEventPublisher;
import com.anbuz.domain.material.model.aggregate.MaterialAggregate;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.model.valobj.MaterialPageResult;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.material.service.impl.MaterialService;
import com.anbuz.domain.material.service.impl.StatusTransitionService;
import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.types.enums.MaterialAction;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import com.anbuz.types.enums.TagType;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialService scenarios")
class MaterialServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long MATERIAL_ID = 100L;
    private static final Long TOPIC_ID = 10L;
    private static final Long GROUP_ID = 20L;
    private static final Long SECOND_GROUP_ID = 21L;

    @Mock
    private IMaterialRepository materialRepository;

    @Mock
    private ITopicRepository topicRepository;

    @Mock
    private IStatusTransitionService statusTransitionService;

    @Mock
    private ISystemTagService systemTagService;

    @Mock
    private MaterialEventPublisher materialEventPublisher;

    @InjectMocks
    private MaterialService materialService;

    @Nested
    @DisplayName("submit")
    class Submit {

        @Test
        @DisplayName("saves material, meta, topic count and publishes content process event")
        void givenEnabledTopicAndArticleCommand_whenSubmit_thenPersistsMaterialMetaTopicAndPublishesEvent() {
            Topic topic = buildTopic(1, 3);
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(materialRepository.saveMaterial(any(Material.class))).thenReturn(MATERIAL_ID);

            Long result = materialService.submit(USER_ID, IMaterialService.SubmitCommand.builder()
                    .topicId(TOPIC_ID)
                    .materialType(MaterialType.ARTICLE.getCode())
                    .title("Redis notes")
                    .description("cache")
                    .sourceUrl("https://example.com/redis")
                    .author("Martin")
                    .sourcePlatform("blog")
                    .durationSeconds(60)
                    .thumbnailKey("covers/redis.png")
                    .build());

            ArgumentCaptor<Material> materialCaptor = ArgumentCaptor.forClass(Material.class);
            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
            verify(materialRepository).saveMaterial(materialCaptor.capture());
            verify(materialRepository).saveMeta(metaCaptor.capture());
            verify(topicRepository).updateTopic(topicCaptor.capture());
            verify(materialEventPublisher).publishMaterialSubmitted(MATERIAL_ID);

            assertThat(result).isEqualTo(MATERIAL_ID);
            assertThat(materialCaptor.getValue())
                    .returns(USER_ID, Material::getUserId)
                    .returns(TOPIC_ID, Material::getTopicId)
                    .returns(MaterialType.ARTICLE, Material::getMaterialType)
                    .returns(MaterialStatus.INBOX, Material::getStatus)
                    .returns("Redis notes", Material::getTitle)
                    .returns("https://example.com/redis", Material::getSourceUrl)
                    .returns(false, Material::getDeleted)
                    .satisfies(saved -> assertThat(saved.getInboxAt()).isNotNull())
                    .satisfies(saved -> assertThat(saved.getCreatedAt()).isNotNull())
                    .satisfies(saved -> assertThat(saved.getUpdatedAt()).isNotNull());
            assertThat(metaCaptor.getValue())
                    .returns(MATERIAL_ID, MaterialMeta::getMaterialId)
                    .returns("Martin", MaterialMeta::getAuthor)
                    .returns("blog", MaterialMeta::getSourcePlatform)
                    .returns(60, MaterialMeta::getDurationSeconds)
                    .returns("covers/redis.png", MaterialMeta::getThumbnailKey)
                    .satisfies(saved -> assertThat(saved.getCreatedAt()).isNotNull())
                    .satisfies(saved -> assertThat(saved.getUpdatedAt()).isNotNull());
            assertThat(topicCaptor.getValue())
                    .returns(4, Topic::getMaterialCount)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
            verify(systemTagService).refreshSystemTags(MATERIAL_ID, null, null);
            verify(materialRepository, never()).saveTags(anyList());
        }

        @Test
        @DisplayName("initializes word count from submitted raw content")
        void givenRawContent_whenSubmit_thenSavesMetaWordCount() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(buildTopic(1, 3)));
            when(materialRepository.saveMaterial(any(Material.class))).thenReturn(MATERIAL_ID);

            materialService.submit(USER_ID, IMaterialService.SubmitCommand.builder()
                    .topicId(TOPIC_ID)
                    .materialType(MaterialType.INPUT.getCode())
                    .rawContent("Redis 缓存\n优化")
                    .build());

            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).saveMeta(metaCaptor.capture());
            assertThat(metaCaptor.getValue())
                    .returns(MATERIAL_ID, MaterialMeta::getMaterialId)
                    .returns(9, MaterialMeta::getWordCount)
                    .satisfies(saved -> assertThat(saved.getCreatedAt()).isNotNull())
                    .satisfies(saved -> assertThat(saved.getUpdatedAt()).isNotNull());
        }

        @Test
        @DisplayName("saves submitted user tags in the same submit flow")
        void givenSubmitCommandWithTags_whenSubmit_thenPersistsUserTagsAndRefreshesSystemTags() {
            Topic topic = buildTopic(1, 3);
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(materialRepository.saveMaterial(any(Material.class))).thenReturn(MATERIAL_ID);
            when(topicRepository.findTagGroupsByTopicId(TOPIC_ID)).thenReturn(List.of(buildGroup(GROUP_ID, false, false)));
            when(topicRepository.findTagValuesByGroupId(GROUP_ID)).thenReturn(List.of(buildValue(GROUP_ID, "analysis")));

            materialService.submit(USER_ID, IMaterialService.SubmitCommand.builder()
                    .topicId(TOPIC_ID)
                    .materialType(MaterialType.ARTICLE.getCode())
                    .sourceUrl("https://example.com/redis")
                    .tags(List.of(new IMaterialService.TagInput(String.valueOf(GROUP_ID), "analysis")))
                    .build());

            ArgumentCaptor<List<MaterialTag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
            verify(materialRepository, never()).deleteUserTags(MATERIAL_ID);
            verify(materialRepository).saveTags(tagsCaptor.capture());
            verify(systemTagService).refreshSystemTags(MATERIAL_ID, null, null);
            verify(materialEventPublisher).publishMaterialSubmitted(MATERIAL_ID);
            assertThat(tagsCaptor.getValue())
                    .singleElement()
                    .returns(MATERIAL_ID, MaterialTag::getMaterialId)
                    .returns(TagType.USER, MaterialTag::getTagType)
                    .returns(String.valueOf(GROUP_ID), MaterialTag::getTagGroupKey)
                    .returns("analysis", MaterialTag::getTagValue)
                    .satisfies(saved -> assertThat(saved.getCreatedAt()).isNotNull());
        }

        @Test
        @DisplayName("accepts submitted ungrouped user tags and persists them with the reserved group key")
        void givenSubmitCommandWithUngroupedTags_whenSubmit_thenPersistsUngroupedUserTags() {
            Topic topic = buildTopic(1, 3);
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(materialRepository.saveMaterial(any(Material.class))).thenReturn(MATERIAL_ID);
            when(topicRepository.findTagGroupsByTopicId(TOPIC_ID)).thenReturn(List.of(buildGroup(GROUP_ID, false, false)));
            when(topicRepository.findTagValuesByGroupId(GROUP_ID)).thenReturn(List.of(buildValue(GROUP_ID, "analysis")));

            materialService.submit(USER_ID, IMaterialService.SubmitCommand.builder()
                    .topicId(TOPIC_ID)
                    .materialType(MaterialType.ARTICLE.getCode())
                    .sourceUrl("https://example.com/redis")
                    .tags(List.of(new IMaterialService.TagInput(null, "quick-note")))
                    .build());

            ArgumentCaptor<List<MaterialTag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
            verify(materialRepository).saveTags(tagsCaptor.capture());
            assertThat(tagsCaptor.getValue())
                    .singleElement()
                    .returns(MATERIAL_ID, MaterialTag::getMaterialId)
                    .returns(TagType.USER, MaterialTag::getTagType)
                    .returns(MaterialTag.UNGROUPED_USER_TAG_GROUP_KEY, MaterialTag::getTagGroupKey)
                    .returns("quick-note", MaterialTag::getTagValue)
                    .satisfies(saved -> assertThat(saved.getCreatedAt()).isNotNull());
        }

        @Test
        @DisplayName("publishes submitted event only after transaction commit when synchronization is active")
        void givenActiveTransactionSynchronization_whenSubmit_thenPublishesEventAfterCommit() {
            Topic topic = buildTopic(1, 3);
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(materialRepository.saveMaterial(any(Material.class))).thenReturn(MATERIAL_ID);

            TransactionSynchronizationManager.initSynchronization();
            try {
                materialService.submit(USER_ID, IMaterialService.SubmitCommand.builder()
                        .topicId(TOPIC_ID)
                        .materialType(MaterialType.ARTICLE.getCode())
                        .sourceUrl("https://example.com/redis")
                        .build());

                verify(materialEventPublisher, never()).publishMaterialSubmitted(MATERIAL_ID);
                assertThat(TransactionSynchronizationManager.getSynchronizations())
                        .hasSize(1)
                        .first()
                        .satisfies(TransactionSynchronization::afterCommit);
                verify(materialEventPublisher).publishMaterialSubmitted(MATERIAL_ID);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("rejects submit when topic is disabled")
        void givenDisabledTopic_whenSubmit_thenThrowsBusinessConflictAndSkipsWrites() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(buildTopic(0, 3)));

            assertThatThrownBy(() -> materialService.submit(USER_ID, IMaterialService.SubmitCommand.builder()
                    .topicId(TOPIC_ID)
                    .materialType(MaterialType.ARTICLE.getCode())
                    .sourceUrl("https://example.com/redis")
                    .build()))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "主题已停用，无法提交资料");

            verify(materialRepository, never()).saveMaterial(any(Material.class));
            verify(materialRepository, never()).saveMeta(any(MaterialMeta.class));
            verify(materialEventPublisher, never()).publishMaterialSubmitted(any());
        }

        @Test
        @DisplayName("rejects image submit without fileKey")
        void givenImageWithoutFileKey_whenSubmit_thenThrowsParamInvalid() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(buildTopic(1, 0)));

            assertThatThrownBy(() -> materialService.submit(USER_ID, IMaterialService.SubmitCommand.builder()
                    .topicId(TOPIC_ID)
                    .materialType(MaterialType.IMAGE.getCode())
                    .build()))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.PARAM_INVALID.getCode(), "image 类型必须提供 fileKey");

            verify(materialRepository, never()).saveMaterial(any(Material.class));
        }

        @Test
        @DisplayName("accepts thumbnailKey without ownership lookup")
        void givenThumbnailKey_whenSubmit_thenSavesMeta() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(buildTopic(1, 0)));
            when(materialRepository.saveMaterial(any(Material.class))).thenReturn(MATERIAL_ID);
            Long result = materialService.submit(USER_ID, IMaterialService.SubmitCommand.builder()
                    .topicId(TOPIC_ID)
                    .materialType(MaterialType.ARTICLE.getCode())
                    .sourceUrl("https://example.com/redis")
                    .thumbnailKey("covers/foreign.png")
                    .build());
            assertThat(result).isEqualTo(MATERIAL_ID);
            verify(materialRepository).saveMaterial(any(Material.class));
            verify(materialRepository).saveMeta(any(MaterialMeta.class));
        }
    }

    @Nested
    @DisplayName("list materials")
    class ListMaterials {

        @Test
        @DisplayName("normalizes null query and builds aggregate page")
        void givenNullQuery_whenListMaterials_thenNormalizesDefaultsAndBuildsAggregatePage() {
            Material material = buildMaterial();
            MaterialMeta meta = buildMeta();
            MaterialTag tag = buildTag(String.valueOf(GROUP_ID), "analysis");
            when(materialRepository.queryMaterials(any(MaterialListQuery.class))).thenReturn(List.of(material));
            when(materialRepository.countMaterials(any(MaterialListQuery.class))).thenReturn(1L);
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.of(meta));
            when(materialRepository.findTagsByMaterialId(MATERIAL_ID)).thenReturn(List.of(tag));

            MaterialPageResult result = materialService.listMaterials(USER_ID, null);

            ArgumentCaptor<MaterialListQuery> queryCaptor = ArgumentCaptor.forClass(MaterialListQuery.class);
            verify(materialRepository).queryMaterials(queryCaptor.capture());
            assertThat(queryCaptor.getValue())
                    .returns(USER_ID, MaterialListQuery::getUserId)
                    .returns(false, MaterialListQuery::isIncludeComment)
                    .returns(1, MaterialListQuery::getPage)
                    .returns(20, MaterialListQuery::getPageSize)
                    .returns("createdAt", MaterialListQuery::getSortBy)
                    .returns("DESC", MaterialListQuery::getSortDirection)
                    .satisfies(query -> assertThat(query.getStatuses()).containsExactly(
                            MaterialStatus.INBOX.getCode(),
                            MaterialStatus.PENDING_REVIEW.getCode(),
                            MaterialStatus.COLLECTED.getCode(),
                            MaterialStatus.ARCHIVED.getCode()));
            assertThat(result)
                    .returns(1L, MaterialPageResult::getTotal)
                    .returns(1, MaterialPageResult::getPage)
                    .returns(20, MaterialPageResult::getPageSize);
            assertThat(result.getItems())
                    .singleElement()
                    .satisfies(item -> assertThat(item)
                            .returns(material, MaterialAggregate::getMaterial)
                            .returns(meta, MaterialAggregate::getMeta)
                            .returns(List.of(tag), MaterialAggregate::getTags));
        }

        @Test
        @DisplayName("validates topic ownership and caps page size")
        void givenTopicFilterAndOversizedPageSize_whenListMaterials_thenValidatesTopicAndCapsPageSize() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(buildTopic(1, 0)));
            when(materialRepository.queryMaterials(any(MaterialListQuery.class))).thenReturn(List.of());
            when(materialRepository.countMaterials(any(MaterialListQuery.class))).thenReturn(0L);

            materialService.listMaterials(USER_ID, MaterialListQuery.builder()
                    .topicId(TOPIC_ID)
                    .page(-1)
                    .pageSize(500)
                    .sortBy("unknown")
                    .sortDirection("ASC")
                    .build());

            ArgumentCaptor<MaterialListQuery> queryCaptor = ArgumentCaptor.forClass(MaterialListQuery.class);
            verify(materialRepository).queryMaterials(queryCaptor.capture());
            assertThat(queryCaptor.getValue())
                    .returns(TOPIC_ID, MaterialListQuery::getTopicId)
                    .returns(1, MaterialListQuery::getPage)
                    .returns(100, MaterialListQuery::getPageSize)
                    .returns("createdAt", MaterialListQuery::getSortBy)
                    .returns("ASC", MaterialListQuery::getSortDirection);
        }
    }

    @Nested
    @DisplayName("search materials")
    class SearchMaterials {

        @Test
        @DisplayName("updates lastRetrievedAt for the matched materials after search")
        void givenMatchedMaterials_whenSearch_thenUpdatesLastRetrievedAtAndReturnsPage() {
            Material first = buildMaterial();
            first.setTitle("Redis notes");
            Material second = buildMaterial();
            second.setId(101L);
            second.setTitle("Backend cache");

            when(materialRepository.queryMaterials(any(MaterialListQuery.class))).thenReturn(List.of(first, second));
            when(materialRepository.countMaterials(any(MaterialListQuery.class))).thenReturn(2L);
            when(materialRepository.findMetaByMaterialId(any())).thenReturn(Optional.empty());
            when(materialRepository.findTagsByMaterialId(any())).thenReturn(List.of());

            MaterialPageResult result = materialService.searchMaterials(USER_ID, MaterialListQuery.builder()
                    .keyword("redis")
                    .page(2)
                    .pageSize(10)
                    .build());

            ArgumentCaptor<MaterialListQuery> queryCaptor = ArgumentCaptor.forClass(MaterialListQuery.class);
            verify(materialRepository).queryMaterials(queryCaptor.capture());
            assertThat(queryCaptor.getValue())
                    .returns(USER_ID, MaterialListQuery::getUserId)
                    .returns(true, MaterialListQuery::isIncludeComment)
                    .satisfies(query -> assertThat(query.getStatuses()).containsExactly(
                            MaterialStatus.INBOX.getCode(),
                            MaterialStatus.PENDING_REVIEW.getCode(),
                            MaterialStatus.COLLECTED.getCode(),
                            MaterialStatus.ARCHIVED.getCode()));
            assertThat(result)
                    .returns(2L, MaterialPageResult::getTotal)
                    .returns(2, MaterialPageResult::getPage)
                    .returns(10, MaterialPageResult::getPageSize);
            assertThat(result.getItems())
                    .extracting(item -> item.getMaterial().getId())
                    .containsExactly(MATERIAL_ID, 101L);
            verify(materialRepository).updateLastRetrievedAt(eq(List.of(MATERIAL_ID, 101L)), any());
        }

        @Test
        @DisplayName("skips lastRetrievedAt update when search has no matches")
        void givenNoMatches_whenSearch_thenSkipsLastRetrievedAtUpdate() {
            when(materialRepository.queryMaterials(any(MaterialListQuery.class))).thenReturn(List.of());
            when(materialRepository.countMaterials(any(MaterialListQuery.class))).thenReturn(0L);

            MaterialPageResult result = materialService.searchMaterials(USER_ID, MaterialListQuery.builder()
                    .keyword("missing")
                    .build());

            assertThat(result.getItems()).isEmpty();
            verify(materialRepository, never()).updateLastRetrievedAt(anyList(), any());
        }
    }

    @Nested
    @DisplayName("inbox")
    class Inbox {

        @Test
        @DisplayName("queries inbox materials with default sorting")
        void givenTopicFilter_whenInbox_thenBuildsInboxQuery() {
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(buildTopic(1, 0)));
            when(materialRepository.queryMaterials(any(MaterialListQuery.class))).thenReturn(List.of());
            when(materialRepository.countMaterials(any(MaterialListQuery.class))).thenReturn(0L);

            MaterialPageResult result = materialService.inbox(USER_ID, TOPIC_ID, 3, 15);

            ArgumentCaptor<MaterialListQuery> queryCaptor = ArgumentCaptor.forClass(MaterialListQuery.class);
            verify(materialRepository).queryMaterials(queryCaptor.capture());
            assertThat(result)
                    .returns(3, MaterialPageResult::getPage)
                    .returns(15, MaterialPageResult::getPageSize);
            assertThat(queryCaptor.getValue())
                    .returns(TOPIC_ID, MaterialListQuery::getTopicId)
                    .returns(List.of(MaterialStatus.INBOX.getCode()), MaterialListQuery::getStatuses)
                    .returns("createdAt", MaterialListQuery::getSortBy)
                    .returns("DESC", MaterialListQuery::getSortDirection);
        }
    }

    @Nested
    @DisplayName("get detail")
    class GetDetail {

        @Test
        @DisplayName("builds aggregate with meta, tags and sorted status history")
        void givenOwnedMaterial_whenGetDetail_thenBuildsAggregate() {
            LocalDateTime now = LocalDateTime.now();
            Material material = buildMaterial();
            material.setStatus(MaterialStatus.COLLECTED);
            material.setInboxAt(now.minusDays(3));
            material.setCollectedAt(now.minusDays(1));
            MaterialMeta meta = buildMeta();
            MaterialTag tag = buildTag(String.valueOf(GROUP_ID), "analysis");
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.of(meta));
            when(materialRepository.findTagsByMaterialId(MATERIAL_ID)).thenReturn(List.of(tag));

            MaterialAggregate result = materialService.getDetail(USER_ID, MATERIAL_ID);

            assertThat(result)
                    .returns(material, MaterialAggregate::getMaterial)
                    .returns(meta, MaterialAggregate::getMeta)
                    .returns(List.of(tag), MaterialAggregate::getTags);
            assertThat(result.getStatusHistory())
                    .extracting("status")
                    .containsExactly(MaterialStatus.INBOX.getCode(), MaterialStatus.COLLECTED.getCode());
        }

        @Test
        @DisplayName("rejects access to material owned by another user")
        void givenForeignMaterial_whenGetDetail_thenThrowsForbidden() {
            Material material = buildMaterial();
            material.setUserId(OTHER_USER_ID);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            assertThatThrownBy(() -> materialService.getDetail(USER_ID, MATERIAL_ID))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode());

            verify(materialRepository, never()).findMetaByMaterialId(MATERIAL_ID);
        }
    }

    @Nested
    @DisplayName("update basic")
    class UpdateBasic {

        @Test
        @DisplayName("updates only provided basic fields")
        void givenPartialBasicCommand_whenUpdateBasic_thenUpdatesOnlyProvidedFields() {
            Material material = buildMaterial();
            material.setTitle("old");
            material.setRawContent("raw");
            material.setSourceUrl("https://old.example.com");
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.empty());
            when(materialRepository.findTagsByMaterialId(MATERIAL_ID)).thenReturn(List.of());

            MaterialAggregate result = materialService.updateBasic(USER_ID, MATERIAL_ID,
                    IMaterialService.UpdateBasicCommand.builder()
                            .title("new")
                            .sourceUrl("https://new.example.com")
                            .build());

            ArgumentCaptor<Material> materialCaptor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).updateMaterial(materialCaptor.capture());
            assertThat(materialCaptor.getValue())
                    .returns("new", Material::getTitle)
                    .returns("raw", Material::getRawContent)
                    .returns("https://new.example.com", Material::getSourceUrl)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
            assertThat(result.getMaterial()).isSameAs(material);
        }

        @Test
        @DisplayName("syncs existing meta word count when raw content changes")
        void givenRawContentAndExistingMeta_whenUpdateBasic_thenSyncsWordCount() {
            Material material = buildMaterial();
            MaterialMeta meta = buildMeta();
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.of(meta));
            when(materialRepository.findTagsByMaterialId(MATERIAL_ID)).thenReturn(List.of());

            MaterialAggregate result = materialService.updateBasic(USER_ID, MATERIAL_ID,
                    IMaterialService.UpdateBasicCommand.builder()
                            .rawContent("Redis 缓存\n优化")
                            .build());

            ArgumentCaptor<Material> materialCaptor = ArgumentCaptor.forClass(Material.class);
            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).updateMaterial(materialCaptor.capture());
            verify(materialRepository).updateMeta(metaCaptor.capture());
            verify(materialRepository, never()).saveMeta(any(MaterialMeta.class));
            assertThat(materialCaptor.getValue())
                    .returns("Redis 缓存\n优化", Material::getRawContent)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
            assertThat(metaCaptor.getValue())
                    .returns(9, MaterialMeta::getWordCount)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
            assertThat(result.getMeta()).isSameAs(meta);
        }

        @Test
        @DisplayName("creates meta with word count when raw content changes and meta is missing")
        void givenRawContentAndMissingMeta_whenUpdateBasic_thenCreatesMetaWithWordCount() {
            Material material = buildMaterial();
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.empty());
            when(materialRepository.findTagsByMaterialId(MATERIAL_ID)).thenReturn(List.of());

            materialService.updateBasic(USER_ID, MATERIAL_ID,
                    IMaterialService.UpdateBasicCommand.builder()
                            .rawContent("Redis 缓存\n优化")
                            .build());

            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).saveMeta(metaCaptor.capture());
            verify(materialRepository, never()).updateMeta(any(MaterialMeta.class));
            assertThat(metaCaptor.getValue())
                    .returns(MATERIAL_ID, MaterialMeta::getMaterialId)
                    .returns(9, MaterialMeta::getWordCount)
                    .satisfies(saved -> assertThat(saved.getCreatedAt()).isNotNull())
                    .satisfies(saved -> assertThat(saved.getUpdatedAt()).isNotNull());
        }

        @Test
        @DisplayName("rejects editing invalid material")
        void givenInvalidMaterial_whenUpdateBasic_thenThrowsBusinessConflict() {
            Material material = buildMaterial();
            material.setStatus(MaterialStatus.INVALID);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            assertThatThrownBy(() -> materialService.updateBasic(USER_ID, MATERIAL_ID,
                    IMaterialService.UpdateBasicCommand.builder().title("new").build()))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "已失效资料不可编辑");

            verify(materialRepository, never()).updateMaterial(any(Material.class));
        }
    }

    @Nested
    @DisplayName("update meta")
    class UpdateMeta {

        @Test
        @DisplayName("updates existing meta with provided fields")
        void givenExistingMeta_whenUpdateMeta_thenUpdatesMeta() {
            Material material = buildMaterial();
            MaterialMeta meta = buildMeta();
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.of(meta));
            when(materialRepository.findTagsByMaterialId(MATERIAL_ID)).thenReturn(List.of());

            MaterialAggregate result = materialService.updateMeta(USER_ID, MATERIAL_ID,
                    IMaterialService.UpdateMetaCommand.builder()
                            .author("new-author")
                            .wordCount(1024)
                            .extraJson("{\"lang\":\"zh\"}")
                            .build());

            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).updateMeta(metaCaptor.capture());
            verify(materialRepository, never()).saveMeta(any(MaterialMeta.class));
            assertThat(metaCaptor.getValue())
                    .returns("new-author", MaterialMeta::getAuthor)
                    .returns("platform", MaterialMeta::getSourcePlatform)
                    .returns(1024, MaterialMeta::getWordCount)
                    .returns("{\"lang\":\"zh\"}", MaterialMeta::getExtraJson)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
            assertThat(result.getMeta()).isSameAs(meta);
        }

        @Test
        @DisplayName("creates meta when material has no meta")
        void givenMissingMeta_whenUpdateMeta_thenCreatesMeta() {
            Material material = buildMaterial();
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.empty());
            when(materialRepository.findTagsByMaterialId(MATERIAL_ID)).thenReturn(List.of());

            materialService.updateMeta(USER_ID, MATERIAL_ID,
                    IMaterialService.UpdateMetaCommand.builder()
                            .thumbnailKey("covers/new.png")
                            .durationSeconds(90)
                            .build());

            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).saveMeta(metaCaptor.capture());
            verify(materialRepository, never()).updateMeta(any(MaterialMeta.class));
            assertThat(metaCaptor.getValue())
                    .returns(MATERIAL_ID, MaterialMeta::getMaterialId)
                    .returns("covers/new.png", MaterialMeta::getThumbnailKey)
                    .returns(90, MaterialMeta::getDurationSeconds)
                    .satisfies(saved -> assertThat(saved.getCreatedAt()).isNotNull())
                    .satisfies(saved -> assertThat(saved.getUpdatedAt()).isNotNull());
        }
    }

    @Nested
    @DisplayName("delete material")
    class DeleteMaterial {

        @Test
        @DisplayName("marks invalid material deleted and decrements topic count")
        void givenInvalidMaterial_whenDeleteMaterial_thenMarksDeletedAndUpdatesTopicCount() {
            Material material = buildMaterial();
            material.setStatus(MaterialStatus.INVALID);
            Topic topic = buildTopic(1, 2);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(topicRepository.findTopicById(TOPIC_ID)).thenReturn(Optional.of(topic));

            materialService.deleteMaterial(USER_ID, MATERIAL_ID);

            ArgumentCaptor<Material> materialCaptor = ArgumentCaptor.forClass(Material.class);
            ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
            verify(materialRepository).updateMaterial(materialCaptor.capture());
            verify(topicRepository).updateTopic(topicCaptor.capture());
            assertThat(materialCaptor.getValue())
                    .returns(true, Material::getDeleted)
                    .satisfies(deleted -> assertThat(deleted.getDeletedAt()).isNotNull())
                    .satisfies(deleted -> assertThat(deleted.getUpdatedAt()).isNotNull());
            assertThat(topicCaptor.getValue())
                    .returns(1, Topic::getMaterialCount)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
        }

        @Test
        @DisplayName("rejects deleting non-invalid material")
        void givenInboxMaterial_whenDeleteMaterial_thenThrowsBusinessConflict() {
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(buildMaterial()));

            assertThatThrownBy(() -> materialService.deleteMaterial(USER_ID, MATERIAL_ID))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "仅失效状态资料允许删除");

            verify(materialRepository, never()).updateMaterial(any(Material.class));
            verify(topicRepository, never()).updateTopic(any(Topic.class));
        }
    }

    @Nested
    @DisplayName("update tags")
    class UpdateTags {

        @Test
        @DisplayName("replaces user tags and refreshes system tags once")
        void givenValidTags_whenUpdateTags_thenReplacesUserTagsAndRefreshesSystemTagsOnce() {
            Material material = buildMaterial();
            material.setScore(new BigDecimal("4.5"));
            material.setComment("good");
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(topicRepository.findTagGroupsByTopicId(TOPIC_ID)).thenReturn(List.of(buildGroup(GROUP_ID, false, false)));
            when(topicRepository.findTagValuesByGroupId(GROUP_ID)).thenReturn(List.of(buildValue(GROUP_ID, "analysis")));

            materialService.updateTags(USER_ID, MATERIAL_ID,
                    List.of(new IMaterialService.TagInput(String.valueOf(GROUP_ID), "analysis")));

            ArgumentCaptor<List<MaterialTag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
            verify(materialRepository).deleteUserTags(MATERIAL_ID);
            verify(materialRepository).saveTags(tagsCaptor.capture());
            verify(systemTagService).refreshSystemTags(MATERIAL_ID, new BigDecimal("4.5"), "good");
            verifyNoInteractions(materialEventPublisher);
            assertThat(tagsCaptor.getValue())
                    .singleElement()
                    .returns(MATERIAL_ID, MaterialTag::getMaterialId)
                    .returns(TagType.USER, MaterialTag::getTagType)
                    .returns(String.valueOf(GROUP_ID), MaterialTag::getTagGroupKey)
                    .returns("analysis", MaterialTag::getTagValue)
                    .satisfies(saved -> assertThat(saved.getCreatedAt()).isNotNull());
        }

        @Test
        @DisplayName("replaces user tags with ungrouped tags and refreshes system tags once")
        void givenUngroupedTags_whenUpdateTags_thenPersistsReservedGroupKey() {
            Material material = buildMaterial();
            material.setScore(new BigDecimal("4.5"));
            material.setComment("good");
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(topicRepository.findTagGroupsByTopicId(TOPIC_ID)).thenReturn(List.of(buildGroup(GROUP_ID, false, false)));
            when(topicRepository.findTagValuesByGroupId(GROUP_ID)).thenReturn(List.of(buildValue(GROUP_ID, "analysis")));

            materialService.updateTags(USER_ID, MATERIAL_ID,
                    List.of(new IMaterialService.TagInput("", "quick-note")));

            ArgumentCaptor<List<MaterialTag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
            verify(materialRepository).deleteUserTags(MATERIAL_ID);
            verify(materialRepository).saveTags(tagsCaptor.capture());
            verify(systemTagService).refreshSystemTags(MATERIAL_ID, new BigDecimal("4.5"), "good");
            assertThat(tagsCaptor.getValue())
                    .singleElement()
                    .returns(MaterialTag.UNGROUPED_USER_TAG_GROUP_KEY, MaterialTag::getTagGroupKey)
                    .returns("quick-note", MaterialTag::getTagValue);
        }

        @Test
        @DisplayName("rejects duplicate tag values inside the same group before writing to the repository")
        void givenDuplicateTagValueInSameGroup_whenUpdateTags_thenThrowsBusinessConflict() {
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(buildMaterial()));
            when(topicRepository.findTagGroupsByTopicId(TOPIC_ID)).thenReturn(List.of(buildGroup(GROUP_ID, false, false)));
            when(topicRepository.findTagValuesByGroupId(GROUP_ID)).thenReturn(List.of(buildValue(GROUP_ID, "analysis")));

            assertThatThrownBy(() -> materialService.updateTags(USER_ID, MATERIAL_ID, List.of(
                    new IMaterialService.TagInput(String.valueOf(GROUP_ID), "analysis"),
                    new IMaterialService.TagInput(String.valueOf(GROUP_ID), "analysis"))))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "duplicate tag value: analysis");

            verify(materialRepository, never()).deleteUserTags(MATERIAL_ID);
            verify(materialRepository, never()).saveTags(anyList());
            verify(systemTagService, never()).refreshSystemTags(any(), any(), any());
            verifyNoInteractions(materialEventPublisher);
        }

        @Test
        @DisplayName("rejects malformed tag group key")
        void givenMalformedTagGroupKey_whenUpdateTags_thenThrowsParamInvalid() {
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(buildMaterial()));
            when(topicRepository.findTagGroupsByTopicId(TOPIC_ID)).thenReturn(List.of(buildGroup(GROUP_ID, false, false)));
            when(topicRepository.findTagValuesByGroupId(GROUP_ID)).thenReturn(List.of(buildValue(GROUP_ID, "analysis")));

            assertThatThrownBy(() -> materialService.updateTags(USER_ID, MATERIAL_ID,
                    List.of(new IMaterialService.TagInput("bad", "analysis"))))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.PARAM_INVALID.getCode(), "tag_group_key 非法: bad");

            verify(materialRepository, never()).deleteUserTags(MATERIAL_ID);
        }

        @Test
        @DisplayName("rejects missing required tag group")
        void givenRequiredGroupNotFilled_whenUpdateTags_thenThrowsBusinessConflict() {
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(buildMaterial()));
            when(topicRepository.findTagGroupsByTopicId(TOPIC_ID)).thenReturn(List.of(
                    buildGroup(GROUP_ID, false, false),
                    buildGroup(SECOND_GROUP_ID, false, true)));
            when(topicRepository.findTagValuesByGroupId(GROUP_ID)).thenReturn(List.of(buildValue(GROUP_ID, "analysis")));
            when(topicRepository.findTagValuesByGroupId(SECOND_GROUP_ID)).thenReturn(List.of(buildValue(SECOND_GROUP_ID, "must")));

            assertThatThrownBy(() -> materialService.updateTags(USER_ID, MATERIAL_ID,
                    List.of(new IMaterialService.TagInput(String.valueOf(GROUP_ID), "analysis"))))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "必填标签组未填写: Stage-" + SECOND_GROUP_ID);

            verify(materialRepository, never()).deleteUserTags(MATERIAL_ID);
        }

        @Test
        @DisplayName("rejects multiple selected values in exclusive group")
        void givenExclusiveGroupWithMultipleValues_whenUpdateTags_thenThrowsBusinessConflict() {
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(buildMaterial()));
            when(topicRepository.findTagGroupsByTopicId(TOPIC_ID)).thenReturn(List.of(buildGroup(GROUP_ID, true, false)));
            when(topicRepository.findTagValuesByGroupId(GROUP_ID)).thenReturn(List.of(
                    buildValue(GROUP_ID, "analysis"),
                    buildValue(GROUP_ID, "review")));

            assertThatThrownBy(() -> materialService.updateTags(USER_ID, MATERIAL_ID, List.of(
                    new IMaterialService.TagInput(String.valueOf(GROUP_ID), "analysis"),
                    new IMaterialService.TagInput(String.valueOf(GROUP_ID), "review"))))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "互斥标签组只允许选择一个值: Stage-" + GROUP_ID);

            verify(materialRepository, never()).deleteUserTags(MATERIAL_ID);
        }
    }

    @Nested
    @DisplayName("status actions")
    class StatusActions {

        @Test
        @DisplayName("delegates mark read action to status transition service")
        void givenMaterialId_whenMarkRead_thenDelegatesMarkReadAction() {
            materialService.markRead(USER_ID, MATERIAL_ID);

            verify(statusTransitionService).transit(MATERIAL_ID, USER_ID, MaterialAction.MARK_READ, null, null, null);
            verifyNoInteractions(materialRepository, topicRepository, systemTagService, materialEventPublisher);
        }

        @Test
        @DisplayName("collect requires tags, transits status and refreshes system tags once")
        void givenCollectRequest_whenCollect_thenRequiresTagsAndRefreshesSystemTagsOnce() {
            Material material = buildMaterial();
            Material updated = buildMaterial();
            updated.setScore(new BigDecimal("4.8"));
            updated.setComment("useful");
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(topicRepository.findRequiredTagGroupIdsByTopicId(TOPIC_ID)).thenReturn(List.of(GROUP_ID));
            when(statusTransitionService.transit(MATERIAL_ID, USER_ID, MaterialAction.COLLECT, "useful",
                    new BigDecimal("4.8"), null)).thenReturn(updated);

            materialService.collect(USER_ID, MATERIAL_ID, "useful", new BigDecimal("4.8"));

            verify(statusTransitionService).assertRequiredTagsFilled(MATERIAL_ID, List.of(GROUP_ID));
            verify(statusTransitionService).transit(MATERIAL_ID, USER_ID, MaterialAction.COLLECT, "useful",
                    new BigDecimal("4.8"), null);
            verify(systemTagService).refreshSystemTags(MATERIAL_ID, new BigDecimal("4.8"), "useful");
            verifyNoInteractions(materialEventPublisher);
        }

        @Test
        @DisplayName("delegates archive action to status transition service")
        void givenMaterialId_whenArchive_thenDelegatesArchiveAction() {
            materialService.archive(USER_ID, MATERIAL_ID);

            verify(statusTransitionService).transit(MATERIAL_ID, USER_ID, MaterialAction.ARCHIVE, null, null, null);
            verifyNoInteractions(materialRepository, topicRepository, systemTagService, materialEventPublisher);
        }

        @Test
        @DisplayName("delegates invalidate action with reason to status transition service")
        void givenInvalidReason_whenInvalidate_thenDelegatesInvalidateAction() {
            materialService.invalidate(USER_ID, MATERIAL_ID, "404");

            verify(statusTransitionService).transit(MATERIAL_ID, USER_ID, MaterialAction.INVALIDATE, null, null, "404");
            verify(materialRepository).deleteTags(MATERIAL_ID);
            verifyNoInteractions(topicRepository, systemTagService, materialEventPublisher);
        }

        @Test
        @DisplayName("delegates restore action to status transition service")
        void givenMaterialId_whenRestore_thenDelegatesRestoreAction() {
            materialService.restore(USER_ID, MATERIAL_ID);

            verify(statusTransitionService).transit(MATERIAL_ID, USER_ID, MaterialAction.RESTORE, null, null, null);
            verifyNoInteractions(materialRepository, topicRepository, systemTagService, materialEventPublisher);
        }

        @Test
        @DisplayName("delegates restore collected action to status transition service")
        void givenMaterialId_whenRestoreCollected_thenDelegatesRestoreCollectedAction() {
            materialService.restoreCollected(USER_ID, MATERIAL_ID);

            verify(statusTransitionService).transit(MATERIAL_ID, USER_ID, MaterialAction.RESTORE_COLLECTED, null, null, null);
            verifyNoInteractions(materialRepository, topicRepository, systemTagService, materialEventPublisher);
        }
    }

    @Nested
    @DisplayName("transaction boundaries")
    class TransactionBoundaries {

        @Test
        @DisplayName("marks material service multi-write methods as transactional")
        void givenMaterialWriteMethods_whenInspectAnnotations_thenTheyAreTransactional() {
            assertThat(List.of(
                    method(MaterialService.class, "submit", Long.class, IMaterialService.SubmitCommand.class),
                    method(MaterialService.class, "searchMaterials", Long.class, MaterialListQuery.class),
                    method(MaterialService.class, "updateBasic", Long.class, Long.class, IMaterialService.UpdateBasicCommand.class),
                    method(MaterialService.class, "updateMeta", Long.class, Long.class, IMaterialService.UpdateMetaCommand.class),
                    method(MaterialService.class, "deleteMaterial", Long.class, Long.class),
                    method(MaterialService.class, "updateTags", Long.class, Long.class, List.class),
                    method(MaterialService.class, "markRead", Long.class, Long.class),
                    method(MaterialService.class, "collect", Long.class, Long.class, String.class, BigDecimal.class),
                    method(MaterialService.class, "archive", Long.class, Long.class),
                    method(MaterialService.class, "invalidate", Long.class, Long.class, String.class),
                    method(MaterialService.class, "restore", Long.class, Long.class),
                    method(MaterialService.class, "restoreCollected", Long.class, Long.class)))
                    .allSatisfy(method -> assertThat(method.getAnnotation(Transactional.class)).isNotNull());
        }

        @Test
        @DisplayName("marks status transition write method as transactional")
        void givenStatusTransitionWriteMethod_whenInspectAnnotation_thenItIsTransactional() {
            assertThat(method(StatusTransitionService.class, "transit", Long.class, Long.class, MaterialAction.class,
                    String.class, BigDecimal.class, String.class).getAnnotation(Transactional.class))
                    .isNotNull();
        }
    }

    private Material buildMaterial() {
        LocalDateTime now = LocalDateTime.now();
        return Material.builder()
                .id(MATERIAL_ID)
                .userId(USER_ID)
                .topicId(TOPIC_ID)
                .materialType(MaterialType.ARTICLE)
                .status(MaterialStatus.INBOX)
                .title("Redis notes")
                .deleted(false)
                .inboxAt(now.minusDays(2))
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusDays(1))
                .build();
    }

    private MaterialMeta buildMeta() {
        return MaterialMeta.builder()
                .id(9L)
                .materialId(MATERIAL_ID)
                .author("author")
                .sourcePlatform("platform")
                .wordCount(512)
                .thumbnailKey("covers/old.png")
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private MaterialTag buildTag(String groupKey, String value) {
        return MaterialTag.builder()
                .materialId(MATERIAL_ID)
                .tagType(TagType.USER)
                .tagGroupKey(groupKey)
                .tagValue(value)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Topic buildTopic(Integer status, Integer materialCount) {
        return Topic.builder()
                .id(TOPIC_ID)
                .userId(USER_ID)
                .name("Backend")
                .status(status)
                .materialCount(materialCount)
                .build();
    }

    private UserTagGroup buildGroup(Long groupId, boolean exclusive, boolean required) {
        return UserTagGroup.builder()
                .id(groupId)
                .topicId(TOPIC_ID)
                .name("Stage-" + groupId)
                .exclusive(exclusive)
                .required(required)
                .build();
    }

    private UserTagValue buildValue(Long groupId, String value) {
        return UserTagValue.builder()
                .groupId(groupId)
                .value(value)
                .build();
    }

    private Method method(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

}
