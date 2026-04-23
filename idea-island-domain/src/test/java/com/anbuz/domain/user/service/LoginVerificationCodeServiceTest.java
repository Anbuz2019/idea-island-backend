package com.anbuz.domain.user.service;

import com.anbuz.domain.user.repository.ILoginVerificationCodeRepository;
import com.anbuz.domain.user.service.impl.LoginVerificationCodeService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginVerificationCodeService verification scenarios")
class LoginVerificationCodeServiceTest {

    @Mock
    private ILoginVerificationCodeRepository loginVerificationCodeRepository;

    private LoginVerificationCodeService loginVerificationCodeService;

    @BeforeEach
    void setUp() {
        loginVerificationCodeService = new LoginVerificationCodeService(loginVerificationCodeRepository);
        ReflectionTestUtils.setField(loginVerificationCodeService, "ttlMinutes", 5L);
    }

    @Nested
    @DisplayName("issue code")
    class IssueCode {

        @Test
        @DisplayName("stores a six-digit code with ttl and returns metadata")
        void givenPhone_whenIssuePhoneCode_thenStoresGeneratedCodeWithTtl() {
            ILoginVerificationCodeService.IssuedPhoneCode issued =
                    loginVerificationCodeService.issuePhoneCode("13800138000");

            assertThat(issued)
                    .returns(300L, ILoginVerificationCodeService.IssuedPhoneCode::expireSeconds)
                    .satisfies(it -> assertThat(it.code()).matches("\\d{6}"));
            verify(loginVerificationCodeRepository)
                    .storeCode("13800138000", issued.code(), Duration.ofMinutes(5));
        }
    }

    @Nested
    @DisplayName("verify code")
    class VerifyCode {

        @Test
        @DisplayName("returns true and deletes the code when it matches")
        void givenMatchingCode_whenVerifyPhoneCode_thenDeletesCodeAndReturnsTrue() {
            when(loginVerificationCodeRepository.getCode("13800138000")).thenReturn("123456");

            assertThat(loginVerificationCodeService.verifyPhoneCode("13800138000", "123456")).isTrue();
            verify(loginVerificationCodeRepository).removeCode("13800138000");
        }

        @Test
        @DisplayName("returns false when the code is missing")
        void givenMissingCode_whenVerifyPhoneCode_thenReturnsFalse() {
            when(loginVerificationCodeRepository.getCode("13800138000")).thenReturn(null);

            assertThat(loginVerificationCodeService.verifyPhoneCode("13800138000", "123456")).isFalse();
            verify(loginVerificationCodeRepository, never()).removeCode("13800138000");
        }

        @Test
        @DisplayName("returns false when the code does not match")
        void givenDifferentCode_whenVerifyPhoneCode_thenReturnsFalse() {
            when(loginVerificationCodeRepository.getCode("13800138000")).thenReturn("654321");

            assertThat(loginVerificationCodeService.verifyPhoneCode("13800138000", "123456")).isFalse();
            verify(loginVerificationCodeRepository, never()).removeCode("13800138000");
        }
    }

}
