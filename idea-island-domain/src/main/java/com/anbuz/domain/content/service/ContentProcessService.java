package com.anbuz.domain.content.service;

import com.anbuz.domain.content.adapter.ICoverStorageAdapter;
import com.anbuz.domain.content.adapter.IUrlParserAdapter;
import com.anbuz.domain.content.model.valobj.ParseResult;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.types.enums.MaterialType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentProcessService {

    private final IMaterialRepository materialRepository;
    private final IUrlParserAdapter urlParserAdapter;
    private final ICoverStorageAdapter coverStorageAdapter;

    public void process(Long materialId) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material == null) {
            log.warn("ContentProcess: 资料不存在 materialId={}", materialId);
            return;
        }

        MaterialMeta meta = materialRepository.findMetaByMaterialId(materialId)
                .orElse(MaterialMeta.builder().materialId(materialId)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        // 仅当封面未设置时才自动生成
        if (meta.getThumbnailKey() == null && material.getSourceUrl() != null) {
            tryGenerateCover(material, meta);
        }

        // 补全 OG title（仅 article 类型，且标题为空）
        if (material.getMaterialType() == MaterialType.ARTICLE
                && material.getTitle() == null
                && material.getSourceUrl() != null) {
            urlParserAdapter.extractOgTitle(material.getSourceUrl())
                    .ifPresent(material::setTitle);
            material.setUpdatedAt(LocalDateTime.now());
            materialRepository.updateMaterial(material);
        }

        meta.setUpdatedAt(LocalDateTime.now());
        if (meta.getId() == null) {
            materialRepository.saveMeta(meta);
        } else {
            materialRepository.updateMeta(meta);
        }
    }

    private void tryGenerateCover(Material material, MaterialMeta meta) {
        try {
            String coverKey = null;
            if (material.getSourceUrl() != null) {
                String ogImage = urlParserAdapter.extractOgImage(material.getSourceUrl()).orElse(null);
                if (ogImage != null) {
                    coverKey = coverStorageAdapter.downloadAndUploadCover(
                            ogImage, "cover_" + material.getId() + ".jpg");
                }
            }
            if (coverKey != null) {
                meta.setThumbnailKey(coverKey);
            }
        } catch (Exception e) {
            log.warn("封面生成失败 materialId={}", material.getId(), e);
        }
    }

}
