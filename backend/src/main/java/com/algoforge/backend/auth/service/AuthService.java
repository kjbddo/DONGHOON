package com.algoforge.backend.auth.service;

import com.algoforge.backend.auth.domain.RefreshToken;
import com.algoforge.backend.auth.dto.LoginRequest;
import com.algoforge.backend.auth.dto.RefreshRequest;
import com.algoforge.backend.auth.dto.SignUpRequest;
import com.algoforge.backend.auth.dto.SignUpResponse;
import com.algoforge.backend.auth.dto.TokenResponse;
import com.algoforge.backend.auth.repository.RefreshTokenRepository;
import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.common.util.HashUtil;
import com.algoforge.backend.security.jwt.JwtProperties;
import com.algoforge.backend.security.jwt.JwtTokenProvider;
import com.algoforge.backend.user.domain.Role;
import com.algoforge.backend.user.domain.User;
import com.algoforge.backend.user.domain.UserStatus;
import com.algoforge.backend.user.repository.RoleRepository;
import com.algoforge.backend.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    // ===== Sign Up =====
    @Transactional
    public SignUpResponse signUp(SignUpRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 username 입니다.");
        }

        Role userRole = roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER 시드가 없습니다. Flyway 마이그레이션을 확인하세요."));

        User user = User.builder()
                .email(req.email())
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .status(UserStatus.ACTIVE)
                .build();
        user.assignRoles(List.of(userRole));

        User saved = userRepository.save(user);
        log.info("회원가입: userId={} email={}", saved.getId(), saved.getEmail());
        return new SignUpResponse(saved.getId(), saved.getEmail(), saved.getUsername());
    }

    // ===== Login =====
    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        return issueTokenPair(user);
    }

    // ===== Refresh (Rotation) =====
    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        Claims claims = parseRefresh(req.refreshToken());
        Long userId = jwtTokenProvider.getUserId(claims);

        String hash = HashUtil.sha256Hex(req.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (!stored.isUsable(OffsetDateTime.now()) || !stored.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // Rotation: 기존 refresh 즉시 폐기
        stored.revoke();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        return issueTokenPair(user);
    }

    // ===== Logout =====
    @Transactional
    public void logout(String accessTokenHeader, String refreshTokenOrNull) {
        // 1) Access jti 블랙리스트
        String accessToken = stripBearer(accessTokenHeader);
        if (StringUtils.hasText(accessToken)) {
            try {
                Claims claims = jwtTokenProvider.parse(accessToken).getPayload();
                String jti = claims.getId();
                long ttl = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
                tokenBlacklistService.blacklist(jti, ttl);
            } catch (Exception e) {
                log.debug("logout: access token 파싱 실패 (만료 등) - 무시");
            }
        }

        // 2) Refresh 토큰 폐기
        if (StringUtils.hasText(refreshTokenOrNull)) {
            String hash = HashUtil.sha256Hex(refreshTokenOrNull);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(RefreshToken::revoke);
        }
    }

    // ===== private =====
    private TokenResponse issueTokenPair(User user) {
        List<String> roles = user.getRoleNames();
        String access = jwtTokenProvider.createAccessToken(user.getId(), roles);
        String refresh = jwtTokenProvider.createRefreshToken(user.getId());

        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusSeconds(jwtProperties.refreshTokenValiditySeconds());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(HashUtil.sha256Hex(refresh))
                .expiresAt(expiresAt)
                .build());

        return TokenResponse.bearer(access, refresh, jwtProperties.accessTokenValiditySeconds());
    }

    private Claims parseRefresh(String refreshToken) {
        try {
            Jws<Claims> jws = jwtTokenProvider.parse(refreshToken);
            Claims claims = jws.getPayload();
            if (!JwtTokenProvider.TYPE_REFRESH.equals(jwtTokenProvider.getType(claims))) {
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
            }
            return claims;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    private String stripBearer(String header) {
        if (header == null) return null;
        if (header.startsWith("Bearer ")) return header.substring(7);
        return header;
    }
}
