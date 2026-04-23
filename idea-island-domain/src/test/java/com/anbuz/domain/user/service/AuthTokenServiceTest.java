package com.anbuz.domain.user.service;

import com.anbuz.domain.user.repository.IAuthTokenRepository;
import com.anbuz.domain.user.service.impl.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthTokenService session scenarios")
class AuthTokenServiceTest {

    @Mock
    private IAuthTokenRepository authTokenRepository;

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService(authTokenRepository);
        ReflectionTestUtils.setField(authTokenService, "expireDays", 7);
    }

    @Nested
    @DisplayName("store token")
    class StoreToken {

        @Test
        @DisplayName("stores the token with the configured ttl")
        void givenUserIdAndToken_whenStoreToken_thenDelegatesWithConfiguredTtl() {
            authTokenService.storeToken(1L, "access-token");

            verify(authTokenRepository).storeToken(1L, "access-token", Duration.ofDays(7));
        }
    }

    @Nested
    @DisplayName("get token")
    class GetToken {

        @Test
        @DisplayName("returns the token read from the repository")
        void givenStoredToken_whenGetToken_thenReturnsRepositoryValue() {
            when(authTokenRepository.getToken(2L)).thenReturn("stored-token");

            assertThat(authTokenService.getToken(2L)).isEqualTo("stored-token");
            verify(authTokenRepository).getToken(2L);
        }
    }

    @Nested
    @DisplayName("remove token")
    class RemoveToken {

        @Test
        @DisplayName("removes the token from the repository")
        void givenUserId_whenRemoveToken_thenDelegatesRemoval() {
            authTokenService.removeToken(3L);

            verify(authTokenRepository).removeToken(3L);
        }
    }

}
