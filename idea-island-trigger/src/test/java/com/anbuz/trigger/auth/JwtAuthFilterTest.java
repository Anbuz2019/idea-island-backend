package com.anbuz.trigger.auth;

import com.anbuz.domain.user.service.IAuthService;
import com.anbuz.domain.user.service.IAuthTokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter request scenarios")
class JwtAuthFilterTest {

    @Mock
    private IAuthService authService;

    @Mock
    private IAuthTokenService authTokenService;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Nested
    @DisplayName("open auth paths")
    class OpenAuthPaths {

        @Test
        @DisplayName("skips token validation for phone code requests")
        void givenPhoneCodePath_whenDoFilter_thenSkipsValidation() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/phone-code");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtAuthFilter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verifyNoInteractions(authService, authTokenService);
        }

        @Test
        @DisplayName("skips token validation for Knife4j and OpenAPI endpoints")
        void givenKnife4jPath_whenDoFilter_thenSkipsValidation() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs/default");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtAuthFilter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verifyNoInteractions(authService, authTokenService);
        }
    }

    @Nested
    @DisplayName("protected paths")
    class ProtectedPaths {

        @Test
        @DisplayName("returns unauthorized when the authorization header is missing")
        void givenMissingAuthorizationHeader_whenDoFilter_thenReturnsUnauthorized() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtAuthFilter.doFilter(request, response, chain);

            assertUnauthorized(response, "\"code\":1003");
        }

        @Test
        @DisplayName("returns unauthorized when the bearer token is blank")
        void givenBlankBearerToken_whenDoFilter_thenReturnsUnauthorized() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            request.addHeader("Authorization", "Bearer   ");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            jwtAuthFilter.doFilter(request, response, chain);

            assertUnauthorized(response, "Token 无效");
        }

        @Test
        @DisplayName("returns unauthorized when the stored token does not match")
        void givenStoredTokenMismatch_whenDoFilter_thenReturnsUnauthorized() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            request.addHeader("Authorization", "Bearer valid-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            when(authService.parseUserId("valid-token")).thenReturn(7L);
            when(authTokenService.getToken(7L)).thenReturn("other-token");

            jwtAuthFilter.doFilter(request, response, chain);

            assertUnauthorized(response, "\"code\":1003");
        }

        @Test
        @DisplayName("rejects the old token after a repeated login replaced it in redis")
        void givenOldTokenAfterRepeatedLogin_whenDoFilter_thenReturnsUnauthorized() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            request.addHeader("Authorization", "Bearer old-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            when(authService.parseUserId("old-token")).thenReturn(8L);
            when(authTokenService.getToken(8L)).thenReturn("new-token");

            jwtAuthFilter.doFilter(request, response, chain);

            assertUnauthorized(response, "Token 已失效");
        }

        @Test
        @DisplayName("refreshes the token when it is expiring soon and continues the chain")
        void givenExpiringToken_whenDoFilter_thenRefreshesTokenAndContinues() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            request.addHeader("Authorization", "Bearer valid-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            when(authService.parseUserId("valid-token")).thenReturn(9L);
            when(authTokenService.getToken(9L)).thenReturn("valid-token");
            when(authService.isExpiringSoon("valid-token")).thenReturn(true);
            when(authService.generateToken(9L)).thenReturn("new-token");

            jwtAuthFilter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verify(authTokenService).storeToken(9L, "new-token");
            assertThat(response).returns("new-token", it -> it.getHeader("X-Refresh-Token"));
        }

        @Test
        @DisplayName("returns unauthorized when jwt parsing fails")
        void givenInvalidJwt_whenDoFilter_thenReturnsUnauthorized() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
            request.addHeader("Authorization", "Bearer broken-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            when(authService.parseUserId("broken-token")).thenThrow(new IllegalArgumentException("invalid token"));

            jwtAuthFilter.doFilter(request, response, chain);

            assertUnauthorized(response, "\"code\":1003");
        }
    }

    private void assertUnauthorized(MockHttpServletResponse response, String expectedBodyPart) {
        assertThat(response)
                .returns(401, MockHttpServletResponse::getStatus)
                .extracting(JwtAuthFilterTest::responseBody, STRING)
                .contains(expectedBodyPart);
    }

    private static String responseBody(MockHttpServletResponse response) {
        try {
            return response.getContentAsString();
        } catch (Exception e) {
            throw new AssertionError("Failed to read response body", e);
        }
    }

}
