package com.anbuz.domain.content.service.impl;

import com.anbuz.domain.content.adapter.ICoverStorageAdapter;
import com.anbuz.domain.content.adapter.IUrlParserAdapter;
import com.anbuz.domain.content.service.IContentProcessService;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.types.enums.MaterialType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 内容加工领域服务，负责在资料提交后补全标题、封面和 material_meta。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentProcessService implements IContentProcessService {

    private final IMaterialRepository materialRepository;
    private final IUrlParserAdapter urlParserAdapter;
    private final ICoverStorageAdapter coverStorageAdapter;

    @Override
    public void process(Long materialId) {
        log.info("Content process started materialId={}", materialId);
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material == null) {
            log.warn("Content process skipped because material was not found materialId={}", materialId);
            return;
        }

        MaterialMeta meta = materialRepository.findMetaByMaterialId(materialId)
                .orElse(MaterialMeta.builder()
                        .materialId(materialId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

        if (meta.getThumbnailKey() == null) {
            populateThumbnail(material, meta);
        }

        if (material.getTitle() == null && material.getSourceUrl() != null) {
            urlParserAdapter.extractOgTitle(material.getSourceUrl())
                    .ifPresent(title -> {
                        material.setTitle(title);
                        material.setUpdatedAt(LocalDateTime.now());
                        materialRepository.updateMaterial(material);
                    });
        }

        meta.setUpdatedAt(LocalDateTime.now());
        if (meta.getId() == null) {
            materialRepository.saveMeta(meta);
        } else {
            materialRepository.updateMeta(meta);
        }
        log.info("Content process completed materialId={} thumbnailGenerated={} titlePresent={}",
                materialId, meta.getThumbnailKey() != null, material.getTitle() != null);
    }

    private void populateThumbnail(Material material, MaterialMeta meta) {
        if (material.getMaterialType() == MaterialType.IMAGE && material.getFileKey() != null) {
            meta.setThumbnailKey(material.getFileKey());
            return;
        }
        if (material.getSourceUrl() != null) {
            tryGenerateCover(material, meta);
        }
    }

    private void tryGenerateCover(Material material, MaterialMeta meta) {
        try {
            String coverKey = null;
            if (material.getSourceUrl() != null) {
                String ogImage = urlParserAdapter.extractOgImage(material.getSourceUrl()).orElse(null);
                if (ogImage != null) {
                    coverKey = coverStorageAdapter.downloadAndUploadCover(ogImage, "cover_" + material.getId() + ".jpg");
                }
            }
            if (coverKey != null) {
                meta.setThumbnailKey(coverKey);
            }
        } catch (Exception e) {
            log.warn("Cover generation failed materialId={}", material.getId(), e);
        }
    }
}
