package com.anbuz.trigger.http;

import com.anbuz.api.http.IAuthController;
import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.service.IAuthService;
import com.anbuz.domain.user.service.IAuthTokenService;
import com.anbuz.domain.user.service.ILoginVerificationCodeService;
import com.anbuz.domain.user.service.IUserService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController authentication scenarios")
class AuthControllerTest {

    @Mock
    private IUserService userService;

    @Mock
    private IAuthService authService;

    @Mock
    private IAuthTokenService authTokenService;

    @Mock
    private ILoginVerificationCodeService loginVerificationCodeService;

    @InjectMocks
    private AuthController authController;

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("stores the token and returns the register payload")
        void givenValidRequest_whenRegister_thenStoresTokenAndReturnsPayload() {
            IAuthController.RegisterRequest request = new IAuthController.RegisterRequest();
            request.setEmail("register@example.com");
            request.setPassword("password123");
            request.setNickname("tester");

            User user = User.builder().id(1L).nickname("tester").build();
            when(userService.registerByEmail("register@example.com", "password123", "tester")).thenReturn(user);
            when(authService.generateToken(1L)).thenReturn("register-token");

            Result<IAuthController.RegisterResponse> result = authController.register(request);

            assertThat(result).returns(0, Result::getCode);
            assertThat(result.getData())
                    .extracting(IAuthController.RegisterResponse::userId, IAuthController.RegisterResponse::token)
                    .containsExactly(1L, "register-token");
            verify(authTokenService).storeToken(1L, "register-token");
        }
    }

    @Nested
    @DisplayName("login by email")
    class LoginByEmail {

        @Test
        @DisplayName("stores the token and returns the login payload")
        void givenValidRequest_whenLogin_thenStoresTokenAndReturnsPayload() {
            IAuthController.LoginRequest request = new IAuthController.LoginRequest();
            request.setEmail("login@example.com");
            request.setPassword("password123");

            User user = User.builder().id(2L).nickname("reader").build();
            when(userService.loginByEmail("login@example.com", "password123")).thenReturn(user);
            when(authService.generateToken(2L)).thenReturn("login-token");

            Result<IAuthController.LoginResponse> result = authController.login(request);

            assertThat(result).returns(0, Result::getCode);
            assertThat(result.getData())
                    .extracting(IAuthController.LoginResponse::userId,
                            IAuthController.LoginResponse::nickname,
                            IAuthController.LoginResponse::token)
                    .containsExactly(2L, "reader", "login-token");
            verify(authTokenService).storeToken(2L, "login-token");
        }
    }

    @Nested
    @DisplayName("issue phone code")
    class IssuePhoneCode {

        @Test
        @DisplayName("returns the code in the response when expose mode is enabled")
        void givenExposeModeEnabled_whenIssuePhoneCode_thenReturnsVisibleCode() {
            ReflectionTestUtils.setField(authController, "exposePhoneCode", true);
            IAuthController.PhoneCodeRequest request = new IAuthController.PhoneCodeRequest();
            request.setPhone("13800138000");
            when(loginVerificationCodeService.issuePhoneCode("13800138000"))
                    .thenReturn(new ILoginVerificationCodeService.IssuedPhoneCode("123456", 300));

            Result<IAuthController.PhoneCodeResponse> result = authController.issuePhoneCode(request);

            assertThat(result).returns(0, Result::getCode);
            assertThat(result.getData())
                    .extracting(IAuthController.PhoneCodeResponse::sent,
                            IAuthController.PhoneCodeResponse::code,
                            IAuthController.PhoneCodeResponse::expireSeconds)
                    .containsExactly(true, "123456", 300L);
        }

        @Test
        @DisplayName("hides the code in the response when expose mode is disabled")
        void givenExposeModeDisabled_whenIssuePhoneCode_thenHidesCode() {
            ReflectionTestUtils.setField(authController, "exposePhoneCode", false);
            IAuthController.PhoneCodeRequest request = new IAuthController.PhoneCodeRequest();
            request.setPhone("13800138000");
            when(loginVerificationCodeService.issuePhoneCode("13800138000"))
                    .thenReturn(new ILoginVerificationCodeService.IssuedPhoneCode("123456", 300));

            Result<IAuthController.PhoneCodeResponse> result = authController.issuePhoneCode(request);

            assertThat(result)
                    .returns(0, Result::getCode)
                    .extracting(Result::getData)
                    .extracting(IAuthController.PhoneCodeResponse::code)
                    .isNull();
        }
    }

    @Nested
    @DisplayName("login by phone")
    class LoginByPhone {

        @Test
        @DisplayName("verifies the code, stores the token, and returns the login payload")
        void givenValidPhoneCode_whenPhoneLogin_thenReturnsPayload() {
            IAuthController.PhoneLoginRequest request = new IAuthController.PhoneLoginRequest();
            request.setPhone("13800138000");
            request.setCode("123456");

            User user = User.builder().id(3L).nickname("user_8000").build();
            when(loginVerificationCodeService.verifyPhoneCode("13800138000", "123456")).thenReturn(true);
            when(userService.loginByPhone("13800138000")).thenReturn(user);
            when(authService.generateToken(3L)).thenReturn("phone-token");

            Result<IAuthController.LoginResponse> result = authController.phoneLogin(request);

            assertThat(result).returns(0, Result::getCode);
            assertThat(result.getData())
                    .extracting(IAuthController.LoginResponse::userId, IAuthController.LoginResponse::token)
                    .containsExactly(3L, "phone-token");
            verify(authTokenService).storeToken(3L, "phone-token");
        }

        @Test
        @DisplayName("throws when the verification code is invalid")
        void givenInvalidPhoneCode_whenPhoneLogin_thenThrowsParamInvalid() {
            IAuthController.PhoneLoginRequest request = new IAuthController.PhoneLoginRequest();
            request.setPhone("13800138000");
            request.setCode("000000");

            when(loginVerificationCodeService.verifyPhoneCode("13800138000", "000000")).thenReturn(false);

            assertThatThrownBy(() -> authController.phoneLogin(request))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("removes the current user's token")
        void givenCurrentUser_whenLogout_thenRemovesToken() {
            UserContext.set(9L);
            try {
                Result<Void> result = authController.logout();

                assertThat(result).returns(0, Result::getCode);
                verify(authTokenService).removeToken(9L);
            } finally {
                UserContext.clear();
            }
        }
    }

}
