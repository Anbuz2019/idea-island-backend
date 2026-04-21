package com.anbuz.domain.user.service;

import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.repository.IUserRepository;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 用户领域服务")
class UserServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("邮箱注册")
    class RegisterByEmail {

        @Test
        @DisplayName("邮箱未注册时，注册成功并保存用户")
        void givenNewEmail_whenRegister_thenUserSaved() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_pw");

            User result = userService.registerByEmail("test@example.com", "password123", "测试用户");

            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getNickname()).isEqualTo("测试用户");
            assertThat(result.getPasswordHash()).isEqualTo("hashed_pw");
            assertThat(result.getStatus()).isEqualTo(1);
            verify(userRepository).save(any());
        }

        @Test
        @DisplayName("邮箱已注册，应抛出业务冲突异常")
        void givenExistingEmail_whenRegister_thenThrowsBusinessConflict() {
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.registerByEmail("existing@example.com", "pw", null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("邮箱登录")
    class LoginByEmail {

        @Test
        @DisplayName("邮箱和密码正确，登录成功")
        void givenCorrectCredentials_whenLogin_thenReturnsUser() {
            User user = User.builder()
                    .id(1L).email("test@example.com")
                    .passwordHash("hashed_pw").status(1).build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "hashed_pw")).thenReturn(true);

            User result = userService.loginByEmail("test@example.com", "password123");

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("密码错误，应抛出参数校验异常")
        void givenWrongPassword_whenLogin_thenThrowsParamInvalid() {
            User user = User.builder()
                    .id(1L).email("test@example.com")
                    .passwordHash("hashed_pw").status(1).build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpw", "hashed_pw")).thenReturn(false);

            assertThatThrownBy(() -> userService.loginByEmail("test@example.com", "wrongpw"))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
        }

        @Test
        @DisplayName("账号被禁用，应抛出无权限异常")
        void givenDisabledUser_whenLogin_thenThrowsForbidden() {
            User user = User.builder()
                    .id(1L).email("test@example.com")
                    .passwordHash("hashed_pw").status(0).build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.loginByEmail("test@example.com", "password123"))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        }
    }

}
