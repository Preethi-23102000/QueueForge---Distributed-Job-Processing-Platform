package com.queueforge.worker;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for worker integration tests. Boots the worker against a real
 * Postgres and RabbitMQ via Testcontainers. The worker does not own the schema
 * (the API runs Flyway), so Hibernate generates it from the entities for tests.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
public abstract class AbstractWorkerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"));

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.3-management"));
}
