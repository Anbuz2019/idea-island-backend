package com.anbuz.infrastructure.redis;

import com.anbuz.domain.user.repository.ILoginVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * Redis 验证码仓储实现，负责手机号验证码的短期存储和读取。
 */
@Repository
@RequiredArgsConstructor
public class LoginVerificationCodeRepository implements ILoginVerificationCodeRepository {

    private static final String PHONE_LOGIN_CODE_KEY_PREFIX = "login:code:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void storeCode(String phone, String code, Duration ttl) {
        redisTemplate.opsForValue().set(buildKey(phone), code, ttl);
    }

    @Override
    public String getCode(String phone) {
        return redisTemplate.opsForValue().get(buildKey(phone));
    }

    @Override
    public void removeCode(String phone) {
        redisTemplate.delete(buildKey(phone));
    }

    private String buildKey(String phone) {
        return PHONE_LOGIN_CODE_KEY_PREFIX + phone;
    }

}
