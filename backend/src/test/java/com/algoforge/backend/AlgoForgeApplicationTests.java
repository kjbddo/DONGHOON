package com.algoforge.backend;

import com.algoforge.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 애플리케이션 컨텍스트가 정상적으로 부팅되는지 검증한다.
 * 모든 기본 빈(JPA / Redis / RabbitMQ / Security / Resilience4j) 와이어링을 함께 검증.
 */
class AlgoForgeApplicationTests extends AbstractIntegrationTest {

    @Test
    @DisplayName("Spring Boot 컨텍스트가 정상적으로 로드된다")
    void contextLoads() {
        // AbstractIntegrationTest로 컨테이너 + 컨텍스트 부팅까지만 검증
    }
}
