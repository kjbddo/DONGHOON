package com.algoforge.backend.auth.controller;

import com.algoforge.backend.auth.dto.LoginRequest;
import com.algoforge.backend.auth.dto.RefreshRequest;
import com.algoforge.backend.auth.dto.SignUpRequest;
import com.algoforge.backend.auth.dto.SignUpResponse;
import com.algoforge.backend.auth.dto.TokenResponse;
import com.algoforge.backend.auth.service.AuthService;
import com.algoforge.backend.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰 갱신 / 로그아웃")
@SecurityRequirements // 인증 불필요 (logout만 토큰 사용)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest req) {
        SignUpResponse res = authService.signUp(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(res));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ApiResponse.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) RefreshRequest req
    ) {
        authService.logout(authorization, req == null ? null : req.refreshToken());
        return ApiResponse.ok();
    }
}
