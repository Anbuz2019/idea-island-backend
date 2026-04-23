package com.anbuz.api.http;

import com.anbuz.types.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 主题接口契约，定义主题创建、配置、统计和删除等 HTTP 边界。
 */
@Tag(name = "主题接口", description = "主题创建、查询、编辑、启停、删除与主题维度统计")
@RequestMapping("/api/v1/topics")
public interface ITopicController {

    @Operation(summary = "查询主题列表", description = "查询当前用户的所有主题")
    @GetMapping
    Result<List<TopicResponse>> list();

    @Operation(summary = "创建主题", description = "创建一个新的资料主题")
    @PostMapping
    Result<TopicResponse> create(@Valid @RequestBody CreateTopicRequest req);

    @Operation(summary = "查询主题详情", description = "按主题 ID 查询主题详情")
    @GetMapping("/{id}")
    Result<TopicResponse> detail(@Parameter(description = "主题 ID", required = true) @PathVariable Long id);

    @Operation(summary = "编辑主题", description = "修改主题名称和描述")
    @PutMapping("/{id}")
    Result<TopicResponse> update(@Parameter(description = "主题 ID", required = true) @PathVariable Long id,
                                 @Valid @RequestBody UpdateTopicRequest req);

    @Operation(summary = "停用主题", description = "停用后不可继续向该主题提交新资料")
    @PostMapping("/{id}/disable")
    Result<Void> disable(@Parameter(description = "主题 ID", required = true) @PathVariable Long id);

    @Operation(summary = "启用主题", description = "恢复主题的资料提交能力")
    @PostMapping("/{id}/enable")
    Result<Void> enable(@Parameter(description = "主题 ID", required = true) @PathVariable Long id);

    @Operation(summary = "删除主题", description = "删除没有资料引用的主题")
    @DeleteMapping("/{id}")
    Result<Void> delete(@Parameter(description = "主题 ID", required = true) @PathVariable Long id);

    @Operation(summary = "查询主题统计", description = "返回主题下资料总数、状态分布、类型分布、周新增、平均评分和待处理数")
    @GetMapping("/{id}/stats")
    Result<TopicStatsResponse> stats(@Parameter(description = "主题 ID", required = true) @PathVariable Long id);

    @Schema(description = "创建主题请求")
    @Data
    class CreateTopicRequest {
        @Schema(description = "主题名称", example = "后端知识")
        @NotBlank
        @Size(max = 50)
        private String name;

        @Schema(description = "主题描述", example = "收集 Java、架构和工程实践资料")
        @Size(max = 500)
        private String description;
    }

    @Schema(description = "更新主题请求")
    @Data
    class UpdateTopicRequest {
        @Schema(description = "主题名称", example = "后端知识")
        @Size(max = 50)
        private String name;

        @Schema(description = "主题描述", example = "收集 Java、架构和工程实践资料")
        @Size(max = 500)
        private String description;
    }

    @Schema(description = "主题响应")
    record TopicResponse(Long id, Long userId, String name, String description,
                         Integer status, Integer materialCount,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {}

    @Schema(description = "主题统计响应")
    record TopicStatsResponse(long totalMaterials, Map<String, Long> statusCounts, Map<String, Long> typeCounts,
                              long weeklyNew, BigDecimal averageScore, long pendingCount) {}
}
