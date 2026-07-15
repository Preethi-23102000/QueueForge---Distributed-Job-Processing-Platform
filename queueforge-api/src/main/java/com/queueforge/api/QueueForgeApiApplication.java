package com.queueforge.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.queueforge.common.entity")
@EnableJpaRepositories(basePackages = "com.queueforge.common.repository")
public class QueueForgeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueForgeApiApplication.class, args);
    }
}
