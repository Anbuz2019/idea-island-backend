package com.anbuz.trigger.http;

import com.anbuz.api.http.IUserController;
import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.model.valobj.UserStats;
import com.anbuz.domain.user.service.IUserService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 HTTP 适配器，负责把用户资料和统计请求转换为用户域服务调用。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController implements IUserController {

    private final IUserService userService;

    @Override
    public Result<UserProfileResponse> profile() {
        Long userId = UserContext.currentUserId();
        log.debug("Load user profile userId={}", userId);
        return Result.ok(toProfile(userService.getProfile(userId)));
    }

    @Override
    public Result<UserProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = UserContext.currentUserId();
        log.info("Update user profile requested userId={} nicknameChanged={} avatarChanged={}",
                userId, request.getNickname() != null, request.getAvatarKey() != null);
        userService.updateProfile(userId, request.getNickname(), request.getAvatarKey());
        log.info("Update user profile succeeded userId={}", userId);
        return Result.ok(toProfile(userService.getProfile(userId)));
    }

    @Override
    public Result<UserStatsResponse> stats() {
        Long userId = UserContext.currentUserId();
        log.debug("Load user stats userId={}", userId);
        UserStats stats = userService.getStats(userId);
        return Result.ok(new UserStatsResponse(stats.getTopicCount(), stats.getMaterialCount(), stats.getStatusCounts()));
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getPhone(),
                user.getNickname(), user.getAvatarKey(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
