package com.anbuz.domain.content.service;

import com.anbuz.domain.content.adapter.ICoverStorageAdapter;
import com.anbuz.domain.content.adapter.IUrlParserAdapter;
import com.anbuz.domain.content.service.impl.ContentProcessService;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.types.enums.MaterialType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentProcessService scenarios")
class ContentProcessServiceTest {

    private static final Long MATERIAL_ID = 100L;

    @Mock
    private IMaterialRepository materialRepository;

    @Mock
    private IUrlParserAdapter urlParserAdapter;

    @Mock
    private ICoverStorageAdapter coverStorageAdapter;

    @InjectMocks
    private ContentProcessService contentProcessService;

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("skips processing when the material does not exist")
        void givenMissingMaterial_whenProcess_thenSkipsExternalCalls() {
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.empty());

            contentProcessService.process(MATERIAL_ID);

            verify(materialRepository).findById(MATERIAL_ID);
            verifyNoInteractions(urlParserAdapter, coverStorageAdapter);
        }

        @Test
        @DisplayName("fills OG title and cover for an article with sourceUrl and existing meta")
        void givenArticleWithoutTitleOrThumbnail_whenProcess_thenUpdatesMaterialAndMeta() {
            Material material = Material.builder()
                    .id(MATERIAL_ID)
                    .userId(1L)
                    .materialType(MaterialType.ARTICLE)
                    .sourceUrl("https://example.com/article")
                    .build();
            MaterialMeta meta = MaterialMeta.builder()
                    .id(9L)
                    .materialId(MATERIAL_ID)
                    .build();
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.of(meta));
            when(urlParserAdapter.extractOgImage("https://example.com/article"))
                    .thenReturn(Optional.of("https://example.com/cover.jpg"));
            when(coverStorageAdapter.downloadAndUploadCover("https://example.com/cover.jpg", "cover_100.jpg"))
                    .thenReturn("cover-key");
            when(urlParserAdapter.extractOgTitle("https://example.com/article"))
                    .thenReturn(Optional.of("Redis in action"));

            contentProcessService.process(MATERIAL_ID);

            ArgumentCaptor<Material> materialCaptor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).updateMaterial(materialCaptor.capture());
            assertThat(materialCaptor.getValue())
                    .returns("Redis in action", Material::getTitle)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());

            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).updateMeta(metaCaptor.capture());
            assertThat(metaCaptor.getValue())
                    .returns("cover-key", MaterialMeta::getThumbnailKey)
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
        }

        @Test
        @DisplayName("uses fileKey directly as thumbnail for image materials when meta is missing")
        void givenImageMaterialWithoutMeta_whenProcess_thenCreatesMetaWithFileKeyThumbnail() {
            Material material = Material.builder()
                    .id(MATERIAL_ID)
                    .userId(1L)
                    .materialType(MaterialType.IMAGE)
                    .fileKey("images/cover.png")
                    .build();
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.empty());

            contentProcessService.process(MATERIAL_ID);

            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).saveMeta(metaCaptor.capture());
            assertThat(metaCaptor.getValue())
                    .returns(MATERIAL_ID, MaterialMeta::getMaterialId)
                    .returns("images/cover.png", MaterialMeta::getThumbnailKey)
                    .satisfies(saved -> assertThat(saved.getCreatedAt()).isNotNull())
                    .satisfies(saved -> assertThat(saved.getUpdatedAt()).isNotNull());
            verify(materialRepository, never()).updateMaterial(org.mockito.ArgumentMatchers.any());
            verifyNoInteractions(urlParserAdapter, coverStorageAdapter);
        }
    }
}
