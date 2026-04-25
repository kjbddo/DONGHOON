package com.algoforge.backend.support;

import com.algoforge.backend.auth.dto.LoginRequest;
import com.algoforge.backend.auth.dto.SignUpRequest;
import com.algoforge.backend.auth.dto.TokenResponse;
import com.algoforge.backend.common.response.ApiResponse;
import com.algoforge.backend.user.domain.Role;
import com.algoforge.backend.user.domain.User;
import com.algoforge.backend.user.domain.UserStatus;
import com.algoforge.backend.user.repository.RoleRepository;
import com.algoforge.backend.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 통합 테스트에서 회원가입 / 로그인 / 관리자 권한 부여를 단순화하는 헬퍼.
 */
@Component
public class AuthTestHelper {

    @Autowired private TestRestTemplate rest;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    public void signUp(int port, String email, String username, String password) {
        ResponseEntity<String> res = rest.postForEntity(
                "http://localhost:" + port + "/api/auth/signup",
                new SignUpRequest(email, username, password),
                String.class
        );
        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("signup failed: " + res.getStatusCode() + " body=" + res.getBody());
        }
    }

    public TokenResponse login(int port, String email, String password) {
        ResponseEntity<String> res = rest.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                new LoginRequest(email, password),
                String.class
        );
        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("login failed: " + res.getStatusCode() + " body=" + res.getBody());
        }
        try {
            ApiResponse<TokenResponse> wrapped = objectMapper.readValue(
                    res.getBody(),
                    new TypeReference<>() {}
            );
            return wrapped.data();
        } catch (Exception e) {
            throw new IllegalStateException("login parse failure", e);
        }
    }

    /**
     * 회원가입 + 로그인을 한 번에 수행하고 access token을 반환한다.
     */
    public String signUpAndLogin(int port, String email, String username, String password) {
        signUp(port, email, username, password);
        return login(port, email, password).accessToken();
    }

    /**
     * email로 가입된 유저에게 ROLE_ADMIN을 부여 (이미 부여된 경우 noop).
     */
    @Transactional
    public void grantAdminRole(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Role admin = roleRepository.findByName(Role.ADMIN).orElseThrow();
        if (user.getRoles().stream().noneMatch(r -> r.getName().equals(Role.ADMIN))) {
            user.assignRoles(List.of(admin));
            userRepository.save(user);
        }
    }

    /**
     * 직접 BCrypt 해시 + 역할 부여를 거쳐 관리자 계정을 만든다 (테스트 데이터 시드용).
     */
    @Transactional
    public User createAdmin(String email, String username, String rawPassword) {
        Role admin = roleRepository.findByName(Role.ADMIN).orElseThrow();
        Role basic = roleRepository.findByName(Role.USER).orElseThrow();
        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .build();
        user.assignRoles(List.of(admin, basic));
        return userRepository.save(user);
    }

    public HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    public <T> ResponseEntity<T> getAs(int port, String path, String token, Class<T> type) {
        return rest.exchange(
                "http://localhost:" + port + path,
                HttpMethod.GET,
                new HttpEntity<>(bearer(token)),
                type
        );
    }
}
