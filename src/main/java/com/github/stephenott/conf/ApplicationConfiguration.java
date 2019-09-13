package com.github.stephenott.conf;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.stephenott.executors.usertask.UserTaskConfiguration;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class ApplicationConfiguration {

    private ZeebeConfiguration zeebe;
    private List<ExecutorConfiguration> executors;
    private List<UserTaskExecutorConfiguration> userTaskExecutors;
    private ManagementHttpConfiguration managementServer;
    private FormValidationServerConfiguration formValidatorServer;

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

    public List<UserTaskExecutorConfiguration> getUserTaskExecutors() {
        return userTaskExecutors;
    }

    public void setUserTaskExecutors(List<UserTaskExecutorConfiguration> userTaskExecutors) {
        this.userTaskExecutors = userTaskExecutors;
    }

    public ManagementHttpConfiguration getManagementServer() {
        return managementServer;
    }

    public void setManagementServer(ManagementHttpConfiguration managementServer) {
        this.managementServer = managementServer;
    }

    public FormValidationServerConfiguration getFormValidatorServer() {
        return formValidatorServer;
    }

    public ApplicationConfiguration setFormValidatorServer(FormValidationServerConfiguration formValidatorServer) {
        this.formValidatorServer = formValidatorServer;
        return this;
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
        private String brokerContactPoint = "localhost:25600";

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Duration requestTimeout = Duration.ofSeconds(10);

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

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Duration timeout = Duration.ofSeconds(10);

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

        /**
         * The Timeout of how long the Job is locked to the worker / subscription
         * @return
         */
        public Duration getTimeout() {
            return timeout;
        }

        public ZeebeWorkers setTimeout(Duration timeout) {
            this.timeout = timeout;
            return this;
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

    public static class ManagementHttpConfiguration {
        private boolean enabled = true;
        private String apiRoot = UUID.randomUUID().toString();
        private ZeebeClientConfiguration zeebeClient;
        private String fileUploadPath = "./tmp/uploads";
        private int instances = 1;
        private int port = 8080;
        private String corsRegex;

        public ManagementHttpConfiguration() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiRoot() {
            return apiRoot;
        }

        public void setApiRoot(String apiRoot) {
            this.apiRoot = apiRoot;
        }

        public ZeebeClientConfiguration getZeebeClient() {
            return zeebeClient;
        }

        public void setZeebeClient(ZeebeClientConfiguration zeebeClient) {
            this.zeebeClient = zeebeClient;
        }

        public String getFileUploadPath() {
            return fileUploadPath;
        }

        public void setFileUploadPath(String fileUploadPath) {
            this.fileUploadPath = fileUploadPath;
        }

        public int getInstances() {
            return instances;
        }

        public void setInstances(int instances) {
            this.instances = instances;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getCorsRegex() {
            return corsRegex;
        }

        public void setCorsRegex(String corsRegex) {
            this.corsRegex = corsRegex;
        }
    }

    public static class UserTaskExecutorConfiguration {
        private String name;
        private String address;
        private UserTaskConfiguration defaults;
        private UserTaskConfiguration overrides;
        private int instances;

        public UserTaskExecutorConfiguration() {
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

        public UserTaskConfiguration getDefaults() {
            return defaults;
        }

        public void setDefaults(UserTaskConfiguration defaults) {
            this.defaults = defaults;
        }

        public UserTaskConfiguration getOverrides() {
            return overrides;
        }

        public void setOverrides(UserTaskConfiguration overrides) {
            this.overrides = overrides;
        }

        public int getInstances() {
            return instances;
        }

        public void setInstances(int instances) {
            this.instances = instances;
        }
    }

    /**
     * The Form Validation Server (Verticle)
     */
    public static class FormValidationServerConfiguration {
        private boolean enabled = true;
        private int instances = 1;
        private int port = 8082;
        private String corsRegex;
        private String address = "form-validation";
        private FormValidatorServiceConfiguration formValidatorService;

        public FormValidationServerConfiguration() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public FormValidationServerConfiguration setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public int getInstances() {
            return instances;
        }

        public FormValidationServerConfiguration setInstances(int instances) {
            this.instances = instances;
            return this;
        }

        public int getPort() {
            return port;
        }

        public FormValidationServerConfiguration setPort(int port) {
            this.port = port;
            return this;
        }

        public String getCorsRegex() {
            return corsRegex;
        }

        public FormValidationServerConfiguration setCorsRegex(String corsRegex) {
            this.corsRegex = corsRegex;
            return this;
        }

        public String getAddress() {
            return address;
        }

        public FormValidationServerConfiguration setAddress(String address) {
            this.address = address;
            return this;
        }

        public FormValidatorServiceConfiguration getFormValidatorService() {
            return formValidatorService;
        }

        public FormValidationServerConfiguration setFormValidatorService(FormValidatorServiceConfiguration formValidatorService) {
            this.formValidatorService = formValidatorService;
            return this;
        }
    }

    /**
     * The Form Validator Service
     * (The external service that is communicated over HTTP that performs
     * the actual validation against the supplied schema)
     */
    public static class FormValidatorServiceConfiguration {
        private String host = "localhost";
        private int port = 8083;
        private String validateUri = "/validate";
        private long requestTimeout = 5000;

        public FormValidatorServiceConfiguration() {
        }

        public String getHost() {
            return host;
        }

        public FormValidatorServiceConfiguration setHost(String host) {
            this.host = host;
            return this;
        }

        public int getPort() {
            return port;
        }

        public FormValidatorServiceConfiguration setPort(int port) {
            this.port = port;
            return this;
        }

        public String getValidateUri() {
            return validateUri;
        }

        public FormValidatorServiceConfiguration setValidateUri(String validateUri) {
            this.validateUri = validateUri;
            return this;
        }

        //@TODO Refactor this to support the java8 Duration class
        public long getRequestTimeout() {
            return requestTimeout;
        }

        public FormValidatorServiceConfiguration setRequestTimeout(long requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }
    }

}