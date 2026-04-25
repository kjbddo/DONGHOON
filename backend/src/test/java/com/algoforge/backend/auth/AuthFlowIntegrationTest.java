package com.algoforge.backend.auth;

import com.algoforge.backend.support.AbstractIntegrationTest;
import com.algoforge.backend.support.AuthTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회원가입 → 로그인 → 본인 정보 조회의 가장 기본적인 인증 흐름을 검증한다.
 * Redis 토큰 블랙리스트, JWT 발급/검증, BCrypt 해시 비교까지 모두 실제로 구동된다.
 */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthTestHelper auth;

    @Test
    @DisplayName("회원가입 → 로그인 → /api/users/me 200 OK")
    void signupLoginMe() {
        String email = "alice+" + System.nanoTime() + "@algoforge.test";
        String username = "alice" + (System.nanoTime() % 100000);
        String password = "passw0rd!";

        String accessToken = auth.signUpAndLogin(port, email, username, password);
        assertThat(accessToken).isNotBlank();

        ResponseEntity<String> res = auth.getAs(port, "/api/users/me", accessToken, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains(email);
        assertThat(res.getBody()).contains("ROLE_USER");
    }

    @Test
    @DisplayName("토큰 없이 /api/users/me 호출 시 401")
    void unauthenticatedRejected() {
        ResponseEntity<String> res = restTemplate.getForEntity(url("/api/users/me"), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("일반 사용자가 관리자 API 호출 시 403")
    void adminEndpointForbiddenForUser() {
        String token = auth.signUpAndLogin(
                port,
                "bob+" + System.nanoTime() + "@algoforge.test",
                "bob" + (System.nanoTime() % 100000),
                "passw0rd!"
        );

        ResponseEntity<String> res = auth.getAs(port, "/api/admin/problems", token, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
