package com.anbuz.domain.content.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.anbuz.domain.content.adapter.ICoverStorageAdapter;
import com.anbuz.domain.content.adapter.IUrlParserAdapter;
import com.anbuz.domain.content.adapter.IUrlPreviewAdapter;
import com.anbuz.domain.content.model.UrlPreviewMetadata;
import com.anbuz.domain.content.service.IContentProcessService;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.types.enums.MaterialType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 内容加工领域服务，负责在资料提交后补全标题、封面和 material_meta。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentProcessService implements IContentProcessService {

    private static final int DEFAULT_BACKFILL_LIMIT = 50;
    private static final int MAX_BACKFILL_LIMIT = 200;
    private static final String COVER_BACKFILL_STATUS = "coverBackfillStatus";
    private static final String COVER_BACKFILL_STATUS_UNAVAILABLE = "unavailable";
    private static final String COVER_BACKFILL_REASON = "coverBackfillReason";
    private static final String COVER_BACKFILL_ATTEMPTED_AT = "coverBackfillAttemptedAt";
    private static final String COVER_UNAVAILABLE_THUMBNAIL_KEY = "__cover_unavailable__";

    private final IMaterialRepository materialRepository;
    private final IUrlParserAdapter urlParserAdapter;
    private final ICoverStorageAdapter coverStorageAdapter;
    private final List<IUrlPreviewAdapter> urlPreviewAdapters;

    @Override
    public void process(Long materialId) {
        process(materialId, false);
    }

    private void process(Long materialId, boolean markUnavailableWhenMissingCover) {
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

        UrlPreviewMetadata preview = null;
        if (!hasText(meta.getThumbnailKey())) {
            preview = populateThumbnail(material, meta, preview, markUnavailableWhenMissingCover);
        }

        if (hasText(material.getSourceUrl())) {
            preview = populateTitleAndDescription(material, preview);
            preview = populateMetaFromPreview(material, meta, preview);
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

    @Override
    public int backfillMissingCovers(int limit) {
        int normalizedLimit = normalizeBackfillLimit(limit);
        List<Material> candidates = materialRepository.findMaterialsMissingThumbnail(normalizedLimit);
        int processed = 0;
        log.info("Cover backfill started limit={} candidateCount={}", normalizedLimit, candidates.size());
        for (Material material : candidates) {
            try {
                process(material.getId(), true);
                processed++;
            } catch (Exception e) {
                log.warn("Cover backfill skipped materialId={} due to processing failure",
                        material.getId(), e);
            }
        }
        log.info("Cover backfill completed processed={}", processed);
        return processed;
    }

    private UrlPreviewMetadata populateThumbnail(
            Material material, MaterialMeta meta, UrlPreviewMetadata preview, boolean markUnavailableWhenMissingCover) {
        if (material.getMaterialType() == MaterialType.IMAGE && hasText(material.getFileKey())) {
            meta.setThumbnailKey(material.getFileKey());
            return preview;
        }
        if (hasText(material.getSourceUrl())) {
            preview = tryGenerateCover(material, meta, preview, markUnavailableWhenMissingCover);
        }
        return preview;
    }

    private UrlPreviewMetadata tryGenerateCover(
            Material material, MaterialMeta meta, UrlPreviewMetadata preview, boolean markUnavailableWhenMissingCover) {
        try {
            String coverKey = null;
            String unavailableReason = null;
            if (hasText(material.getSourceUrl())) {
                String ogImage = urlParserAdapter.extractOgImage(material.getSourceUrl()).orElse(null);
                if (ogImage != null) {
                    coverKey = downloadCover(material, ogImage);
                    if (coverKey == null) {
                        unavailableReason = "og_cover_download_failed";
                    }
                }
                if (coverKey == null) {
                    preview = ensurePreviewLoaded(material.getSourceUrl(), preview);
                    if (preview != null && hasText(preview.imageUrl())) {
                        coverKey = downloadCover(material, preview.imageUrl());
                        if (coverKey == null) {
                            unavailableReason = "preview_cover_download_failed";
                        }
                    } else if (preview != null && preview.coverUnavailable()) {
                        unavailableReason = hasText(preview.coverUnavailableReason())
                                ? preview.coverUnavailableReason()
                                : "cover_unavailable";
                    } else if (preview == null) {
                        unavailableReason = "preview_unavailable";
                    } else {
                        unavailableReason = "preview_without_cover";
                    }
                }
            }
            if (coverKey != null) {
                meta.setThumbnailKey(coverKey);
                clearCoverUnavailable(meta);
            } else if (markUnavailableWhenMissingCover && hasText(unavailableReason)) {
                markCoverUnavailable(meta, unavailableReason);
            }
        } catch (Exception e) {
            log.warn("Cover generation failed materialId={}", material.getId(), e);
            if (markUnavailableWhenMissingCover) {
                markCoverUnavailable(meta, "cover_generation_failed");
            }
        }
        return preview;
    }

    private UrlPreviewMetadata populateTitleAndDescription(Material material, UrlPreviewMetadata preview) {
        boolean changed = false;
        if (!hasText(material.getTitle())) {
            Optional<String> parsedTitle = urlParserAdapter.extractOgTitle(material.getSourceUrl());
            if (parsedTitle.isPresent()) {
                material.setTitle(parsedTitle.get());
                changed = true;
            }
        }

        if (!hasText(material.getTitle()) || !hasText(material.getDescription())) {
            preview = ensurePreviewLoaded(material.getSourceUrl(), preview);
        }

        if (preview != null && !hasText(material.getTitle()) && hasText(preview.title())) {
            material.setTitle(preview.title());
            changed = true;
        }
        if (preview != null && !hasText(material.getDescription()) && hasText(preview.description())) {
            material.setDescription(preview.description());
            changed = true;
        }

        if (changed) {
            material.setUpdatedAt(LocalDateTime.now());
            materialRepository.updateMaterial(material);
        }
        return preview;
    }

    private UrlPreviewMetadata populateMetaFromPreview(Material material, MaterialMeta meta, UrlPreviewMetadata preview) {
        if (hasText(meta.getAuthor()) && hasText(meta.getSourcePlatform())) {
            return preview;
        }
        preview = ensurePreviewLoaded(material.getSourceUrl(), preview);
        if (preview == null) {
            return null;
        }
        if (!hasText(meta.getAuthor()) && hasText(preview.author())) {
            meta.setAuthor(preview.author());
        }
        if (!hasText(meta.getSourcePlatform()) && hasText(preview.sourcePlatform())) {
            meta.setSourcePlatform(preview.sourcePlatform());
        }
        return preview;
    }

    private UrlPreviewMetadata ensurePreviewLoaded(String url, UrlPreviewMetadata preview) {
        if (preview != null) {
            return preview;
        }
        if (!hasText(url) || urlPreviewAdapters == null || urlPreviewAdapters.isEmpty()) {
            return null;
        }
        for (IUrlPreviewAdapter adapter : urlPreviewAdapters) {
            try {
                Optional<UrlPreviewMetadata> result = adapter.fetchPreview(url);
                if (result.isPresent() && result.get().hasAny()) {
                    return result.get();
                }
            } catch (Exception e) {
                log.warn("External link preview failed url={}: {}", url, e.getMessage());
            }
        }
        return null;
    }

    private String downloadCover(Material material, String imageUrl) {
        try {
            return coverStorageAdapter.downloadAndUploadCover(imageUrl, "cover_" + material.getId() + ".jpg");
        } catch (Exception e) {
            log.warn("Cover download failed materialId={} imageUrl={}: {}", material.getId(), imageUrl, e.getMessage());
            return null;
        }
    }

    private void markCoverUnavailable(MaterialMeta meta, String reason) {
        meta.setThumbnailKey(COVER_UNAVAILABLE_THUMBNAIL_KEY);
        JSONObject extra = parseExtraJson(meta.getExtraJson());
        extra.put(COVER_BACKFILL_STATUS, COVER_BACKFILL_STATUS_UNAVAILABLE);
        extra.put(COVER_BACKFILL_REASON, reason);
        extra.put(COVER_BACKFILL_ATTEMPTED_AT, LocalDateTime.now().toString());
        meta.setExtraJson(extra.toJSONString());
    }

    private void clearCoverUnavailable(MaterialMeta meta) {
        if (!hasText(meta.getExtraJson())) {
            return;
        }
        JSONObject extra = parseExtraJson(meta.getExtraJson());
        if (!COVER_BACKFILL_STATUS_UNAVAILABLE.equals(extra.getString(COVER_BACKFILL_STATUS))) {
            return;
        }
        extra.remove(COVER_BACKFILL_STATUS);
        extra.remove(COVER_BACKFILL_REASON);
        extra.remove(COVER_BACKFILL_ATTEMPTED_AT);
        meta.setExtraJson(extra.toJSONString());
        if (COVER_UNAVAILABLE_THUMBNAIL_KEY.equals(meta.getThumbnailKey())) {
            meta.setThumbnailKey(null);
        }
    }

    private JSONObject parseExtraJson(String value) {
        if (!hasText(value)) {
            return new JSONObject();
        }
        try {
            JSONObject parsed = JSON.parseObject(value);
            return parsed == null ? new JSONObject() : parsed;
        } catch (Exception ignored) {
            JSONObject fallback = new JSONObject();
            fallback.put("legacyExtraJson", value);
            return fallback;
        }
    }

    private int normalizeBackfillLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_BACKFILL_LIMIT;
        }
        return Math.min(limit, MAX_BACKFILL_LIMIT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
