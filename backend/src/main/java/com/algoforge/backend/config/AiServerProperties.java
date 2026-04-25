package com.algoforge.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "algoforge.ai")
public record AiServerProperties(
        String baseUrl,
        String internalToken,
        int timeoutSeconds
) {}
