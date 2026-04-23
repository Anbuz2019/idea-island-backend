package com.anbuz.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthTokenRepository redis scenarios")
class AuthTokenRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthTokenRepository authTokenRepository;

    @BeforeEach
    void setUp() {
        authTokenRepository = new AuthTokenRepository(redisTemplate);
    }

    @Nested
    @DisplayName("store token")
    class StoreToken {

        @Test
        @DisplayName("writes the token to redis with the provided ttl")
        void givenUserIdAndToken_whenStoreToken_thenWritesRedisValueWithTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            authTokenRepository.storeToken(1L, "access-token", Duration.ofDays(7));

            verify(valueOperations).set("token:1", "access-token", Duration.ofDays(7));
        }
    }

    @Nested
    @DisplayName("get token")
    class GetToken {

        @Test
        @DisplayName("reads the token from redis")
        void givenStoredToken_whenGetToken_thenReturnsRedisValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("token:2")).thenReturn("stored-token");

            assertThat(authTokenRepository.getToken(2L)).isEqualTo("stored-token");
            verify(valueOperations).get("token:2");
        }
    }

    @Nested
    @DisplayName("remove token")
    class RemoveToken {

        @Test
        @DisplayName("deletes the token key from redis")
        void givenUserId_whenRemoveToken_thenDeletesRedisKey() {
            authTokenRepository.removeToken(3L);

            verify(redisTemplate).delete("token:3");
        }
    }

}
