package com.anbuz.api.http;

import com.anbuz.types.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资料接口契约，定义资料提交、检索、状态流转和标签管理等 HTTP 边界。
 */
@Tag(name = "资料接口", description = "资料提交、列表、详情、编辑、标签、状态流转和删除")
@RequestMapping("/api/v1/materials")
public interface IMaterialController {

    @Operation(summary = "查询资料列表", description = "按主题、状态、类型、评分、时间、关键词和标签筛选资料")
    @GetMapping
    Result<MaterialPageResponse> list(@Valid @ModelAttribute ListMaterialsRequest request);

    @Operation(summary = "提交资料", description = "提交文章、社交内容、媒体、图片、摘录或手输资料，初始状态为收件箱")
    @PostMapping
    Result<Long> submit(@Valid @RequestBody SubmitRequest req);

    @Operation(summary = "查询资料详情", description = "返回资料基本信息、元信息、标签和状态历史")
    @GetMapping("/{id}")
    Result<MaterialDetailResponse> detail(@Parameter(description = "资料 ID", required = true) @PathVariable Long id);

    @Operation(summary = "编辑资料基本信息", description = "修改标题、正文内容或来源链接，已失效资料不可编辑")
    @PatchMapping("/{id}")
    Result<MaterialDetailResponse> updateBasic(@Parameter(description = "资料 ID", required = true) @PathVariable Long id,
                                               @Valid @RequestBody UpdateBasicRequest req);

    @Operation(summary = "补全资料元信息", description = "PATCH 语义更新作者、来源平台、发布时间、字数、时长、封面和扩展信息")
    @PatchMapping("/{id}/meta")
    Result<MaterialDetailResponse> updateMeta(@Parameter(description = "资料 ID", required = true) @PathVariable Long id,
                                              @Valid @RequestBody UpdateMetaRequest req);

    @Operation(summary = "删除资料", description = "逻辑删除资料；仅失效状态资料允许删除")
    @DeleteMapping("/{id}")
    Result<Void> delete(@Parameter(description = "资料 ID", required = true) @PathVariable Long id);

    @Operation(summary = "标记资料已读", description = "将收件箱资料流转到待评审状态")
    @PostMapping("/{id}/mark-read")
    Result<Void> markRead(@Parameter(description = "资料 ID", required = true) @PathVariable Long id);

    @Operation(summary = "完成资料评价", description = "填写评语和评分，将资料收录")
    @PostMapping("/{id}/collect")
    Result<Void> collect(@Parameter(description = "资料 ID", required = true) @PathVariable Long id,
                         @Valid @RequestBody CollectRequest req);

    @Operation(summary = "归档资料", description = "将已收录资料归档")
    @PostMapping("/{id}/archive")
    Result<Void> archive(@Parameter(description = "资料 ID", required = true) @PathVariable Long id);

    @Operation(summary = "标记资料失效", description = "填写失效原因，将资料流转为失效状态")
    @PostMapping("/{id}/invalidate")
    Result<Void> invalidate(@Parameter(description = "资料 ID", required = true) @PathVariable Long id,
                            @Valid @RequestBody InvalidateRequest req);

    @Operation(summary = "恢复资料到收件箱", description = "将失效资料恢复到收件箱并清空失效信息")
    @PostMapping("/{id}/restore")
    Result<Void> restore(@Parameter(description = "资料 ID", required = true) @PathVariable Long id);

    @Operation(summary = "恢复资料到已收录", description = "将归档资料恢复到已收录状态并清空归档时间")
    @PostMapping("/{id}/restore-collected")
    Result<Void> restoreCollected(@Parameter(description = "资料 ID", required = true) @PathVariable Long id);

    @Operation(summary = "覆盖更新资料标签", description = "以请求中的用户标签全量替换资料当前用户标签，并刷新系统标签")
    @PutMapping("/{id}/tags")
    Result<Void> updateTags(@Parameter(description = "资料 ID", required = true) @PathVariable Long id,
                            @Valid @RequestBody UpdateTagsRequest req);

    @Schema(description = "资料列表查询请求")
    @Data
    class ListMaterialsRequest {
        @Schema(description = "主题 ID", example = "1")
        @NotNull
        private Long topicId;
        @Schema(description = "状态过滤，可多选", example = "[\"INBOX\",\"COLLECTED\"]")
        private List<String> status;
        @Schema(description = "资料类型过滤，可多选", example = "[\"article\",\"input\"]")
        private List<String> materialType;
        @Schema(description = "最低评分", example = "7.0")
        private BigDecimal scoreMin;
        @Schema(description = "最高评分", example = "10.0")
        private BigDecimal scoreMax;
        @Schema(description = "创建时间起点，ISO 日期时间")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime createdStart;
        @Schema(description = "创建时间终点，ISO 日期时间")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime createdEnd;
        @Schema(description = "关键词，匹配标题和正文", example = "redis")
        private String keyword;
        @Schema(description = "排序字段：createdAt、updatedAt、score、status、statusAt", example = "createdAt")
        private String sortBy;
        @Schema(description = "排序方向：ASC 或 DESC", example = "DESC")
        private String sortDirection;
        @Schema(description = "标签筛选 JSON 字符串，不同组 AND，同组多值 OR")
        private String tagFilters;
        @Schema(description = "鏄惁鍙繑鍥炴湭璇昏祫鏂?, example = \"false\"")
        private Boolean unreadOnly;
        @Schema(description = "页码，从 1 开始", example = "1")
        @Min(1)
        private int page = 1;
        @Schema(description = "每页数量，最大 100", example = "20")
        @Min(1)
        @Max(100)
        private int pageSize = 20;
    }

    @Schema(description = "提交资料请求")
    @Data
    class SubmitRequest {
        @Schema(description = "主题 ID", example = "1")
        @NotNull
        private Long topicId;
        @Schema(description = "资料类型：article/social/media/image/excerpt/input", example = "article")
        @NotBlank
        private String materialType;
        @Schema(description = "标题", example = "Redis 缓存实践")
        @Size(max = 200)
        private String title;
        @Schema(description = "描述", example = "关于缓存设计的一篇文章")
        @Size(max = 500)
        private String description;
        @Schema(description = "原始正文内容")
        @Size(max = 50000)
        private String rawContent;
        @Schema(description = "来源链接", example = "https://example.com/article")
        @Size(max = 2000)
        private String sourceUrl;
        @Schema(description = "文件对象 key", example = "materials/file.pdf")
        @Size(max = 500)
        private String fileKey;
        @Schema(description = "作者", example = "anbuz")
        @Size(max = 100)
        private String author;
        @Schema(description = "来源平台", example = "wechat")
        @Size(max = 50)
        private String sourcePlatform;
        @Schema(description = "发布时间")
        private LocalDateTime publishTime;
        @Schema(description = "媒体时长，单位秒", example = "360")
        private Integer durationSeconds;
        @Schema(description = "封面文件 key；用户主动上传封面时传入", example = "covers/cover.png")
        @Size(max = 500)
        private String thumbnailKey;
        @Schema(description = "提交时同步保存的用户标签列表；不传则资料先进入未打标签状态")
        @Valid
        @Size(max = 100)
        private List<UpdateTagsRequest.TagItem> tags;
    }

    @Schema(description = "编辑资料基本信息请求")
    @Data
    class UpdateBasicRequest {
        @Schema(description = "标题")
        @Size(max = 200)
        private String title;
        @Schema(description = "原始正文内容")
        @Size(max = 50000)
        private String rawContent;
        @Schema(description = "来源链接")
        @Size(max = 2000)
        private String sourceUrl;
    }

    @Schema(description = "补全资料元信息请求")
    @Data
    class UpdateMetaRequest {
        @Schema(description = "作者")
        @Size(max = 100)
        private String author;
        @Schema(description = "来源平台")
        @Size(max = 50)
        private String sourcePlatform;
        @Schema(description = "发布时间")
        private LocalDateTime publishTime;
        @Schema(description = "字数")
        private Integer wordCount;
        @Schema(description = "媒体时长，单位秒")
        private Integer durationSeconds;
        @Schema(description = "封面文件 key")
        @Size(max = 500)
        private String thumbnailKey;
        @Schema(description = "扩展 JSON")
        private String extraJson;
    }

    @Schema(description = "完成评价请求")
    @Data
    class CollectRequest {
        @Schema(description = "评语", example = "结构清晰，值得收录")
        @Size(max = 2000)
        private String comment;
        @Schema(description = "评分，0 到 10，最多 1 位小数", example = "8.5")
        @DecimalMin("0.0")
        @DecimalMax("10.0")
        @Digits(integer = 2, fraction = 1)
        private BigDecimal score;
    }

    @Schema(description = "标记失效请求")
    @Data
    class InvalidateRequest {
        @Schema(description = "失效原因", example = "内容过期")
        @NotBlank
        @Size(max = 500)
        private String invalidReason;
    }

    @Schema(description = "覆盖更新标签请求")
    @Data
    class UpdateTagsRequest {
        @Schema(description = "用户标签列表")
        @Valid
        @Size(max = 100)
        private List<TagItem> tags;

        @Schema(description = "用户标签项")
        @Data
        public static class TagItem {
            @Schema(description = "标签组 key；传用户标签组 ID 字符串时表示分组标签，不传或传空时表示无标签组自由标签", example = "11")
            @Size(max = 50)
            private String tagGroupKey;
            @Schema(description = "标签值", example = "需求分析")
            @NotBlank
            private String tagValue;
        }
    }

    @Schema(description = "资料基本信息响应")
    record MaterialResponse(Long id, Long userId, Long topicId, String materialType, String status,
                            String title, String description, String rawContent, String sourceUrl,
                            String fileKey, String comment, BigDecimal score, Boolean unread,
                            String invalidReason, LocalDateTime inboxAt, LocalDateTime inboxReadAt,
                            LocalDateTime collectedAt, LocalDateTime collectedReadAt, LocalDateTime archivedAt,
                            LocalDateTime invalidAt, LocalDateTime lastRetrievedAt, Boolean deleted,
                            LocalDateTime deletedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    @Schema(description = "资料元信息响应")
    record MaterialMetaResponse(Long id, Long materialId, String author, String sourcePlatform,
                                LocalDateTime publishTime, Integer wordCount, Integer durationSeconds,
                                String thumbnailKey, String extraJson,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {}

    @Schema(description = "资料标签响应")
    record MaterialTagResponse(Long id, Long materialId, String tagType, String tagGroupKey,
                               String tagValue, LocalDateTime createdAt) {}

    @Schema(description = "资料状态历史响应")
    record MaterialStatusRecordResponse(String status, String label, LocalDateTime occurredAt) {}

    @Schema(description = "搜索高亮响应")
    record SearchHighlightResponse(String title, String rawContent, String comment) {}

    @Schema(description = "资料详情响应")
    record MaterialDetailResponse(MaterialResponse material, MaterialMetaResponse meta,
                                  List<MaterialTagResponse> tags,
                                  List<MaterialStatusRecordResponse> statusHistory,
                                  SearchHighlightResponse highlight) {}

    @Schema(description = "资料分页响应")
    record MaterialPageResponse(List<MaterialDetailResponse> items, long total, int page, int pageSize) {}
}
