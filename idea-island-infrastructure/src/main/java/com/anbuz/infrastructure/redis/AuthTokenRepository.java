package com.anbuz.infrastructure.redis;

import com.anbuz.domain.user.repository.IAuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * Redis token 仓储实现，负责登录 token 的存储、校验、删除和续期。
 */
@Repository
@RequiredArgsConstructor
public class AuthTokenRepository implements IAuthTokenRepository {

    private static final String TOKEN_KEY_PREFIX = "token:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void storeToken(Long userId, String clientType, String token, Duration ttl) {
        redisTemplate.opsForValue().set(buildKey(userId, clientType), token, ttl);
    }

    @Override
    public String getToken(Long userId, String clientType) {
        return redisTemplate.opsForValue().get(buildKey(userId, clientType));
    }

    @Override
    public void removeToken(Long userId, String clientType) {
        redisTemplate.delete(buildKey(userId, clientType));
    }

    private String buildKey(Long userId, String clientType) {
        return TOKEN_KEY_PREFIX + normalizeClientType(clientType) + ":" + userId;
    }

    private String normalizeClientType(String clientType) {
        if (clientType == null || clientType.isBlank()) {
            return "web";
        }
        return clientType.trim().toLowerCase();
    }

}
