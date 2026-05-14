package com.anbuz.domain.content.service;

import com.anbuz.domain.content.adapter.ICoverStorageAdapter;
import com.anbuz.domain.content.adapter.IUrlParserAdapter;
import com.anbuz.domain.content.adapter.IUrlPreviewAdapter;
import com.anbuz.domain.content.model.UrlPreviewMetadata;
import com.anbuz.domain.content.service.impl.ContentProcessService;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.types.enums.MaterialType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private IUrlPreviewAdapter urlPreviewAdapter;

    private ContentProcessService contentProcessService;

    @BeforeEach
    void setUp() {
        contentProcessService = new ContentProcessService(
                materialRepository,
                urlParserAdapter,
                coverStorageAdapter,
                List.of(urlPreviewAdapter)
        );
    }

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("skips processing when the material does not exist")
        void givenMissingMaterial_whenProcess_thenSkipsExternalCalls() {
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.empty());

            contentProcessService.process(MATERIAL_ID);

            verify(materialRepository).findById(MATERIAL_ID);
            verifyNoInteractions(urlParserAdapter, coverStorageAdapter, urlPreviewAdapter);
        }

        @Test
        @DisplayName("fills OG title and cover for an article with sourceUrl and existing meta")
        void givenArticleWithoutTitleOrThumbnail_whenProcess_thenUpdatesMaterialAndMeta() {
            Material material = Material.builder()
                    .id(MATERIAL_ID)
                    .userId(1L)
                    .materialType(MaterialType.ARTICLE)
                    .sourceUrl("https://example.com/article")
                    .description("Existing description")
                    .build();
            MaterialMeta meta = MaterialMeta.builder()
                    .id(9L)
                    .materialId(MATERIAL_ID)
                    .author("Existing author")
                    .sourcePlatform("Existing platform")
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
                    .returns("Existing description", Material::getDescription)
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

        @Test
        @DisplayName("uses LinkPreview metadata when local parsing cannot provide title or cover")
        void givenLocalParsingMisses_whenProcess_thenUsesLinkPreviewFallback() {
            Material material = Material.builder()
                    .id(MATERIAL_ID)
                    .userId(1L)
                    .materialType(MaterialType.MEDIA)
                    .sourceUrl("https://example.com/video")
                    .build();
            MaterialMeta meta = MaterialMeta.builder()
                    .id(10L)
                    .materialId(MATERIAL_ID)
                    .build();
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.of(meta));
            when(urlParserAdapter.extractOgImage("https://example.com/video")).thenReturn(Optional.empty());
            when(urlParserAdapter.extractOgTitle("https://example.com/video")).thenReturn(Optional.empty());
            when(urlPreviewAdapter.fetchPreview("https://example.com/video"))
                    .thenReturn(Optional.of(new UrlPreviewMetadata(
                            "Fallback title",
                            "Fallback description",
                            "https://cdn.example.com/cover.png",
                            "Fallback author",
                            "Fallback platform"
                    )));
            when(coverStorageAdapter.downloadAndUploadCover("https://cdn.example.com/cover.png", "cover_100.jpg"))
                    .thenReturn("fallback-cover-key");

            contentProcessService.process(MATERIAL_ID);

            ArgumentCaptor<Material> materialCaptor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).updateMaterial(materialCaptor.capture());
            assertThat(materialCaptor.getValue())
                    .returns("Fallback title", Material::getTitle)
                    .returns("Fallback description", Material::getDescription);

            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).updateMeta(metaCaptor.capture());
            assertThat(metaCaptor.getValue())
                    .returns("fallback-cover-key", MaterialMeta::getThumbnailKey)
                    .returns("Fallback author", MaterialMeta::getAuthor)
                    .returns("Fallback platform", MaterialMeta::getSourcePlatform);
        }

        @Test
        @DisplayName("does not write placeholder during immediate content processing")
        void givenCoverUnavailable_whenProcess_thenKeepsThumbnailEmptyForLaterBackfill() {
            Material material = Material.builder()
                    .id(MATERIAL_ID)
                    .userId(1L)
                    .materialType(MaterialType.MEDIA)
                    .sourceUrl("https://example.com/restricted")
                    .title("Existing title")
                    .description("Existing description")
                    .build();
            MaterialMeta meta = MaterialMeta.builder()
                    .id(11L)
                    .materialId(MATERIAL_ID)
                    .build();
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.of(meta));
            when(urlParserAdapter.extractOgImage("https://example.com/restricted")).thenReturn(Optional.empty());
            when(urlPreviewAdapter.fetchPreview("https://example.com/restricted"))
                    .thenReturn(Optional.of(UrlPreviewMetadata.coverUnavailable("link_preview_robots_forbidden")));

            contentProcessService.process(MATERIAL_ID);

            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).updateMeta(metaCaptor.capture());
            assertThat(metaCaptor.getValue().getThumbnailKey()).isNull();
            assertThat(metaCaptor.getValue().getExtraJson()).isNull();
            verify(coverStorageAdapter, never()).downloadAndUploadCover(
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Nested
    @DisplayName("backfillMissingCovers")
    class BackfillMissingCovers {

        @Test
        @DisplayName("loads missing thumbnail candidates and processes each material")
        void givenCandidates_whenBackfillMissingCovers_thenProcessesEachCandidate() {
            Material first = Material.builder().id(101L).build();
            Material second = Material.builder().id(102L).build();
            when(materialRepository.findMaterialsMissingThumbnail(2)).thenReturn(List.of(first, second));
            when(materialRepository.findById(101L)).thenReturn(Optional.empty());
            when(materialRepository.findById(102L)).thenReturn(Optional.empty());

            int processed = contentProcessService.backfillMissingCovers(2);

            assertThat(processed).isEqualTo(2);
            verify(materialRepository).findMaterialsMissingThumbnail(2);
            verify(materialRepository).findById(101L);
            verify(materialRepository).findById(102L);
        }

        @Test
        @DisplayName("marks placeholder when scheduled backfill cannot resolve a cover")
        void givenNoCoverResolved_whenBackfillMissingCovers_thenWritesThumbnailPlaceholder() {
            Material material = Material.builder()
                    .id(MATERIAL_ID)
                    .userId(1L)
                    .materialType(MaterialType.ARTICLE)
                    .sourceUrl("https://example.com/no-cover")
                    .title("Existing title")
                    .description("Existing description")
                    .build();
            MaterialMeta meta = MaterialMeta.builder()
                    .id(11L)
                    .materialId(MATERIAL_ID)
                    .build();
            when(materialRepository.findMaterialsMissingThumbnail(1)).thenReturn(List.of(material));
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));
            when(materialRepository.findMetaByMaterialId(MATERIAL_ID)).thenReturn(Optional.of(meta));
            when(urlParserAdapter.extractOgImage("https://example.com/no-cover")).thenReturn(Optional.empty());
            when(urlPreviewAdapter.fetchPreview("https://example.com/no-cover")).thenReturn(Optional.empty());

            int processed = contentProcessService.backfillMissingCovers(1);

            ArgumentCaptor<MaterialMeta> metaCaptor = ArgumentCaptor.forClass(MaterialMeta.class);
            verify(materialRepository).updateMeta(metaCaptor.capture());
            assertThat(processed).isEqualTo(1);
            assertThat(metaCaptor.getValue().getThumbnailKey()).isEqualTo("__cover_unavailable__");
            assertThat(metaCaptor.getValue().getExtraJson())
                    .contains("\"coverBackfillStatus\":\"unavailable\"")
                    .contains("\"coverBackfillReason\":\"preview_unavailable\"");
        }

        @Test
        @DisplayName("uses default batch size when requested limit is invalid")
        void givenInvalidLimit_whenBackfillMissingCovers_thenUsesDefaultLimit() {
            when(materialRepository.findMaterialsMissingThumbnail(50)).thenReturn(List.of());

            int processed = contentProcessService.backfillMissingCovers(0);

            assertThat(processed).isZero();
            verify(materialRepository).findMaterialsMissingThumbnail(50);
        }

        @Test
        @DisplayName("caps backfill batch size")
        void givenTooLargeLimit_whenBackfillMissingCovers_thenCapsLimit() {
            when(materialRepository.findMaterialsMissingThumbnail(200)).thenReturn(List.of());

            int processed = contentProcessService.backfillMissingCovers(1000);

            assertThat(processed).isZero();
            verify(materialRepository).findMaterialsMissingThumbnail(200);
            verify(materialRepository, times(1)).findMaterialsMissingThumbnail(200);
        }
    }
}
