package com.algoforge.backend.submission;

import com.algoforge.backend.config.JudgeProperties;
import com.algoforge.backend.judge.message.JudgeRequestMessage;
import com.algoforge.backend.judge.message.JudgeResultMessage;
import com.algoforge.backend.problem.repository.ProblemRepository;
import com.algoforge.backend.submission.dto.SubmitCodeRequest;
import com.algoforge.backend.support.AbstractIntegrationTest;
import com.algoforge.backend.support.AuthTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 제출 → judge.submission publish → (가짜 worker) judge.result publish → DB 갱신
 * 전체 채점 파이프라인을 RabbitMQ Testcontainer 위에서 검증한다.
 */
class SubmissionRabbitFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private AuthTestHelper auth;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private JudgeProperties judgeProperties;
    @Autowired private ProblemRepository problemRepository;

    @Test
    @DisplayName("제출 후 judge.submission에 메시지가 발행되고, ACCEPTED 결과를 받으면 상태가 갱신된다")
    void submitAndAccept() {
        long problemId = problemRepository.findBySlug("two-sum-basic")
                .orElseThrow()
                .getId();

        // 1) 사용자 가입 + 로그인
        String email = "carol+" + System.nanoTime() + "@algoforge.test";
        String token = auth.signUpAndLogin(port, email, "carol" + (System.nanoTime() % 100000), "passw0rd!");

        // 2) 코드 제출
        HttpHeaders headers = auth.bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        SubmitCodeRequest body = new SubmitCodeRequest(
                problemId,
                "PYTHON",
                "import sys\nA,B=map(int,sys.stdin.read().split())\nprint(A+B)\n"
        );
        ResponseEntity<String> submitRes = restTemplate.exchange(
                url("/api/submissions"),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
        assertThat(submitRes.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // 응답에서 submission id 추출 (간단 파싱: "id":<num>)
        String submitBody = submitRes.getBody();
        assertThat(submitBody).isNotNull();
        long submissionId = extractId(submitBody);

        // 3) 백엔드가 judge.submission 큐에 발행한 메시지를 수신
        JudgeRequestMessage msg = await()
                .atMost(Duration.ofSeconds(10))
                .until(
                        () -> (JudgeRequestMessage) rabbitTemplate.receiveAndConvert(
                                judgeProperties.submissionQueue(), 200
                        ),
                        m -> m != null
                );
        assertThat(msg.submissionId()).isEqualTo(submissionId);
        assertThat(msg.problemId()).isEqualTo(problemId);
        assertThat(msg.languageName()).isEqualTo("PYTHON");

        // 4) 가짜 worker로서 ACCEPTED 결과 전송
        JudgeResultMessage result = new JudgeResultMessage(
                submissionId,
                "ACCEPTED",
                42,
                10240,
                null,
                null,
                List.<JudgeResultMessage.TestCaseResult>of()
        );
        rabbitTemplate.convertAndSend(judgeProperties.resultQueue(), result);

        // 5) JudgeResultConsumer가 처리한 후 GET /api/submissions/{id} 가 ACCEPTED 가 될 때까지 대기
        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    ResponseEntity<String> detail = auth.getAs(
                            port, "/api/submissions/" + submissionId, token, String.class
                    );
                    assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(detail.getBody()).contains("\"ACCEPTED\"");
                });
    }

    private long extractId(String json) {
        // "id":12 형태에서 첫 매칭만 사용
        int idx = json.indexOf("\"id\"");
        if (idx < 0) throw new IllegalStateException("id missing in: " + json);
        int colon = json.indexOf(':', idx);
        int comma = json.indexOf(',', colon);
        int brace = json.indexOf('}', colon);
        int end = (comma < 0 || (brace > 0 && brace < comma)) ? brace : comma;
        return Long.parseLong(json.substring(colon + 1, end).trim());
    }
}
