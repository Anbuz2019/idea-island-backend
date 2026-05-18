package com.anbuz.trigger.http;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.anbuz.api.http.IMaterialController;
import com.anbuz.domain.content.model.UrlPreviewMetadata;
import com.anbuz.domain.content.service.IContentProcessService;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.service.IMaterialService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * 资料 HTTP 适配器，负责把资料请求转换为资料域服务调用并组织 API 响应。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MaterialController implements IMaterialController {

    private final IMaterialService materialService;
    private final IContentProcessService contentProcessService;

    @Override
    public Result<MaterialPageResponse> list(@Valid ListMaterialsRequest request) {
        Long userId = UserContext.currentUserId();
        log.debug("List materials userId={} topicId={} page={} pageSize={}",
                userId, request.getTopicId(), request.getPage(), request.getPageSize());
        return Result.ok(MaterialApiMapper.toPageResponse(materialService.listMaterials(userId, MaterialListQuery.builder()
                .topicId(request.getTopicId())
                .statuses(request.getStatus())
                .materialTypes(request.getMaterialType())
                .scoreMin(request.getScoreMin())
                .scoreMax(request.getScoreMax())
                .createdStart(request.getCreatedStart())
                .createdEnd(request.getCreatedEnd())
                .keyword(request.getKeyword())
                .sortBy(request.getSortBy())
                .sortDirection(request.getSortDirection())
                .tagFilters(parseTagFilters(request.getTagFilters()))
                .unreadOnly(Boolean.TRUE.equals(request.getUnreadOnly()))
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .build())));
    }

    @Override
    public Result<Long> submit(@Valid @RequestBody SubmitRequest req) {
        Long userId = UserContext.currentUserId();
        log.info("Submit material requested userId={} topicId={} materialType={}",
                userId, req.getTopicId(), req.getMaterialType());
        Long materialId = materialService.submit(userId, IMaterialService.SubmitCommand.builder()
                .topicId(req.getTopicId())
                .materialType(req.getMaterialType())
                .title(req.getTitle())
                .description(req.getDescription())
                .rawContent(req.getRawContent())
                .sourceUrl(req.getSourceUrl())
                .fileKey(req.getFileKey())
                .author(req.getAuthor())
                .sourcePlatform(req.getSourcePlatform())
                .publishTime(req.getPublishTime())
                .durationSeconds(req.getDurationSeconds())
                .thumbnailKey(req.getThumbnailKey())
                .tags(toTagInputs(req.getTags()))
                .build());
        log.info("Submit material succeeded userId={} topicId={} materialId={}", userId, req.getTopicId(), materialId);
        return Result.ok(materialId);
    }

    @Override
    public Result<LinkPreviewResponse> previewLink(String url) {
        Long userId = UserContext.currentUserId();
        String normalizedUrl = normalizeUrl(url);
        log.info("Preview material link requested userId={} url={}", userId, normalizedUrl);
        Optional<UrlPreviewMetadata> metadata = contentProcessService.previewUrl(normalizedUrl);
        LinkPreviewResponse response = new LinkPreviewResponse();
        response.setUrl(normalizedUrl);
        response.setMaterialType(inferMaterialType(normalizedUrl));
        if (metadata.isPresent()) {
            UrlPreviewMetadata preview = metadata.get();
            response.setTitle(preview.title());
            response.setDescription(preview.description());
            response.setImageUrl(preview.imageUrl());
            response.setAuthor(preview.author());
            response.setSourcePlatform(preview.sourcePlatform());
        }
        if (response.getSourcePlatform() == null || response.getSourcePlatform().isBlank()) {
            response.setSourcePlatform(inferSourcePlatform(normalizedUrl));
        }
        log.info("Preview material link completed userId={} titlePresent={} coverPresent={}",
                userId, response.getTitle() != null, response.getImageUrl() != null);
        return Result.ok(response);
    }

    @Override
    public Result<MaterialDetailResponse> detail(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        log.debug("Load material detail userId={} materialId={}", userId, id);
        return Result.ok(MaterialApiMapper.toDetailResponse(materialService.getDetail(userId, id)));
    }

    @Override
    public Result<MaterialDetailResponse> updateBasic(@PathVariable Long id, @Valid @RequestBody UpdateBasicRequest req) {
        Long userId = UserContext.currentUserId();
        log.info("Update material basic requested userId={} materialId={} typeChanged={} titleChanged={} descriptionChanged={} contentChanged={} sourceUrlChanged={} commentChanged={} scoreChanged={}",
                userId, id, req.getMaterialType() != null, req.getTitle() != null, req.getDescription() != null,
                req.getRawContent() != null, req.getSourceUrl() != null, req.getComment() != null, req.getScore() != null);
        MaterialDetailResponse response = MaterialApiMapper.toDetailResponse(materialService.updateBasic(userId, id,
                IMaterialService.UpdateBasicCommand.builder()
                        .materialType(req.getMaterialType())
                        .title(req.getTitle())
                        .description(req.getDescription())
                        .rawContent(req.getRawContent())
                        .sourceUrl(req.getSourceUrl())
                        .comment(req.getComment())
                        .score(req.getScore())
                        .build()));
        log.info("Update material basic succeeded userId={} materialId={}", userId, id);
        return Result.ok(response);
    }

    @Override
    public Result<MaterialDetailResponse> updateMeta(@PathVariable Long id, @Valid @RequestBody UpdateMetaRequest req) {
        Long userId = UserContext.currentUserId();
        log.info("Update material meta requested userId={} materialId={} authorChanged={} platformChanged={} publishTimeChanged={} wordCountChanged={} durationChanged={} thumbnailChanged={} extraJsonChanged={}",
                userId, id, req.getAuthor() != null, req.getSourcePlatform() != null, req.getPublishTime() != null,
                req.getWordCount() != null, req.getDurationSeconds() != null, req.getThumbnailKey() != null, req.getExtraJson() != null);
        MaterialDetailResponse response = MaterialApiMapper.toDetailResponse(materialService.updateMeta(userId, id,
                IMaterialService.UpdateMetaCommand.builder()
                        .author(req.getAuthor())
                        .sourcePlatform(req.getSourcePlatform())
                        .publishTime(req.getPublishTime())
                        .wordCount(req.getWordCount())
                        .durationSeconds(req.getDurationSeconds())
                        .thumbnailKey(req.getThumbnailKey())
                        .extraJson(req.getExtraJson())
                        .build()));
        log.info("Update material meta succeeded userId={} materialId={}", userId, id);
        return Result.ok(response);
    }

    @Override
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        materialService.deleteMaterial(userId, id);
        log.info("Delete material succeeded userId={} materialId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        materialService.markRead(userId, id);
        log.info("Mark material read succeeded userId={} materialId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<Void> collect(@PathVariable Long id, @Valid @RequestBody CollectRequest req) {
        Long userId = UserContext.currentUserId();
        materialService.collect(userId, id, req.getComment(), req.getScore());
        log.info("Collect material succeeded userId={} materialId={} score={}", userId, id, req.getScore());
        return Result.ok();
    }

    @Override
    public Result<Void> archive(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        materialService.archive(userId, id);
        log.info("Archive material succeeded userId={} materialId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<Void> invalidate(@PathVariable Long id, @Valid @RequestBody InvalidateRequest req) {
        Long userId = UserContext.currentUserId();
        materialService.invalidate(userId, id, req.getInvalidReason());
        log.info("Invalidate material succeeded userId={} materialId={} reason={}", userId, id, req.getInvalidReason());
        return Result.ok();
    }

    @Override
    public Result<Void> restore(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        materialService.restore(userId, id);
        log.info("Restore material to inbox succeeded userId={} materialId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<Void> restoreCollected(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        materialService.restoreCollected(userId, id);
        log.info("Restore material to collected succeeded userId={} materialId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<Void> moveToTopicInbox(@PathVariable Long id, @Valid @RequestBody MoveTopicRequest req) {
        Long userId = UserContext.currentUserId();
        materialService.moveToTopicInbox(userId, id, req.getTargetTopicId());
        log.info("Move material to topic inbox succeeded userId={} materialId={} targetTopicId={}", userId, id, req.getTargetTopicId());
        return Result.ok();
    }
    @Override
    public Result<Void> updateTags(@PathVariable Long id, @Valid @RequestBody UpdateTagsRequest req) {
        Long userId = UserContext.currentUserId();
        materialService.updateTags(userId, id, toTagInputs(req.getTags()));
        log.info("Update material tags succeeded userId={} materialId={} tagCount={}",
                userId, id, req.getTags() == null ? 0 : req.getTags().size());
        return Result.ok();
    }

    private List<IMaterialService.TagInput> toTagInputs(List<UpdateTagsRequest.TagItem> tags) {
        return tags == null ? List.of() : tags.stream()
                .map(tag -> new IMaterialService.TagInput(tag.getTagGroupKey(), tag.getTagValue()))
                .toList();
    }

    private List<MaterialListQuery.TagFilter> parseTagFilters(String tagFilters) {
        if (tagFilters == null || tagFilters.isBlank()) {
            return null;
        }
        try {
            return JSON.parseObject(tagFilters, new TypeReference<>() {});
        } catch (Exception e) {
            throw new AppException(ErrorCode.PARAM_INVALID, "tagFilters 格式非法");
        }
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new AppException(ErrorCode.PARAM_INVALID, "链接不能为空");
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new AppException(ErrorCode.PARAM_INVALID, "链接格式不正确");
        }
        return trimmed;
    }

    private String inferMaterialType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("bilibili.com/video/")
                || lowerUrl.contains("b23.tv/")
                || lowerUrl.matches(".*\\.(mp4|mov|m4v|webm)(\\?.*)?$")) {
            return "media";
        }
        if (lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)(\\?.*)?$")) {
            return "image";
        }
        return "article";
    }

    private String inferSourcePlatform(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            String lowerHost = host.toLowerCase();
            if (lowerHost.endsWith("bilibili.com") || lowerHost.endsWith("b23.tv")) {
                return "哔哩哔哩";
            }
            return lowerHost.replaceFirst("^www\\.", "");
        } catch (Exception ignored) {
            return null;
        }
    }
}
