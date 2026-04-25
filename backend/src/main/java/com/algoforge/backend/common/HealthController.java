package com.algoforge.backend.common;

import com.algoforge.backend.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Ops", description = "헬스체크 / 운영 엔드포인트")
@SecurityRequirements // 인증 불필요
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.ok(Map.of(
                "service", "algoforge-backend",
                "status", "UP"
        ));
    }
}
