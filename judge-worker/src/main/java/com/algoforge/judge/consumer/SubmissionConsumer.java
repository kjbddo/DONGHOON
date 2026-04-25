package com.algoforge.judge.consumer;

import com.algoforge.judge.config.JudgeProperties;
import com.algoforge.judge.dto.JudgeRequestMessage;
import com.algoforge.judge.dto.JudgeResultMessage;
import com.algoforge.judge.service.JudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 채점 요청 큐를 소비하고 결과를 result 큐에 publish 한다.
 *
 * 메시지 처리 도중 예외가 발생하면 catch 하여 SYSTEM_ERROR 결과를 publish 함으로써
 * 백엔드의 타임아웃·재시도 흐름과 충돌하지 않도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final JudgeService judgeService;
    private final JudgeProperties judgeProperties;

    @RabbitListener(queues = "${algoforge.judge.submission-queue}")
    public void onMessage(JudgeRequestMessage request) {
        log.info("[Judge] received submissionId={} problemId={} language={}",
                request.submissionId(), request.problemId(), request.languageName());

        JudgeResultMessage result;
        try {
            result = judgeService.judge(request);
        } catch (RuntimeException e) {
            log.error("[Judge] unexpected error judging submissionId={}", request.submissionId(), e);
            result = new JudgeResultMessage(
                    request.submissionId(),
                    "SYSTEM_ERROR",
                    0, 0,
                    null,
                    "judge worker exception: " + e.getClass().getSimpleName(),
                    List.of()
            );
        }

        rabbitTemplate.convertAndSend(judgeProperties.resultQueue(), result);
        log.info("[Judge] published result submissionId={} status={} cases={}",
                result.submissionId(), result.status(),
                result.testCaseResults() == null ? 0 : result.testCaseResults().size());
    }
}
