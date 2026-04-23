package com.anbuz.domain.user.service;

import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.model.valobj.UserStats;
import com.anbuz.domain.user.repository.IUserRepository;
import com.anbuz.domain.user.service.impl.UserService;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService user scenarios")
class UserServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ITopicRepository topicRepository;

    @Mock
    private IMaterialRepository materialRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("register by email")
    class RegisterByEmail {

        @Test
        @DisplayName("creates a new user when email is unused")
        void givenNewEmail_whenRegister_thenUserSaved() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_pw");

            User result = userService.registerByEmail("test@example.com", "password123", "tester");

            assertThat(result)
                    .extracting(User::getEmail, User::getNickname, User::getPasswordHash, User::getStatus)
                    .containsExactly("test@example.com", "tester", "hashed_pw", 1);
            verify(userRepository).save(any());
        }

        @Test
        @DisplayName("falls back to the email prefix when nickname is blank")
        void givenBlankNickname_whenRegister_thenUsesEmailPrefix() {
            when(userRepository.existsByEmail("prefix@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_pw");

            assertThat(userService.registerByEmail("prefix@example.com", "password123", " "))
                    .extracting(User::getNickname)
                    .isEqualTo("prefix");
        }

        @Test
        @DisplayName("throws when email already exists")
        void givenExistingEmail_whenRegister_thenThrowsBusinessConflict() {
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.registerByEmail("existing@example.com", "pw", null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.BUSINESS_CONFLICT.getCode());

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws business conflict when save detects a concurrent duplicate email")
        void givenConcurrentDuplicateEmail_whenRegister_thenThrowsBusinessConflict() {
            when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_pw");
            doThrow(new AppException(ErrorCode.BUSINESS_CONFLICT, "邮箱已被注册"))
                    .when(userRepository).save(any(User.class));

            assertThatThrownBy(() -> userService.registerByEmail("race@example.com", "password123", "tester"))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "邮箱已被注册");
        }
    }

    @Nested
    @DisplayName("login by email")
    class LoginByEmail {

        @Test
        @DisplayName("returns user when email and password are correct")
        void givenCorrectCredentials_whenLogin_thenReturnsUser() {
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hashed_pw")
                    .status(1)
                    .build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "hashed_pw")).thenReturn(true);

            assertThat(userService.loginByEmail("test@example.com", "password123"))
                    .extracting(User::getId)
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("throws when the user does not exist")
        void givenUnknownEmail_whenLogin_thenThrowsNotFound() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.loginByEmail("missing@example.com", "password123"))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("throws when password is incorrect")
        void givenWrongPassword_whenLogin_thenThrowsParamInvalid() {
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hashed_pw")
                    .status(1)
                    .build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpw", "hashed_pw")).thenReturn(false);

            assertThatThrownBy(() -> userService.loginByEmail("test@example.com", "wrongpw"))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
        }

        @Test
        @DisplayName("throws when user is disabled")
        void givenDisabledUser_whenLogin_thenThrowsForbidden() {
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hashed_pw")
                    .status(0)
                    .build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.loginByEmail("test@example.com", "password123"))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        }
    }

    @Nested
    @DisplayName("login by phone")
    class LoginByPhone {

        @Test
        @DisplayName("returns existing user when phone is registered")
        void givenExistingPhone_whenLogin_thenReturnsUser() {
            User user = User.builder()
                    .id(2L)
                    .phone("13800138000")
                    .status(1)
                    .build();
            when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user));

            assertThat(userService.loginByPhone("13800138000"))
                    .extracting(User::getId)
                    .isEqualTo(2L);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates a new user on first phone login")
        void givenNewPhone_whenLogin_thenCreatesUser() {
            when(userRepository.findByPhone("13800138001")).thenReturn(Optional.empty());

            User result = userService.loginByPhone("13800138001");

            assertThat(result)
                    .extracting(User::getPhone, User::getUsername, User::getNickname, User::getStatus)
                    .containsExactly("13800138001", "13800138001", "user_8001", 1);
            verify(userRepository).save(any());
        }

        @Test
        @DisplayName("throws when phone user is disabled")
        void givenDisabledPhoneUser_whenLogin_thenThrowsForbidden() {
            User user = User.builder()
                    .id(3L)
                    .phone("13800138002")
                    .status(0)
                    .build();
            when(userRepository.findByPhone("13800138002")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.loginByPhone("13800138002"))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        }

        @Test
        @DisplayName("returns the persisted user when first-login save loses a race")
        void givenConcurrentPhoneCreation_whenLogin_thenReloadsExistingUser() {
            User existingUser = User.builder()
                    .id(4L)
                    .phone("13800138003")
                    .username("13800138003")
                    .nickname("user_8003")
                    .status(1)
                    .build();
            when(userRepository.findByPhone("13800138003"))
                    .thenReturn(Optional.empty(), Optional.of(existingUser));
            doThrow(new AppException(ErrorCode.BUSINESS_CONFLICT, "手机号已被注册"))
                    .when(userRepository).save(any(User.class));

            assertThat(userService.loginByPhone("13800138003"))
                    .isSameAs(existingUser)
                    .extracting(User::getId)
                    .isEqualTo(4L);
        }
    }

    @Nested
    @DisplayName("update profile")
    class UpdateProfile {

        @Test
        @DisplayName("updates nickname and avatar for an existing user")
        void givenExistingUser_whenUpdateProfile_thenPersistsChanges() {
            User user = buildUser(10L, "user@example.com");
            when(userRepository.findById(10L)).thenReturn(Optional.of(user));

            userService.updateProfile(10L, "new-name", "avatar-key");

            assertThat(user)
                    .extracting(User::getNickname, User::getAvatarKey)
                    .containsExactly("new-name", "avatar-key");
            verify(userRepository).update(user);
        }

        @Test
        @DisplayName("keeps original fields when nullable inputs are omitted")
        void givenNullFields_whenUpdateProfile_thenKeepsOriginalValues() {
            User user = buildUser(11L, "keep@example.com");
            user.setAvatarKey("old-avatar");
            when(userRepository.findById(11L)).thenReturn(Optional.of(user));

            userService.updateProfile(11L, null, null);

            assertThat(user)
                    .extracting(User::getNickname, User::getAvatarKey)
                    .containsExactly("tester", "old-avatar");
            verify(userRepository).update(user);
        }

        @Test
        @DisplayName("throws when the user does not exist")
        void givenMissingUser_whenUpdateProfile_thenThrowsNotFound() {
            when(userRepository.findById(12L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile(12L, "new-name", "avatar-key"))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("get profile")
    class GetProfile {

        @Test
        @DisplayName("returns the user profile when the user exists")
        void givenExistingUser_whenGetProfile_thenReturnsUser() {
            User user = buildUser(20L, "profile@example.com");
            when(userRepository.findById(20L)).thenReturn(Optional.of(user));

            User result = userService.getProfile(20L);

            assertThat(result).isSameAs(user);
        }

        @Test
        @DisplayName("throws when the user does not exist")
        void givenMissingUser_whenGetProfile_thenThrowsNotFound() {
            when(userRepository.findById(21L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(21L))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("get stats")
    class GetStats {

        @Test
        @DisplayName("aggregates topic count, material count, and status counts")
        void givenRepositoryCounts_whenGetStats_thenReturnsAggregatedStats() {
            when(topicRepository.countTopicsByUserId(30L)).thenReturn(5L);
            when(materialRepository.countMaterials(any(MaterialListQuery.class))).thenReturn(12L);
            when(materialRepository.countByStatus(30L, null))
                    .thenReturn(Map.of("INBOX", 3L, "COLLECTED", 9L));

            UserStats result = userService.getStats(30L);

            ArgumentCaptor<MaterialListQuery> queryCaptor = ArgumentCaptor.forClass(MaterialListQuery.class);
            verify(materialRepository).countMaterials(queryCaptor.capture());
            MaterialListQuery query = queryCaptor.getValue();
            assertThat(query)
                    .extracting(MaterialListQuery::getUserId, MaterialListQuery::getPage, MaterialListQuery::getPageSize)
                    .containsExactly(30L, 1, 1);
            assertThat(result)
                    .extracting(UserStats::getTopicCount, UserStats::getMaterialCount)
                    .containsExactly(5L, 12L);
            assertThat(result.getStatusCounts()).containsEntry("INBOX", 3L);
            assertThat(result.getStatusCounts()).containsEntry("COLLECTED", 9L);
        }
    }

    private User buildUser(Long id, String email) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .id(id)
                .username(email)
                .email(email)
                .nickname("tester")
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
