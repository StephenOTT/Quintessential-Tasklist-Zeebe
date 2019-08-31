package com.github.stephenott;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.util.List;

public class ApplicationConfiguration {

    private ZeebeConfiguration zeebe;
    private List<ExecutorConfiguration> executors;

    public ApplicationConfiguration() {
    }

    public ZeebeConfiguration getZeebe() {
        return zeebe;
    }

    public void setZeebe(ZeebeConfiguration zeebe) {
        this.zeebe = zeebe;
    }

    public List<ExecutorConfiguration> getExecutors() {
        return executors;
    }

    public void setExecutors(List<ExecutorConfiguration> executors) {
        this.executors = executors;
    }



    public static class ZeebeConfiguration{
        private List<ZeebeClientConfiguration> clients;

        public ZeebeConfiguration() {
        }

        public List<ZeebeClientConfiguration> getClients() {
            return clients;
        }

        public void setClients(List<ZeebeClientConfiguration> clients) {
            this.clients = clients;
        }
    }


    public static class ZeebeClientConfiguration{
        private String name;
        private String brokerContactPoint;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Duration requestTimeout;

        private List<ZeebeWorkers> workers;

        public ZeebeClientConfiguration() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBrokerContactPoint() {
            return brokerContactPoint;
        }

        public void setBrokerContactPoint(String brokerContactPoint) {
            this.brokerContactPoint = brokerContactPoint;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }

        public List<ZeebeWorkers> getWorkers() {
            return workers;
        }

        public void setWorkers(List<ZeebeWorkers> workers) {
            this.workers = workers;
        }
    }


    public static class ZeebeWorkers {
        private String name;
        private List<String> jobTypes;

        public ZeebeWorkers() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getJobTypes() {
            return jobTypes;
        }

        public void setJobTypes(List<String> jobTypes) {
            this.jobTypes = jobTypes;
        }
    }


    public static class ExecutorConfiguration {
        private String name;
        private String address;
        private String execute;
        private int instances = 1;

        public ExecutorConfiguration() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getExecute() {
            return execute;
        }

        public void setExecute(String execute) {
            this.execute = execute;
        }

        public int getInstances() {
            return instances;
        }

        public void setInstances(int instances) {
            this.instances = instances;
        }
    }


}
