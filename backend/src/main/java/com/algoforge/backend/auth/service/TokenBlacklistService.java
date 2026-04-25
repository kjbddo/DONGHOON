package com.algoforge.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Access Token 블랙리스트.
 *
 * 로그아웃 시 access token의 jti를 Redis에 저장하고, JwtAuthenticationFilter에서 매 요청마다 확인.
 * TTL은 access 만료 시각까지로 설정해 자동으로 정리되게 한다.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) return;
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        Boolean has = redisTemplate.hasKey(KEY_PREFIX + jti);
        return Boolean.TRUE.equals(has);
    }
}
