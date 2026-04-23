package com.anbuz.domain.user.service.impl;

import com.anbuz.domain.user.repository.IAuthTokenRepository;
import com.anbuz.domain.user.service.IAuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 认证 token 领域服务，负责保存、移除、校验和临期续签登录 token。
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService implements IAuthTokenService {

    private final IAuthTokenRepository authTokenRepository;

    @Value("${jwt.expire-days:7}")
    private int expireDays;

    public void storeToken(Long userId, String token) {
        authTokenRepository.storeToken(userId, token, Duration.ofDays(expireDays));
    }

    public String getToken(Long userId) {
        return authTokenRepository.getToken(userId);
    }

    public void removeToken(Long userId) {
        authTokenRepository.removeToken(userId);
    }

}
