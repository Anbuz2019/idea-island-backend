package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.user.model.entity.User;
import com.anbuz.infrastructure.persistent.dao.IUserDao;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepository duplicate-key scenarios")
class UserRepositoryDuplicateKeyTest {

    @Mock
    private IUserDao userDao;

    @Nested
    @DisplayName("save user")
    class SaveUser {

        @Test
        @DisplayName("translates duplicate email into business conflict")
        void givenDuplicateEmail_whenSave_thenThrowsBusinessConflict() {
            UserRepository userRepository = new UserRepository(userDao);
            doThrow(new DuplicateKeyException("duplicate email")).when(userDao).insert(any());

            assertThatThrownBy(() -> userRepository.save(buildUser("email-user", "dup@example.com", null)))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "邮箱已被注册");
        }

        @Test
        @DisplayName("translates duplicate phone into business conflict")
        void givenDuplicatePhone_whenSave_thenThrowsBusinessConflict() {
            UserRepository userRepository = new UserRepository(userDao);
            doThrow(new DuplicateKeyException("duplicate phone")).when(userDao).insert(any());

            assertThatThrownBy(() -> userRepository.save(buildUser("phone-user", null, "13800138009")))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "手机号已被注册");
        }

        @Test
        @DisplayName("translates duplicate username into business conflict")
        void givenDuplicateUsername_whenSave_thenThrowsBusinessConflict() {
            UserRepository userRepository = new UserRepository(userDao);
            doThrow(new DuplicateKeyException("duplicate username")).when(userDao).insert(any());

            assertThatThrownBy(() -> userRepository.save(buildUser("dup-user", null, null)))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "用户名已存在");
        }
    }

    private User buildUser(String username, String email, String phone) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .username(username)
                .email(email)
                .phone(phone)
                .passwordHash("hashed")
                .nickname("tester")
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
