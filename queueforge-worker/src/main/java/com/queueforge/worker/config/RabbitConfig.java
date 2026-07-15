package com.queueforge.worker.config;

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
 * Declares the same RabbitMQ topology as the API so the worker can consume jobs
 * regardless of start order, and configures JSON message conversion.
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

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTrustedPackages("com.queueforge.common.messaging");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
