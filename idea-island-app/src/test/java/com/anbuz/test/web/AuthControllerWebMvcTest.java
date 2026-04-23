package com.anbuz.test.web;

import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.service.IAuthService;
import com.anbuz.domain.user.service.IAuthTokenService;
import com.anbuz.domain.user.service.ILoginVerificationCodeService;
import com.anbuz.domain.user.service.IUserService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.trigger.http.AuthController;
import com.anbuz.trigger.http.GlobalExceptionHandler;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController MockMvc scenarios")
class AuthControllerWebMvcTest {

    private static final String TEST_USER_HEADER = "X-Test-UserId";

    @Mock
    private IUserService userService;

    @Mock
    private IAuthService authService;

    @Mock
    private IAuthTokenService authTokenService;

    @Mock
    private ILoginVerificationCodeService loginVerificationCodeService;

    private MockMvc mockMvc;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userService, authService, authTokenService, loginVerificationCodeService);
        ReflectionTestUtils.setField(authController, "exposePhoneCode", true);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
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
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("returns a success payload when the request is valid")
        void givenValidRequest_whenRegister_thenReturnsSuccessPayload() throws Exception {
            User user = User.builder().id(1L).nickname("tester").build();
            when(userService.registerByEmail("register@example.com", "password123", "tester")).thenReturn(user);
            when(authService.generateToken(1L)).thenReturn("register-token");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"register@example.com","password":"password123","nickname":"tester"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.token").value("register-token"));
        }

        @Test
        @DisplayName("returns a parameter validation error when the email is invalid")
        void givenInvalidEmail_whenRegister_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"not-an-email","password":"password123","nickname":"tester"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));
        }
    }

    @Nested
    @DisplayName("login by email")
    class LoginByEmail {

        @Test
        @DisplayName("returns the login payload when the credentials are valid")
        void givenValidRequest_whenLogin_thenReturnsSuccessPayload() throws Exception {
            User user = User.builder().id(2L).nickname("reader").build();
            when(userService.loginByEmail("login@example.com", "password123")).thenReturn(user);
            when(authService.generateToken(2L)).thenReturn("login-token");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"login@example.com","password":"password123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").value(2))
                    .andExpect(jsonPath("$.data.nickname").value("reader"))
                    .andExpect(jsonPath("$.data.token").value("login-token"));
        }
    }

    @Nested
    @DisplayName("issue phone code")
    class IssuePhoneCode {

        @Test
        @DisplayName("returns the generated code in exposed mode")
        void givenValidPhone_whenIssuePhoneCode_thenReturnsVisibleCode() throws Exception {
            when(loginVerificationCodeService.issuePhoneCode("13800138000"))
                    .thenReturn(new ILoginVerificationCodeService.IssuedPhoneCode("123456", 300));

            mockMvc.perform(post("/api/v1/auth/phone-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"13800138000"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.sent").value(true))
                    .andExpect(jsonPath("$.data.code").value("123456"))
                    .andExpect(jsonPath("$.data.expireSeconds").value(300));
        }

        @Test
        @DisplayName("hides the generated code when exposure is disabled")
        void givenProductionMode_whenIssuePhoneCode_thenHidesVerificationCode() throws Exception {
            ReflectionTestUtils.setField(authController, "exposePhoneCode", false);
            when(loginVerificationCodeService.issuePhoneCode("13800138000"))
                    .thenReturn(new ILoginVerificationCodeService.IssuedPhoneCode("123456", 300));

            mockMvc.perform(post("/api/v1/auth/phone-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"13800138000"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.sent").value(true))
                    .andExpect(jsonPath("$.data.code").isEmpty())
                    .andExpect(jsonPath("$.data.expireSeconds").value(300));
        }
    }

    @Nested
    @DisplayName("login by phone")
    class LoginByPhone {

        @Test
        @DisplayName("returns the login payload when the verification code is valid")
        void givenValidVerificationCode_whenPhoneLogin_thenReturnsSuccessPayload() throws Exception {
            User user = User.builder().id(3L).nickname("mobile-user").build();
            when(loginVerificationCodeService.verifyPhoneCode("13800138000", "123456")).thenReturn(true);
            when(userService.loginByPhone("13800138000")).thenReturn(user);
            when(authService.generateToken(3L)).thenReturn("phone-token");

            mockMvc.perform(post("/api/v1/auth/phone-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"13800138000","code":"123456"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").value(3))
                    .andExpect(jsonPath("$.data.nickname").value("mobile-user"))
                    .andExpect(jsonPath("$.data.token").value("phone-token"));
        }

        @Test
        @DisplayName("returns a business error payload when the verification code is invalid")
        void givenInvalidVerificationCode_whenPhoneLogin_thenReturnsParamInvalid() throws Exception {
            when(loginVerificationCodeService.verifyPhoneCode("13800138000", "000000")).thenReturn(false);

            mockMvc.perform(post("/api/v1/auth/phone-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"13800138000","code":"000000"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("removes the current user's token and returns success")
        void givenCurrentUser_whenLogout_thenReturnsSuccessPayload() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header(TEST_USER_HEADER, "9"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            verify(authTokenService).removeToken(9L);
        }
    }

    private Filter testUserContextFilter() {
        return (request, response, chain) -> {
            String userId = request instanceof jakarta.servlet.http.HttpServletRequest httpServletRequest
                    ? httpServletRequest.getHeader(TEST_USER_HEADER)
                    : null;
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
}
