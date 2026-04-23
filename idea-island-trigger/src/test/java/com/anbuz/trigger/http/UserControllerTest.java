package com.anbuz.trigger.http;

import com.anbuz.api.http.IUserController;
import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.model.valobj.UserStats;
import com.anbuz.domain.user.service.IUserService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController profile scenarios")
class UserControllerTest {

    @Mock
    private IUserService userService;

    @InjectMocks
    private UserController userController;

    @Nested
    @DisplayName("get profile")
    class GetProfile {

        @Test
        @DisplayName("returns the current user's profile")
        void givenCurrentUser_whenProfile_thenReturnsProfile() {
            UserContext.set(1L);
            try {
                when(userService.getProfile(1L)).thenReturn(buildUser(1L, "reader"));

                Result<IUserController.UserProfileResponse> result = userController.profile();

                assertThat(result).returns(0, Result::getCode);
                assertThat(result.getData())
                        .extracting(IUserController.UserProfileResponse::id, IUserController.UserProfileResponse::nickname)
                        .containsExactly(1L, "reader");
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("update profile")
    class UpdateProfile {

        @Test
        @DisplayName("updates the profile and returns the refreshed user data")
        void givenValidRequest_whenUpdate_thenReturnsUpdatedProfile() {
            IUserController.UpdateProfileRequest request = new IUserController.UpdateProfileRequest();
            request.setNickname("updated");
            request.setAvatarKey("avatar-key");

            UserContext.set(2L);
            try {
                when(userService.getProfile(2L)).thenReturn(buildUser(2L, "updated"));

                Result<IUserController.UserProfileResponse> result = userController.update(request);

                verify(userService).updateProfile(2L, "updated", "avatar-key");
                assertThat(result).returns(0, Result::getCode);
                assertThat(result.getData())
                        .extracting(IUserController.UserProfileResponse::nickname,
                                IUserController.UserProfileResponse::avatarKey)
                        .containsExactly("updated", "avatar-key");
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("get stats")
    class GetStats {

        @Test
        @DisplayName("returns the current user's statistics")
        void givenCurrentUser_whenStats_thenReturnsStats() {
            UserContext.set(3L);
            try {
                UserStats stats = UserStats.builder()
                        .topicCount(5L)
                        .materialCount(12L)
                        .statusCounts(Map.of("INBOX", 4L, "COLLECTED", 8L))
                        .build();
                when(userService.getStats(3L)).thenReturn(stats);

                Result<IUserController.UserStatsResponse> result = userController.stats();

                assertThat(result).returns(0, Result::getCode);
                assertThat(result.getData())
                        .extracting(IUserController.UserStatsResponse::topicCount)
                        .isEqualTo(5L);
                assertThat(result.getData().statusCounts()).containsEntry("COLLECTED", 8L);
            } finally {
                UserContext.clear();
            }
        }
    }

    private User buildUser(Long id, String nickname) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .id(id)
                .username("user-" + id)
                .email("user" + id + "@example.com")
                .nickname(nickname)
                .avatarKey("avatar-key")
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
