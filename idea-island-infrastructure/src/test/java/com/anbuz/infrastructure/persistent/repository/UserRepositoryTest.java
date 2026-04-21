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
@DisplayName("UserRepository H2 集成测试")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IUserDao userDao;

    @Nested
    @DisplayName("保存用户")
    class SaveUser {

        @Test
        @DisplayName("保存用户后可按邮箱查询到")
        void givenNewUser_whenSave_thenCanFindByEmail() {
            User user = buildUser("test@example.com");

            userRepository.save(user);

            assertThat(userRepository.findByEmail("test@example.com"))
                    .isPresent()
                    .get()
                    .satisfies(u -> {
                        assertThat(u.getEmail()).isEqualTo("test@example.com");
                        assertThat(u.getNickname()).isEqualTo("测试");
                        assertThat(u.getStatus()).isEqualTo(1);
                    });
        }

        @Test
        @DisplayName("邮箱存在性检查返回正确结果")
        void givenExistingEmail_whenCheckExists_thenReturnsTrue() {
            userRepository.save(buildUser("exists@example.com"));

            assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
            assertThat(userRepository.existsByEmail("notexists@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("更新用户")
    class UpdateUser {

        @Test
        @DisplayName("更新昵称后查询到新昵称")
        void givenSavedUser_whenUpdateNickname_thenNewNicknameReturned() {
            User user = buildUser("update@example.com");
            userRepository.save(user);

            User saved = userRepository.findByEmail("update@example.com").orElseThrow();
            saved.setNickname("新昵称");
            saved.setUpdatedAt(LocalDateTime.now());
            userRepository.update(saved);

            assertThat(userRepository.findById(saved.getId()))
                    .isPresent()
                    .get()
                    .extracting(User::getNickname)
                    .isEqualTo("新昵称");
        }
    }

    private User buildUser(String email) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .username(email)
                .email(email)
                .passwordHash("hashed")
                .nickname("测试")
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
