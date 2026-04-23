package com.anbuz.domain.material.service;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.Completeness;
import com.anbuz.domain.material.model.valobj.ScoreRange;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.material.service.impl.SystemTagService;
import com.anbuz.types.enums.TagType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemTagService scenarios")
class SystemTagServiceTest {

    private static final Long MATERIAL_ID = 100L;

    @Mock
    private IMaterialRepository materialRepository;

    @InjectMocks
    private SystemTagService systemTagService;

    @Nested
    @DisplayName("refresh system tags")
    class RefreshSystemTags {

        @Test
        @DisplayName("loads missing score and comment from the material before rebuilding the two system tags")
        void givenMissingScoreAndComment_whenRefreshSystemTags_thenLoadsMaterialAndRebuildsTags() {
            Material material = Material.builder()
                    .id(MATERIAL_ID)
                    .score(new BigDecimal("8.5"))
                    .comment("clear summary")
                    .build();
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findTagsByMaterialIdAndType(MATERIAL_ID, TagType.USER))
                    .thenReturn(List.of(MaterialTag.builder().tagValue("backend").build()));

            systemTagService.refreshSystemTags(MATERIAL_ID, null, null);

            verify(materialRepository).deleteTagByMaterialIdAndGroupKey(MATERIAL_ID, SystemTagService.SYS_SCORE_RANGE);
            verify(materialRepository).deleteTagByMaterialIdAndGroupKey(MATERIAL_ID, SystemTagService.SYS_COMPLETENESS);

            ArgumentCaptor<List<MaterialTag>> captor = ArgumentCaptor.forClass(List.class);
            verify(materialRepository, org.mockito.Mockito.times(2)).saveTags(captor.capture());
            assertThat(captor.getAllValues())
                    .flatExtracting(tags -> tags)
                    .extracting(MaterialTag::getTagGroupKey, MaterialTag::getTagValue, MaterialTag::getTagType)
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple(
                                    SystemTagService.SYS_SCORE_RANGE,
                                    ScoreRange.of(new BigDecimal("8.5")).getLabel(),
                                    TagType.SYSTEM),
                            org.assertj.core.groups.Tuple.tuple(
                                    SystemTagService.SYS_COMPLETENESS,
                                    Completeness.of(List.of("backend"), "clear summary", new BigDecimal("8.5")).getLabel(),
                                    TagType.SYSTEM)
                    );
        }

        @Test
        @DisplayName("uses provided score and comment directly without reloading the material")
        void givenScoreAndComment_whenRefreshSystemTags_thenSkipsMaterialLookup() {
            when(materialRepository.findTagsByMaterialIdAndType(MATERIAL_ID, TagType.USER)).thenReturn(List.of());

            systemTagService.refreshSystemTags(MATERIAL_ID, new BigDecimal("0.0"), "captured");

            verify(materialRepository, never()).findById(MATERIAL_ID);
            verify(materialRepository).deleteTagByMaterialIdAndGroupKey(MATERIAL_ID, SystemTagService.SYS_SCORE_RANGE);
            verify(materialRepository).deleteTagByMaterialIdAndGroupKey(MATERIAL_ID, SystemTagService.SYS_COMPLETENESS);
        }
    }
}
