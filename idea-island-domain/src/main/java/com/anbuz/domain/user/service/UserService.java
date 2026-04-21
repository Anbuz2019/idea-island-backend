package com.anbuz.domain.user.service;

import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.repository.IUserRepository;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public User registerByEmail(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
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
        return user;
    }

    public User loginByEmail(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (!user.isActive()) {
            throw new AppException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AppException(ErrorCode.PARAM_INVALID, "密码错误");
        }
        return user;
    }

    public void updateProfile(Long userId, String nickname, String avatarKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (nickname != null) user.setNickname(nickname);
        if (avatarKey != null) user.setAvatarKey(avatarKey);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.update(user);
    }

}
