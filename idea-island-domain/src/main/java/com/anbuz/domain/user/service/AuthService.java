package com.anbuz.domain.user.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-days:7}")
    private int expireDays;

    public String generateToken(Long userId) {
        long expireMs = (long) expireDays * 24 * 60 * 60 * 1000;
        return JWT.create()
                .withClaim("userId", userId)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expireMs))
                .sign(Algorithm.HMAC256(secret));
    }

    public Long parseUserId(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
            return jwt.getClaim("userId").asLong();
        } catch (JWTVerificationException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    public boolean isExpiringSoon(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
            long remainingMs = jwt.getExpiresAt().getTime() - System.currentTimeMillis();
            return remainingMs < 24L * 60 * 60 * 1000;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

}
