package com.anbuz.trigger.http;

import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.service.AuthService;
import com.anbuz.domain.user.service.UserService;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        User user = userService.registerByEmail(req.getEmail(), req.getPassword(), req.getNickname());
        String token = authService.generateToken(user.getId());
        return Result.ok(new RegisterResponse(user.getId(), token));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        User user = userService.loginByEmail(req.getEmail(), req.getPassword());
        String token = authService.generateToken(user.getId());
        return Result.ok(new LoginResponse(user.getId(), user.getNickname(), token));
    }

    @Data
    public static class RegisterRequest {
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 6, max = 100)
        private String password;
        @Size(max = 50)
        private String nickname;
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    public record RegisterResponse(Long userId, String token) {}
    public record LoginResponse(Long userId, String nickname, String token) {}

}
