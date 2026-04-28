package com.anbuz.api.http;

import com.anbuz.types.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索接口契约，定义跨资料查询和收件箱视图的 HTTP 边界。
 */
@Tag(name = "检索接口", description = "全文检索与收件箱快速处理视图")
@RequestMapping("/api/v1")
public interface ISearchController {

    @Operation(summary = "全文检索资料", description = "按关键词检索标题、正文和评语，支持跨主题检索并返回高亮字段")
    @GetMapping("/search")
    Result<IMaterialController.MaterialPageResponse> search(@Valid @ModelAttribute SearchRequest request);

    @Operation(summary = "查询收件箱资料", description = "查询 INBOX 状态资料，默认按提交时间倒序")
    @GetMapping("/inbox")
    Result<IMaterialController.MaterialPageResponse> inbox(@Valid @ModelAttribute InboxRequest request);

    @Schema(description = "全文检索请求")
    @Data
    class SearchRequest {
        @Schema(description = "检索关键词", example = "redis")
        @NotBlank
        private String keyword;
        @Schema(description = "主题 ID；不传时跨当前用户所有主题检索", example = "1")
        private Long topicId;
        @Schema(description = "状态过滤，可多选")
        private List<String> status;
        @Schema(description = "资料类型过滤，可多选")
        private List<String> materialType;
        @Schema(description = "最低评分")
        private BigDecimal scoreMin;
        @Schema(description = "最高评分")
        private BigDecimal scoreMax;
        @Schema(description = "创建时间起点，ISO 日期时间")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime createdStart;
        @Schema(description = "创建时间终点，ISO 日期时间")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime createdEnd;
        @Schema(description = "排序字段：createdAt、updatedAt、score、status、statusAt", example = "createdAt")
        private String sortBy;
        @Schema(description = "排序方向：ASC 或 DESC", example = "DESC")
        private String sortDirection;
        @Schema(description = "标签筛选 JSON 字符串，不同组 AND，同组多值 OR")
        private String tagFilters;
        @Schema(description = "页码，从 1 开始", example = "1")
        @Min(1)
        private int page = 1;
        @Schema(description = "每页数量，最大 100", example = "20")
        @Min(1)
        @Max(100)
        private int pageSize = 20;
    }

    @Schema(description = "收件箱查询请求")
    @Data
    class InboxRequest {
        @Schema(description = "主题 ID；不传时查询当前用户所有主题的收件箱资料", example = "1")
        private Long topicId;
        @Schema(description = "页码，从 1 开始", example = "1")
        @Min(1)
        private int page = 1;
        @Schema(description = "每页数量，最大 100", example = "20")
        @Min(1)
        @Max(100)
        private int pageSize = 20;
    }
}
