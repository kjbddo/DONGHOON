package com.algoforge.judge.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${algoforge.judge.submission-queue}")
    private String submissionQueue;

    @Value("${algoforge.judge.result-queue}")
    private String resultQueue;

    @Value("${algoforge.judge.dlx-exchange}")
    private String dlxExchange;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        return t;
    }

    @Bean
    public Queue submissionQueue() {
        return QueueBuilder.durable(submissionQueue)
                .deadLetterExchange(dlxExchange)
                .deadLetterRoutingKey(submissionQueue + ".dead")
                .build();
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(resultQueue).build();
    }
}
