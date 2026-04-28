package com.anbuz.domain.user.repository;

import java.time.Duration;

/**
 * 认证 token 仓储接口，负责隔离 token 存储、校验和续期能力。
 */
public interface IAuthTokenRepository {

    void storeToken(Long userId, String clientType, String token, Duration ttl);

    String getToken(Long userId, String clientType);

    void removeToken(Long userId, String clientType);

}
