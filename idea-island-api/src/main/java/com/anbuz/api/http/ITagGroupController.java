package com.anbuz.api.http;

import com.anbuz.types.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签组接口契约，定义用户自定义标签组和值维护的 HTTP 边界。
 */
@Tag(name = "标签组接口", description = "主题标签组和标签值的配置管理")
public interface ITagGroupController {

    @Operation(summary = "查询主题标签组", description = "查询主题下所有标签组及其标签值")
    @GetMapping("/api/v1/topics/{topicId}/tag-groups")
    Result<List<TagGroupDetailResponse>> list(
            @Parameter(description = "主题 ID", required = true) @PathVariable Long topicId);

    @Operation(summary = "创建标签组", description = "在指定主题下创建用户标签组")
    @PostMapping("/api/v1/topics/{topicId}/tag-groups")
    Result<TagGroupResponse> create(
            @Parameter(description = "主题 ID", required = true) @PathVariable Long topicId,
            @Valid @RequestBody CreateTagGroupRequest request);

    @Operation(summary = "更新标签组", description = "修改标签组名称、颜色、互斥、必填和排序配置")
    @PutMapping("/api/v1/topics/{topicId}/tag-groups/{groupId}")
    Result<TagGroupResponse> update(@Parameter(description = "主题 ID", required = true) @PathVariable Long topicId,
                                    @Parameter(description = "标签组 ID", required = true) @PathVariable Long groupId,
                                    @Valid @RequestBody UpdateTagGroupRequest request);

    @Operation(summary = "删除标签组", description = "删除未被资料使用的标签组")
    @DeleteMapping("/api/v1/topics/{topicId}/tag-groups/{groupId}")
    Result<Void> delete(@Parameter(description = "主题 ID", required = true) @PathVariable Long topicId,
                        @Parameter(description = "标签组 ID", required = true) @PathVariable Long groupId);

    @Operation(summary = "新增标签值", description = "在指定标签组下新增一个可选标签值")
    @PostMapping("/api/v1/tag-groups/{groupId}/values")
    Result<TagValueResponse> addValue(
            @Parameter(description = "标签组 ID", required = true) @PathVariable Long groupId,
            @Valid @RequestBody CreateTagValueRequest request);

    @Operation(summary = "更新标签值", description = "修改标签值文本、颜色和排序")
    @PutMapping("/api/v1/tag-groups/{groupId}/values/{valueId}")
    Result<TagValueResponse> updateValue(@Parameter(description = "标签组 ID", required = true) @PathVariable Long groupId,
                                         @Parameter(description = "标签值 ID", required = true) @PathVariable Long valueId,
                                         @Valid @RequestBody UpdateTagValueRequest request);

    @Operation(summary = "删除标签值", description = "删除未被资料使用的标签值")
    @DeleteMapping("/api/v1/tag-groups/{groupId}/values/{valueId}")
    Result<Void> deleteValue(@Parameter(description = "标签组 ID", required = true) @PathVariable Long groupId,
                             @Parameter(description = "标签值 ID", required = true) @PathVariable Long valueId);

    @Schema(description = "创建标签组请求")
    @Data
    class CreateTagGroupRequest {
        @Schema(description = "标签组名称", example = "阶段")
        @NotBlank
        @Size(max = 50)
        private String name;

        @Schema(description = "是否互斥，true 表示同组最多选一个", example = "true")
        @NotNull
        private Boolean exclusive;

        @Schema(description = "是否必填", example = "false")
        @NotNull
        private Boolean required;

        @Schema(description = "排序值，越小越靠前", example = "10")
        private Integer sortOrder;
    }

    @Schema(description = "更新标签组请求")
    @Data
    class UpdateTagGroupRequest {
        @Schema(description = "标签组名称", example = "阶段")
        @Size(max = 50)
        private String name;

        @Schema(description = "标签组颜色，HEX 格式", example = "#FFAA00")
        @Pattern(regexp = "^#?[0-9A-Fa-f]{6}$", message = "颜色必须为 HEX 格式")
        private String color;

        @Schema(description = "是否互斥")
        private Boolean exclusive;
        @Schema(description = "是否必填")
        private Boolean required;
        @Schema(description = "排序值，越小越靠前")
        private Integer sortOrder;
    }

    @Schema(description = "创建标签值请求")
    @Data
    class CreateTagValueRequest {
        @Schema(description = "标签值", example = "需求分析")
        @NotBlank
        @Size(max = 50)
        private String value;

        @Schema(description = "标签值颜色，HEX 格式", example = "#00AAFF")
        @Pattern(regexp = "^#?[0-9A-Fa-f]{6}$", message = "颜色必须为 HEX 格式")
        private String color;

        @Schema(description = "排序值，越小越靠前")
        private Integer sortOrder;
    }

    @Schema(description = "更新标签值请求")
    @Data
    class UpdateTagValueRequest {
        @Schema(description = "标签值", example = "方案设计")
        @Size(max = 50)
        private String value;

        @Schema(description = "标签值颜色，HEX 格式", example = "#00AAFF")
        @Pattern(regexp = "^#?[0-9A-Fa-f]{6}$", message = "颜色必须为 HEX 格式")
        private String color;

        @Schema(description = "排序值，越小越靠前")
        private Integer sortOrder;
    }

    @Schema(description = "标签组响应")
    record TagGroupResponse(Long id, Long topicId, String tagType, String tagGroupKey, String name, String color,
                            Boolean exclusive, Boolean required, Integer sortOrder,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {}

    @Schema(description = "标签值响应")
    record TagValueResponse(Long id, Long groupId, String value, String color,
                            Integer sortOrder, LocalDateTime createdAt) {}

    @Schema(description = "标签组详情响应")
    record TagGroupDetailResponse(TagGroupResponse group, List<TagValueResponse> values) {}
}
