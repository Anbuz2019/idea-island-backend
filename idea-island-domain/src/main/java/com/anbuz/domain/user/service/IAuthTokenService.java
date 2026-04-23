package com.anbuz.domain.user.service;

/**
 * 认证 token 领域服务接口，定义 token 存储、失效和临期续签能力。
 */
public interface IAuthTokenService {

    void storeToken(Long userId, String token);

    String getToken(Long userId);

    void removeToken(Long userId);
}
