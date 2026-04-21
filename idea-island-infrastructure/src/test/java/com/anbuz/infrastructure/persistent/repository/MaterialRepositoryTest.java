package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.infrastructure.persistent.dao.IMaterialDao;
import com.anbuz.infrastructure.persistent.dao.IMaterialMetaDao;
import com.anbuz.infrastructure.persistent.dao.IMaterialTagDao;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(MaterialRepository.class)
@DisplayName("MaterialRepository H2 集成测试")
class MaterialRepositoryTest {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private IMaterialDao materialDao;

    @Autowired
    private IMaterialMetaDao materialMetaDao;

    @Autowired
    private IMaterialTagDao materialTagDao;

    @Nested
    @DisplayName("保存资料")
    class SaveMaterial {

        @Test
        @DisplayName("保存资料后可按 ID 查询到，且状态为 INBOX")
        void givenNewMaterial_whenSave_thenCanFindById() {
            Material material = buildMaterial();

            Long id = materialRepository.saveMaterial(material);

            assertThat(id).isNotNull().isPositive();
            assertThat(materialRepository.findById(id))
                    .isPresent()
                    .get()
                    .satisfies(m -> {
                        assertThat(m.getStatus()).isEqualTo(MaterialStatus.INBOX);
                        assertThat(m.getMaterialType()).isEqualTo(MaterialType.ARTICLE);
                        assertThat(m.getUserId()).isEqualTo(1L);
                    });
        }
    }

    @Nested
    @DisplayName("标签操作")
    class TagOperations {

        @Test
        @DisplayName("批量保存标签后可按资料 ID 查询到")
        void givenTags_whenSave_thenCanFindByMaterialId() {
            Material material = buildMaterial();
            Long materialId = materialRepository.saveMaterial(material);

            List<MaterialTag> tags = List.of(
                    buildTag(materialId, "sys_score_range", "高"),
                    buildTag(materialId, "user_group_1", "需求分析")
            );
            materialRepository.saveTags(tags);

            List<MaterialTag> found = materialRepository.findTagsByMaterialId(materialId);
            assertThat(found).hasSize(2);
            assertThat(found).extracting(MaterialTag::getTagGroupKey)
                    .containsExactlyInAnyOrder("sys_score_range", "user_group_1");
        }

        @Test
        @DisplayName("删除用户标签后，系统标签仍保留")
        void givenMixedTags_whenDeleteUserTags_thenSystemTagsRemain() {
            Material material = buildMaterial();
            Long materialId = materialRepository.saveMaterial(material);

            materialRepository.saveTags(List.of(
                    buildTag(materialId, "sys_score_range", "高"),
                    buildTag(materialId, "user_group_1", "需求分析")
            ));

            materialRepository.deleteUserTags(materialId);

            List<MaterialTag> remaining = materialRepository.findTagsByMaterialId(materialId);
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getTagGroupKey()).isEqualTo("sys_score_range");
        }
    }

    private Material buildMaterial() {
        LocalDateTime now = LocalDateTime.now();
        return Material.builder()
                .userId(1L)
                .topicId(1L)
                .materialType(MaterialType.ARTICLE)
                .status(MaterialStatus.INBOX)
                .title("测试资料")
                .isDeleted(false)
                .inboxAt(now)
                .createdAt(now)
                .updatedAt(now)
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
