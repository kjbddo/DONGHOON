package com.algoforge.backend.config;

import com.algoforge.backend.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        WebConfig.CorsProperties.class,
        AiServerProperties.class,
        AiQuotaProperties.class,
        JudgeProperties.class,
        ProblemImportProperties.class
})
public class PropertiesConfig {
}
