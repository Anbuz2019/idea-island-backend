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
@DisplayName("LoginVerificationCodeRepository redis scenarios")
class LoginVerificationCodeRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginVerificationCodeRepository loginVerificationCodeRepository;

    @BeforeEach
    void setUp() {
        loginVerificationCodeRepository = new LoginVerificationCodeRepository(redisTemplate);
    }

    @Nested
    @DisplayName("store code")
    class StoreCode {

        @Test
        @DisplayName("writes the verification code to redis with the provided ttl")
        void givenPhoneAndCode_whenStoreCode_thenWritesRedisValueWithTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            loginVerificationCodeRepository.storeCode("13800138000", "123456", Duration.ofMinutes(5));

            verify(valueOperations).set("login:code:13800138000", "123456", Duration.ofMinutes(5));
        }
    }

    @Nested
    @DisplayName("get code")
    class GetCode {

        @Test
        @DisplayName("reads the verification code from redis")
        void givenStoredCode_whenGetCode_thenReturnsRedisValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("login:code:13800138000")).thenReturn("123456");

            assertThat(loginVerificationCodeRepository.getCode("13800138000")).isEqualTo("123456");
            verify(valueOperations).get("login:code:13800138000");
        }
    }

    @Nested
    @DisplayName("remove code")
    class RemoveCode {

        @Test
        @DisplayName("deletes the verification code key from redis")
        void givenPhone_whenRemoveCode_thenDeletesRedisKey() {
            loginVerificationCodeRepository.removeCode("13800138000");

            verify(redisTemplate).delete("login:code:13800138000");
        }
    }

}
