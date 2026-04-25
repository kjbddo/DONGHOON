package com.algoforge.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "algoforge.judge")
public record JudgeProperties(
        String submissionQueue,
        String resultQueue,
        String dlxExchange
) {}
