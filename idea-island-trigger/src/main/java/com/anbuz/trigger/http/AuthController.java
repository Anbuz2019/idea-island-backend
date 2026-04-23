package com.anbuz.trigger.http;

import com.anbuz.api.http.IAuthController;
import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.service.IAuthService;
import com.anbuz.domain.user.service.IAuthTokenService;
import com.anbuz.domain.user.service.ILoginVerificationCodeService;
import com.anbuz.domain.user.service.IUserService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 HTTP 适配器，负责把注册登录请求转换为用户域服务调用并返回统一结果。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements IAuthController {

    private final IUserService userService;
    private final IAuthService authService;
    private final IAuthTokenService authTokenService;
    private final ILoginVerificationCodeService loginVerificationCodeService;

    @Value("${auth.phone-code.expose-code:false}")
    private boolean exposePhoneCode;

    @Override
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        log.info("Auth register requested email={}", maskEmail(req.getEmail()));
        User user = userService.registerByEmail(req.getEmail(), req.getPassword(), req.getNickname());
        String token = authService.generateToken(user.getId());
        authTokenService.storeToken(user.getId(), token);
        log.info("Auth register succeeded userId={} email={}", user.getId(), maskEmail(req.getEmail()));
        return Result.ok(new RegisterResponse(user.getId(), token));
    }

    @Override
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        log.info("Auth login requested email={}", maskEmail(req.getEmail()));
        User user = userService.loginByEmail(req.getEmail(), req.getPassword());
        String token = authService.generateToken(user.getId());
        authTokenService.storeToken(user.getId(), token);
        log.info("Auth login succeeded userId={} email={}", user.getId(), maskEmail(req.getEmail()));
        return Result.ok(new LoginResponse(user.getId(), user.getNickname(), token));
    }

    @Override
    public Result<PhoneCodeResponse> issuePhoneCode(@Valid @RequestBody PhoneCodeRequest req) {
        log.info("Phone code requested phone={}", maskPhone(req.getPhone()));
        ILoginVerificationCodeService.IssuedPhoneCode issued = loginVerificationCodeService.issuePhoneCode(req.getPhone());
        log.info("Phone code issued phone={} expireSeconds={}", maskPhone(req.getPhone()), issued.expireSeconds());
        return Result.ok(new PhoneCodeResponse(true, exposePhoneCode ? issued.code() : null, issued.expireSeconds()));
    }

    @Override
    public Result<LoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest req) {
        log.info("Phone login requested phone={}", maskPhone(req.getPhone()));
        if (!loginVerificationCodeService.verifyPhoneCode(req.getPhone(), req.getCode())) {
            log.warn("Phone login rejected due to invalid verification code phone={}", maskPhone(req.getPhone()));
            throw new AppException(ErrorCode.PARAM_INVALID, "验证码错误或已过期");
        }
        User user = userService.loginByPhone(req.getPhone());
        String token = authService.generateToken(user.getId());
        authTokenService.storeToken(user.getId(), token);
        log.info("Phone login succeeded userId={} phone={}", user.getId(), maskPhone(req.getPhone()));
        return Result.ok(new LoginResponse(user.getId(), user.getNickname(), token));
    }

    @Override
    public Result<Void> logout() {
        Long userId = UserContext.currentUserId();
        authTokenService.removeToken(userId);
        log.info("Auth logout succeeded userId={}", userId);
        return Result.ok();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<empty>";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(Math.max(atIndex, 0));
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "<empty>";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
