package com.algoforge.backend.problem;

import com.algoforge.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V2__seed_sample_data.sql로 시딩된 "두 수의 합" 문제가
 * 공개 목록/상세 API에서 정상적으로 노출되는지 검증.
 *
 *  - hidden 테스트 케이스(seq=3)는 공개 응답에서 제외되어야 한다.
 *  - 인증 없이도 GET /api/problems 는 호출 가능해야 한다.
 */
class ProblemFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("GET /api/problems 시드된 문제 목록을 반환한다")
    void listPublicProblems() {
        ResponseEntity<String> res = restTemplate.getForEntity(url("/api/problems"), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"두 수의 합\"");
        assertThat(res.getBody()).contains("\"two-sum-basic\"");
        assertThat(res.getBody()).contains("\"BRONZE\"");
    }

    @Test
    @DisplayName("GET /api/problems/slug/{slug} 상세 응답에 hidden 테스트 케이스가 노출되지 않는다")
    void detailHidesPrivateTestCases() {
        ResponseEntity<String> res = restTemplate.getForEntity(
                url("/api/problems/slug/two-sum-basic"), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = res.getBody();
        assertThat(body).isNotNull();
        // 공개 케이스의 입력은 노출
        assertThat(body).contains("\"1 2\"");
        assertThat(body).contains("\"100 200\"");
        // hidden 케이스의 입력은 노출 금지
        assertThat(body).doesNotContain("-1000000000 1000000000");
    }
}
