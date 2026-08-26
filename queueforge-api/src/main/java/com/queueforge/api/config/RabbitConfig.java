package com.queueforge.api.config;

import com.queueforge.common.messaging.QueueForgeQueues;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ topology (exchange, jobs queue, binding) and the JSON
 * message converter used when publishing job messages.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange queueForgeExchange() {
        return new DirectExchange(QueueForgeQueues.EXCHANGE, true, false);
    }

    @Bean
    public Queue jobsQueue() {
        return QueueBuilder.durable(QueueForgeQueues.JOBS_QUEUE).build();
    }

    @Bean
    public Binding jobsBinding(Queue jobsQueue, DirectExchange queueForgeExchange) {
        return BindingBuilder.bind(jobsQueue)
                .to(queueForgeExchange)
                .with(QueueForgeQueues.JOB_ROUTING_KEY);
    }

    // Retry queue: a job waits here for a per-message delay, then dead-letters
    // back to the main exchange for another attempt.

    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange(QueueForgeQueues.RETRY_EXCHANGE, true, false);
    }

    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(QueueForgeQueues.RETRY_QUEUE)
                .deadLetterExchange(QueueForgeQueues.EXCHANGE)
                .deadLetterRoutingKey(QueueForgeQueues.JOB_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding retryBinding(Queue retryQueue, DirectExchange retryExchange) {
        return BindingBuilder.bind(retryQueue)
                .to(retryExchange)
                .with(QueueForgeQueues.RETRY_ROUTING_KEY);
    }

    // Dead-letter queue: terminal home for jobs that exhausted their retries.

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(QueueForgeQueues.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QueueForgeQueues.DLQ_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(QueueForgeQueues.DLQ_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTrustedPackages("com.queueforge.common.messaging");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
