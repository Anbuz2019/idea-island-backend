package com.anbuz.api.http;

import com.anbuz.types.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户接口契约，定义用户资料、统计和用户维度资料列表的 HTTP 边界。
 */
@Tag(name = "用户接口", description = "当前用户资料查询、资料更新与个人统计")
@RequestMapping("/api/v1/users/me")
public interface IUserController {

    @Operation(summary = "查询当前用户资料", description = "返回当前登录用户的基础资料")
    @GetMapping
    Result<UserProfileResponse> profile();

    @Operation(summary = "更新当前用户资料", description = "更新昵称和头像 key")
    @PutMapping
    Result<UserProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request);

    @Operation(summary = "查询当前用户统计", description = "返回主题数量、资料总数和资料状态分布")
    @GetMapping("/stats")
    Result<UserStatsResponse> stats();

    @Schema(description = "更新用户资料请求")
    @Data
    class UpdateProfileRequest {
        @Schema(description = "昵称", example = "岛民")
        @Size(max = 50)
        private String nickname;

        @Schema(description = "头像文件 key", example = "avatars/user-avatar.png")
        @Size(max = 500)
        private String avatarKey;
    }

    @Schema(description = "用户资料响应")
    record UserProfileResponse(Long id, String username, String email, String phone,
                               String nickname, String avatarKey, Integer status,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {}

    @Schema(description = "用户统计响应")
    record UserStatsResponse(long topicCount, long materialCount, Map<String, Long> statusCounts) {}
}
