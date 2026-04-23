package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import com.anbuz.types.enums.TagType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(MaterialRepository.class)
@DisplayName("MaterialRepository H2 integration scenarios")
class MaterialRepositoryTest {

    @Autowired
    private MaterialRepository materialRepository;

    @Nested
    @DisplayName("save material")
    class SaveMaterial {

        @Test
        @DisplayName("stores a new material and loads it back by id")
        void givenNewMaterial_whenSave_thenCanFindById() {
            Material material = buildMaterial(MaterialStatus.INBOX, LocalDateTime.of(2026, 4, 1, 10, 0));

            Long id = materialRepository.saveMaterial(material);

            assertThat(id).isNotNull().isPositive();
            assertThat(materialRepository.findById(id))
                    .hasValueSatisfying(found -> assertThat(found)
                            .returns(MaterialStatus.INBOX, Material::getStatus)
                            .returns(MaterialType.ARTICLE, Material::getMaterialType)
                            .returns(1L, Material::getUserId));
        }
    }

    @Nested
    @DisplayName("tag operations")
    class TagOperations {

        @Test
        @DisplayName("stores tags and loads them by material id")
        void givenTags_whenSave_thenCanFindByMaterialId() {
            Long materialId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.INBOX, LocalDateTime.of(2026, 4, 1, 10, 0)));

            materialRepository.saveTags(List.of(
                    buildTag(materialId, "sys_score_range", "高"),
                    buildTag(materialId, "11", "需求分析")
            ));

            assertThat(materialRepository.findTagsByMaterialId(materialId))
                    .extracting(MaterialTag::getTagGroupKey)
                    .containsExactlyInAnyOrder("sys_score_range", "11");
        }

        @Test
        @DisplayName("removes only user tags when deleting user tags")
        void givenMixedTags_whenDeleteUserTags_thenSystemTagsRemain() {
            Long materialId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.INBOX, LocalDateTime.of(2026, 4, 1, 10, 0)));
            materialRepository.saveTags(List.of(
                    buildTag(materialId, "sys_score_range", "高"),
                    buildTag(materialId, "11", "需求分析")
            ));

            materialRepository.deleteUserTags(materialId);

            assertThat(materialRepository.findTagsByMaterialId(materialId))
                    .singleElement()
                    .returns("sys_score_range", MaterialTag::getTagGroupKey)
                    .returns(TagType.SYSTEM, MaterialTag::getTagType);
        }
    }

    @Nested
    @DisplayName("clear fields")
    class ClearFields {

        @Test
        @DisplayName("clears invalidation fields explicitly when restoring a material")
        void givenInvalidMaterial_whenClearInvalidation_thenInvalidFieldsBecomeNull() {
            LocalDateTime originalTime = LocalDateTime.of(2026, 4, 1, 10, 0);
            Material material = buildMaterial(MaterialStatus.INVALID, originalTime);
            material.setInvalidReason("stale");
            material.setInvalidAt(originalTime);
            material.setUpdatedAt(originalTime);
            Long materialId = materialRepository.saveMaterial(material);
            LocalDateTime clearedAt = LocalDateTime.of(2026, 4, 2, 10, 0);

            materialRepository.clearInvalidation(materialId, clearedAt);

            assertThat(materialRepository.findById(materialId))
                    .hasValueSatisfying(found -> assertThat(found)
                            .returns(null, Material::getInvalidReason)
                            .returns(null, Material::getInvalidAt)
                            .returns(clearedAt, Material::getUpdatedAt));
        }

        @Test
        @DisplayName("clears archivedAt explicitly when restoring an archived material")
        void givenArchivedMaterial_whenClearArchivedAt_thenArchivedAtBecomesNull() {
            LocalDateTime originalTime = LocalDateTime.of(2026, 4, 3, 10, 0);
            Material material = buildMaterial(MaterialStatus.ARCHIVED, originalTime);
            material.setArchivedAt(originalTime);
            material.setUpdatedAt(originalTime);
            Long materialId = materialRepository.saveMaterial(material);
            LocalDateTime clearedAt = LocalDateTime.of(2026, 4, 4, 10, 0);

            materialRepository.clearArchivedAt(materialId, clearedAt);

            assertThat(materialRepository.findById(materialId))
                    .hasValueSatisfying(found -> assertThat(found)
                            .returns(null, Material::getArchivedAt)
                            .returns(clearedAt, Material::getUpdatedAt));
        }
    }

    @Nested
    @DisplayName("query materials")
    class QueryMaterials {

        @Test
        @DisplayName("filters by topic, status, type, score, keyword, and tag filters")
        void givenStructuredQuery_whenQueryMaterials_thenReturnsOnlyMatchingItems() {
            Material matched = buildMaterial(MaterialStatus.COLLECTED, LocalDateTime.of(2026, 4, 5, 10, 0));
            matched.setTitle("Redis design");
            matched.setRawContent("cache notes");
            matched.setComment("redis checklist");
            matched.setScore(new BigDecimal("8.5"));
            Long matchedId = materialRepository.saveMaterial(matched);

            Material otherTopic = buildMaterial(MaterialStatus.COLLECTED, LocalDateTime.of(2026, 4, 5, 11, 0));
            otherTopic.setTopicId(2L);
            otherTopic.setTitle("Redis design");
            otherTopic.setScore(new BigDecimal("8.5"));
            Long otherTopicId = materialRepository.saveMaterial(otherTopic);

            Material wrongTag = buildMaterial(MaterialStatus.COLLECTED, LocalDateTime.of(2026, 4, 5, 12, 0));
            wrongTag.setTitle("Redis design");
            wrongTag.setScore(new BigDecimal("8.5"));
            Long wrongTagId = materialRepository.saveMaterial(wrongTag);

            materialRepository.saveTags(List.of(
                    buildTag(matchedId, "11", "backend"),
                    buildTag(wrongTagId, "11", "frontend"),
                    buildTag(otherTopicId, "11", "backend")
            ));

            MaterialListQuery query = MaterialListQuery.builder()
                    .userId(1L)
                    .topicId(1L)
                    .statuses(List.of(MaterialStatus.COLLECTED.getCode()))
                    .materialTypes(List.of(MaterialType.ARTICLE.getCode()))
                    .scoreMin(new BigDecimal("7.0"))
                    .scoreMax(new BigDecimal("9.0"))
                    .keyword("redis")
                    .includeComment(true)
                    .tagFilters(List.of(MaterialListQuery.TagFilter.builder()
                            .tagGroupKey("11")
                            .tagValues(List.of("backend"))
                            .build()))
                    .page(1)
                    .pageSize(20)
                    .build();

            assertThat(materialRepository.queryMaterials(query))
                    .extracting(Material::getId)
                    .containsExactly(matchedId);
            assertThat(materialRepository.countMaterials(query)).isEqualTo(1);
        }

        @Test
        @DisplayName("supports OR within one tag group and AND across tag groups")
        void givenMultipleTagFilters_whenQueryMaterials_thenUsesOrAndAndSemantics() {
            Long backendHighId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.COLLECTED, LocalDateTime.of(2026, 4, 6, 10, 0)));
            Long cacheHighId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.COLLECTED, LocalDateTime.of(2026, 4, 6, 11, 0)));
            Long backendLowId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.COLLECTED, LocalDateTime.of(2026, 4, 6, 12, 0)));

            materialRepository.saveTags(List.of(
                    buildTag(backendHighId, "11", "backend"),
                    buildTag(backendHighId, "12", "high"),
                    buildTag(cacheHighId, "11", "cache"),
                    buildTag(cacheHighId, "12", "high"),
                    buildTag(backendLowId, "11", "backend"),
                    buildTag(backendLowId, "12", "low")
            ));

            MaterialListQuery query = MaterialListQuery.builder()
                    .userId(1L)
                    .tagFilters(List.of(
                            MaterialListQuery.TagFilter.builder()
                                    .tagGroupKey("11")
                                    .tagValues(List.of("backend", "cache"))
                                    .build(),
                            MaterialListQuery.TagFilter.builder()
                                    .tagGroupKey("12")
                                    .tagValues(List.of("high"))
                                    .build()))
                    .sortBy("createdAt")
                    .sortDirection("ASC")
                    .page(1)
                    .pageSize(20)
                    .build();

            assertThat(materialRepository.queryMaterials(query))
                    .extracting(Material::getId)
                    .containsExactly(backendHighId, cacheHighId);
            assertThat(materialRepository.countMaterials(query)).isEqualTo(2);
        }

        @Test
        @DisplayName("orders visible materials by status priority before secondary sorting")
        void givenVisibleStatuses_whenQueryMaterials_thenOrdersInboxPendingCollectedArchived() {
            Long archivedId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.ARCHIVED, LocalDateTime.of(2026, 4, 6, 13, 0)));
            Long collectedId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.COLLECTED, LocalDateTime.of(2026, 4, 6, 12, 0)));
            Long pendingId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.PENDING_REVIEW, LocalDateTime.of(2026, 4, 6, 11, 0)));
            Long inboxId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.INBOX, LocalDateTime.of(2026, 4, 6, 10, 0)));
            materialRepository.saveMaterial(buildMaterial(MaterialStatus.INVALID, LocalDateTime.of(2026, 4, 6, 14, 0)));

            MaterialListQuery query = MaterialListQuery.builder()
                    .userId(1L)
                    .statuses(List.of(
                            MaterialStatus.INBOX.getCode(),
                            MaterialStatus.PENDING_REVIEW.getCode(),
                            MaterialStatus.COLLECTED.getCode(),
                            MaterialStatus.ARCHIVED.getCode()))
                    .sortBy("createdAt")
                    .sortDirection("DESC")
                    .page(1)
                    .pageSize(20)
                    .build();

            assertThat(materialRepository.queryMaterials(query))
                    .extracting(Material::getId)
                    .containsExactly(inboxId, pendingId, collectedId, archivedId);
            assertThat(materialRepository.countMaterials(query)).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("material meta")
    class MaterialMetaOperations {

        @Test
        @DisplayName("updates an existing material meta record with patch semantics")
        void givenExistingMeta_whenUpdateMeta_thenPersistsChangedFields() {
            Long materialId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.INBOX, LocalDateTime.of(2026, 4, 7, 10, 0)));
            LocalDateTime createdAt = LocalDateTime.of(2026, 4, 7, 10, 1);
            materialRepository.saveMeta(MaterialMeta.builder()
                    .materialId(materialId)
                    .author("Alice")
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());

            LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 7, 11, 0);
            materialRepository.updateMeta(MaterialMeta.builder()
                    .materialId(materialId)
                    .sourcePlatform("wechat")
                    .wordCount(1200)
                    .thumbnailKey("covers/meta.png")
                    .extraJson("{\"lang\":\"zh\"}")
                    .updatedAt(updatedAt)
                    .build());

            assertThat(materialRepository.findMetaByMaterialId(materialId))
                    .hasValueSatisfying(meta -> assertThat(meta)
                            .returns(materialId, MaterialMeta::getMaterialId)
                            .returns("Alice", MaterialMeta::getAuthor)
                            .returns("wechat", MaterialMeta::getSourcePlatform)
                            .returns(1200, MaterialMeta::getWordCount)
                            .returns("covers/meta.png", MaterialMeta::getThumbnailKey)
                            .returns("{\"lang\":\"zh\"}", MaterialMeta::getExtraJson)
                            .returns(updatedAt, MaterialMeta::getUpdatedAt));
        }
    }

    @Nested
    @DisplayName("last retrieved at")
    class LastRetrievedAt {

        @Test
        @DisplayName("writes lastRetrievedAt back to all matched materials")
        void givenMaterialIds_whenUpdateLastRetrievedAt_thenUpdatesAllRows() {
            Long firstId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.COLLECTED, LocalDateTime.of(2026, 4, 8, 10, 0)));
            Long secondId = materialRepository.saveMaterial(buildMaterial(MaterialStatus.COLLECTED, LocalDateTime.of(2026, 4, 8, 11, 0)));
            LocalDateTime retrievedAt = LocalDateTime.of(2026, 4, 9, 9, 0);

            materialRepository.updateLastRetrievedAt(List.of(firstId, secondId), retrievedAt);

            assertThat(materialRepository.findById(firstId))
                    .hasValueSatisfying(material -> assertThat(material)
                            .returns(retrievedAt, Material::getLastRetrievedAt)
                            .returns(retrievedAt, Material::getUpdatedAt));
            assertThat(materialRepository.findById(secondId))
                    .hasValueSatisfying(material -> assertThat(material)
                            .returns(retrievedAt, Material::getLastRetrievedAt)
                            .returns(retrievedAt, Material::getUpdatedAt));
        }
    }

    private Material buildMaterial(MaterialStatus status, LocalDateTime time) {
        return Material.builder()
                .userId(1L)
                .topicId(1L)
                .materialType(MaterialType.ARTICLE)
                .status(status)
                .title("测试资料")
                .rawContent("default content")
                .comment("default comment")
                .score(new BigDecimal("8.0"))
                .deleted(false)
                .inboxAt(time)
                .createdAt(time)
                .updatedAt(time)
                .build();
    }

    private MaterialTag buildTag(Long materialId, String groupKey, String value) {
        return MaterialTag.builder()
                .materialId(materialId)
                .tagType(groupKey.startsWith("sys") ? TagType.SYSTEM : TagType.USER)
                .tagGroupKey(groupKey)
                .tagValue(value)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
