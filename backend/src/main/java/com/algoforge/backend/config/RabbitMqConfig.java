package com.algoforge.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final JudgeProperties judgeProperties;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public DirectExchange judgeDlxExchange() {
        return new DirectExchange(judgeProperties.dlxExchange(), true, false);
    }

    @Bean
    public Queue judgeSubmissionQueue() {
        return QueueBuilder.durable(judgeProperties.submissionQueue())
                .deadLetterExchange(judgeProperties.dlxExchange())
                .deadLetterRoutingKey(judgeProperties.submissionQueue() + ".dead")
                .build();
    }

    @Bean
    public Queue judgeSubmissionDeadQueue() {
        return QueueBuilder.durable(judgeProperties.submissionQueue() + ".dead").build();
    }

    @Bean
    public Binding judgeSubmissionDeadBinding() {
        return BindingBuilder.bind(judgeSubmissionDeadQueue())
                .to(judgeDlxExchange())
                .with(judgeProperties.submissionQueue() + ".dead");
    }

    @Bean
    public Queue judgeResultQueue() {
        return QueueBuilder.durable(judgeProperties.resultQueue()).build();
    }
}
