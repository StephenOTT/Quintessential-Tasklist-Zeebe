package com.github.stephenott;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class ZeebeClientConfigurationProperties {

    private JsonObject clientConfig;

    private Broker broker;
    private Worker worker;
    private Message message;

    @JsonCreator
    public ZeebeClientConfigurationProperties(JsonObject clientConfig) {
        Objects.requireNonNull(clientConfig);

        this.clientConfig = clientConfig;

        JsonObject defaults = Optional.ofNullable(
                clientConfig.getJsonObject("default_config"))
                .orElseThrow(IllegalStateException::new);

        this.broker = Optional.ofNullable(
                defaults.getJsonObject("broker"))
                .orElseThrow(IllegalStateException::new).mapTo(Broker.class);

        this.worker = Optional.ofNullable(
                defaults.getJsonObject("worker").mapTo(Worker.class))
                .orElseThrow(IllegalStateException::new);

        this.message = Optional.ofNullable(
                defaults.getJsonObject("message").mapTo(Message.class))
                .orElseThrow(IllegalStateException::new);
    }

    public static class Broker {
        private String contactPoint;
        private Duration requestTimeout;

        public String getContactPoint() {
            return contactPoint;
        }

        public void setContactPoint(String contactPoint) {
            this.contactPoint = contactPoint;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    public static class Worker {
        private String name;
        private Duration timeout;
        private Integer maxJobsActive;
        private Duration pollInterval;
        private Integer threads;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Integer getMaxJobsActive() {
            return maxJobsActive;
        }

        public void setMaxJobsActive(Integer maxJobsActive) {
            this.maxJobsActive = maxJobsActive;
        }

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public Integer getThreads() {
            return threads;
        }

        public void setThreads(Integer threads) {
            this.threads = threads;
        }
    }

    public static class Message {
        private Duration timeToLive;

        public Duration getTimeToLive() {
            return timeToLive;
        }

        public void setTimeToLive(Duration timeToLive) {
            this.timeToLive = timeToLive;
        }
    }

    public JsonObject getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(JsonObject clientConfig) {
        this.clientConfig = clientConfig;
    }

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
