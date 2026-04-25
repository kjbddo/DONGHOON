package com.algoforge.backend.judge.producer;

import com.algoforge.backend.config.JudgeProperties;
import com.algoforge.backend.judge.message.JudgeRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final JudgeProperties judgeProperties;

    public void publish(JudgeRequestMessage message) {
        log.info("[JudgeProducer] submissionId={} -> queue={}",
                message.submissionId(), judgeProperties.submissionQueue());
        rabbitTemplate.convertAndSend(judgeProperties.submissionQueue(), message);
    }
}
