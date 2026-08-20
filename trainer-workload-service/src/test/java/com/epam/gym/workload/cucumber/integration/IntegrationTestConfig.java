package com.epam.gym.workload.cucumber.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class IntegrationTestConfig {

    public static final MongoDBContainer MONGO;
    public static final GenericContainer<?> ACTIVEMQ;

    static {
        System.setProperty("api.version", "1.40");
        System.setProperty("testcontainers.reuse.enable", "true");

        MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7"));
        ACTIVEMQ = new GenericContainer<>(DockerImageName.parse("apache/activemq-classic:5.18.3"))
                .withExposedPorts(61616)
                .waitingFor(Wait.forListeningPort());

        MONGO.start();
        ACTIVEMQ.start();
    }

    private IntegrationTestConfig() {
    }

    public static String activeMqBrokerUrl() {
        return "tcp://" + ACTIVEMQ.getHost() + ":" + ACTIVEMQ.getMappedPort(61616);
    }
}