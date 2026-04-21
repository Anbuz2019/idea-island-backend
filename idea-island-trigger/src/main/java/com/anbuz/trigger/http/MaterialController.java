package com.anbuz.trigger.http;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.material.service.StatusTransitionService;
import com.anbuz.domain.material.service.SystemTagService;
import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.service.TopicService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.enums.MaterialAction;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import com.anbuz.types.enums.TagType;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final IMaterialRepository materialRepository;
    private final TopicService topicService;
    private final StatusTransitionService statusTransitionService;
    private final SystemTagService systemTagService;

    @PostMapping
    public Result<Long> submit(@Valid @RequestBody SubmitRequest req) {
        Long userId = UserContext.currentUserId();
        Topic topic = topicService.getTopic(req.getTopicId(), userId);
        if (!topic.isEnabled()) {
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "主题已停用，无法提交资料");
        }
        LocalDateTime now = LocalDateTime.now();
        Material material = Material.builder()
                .userId(userId)
                .topicId(req.getTopicId())
                .materialType(MaterialType.of(req.getMaterialType()))
                .status(MaterialStatus.INBOX)
                .title(req.getTitle())
                .description(req.getDescription())
                .rawContent(req.getRawContent())
                .sourceUrl(req.getSourceUrl())
                .fileKey(req.getFileKey())
                .deleted(false)
                .inboxAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        Long materialId = materialRepository.saveMaterial(material);
        return Result.ok(materialId);
    }

    @GetMapping("/{id}")
    public Result<Material> detail(@PathVariable Long id) {
        Long userId = UserContext.currentUserId();
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (!userId.equals(material.getUserId())) throw new AppException(ErrorCode.FORBIDDEN);
        return Result.ok(material);
    }

    @PostMapping("/{id}/mark-read")
    public Result<Void> markRead(@PathVariable Long id) {
        statusTransitionService.transit(id, UserContext.currentUserId(), MaterialAction.MARK_READ, null, null, null);
        return Result.ok();
    }

    @PostMapping("/{id}/collect")
    public Result<Void> collect(@PathVariable Long id, @Valid @RequestBody CollectRequest req) {
        Long userId = UserContext.currentUserId();
        Material updated = statusTransitionService.transit(id, userId, MaterialAction.COLLECT,
                req.getComment(), req.getScore(), null);
        systemTagService.refreshSystemTags(id, updated.getScore(), updated.getComment());
        return Result.ok();
    }

    @PostMapping("/{id}/archive")
    public Result<Void> archive(@PathVariable Long id) {
        statusTransitionService.transit(id, UserContext.currentUserId(), MaterialAction.ARCHIVE, null, null, null);
        return Result.ok();
    }

    @PostMapping("/{id}/invalidate")
    public Result<Void> invalidate(@PathVariable Long id, @Valid @RequestBody InvalidateRequest req) {
        statusTransitionService.transit(id, UserContext.currentUserId(), MaterialAction.INVALIDATE,
                null, null, req.getInvalidReason());
        return Result.ok();
    }

    @PostMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        statusTransitionService.transit(id, UserContext.currentUserId(), MaterialAction.RESTORE, null, null, null);
        return Result.ok();
    }

    @PostMapping("/{id}/restore-collected")
    public Result<Void> restoreCollected(@PathVariable Long id) {
        statusTransitionService.transit(id, UserContext.currentUserId(), MaterialAction.RESTORE_COLLECTED, null, null, null);
        return Result.ok();
    }

    @PutMapping("/{id}/tags")
    public Result<Void> updateTags(@PathVariable Long id, @Valid @RequestBody UpdateTagsRequest req) {
        Long userId = UserContext.currentUserId();
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (!userId.equals(material.getUserId())) throw new AppException(ErrorCode.FORBIDDEN);

        materialRepository.deleteUserTags(id);
        if (req.getTags() != null && !req.getTags().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<MaterialTag> tags = req.getTags().stream()
                    .map(t -> MaterialTag.builder()
                            .materialId(id)
                            .tagType(TagType.USER)
                            .tagGroupKey(t.getTagGroupKey())
                            .tagValue(t.getTagValue())
                            .createdAt(now)
                            .build())
                    .toList();
            materialRepository.saveTags(tags);
        }
        systemTagService.refreshSystemTags(id, material.getScore(), material.getComment());
        return Result.ok();
    }

    @Data
    public static class SubmitRequest {
        @NotNull private Long topicId;
        @NotBlank private String materialType;
        private String title;
        private String description;
        private String rawContent;
        private String sourceUrl;
        private String fileKey;
    }

    @Data
    public static class CollectRequest {
        @NotBlank private String comment;
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") private BigDecimal score;
    }

    @Data
    public static class InvalidateRequest {
        @NotBlank private String invalidReason;
    }

    @Data
    public static class UpdateTagsRequest {
        private List<TagItem> tags;

        @Data
        public static class TagItem {
            @NotBlank private String tagGroupKey;
            @NotBlank private String tagValue;
        }
    }

}
