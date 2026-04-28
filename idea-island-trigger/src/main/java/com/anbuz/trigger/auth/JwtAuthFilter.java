package com.anbuz.trigger.auth;

import com.anbuz.domain.user.service.IAuthService;
import com.anbuz.domain.user.service.IAuthTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * JWT authentication filter that validates bearer tokens before entering controllers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    private static final Set<String> OPEN_API_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/phone-code",
            "/api/v1/auth/phone-login",
            "/doc.html",
            "/favicon.ico"
    );
    private static final Set<String> OPEN_API_PATH_PREFIXES = Set.of(
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars",
            "/knife4j"
    );

    private final IAuthService authService;
    private final IAuthTokenService authTokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return OPEN_API_PATHS.contains(requestUri)
                || OPEN_API_PATH_PREFIXES.stream().anyMatch(requestUri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "{\"code\":1003,\"message\":\"未登录\"}");
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response, "{\"code\":1003,\"message\":\"Token 无效\"}");
            return;
        }

        try {
            Long userId = authService.parseUserId(token);
            String clientType = resolveClientType(request);
            String storedToken = authTokenService.getToken(userId, clientType);
            if (!token.equals(storedToken)) {
                writeUnauthorized(response, "{\"code\":1003,\"message\":\"Token 已失效\"}");
                return;
            }

            UserContext.set(userId);
            if (authService.isExpiringSoon(token)) {
                String newToken = authService.generateToken(userId);
                authTokenService.storeToken(userId, clientType, newToken);
                response.setHeader("X-Refresh-Token", newToken);
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            writeUnauthorized(response, "{\"code\":1003,\"message\":\"Token 无效\"}");
        } finally {
            UserContext.clear();
        }
    }

    private String resolveClientType(HttpServletRequest request) {
        String explicit = request.getHeader(CLIENT_TYPE_HEADER);
        if (StringUtils.hasText(explicit)) {
            return explicit.trim().toLowerCase();
        }
        String userAgent = request.getHeader("User-Agent");
        if (!StringUtils.hasText(userAgent)) {
            return "web";
        }
        String normalized = userAgent.toLowerCase();
        return normalized.contains("mobile")
                || normalized.contains("android")
                || normalized.contains("iphone")
                || normalized.contains("ipad")
                ? "mobile"
                : "web";
    }

    private void writeUnauthorized(HttpServletResponse response, String body) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }
}
