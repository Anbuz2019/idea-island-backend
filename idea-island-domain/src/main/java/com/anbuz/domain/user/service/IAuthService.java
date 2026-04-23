package com.anbuz.domain.user.service;

/**
 * 认证领域服务接口，定义 token 生成和解析能力。
 */
public interface IAuthService {

    String generateToken(Long userId);

    Long parseUserId(String token);

    boolean isExpiringSoon(String token);
}
