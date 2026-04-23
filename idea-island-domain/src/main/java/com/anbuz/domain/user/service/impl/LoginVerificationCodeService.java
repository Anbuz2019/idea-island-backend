package com.anbuz.domain.user.service.impl;

import com.anbuz.domain.user.repository.ILoginVerificationCodeRepository;
import com.anbuz.domain.user.service.ILoginVerificationCodeService;
import com.anbuz.domain.user.service.ILoginVerificationCodeService.IssuedPhoneCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 登录验证码领域服务，负责生成、存储和校验手机号登录验证码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginVerificationCodeService implements ILoginVerificationCodeService {

    private final ILoginVerificationCodeRepository loginVerificationCodeRepository;

    @Value("${auth.phone-code.ttl-minutes:5}")
    private long ttlMinutes;

    @Override
    public IssuedPhoneCode issuePhoneCode(String phone) {
        String code = "%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000));
        loginVerificationCodeRepository.storeCode(phone, code, Duration.ofMinutes(ttlMinutes));
        log.info("Issue phone verification code succeeded phone={} ttlMinutes={}", maskPhone(phone), ttlMinutes);
        return new IssuedPhoneCode(code, ttlMinutes * 60);
    }

    @Override
    public boolean verifyPhoneCode(String phone, String code) {
        String cachedCode = loginVerificationCodeRepository.getCode(phone);
        if (cachedCode == null || !cachedCode.equals(code)) {
            log.warn("Verify phone code failed phone={}", maskPhone(phone));
            return false;
        }
        loginVerificationCodeRepository.removeCode(phone);
        log.info("Verify phone code succeeded phone={}", maskPhone(phone));
        return true;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "<empty>";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

}
