package com.anbuz.domain.user.service;

import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.model.valobj.UserStats;

/**
 * 用户领域服务接口，定义注册登录、资料维护、统计和用户资料列表能力。
 */
public interface IUserService {

    User registerByEmail(String email, String password, String nickname);

    User loginByEmail(String email, String password);

    User loginByPhone(String phone);

    void updateProfile(Long userId, String nickname, String avatarKey);

    User getProfile(Long userId);

    UserStats getStats(Long userId);
}
