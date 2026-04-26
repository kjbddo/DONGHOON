package com.algoforge.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * BaseTimeEntity 의 createdAt/updatedAt 이 OffsetDateTime 이므로,
 * 기본 CurrentDateTimeProvider(LocalDateTime) 대신 OffsetDateTime 을 반환하는 provider 가 필요.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingOffsetDateTimeProvider")
public class JpaConfig {

    @Bean
    public DateTimeProvider auditingOffsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
