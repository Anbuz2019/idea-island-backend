package com.anbuz.api.http;

import com.anbuz.types.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 认证接口契约，定义注册登录、验证码、登出等认证 HTTP 边界。
 */
@Tag(name = "认证接口", description = "邮箱注册登录、手机号验证码登录、验证码下发与登出")
@RequestMapping("/api/v1/auth")
public interface IAuthController {

    @Operation(summary = "邮箱注册", description = "使用邮箱和密码创建新用户，并返回登录 token")
    @PostMapping("/register")
    Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest req);

    @Operation(summary = "邮箱登录", description = "使用邮箱和密码登录，登录成功后签发 JWT")
    @PostMapping("/login")
    Result<LoginResponse> login(@Valid @RequestBody LoginRequest req);

    @Operation(summary = "下发手机号验证码", description = "生成手机号登录验证码并写入 Redis，开发环境可回显验证码")
    @PostMapping("/phone-code")
    Result<PhoneCodeResponse> issuePhoneCode(@Valid @RequestBody PhoneCodeRequest req);

    @Operation(summary = "手机号验证码登录", description = "校验手机号验证码，首次手机号登录会自动创建用户")
    @PostMapping("/phone-login")
    Result<LoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest req);

    @Operation(summary = "退出登录", description = "删除当前用户服务端 token，使当前会话失效")
    @PostMapping("/logout")
    Result<Void> logout();

    @Schema(description = "邮箱注册请求")
    @Data
    class RegisterRequest {
        @Schema(description = "邮箱地址", example = "user@example.com")
        @NotBlank
        @Email
        private String email;

        @Schema(description = "登录密码，长度 6 到 100", example = "secret123")
        @NotBlank
        @Size(min = 6, max = 100)
        private String password;

        @Schema(description = "昵称", example = "岛民")
        @Size(max = 50)
        private String nickname;
    }

    @Schema(description = "邮箱登录请求")
    @Data
    class LoginRequest {
        @Schema(description = "邮箱地址", example = "user@example.com")
        @NotBlank
        @Email
        private String email;

        @Schema(description = "登录密码", example = "secret123")
        @NotBlank
        private String password;
    }

    @Schema(description = "手机号验证码下发请求")
    @Data
    class PhoneCodeRequest {
        @Schema(description = "中国大陆手机号", example = "13800138000")
        @NotBlank
        @Pattern(regexp = "^1\\d{10}$")
        private String phone;
    }

    @Schema(description = "手机号验证码登录请求")
    @Data
    class PhoneLoginRequest {
        @Schema(description = "中国大陆手机号", example = "13800138000")
        @NotBlank
        @Pattern(regexp = "^1\\d{10}$")
        private String phone;

        @Schema(description = "验证码", example = "123456")
        @NotBlank
        @Size(min = 4, max = 8)
        private String code;
    }

    @Schema(description = "注册响应")
    record RegisterResponse(Long userId, String token) {}

    @Schema(description = "登录响应")
    record LoginResponse(Long userId, String nickname, String token) {}

    @Schema(description = "手机号验证码下发响应")
    record PhoneCodeResponse(boolean sent, String code, long expireSeconds) {}
}
