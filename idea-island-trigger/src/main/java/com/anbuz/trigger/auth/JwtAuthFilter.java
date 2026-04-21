package com.anbuz.trigger.auth;

import com.anbuz.domain.user.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_KEY_PREFIX = "token:";

    private final AuthService authService;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":1003,\"message\":\"未登录\"}");
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Long userId = authService.parseUserId(token);
            String redisKey = TOKEN_KEY_PREFIX + userId;
            String storedToken = redisTemplate.opsForValue().get(redisKey);
            if (!token.equals(storedToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":1003,\"message\":\"Token 已失效\"}");
                return;
            }

            UserContext.set(userId);

            // 即将过期时续签
            if (authService.isExpiringSoon(token)) {
                String newToken = authService.generateToken(userId);
                redisTemplate.opsForValue().set(redisKey, newToken);
                response.setHeader("X-Refresh-Token", newToken);
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":1003,\"message\":\"Token 无效\"}");
        } finally {
            UserContext.clear();
        }
    }

}
