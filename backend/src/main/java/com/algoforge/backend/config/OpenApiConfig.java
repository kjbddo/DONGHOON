package com.algoforge.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI 3 문서 설정.
 *
 *  - 메인 진입: /swagger-ui/index.html
 *  - JSON 스펙: /v3/api-docs
 *  - JWT Bearer 토큰을 전역 SecurityRequirement로 등록 → 모든 보호 엔드포인트에서 자동 적용.
 *  - public/admin/internal 그룹 분리.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI algoforgeOpenAPI() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT Access Token. /api/auth/login 응답의 accessToken을 그대로 사용");

        return new OpenAPI()
                .info(new Info()
                        .title("AlgoForge API")
                        .description("""
                                AI 기반 알고리즘 학습 플랫폼 백엔드 API.
                                
                                  - 인증: JWT Bearer (`Authorization: Bearer <accessToken>`)
                                  - 비동기 채점: RabbitMQ + judge-worker
                                  - AI: Gemini 기반 문제 생성 / 피드백 / 반례
                                """)
                        .version("v1")
                        .contact(new Contact().name("AlgoForge Team").email("dev@algoforge.local"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(
                        new Server().url("/").description("현재 호스트"),
                        new Server().url("http://localhost:8080").description("로컬")
                ))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME_NAME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }

    /** 일반 사용자 API (인증 + 공개) */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .displayName("User API")
                .pathsToMatch("/api/auth/**", "/api/users/**", "/api/problems/**", "/api/submissions/**")
                .pathsToExclude("/api/admin/**")
                .build();
    }

    /** 관리자 전용 API */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin API")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    /** 운영/헬스 체크 */
    @Bean
    public GroupedOpenApi opsApi() {
        return GroupedOpenApi.builder()
                .group("ops")
                .displayName("Ops API")
                .pathsToMatch("/actuator/**", "/api/health/**")
                .build();
    }
}
