package com.anbuz.test.web;

import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.model.valobj.UserStats;
import com.anbuz.domain.user.service.IUserService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.trigger.http.GlobalExceptionHandler;
import com.anbuz.trigger.http.UserController;
import com.anbuz.types.model.ErrorCode;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController MockMvc scenarios")
class UserControllerWebMvcTest {

    private static final String TEST_USER_HEADER = "X-Test-UserId";

    @Mock
    private IUserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(userService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addFilters(testUserContextFilter())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested
    @DisplayName("get profile")
    class GetProfile {

        @Test
        @DisplayName("returns the current user's profile")
        void givenCurrentUser_whenGetProfile_thenReturnsProfilePayload() throws Exception {
            when(userService.getProfile(1L)).thenReturn(buildUser(1L, "reader"));

            mockMvc.perform(get("/api/v1/users/me").header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.nickname").value("reader"));
        }
    }

    @Nested
    @DisplayName("update profile")
    class UpdateProfile {

        @Test
        @DisplayName("returns validation error when nickname exceeds the limit")
        void givenOversizedNickname_whenUpdateProfile_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(put("/api/v1/users/me")
                            .header(TEST_USER_HEADER, "2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));
        }

        @Test
        @DisplayName("returns the updated profile when the request is valid")
        void givenValidRequest_whenUpdateProfile_thenReturnsUpdatedProfile() throws Exception {
            when(userService.getProfile(2L)).thenReturn(buildUser(2L, "updated"));

            mockMvc.perform(put("/api/v1/users/me")
                            .header(TEST_USER_HEADER, "2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname":"updated","avatarKey":"avatar-key"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.nickname").value("updated"))
                    .andExpect(jsonPath("$.data.avatarKey").value("avatar-key"));
        }
    }

    @Nested
    @DisplayName("get stats")
    class GetStats {

        @Test
        @DisplayName("returns the current user's statistics")
        void givenCurrentUser_whenGetStats_thenReturnsStatsPayload() throws Exception {
            when(userService.getStats(3L)).thenReturn(UserStats.builder()
                    .topicCount(5L)
                    .materialCount(12L)
                    .statusCounts(Map.of("INBOX", 4L, "COLLECTED", 8L))
                    .build());

            mockMvc.perform(get("/api/v1/users/me/stats").header(TEST_USER_HEADER, "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.topicCount").value(5))
                    .andExpect(jsonPath("$.data.materialCount").value(12))
                    .andExpect(jsonPath("$.data.statusCounts.COLLECTED").value(8));
        }
    }

    private Filter testUserContextFilter() {
        return (request, response, chain) -> {
            String userId = request.getParameter(TEST_USER_HEADER);
            if (userId == null && request instanceof jakarta.servlet.http.HttpServletRequest httpServletRequest) {
                userId = httpServletRequest.getHeader(TEST_USER_HEADER);
            }

            if (userId != null && !userId.isBlank()) {
                UserContext.set(Long.parseLong(userId));
            }

            try {
                chain.doFilter(request, response);
            } finally {
                UserContext.clear();
            }
        };
    }

    private User buildUser(Long id, String nickname) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .id(id)
                .username("user-" + id)
                .email("user" + id + "@example.com")
                .phone("13800138000")
                .nickname(nickname)
                .avatarKey("avatar-key")
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
