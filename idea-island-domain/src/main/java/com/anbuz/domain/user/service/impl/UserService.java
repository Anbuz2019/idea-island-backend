package com.anbuz.domain.user.service.impl;

import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.model.valobj.UserStats;
import com.anbuz.domain.user.repository.IUserRepository;
import com.anbuz.domain.user.service.IUserService;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户领域服务，负责用户注册登录、资料更新、统计聚合和用户资料查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final IUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ITopicRepository topicRepository;
    private final IMaterialRepository materialRepository;

    @Override
    public User registerByEmail(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Register by email rejected due to duplicate email={}", maskEmail(email));
            throw new AppException(ErrorCode.BUSINESS_CONFLICT, "邮箱已被注册");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .email(email)
                .username(email)
                .passwordHash(passwordEncoder.encode(password))
                .nickname(nickname == null || nickname.isBlank() ? email.split("@")[0] : nickname)
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(user);
        log.info("Register by email succeeded userId={} email={}", user.getId(), maskEmail(email));
        return user;
    }

    @Override
    public User loginByEmail(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
        assertActive(user);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login by email rejected due to invalid password userId={} email={}", user.getId(), maskEmail(email));
            throw new AppException(ErrorCode.PARAM_INVALID, "密码错误");
        }
        log.info("Login by email succeeded userId={} email={}", user.getId(), maskEmail(email));
        return user;
    }

    @Override
    public User loginByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .map(user -> {
                    assertActive(user);
                    log.info("Login by phone succeeded for existing user userId={} phone={}", user.getId(), maskPhone(phone));
                    return user;
                })
                .orElseGet(() -> registerPhoneUserWithRetry(phone));
    }

    @Override
    public void updateProfile(Long userId, String nickname, String avatarKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatarKey != null) {
            user.setAvatarKey(avatarKey);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.update(user);
        log.info("Update profile succeeded userId={} nicknameChanged={} avatarChanged={}",
                userId, nickname != null, avatarKey != null);
    }

    @Override
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    @Override
    public UserStats getStats(Long userId) {
        long materialCount = materialRepository.countMaterials(MaterialListQuery.builder()
                .userId(userId)
                .page(1)
                .pageSize(1)
                .build());
        return UserStats.builder()
                .topicCount(topicRepository.countTopicsByUserId(userId))
                .materialCount(materialCount)
                .statusCounts(materialRepository.countByStatus(userId, null))
                .build();
    }

    private User registerPhoneUser(String phone) {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .phone(phone)
                .username(phone)
                .nickname(buildPhoneNickname(phone))
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(user);
        log.info("Auto register phone user succeeded userId={} phone={}", user.getId(), maskPhone(phone));
        return user;
    }

    private User registerPhoneUserWithRetry(String phone) {
        try {
            return registerPhoneUser(phone);
        } catch (AppException e) {
            if (e.getCode() != ErrorCode.BUSINESS_CONFLICT.getCode()) {
                throw e;
            }
            log.warn("Auto register phone user encountered concurrent conflict phone={}", maskPhone(phone));
            return userRepository.findByPhone(phone)
                    .map(user -> {
                        assertActive(user);
                        log.info("Load phone user after concurrent conflict userId={} phone={}", user.getId(), maskPhone(phone));
                        return user;
                    })
                    .orElseThrow(() -> e);
        }
    }

    private void assertActive(User user) {
        if (!user.isActive()) {
            log.warn("User access rejected due to disabled status userId={}", user.getId());
            throw new AppException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
    }

    private String buildPhoneNickname(String phone) {
        String suffix = phone.length() <= 4 ? phone : phone.substring(phone.length() - 4);
        return "user_" + suffix;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<empty>";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(Math.max(atIndex, 0));
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "<empty>";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

}
