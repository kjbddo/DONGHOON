package com.algoforge.backend.security.jwt;

import com.algoforge.backend.auth.service.TokenBlacklistService;
import com.algoforge.backend.security.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                Jws<Claims> jws = tokenProvider.parse(token);
                Claims claims = jws.getPayload();
                if (!JwtTokenProvider.TYPE_ACCESS.equals(tokenProvider.getType(claims))) {
                    chain.doFilter(request, response);
                    return;
                }
                String jti = claims.getId();
                if (tokenBlacklistService.isBlacklisted(jti)) {
                    log.debug("JWT 블랙리스트에 등록된 토큰: jti={}", jti);
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }
                Long userId = tokenProvider.getUserId(claims);
                List<String> roles = tokenProvider.getRoles(claims);

                CurrentUser principal = CurrentUser.builder().userId(userId).roles(roles).build();
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null,
                        roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.debug("JWT 검증 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
