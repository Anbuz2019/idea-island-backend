package com.anbuz.domain.user.repository;

import java.time.Duration;

/**
 * 登录验证码仓储接口，负责隔离手机号验证码的写入、读取和删除能力。
 */
public interface ILoginVerificationCodeRepository {

    void storeCode(String phone, String code, Duration ttl);

    String getCode(String phone);

    void removeCode(String phone);

}
