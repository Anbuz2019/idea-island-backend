package com.anbuz.domain.user.service;

/**
 * 登录验证码领域服务接口，定义手机号验证码下发和校验能力。
 */
public interface ILoginVerificationCodeService {

    IssuedPhoneCode issuePhoneCode(String phone);

    boolean verifyPhoneCode(String phone, String code);

    record IssuedPhoneCode(String code, long expireSeconds) {}
}
