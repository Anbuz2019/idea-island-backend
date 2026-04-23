package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.user.model.entity.User;
import com.anbuz.infrastructure.persistent.dao.IUserDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(UserRepository.class)
@DisplayName("UserRepository H2 integration tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IUserDao userDao;

    @Nested
    @DisplayName("save user")
    class SaveUser {

        @Test
        @DisplayName("assigns a generated id and supports lookup by email")
        void givenNewUser_whenSave_thenGeneratedIdAndEmailLookupAreAvailable() {
            User user = buildUser("email-user", "test@example.com", null);

            userRepository.save(user);

            assertThat(user).extracting(User::getId).isNotNull();
            assertThat(userRepository.findByEmail("test@example.com"))
                    .hasValueSatisfying(saved -> assertThat(saved)
                            .extracting(User::getEmail, User::getNickname, User::getStatus)
                            .containsExactly("test@example.com", "tester", 1));
        }

        @Test
        @DisplayName("supports lookup by phone after save")
        void givenPhoneUser_whenSave_thenCanFindByPhone() {
            userRepository.save(buildUser("phone-user", null, "13800138000"));

            assertThat(userRepository.findByPhone("13800138000"))
                    .hasValueSatisfying(saved -> assertThat(saved)
                            .extracting(User::getUsername)
                            .isEqualTo("phone-user"));
        }

        @Test
        @DisplayName("supports lookup by username after save")
        void givenNamedUser_whenSave_thenCanFindByUsername() {
            userRepository.save(buildUser("named-user", "named@example.com", null));

            assertThat(userRepository.findByUsername("named-user"))
                    .hasValueSatisfying(saved -> assertThat(saved)
                            .extracting(User::getEmail)
                            .isEqualTo("named@example.com"));
        }

        @Test
        @DisplayName("reports email, phone, and username existence correctly")
        void givenSavedUsers_whenCheckExists_thenReturnsExpectedFlags() {
            userRepository.save(buildUser("exists-user", "exists@example.com", "13800138001"));

            assertThat(userRepository)
                    .returns(true, repository -> repository.existsByEmail("exists@example.com"))
                    .returns(true, repository -> repository.existsByPhone("13800138001"))
                    .returns(true, repository -> repository.existsByUsername("exists-user"))
                    .returns(false, repository -> repository.existsByEmail("missing@example.com"))
                    .returns(false, repository -> repository.existsByPhone("13800139999"))
                    .returns(false, repository -> repository.existsByUsername("missing-user"));
        }
    }

    @Nested
    @DisplayName("update user")
    class UpdateUser {

        @Test
        @DisplayName("updates nickname and avatar and persists the changes")
        void givenSavedUser_whenUpdate_thenNewValuesAreReturned() {
            User user = buildUser("update-user", "update@example.com", null);
            userRepository.save(user);

            User saved = userRepository.findByEmail("update@example.com").orElseThrow();
            saved.setNickname("new-nickname");
            saved.setAvatarKey("avatar-key");
            saved.setUpdatedAt(LocalDateTime.now());
            userRepository.update(saved);

            assertThat(userRepository.findById(saved.getId()))
                    .hasValueSatisfying(found -> assertThat(found)
                            .extracting(User::getNickname, User::getAvatarKey)
                            .containsExactly("new-nickname", "avatar-key"));
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
