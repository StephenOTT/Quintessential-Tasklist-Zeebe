package com.github.stephenott;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.zeebe.client.api.response.ActivatedJob;

import java.io.IOException;
import java.util.Map;

public class ActivatedJobDto implements ActivatedJob {

    private long key;
    private String type;
    private Map<String, String> customHeaders;
    private long workflowInstanceKey;
    private String bpmnProcessId;
    private int workflowDefinitionVersion;
    private long workflowKey;
    private String elementId;
    private long elementInstanceKey;
    private String worker;
    private int retries;
    private long deadline;
    private String variables;

    public ActivatedJobDto(){}

    public ActivatedJobDto(ActivatedJob activatedJob){
        this.key = activatedJob.getKey();
        this.type = activatedJob.getType();
        this.customHeaders = activatedJob.getCustomHeaders();
        this.workflowInstanceKey = activatedJob.getWorkflowInstanceKey();
        this.bpmnProcessId = activatedJob.getBpmnProcessId();
        this.workflowDefinitionVersion = activatedJob.getWorkflowDefinitionVersion();
        this.workflowKey = activatedJob.getWorkflowKey();
        this.elementId = activatedJob.getElementId();
        this.elementInstanceKey = activatedJob.getElementInstanceKey();
        this.worker = activatedJob.getWorker();
        this.retries = activatedJob.getRetries();
        this.deadline = activatedJob.getDeadline();
        this.variables = activatedJob.getVariables();
    }

    @Override
    public long getKey() {
        return this.key;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public long getWorkflowInstanceKey() {
        return this.workflowInstanceKey;
    }

    @Override
    public String getBpmnProcessId() {
        return this.bpmnProcessId;
    }

    @Override
    public int getWorkflowDefinitionVersion() {
        return this.workflowDefinitionVersion;
    }

    @Override
    public long getWorkflowKey() {
        return this.workflowKey;
    }

    @Override
    public String getElementId() {
        return this.elementId;
    }

    @Override
    public long getElementInstanceKey() {
        return this.elementInstanceKey;
    }

    @Override
    public Map<String, String> getCustomHeaders() {
        return this.customHeaders;
    }

    @Override
    public String getWorker() {
        return this.worker;
    }

    @Override
    public int getRetries() {
        return this.retries;
    }

    @Override
    public long getDeadline() {
        return this.deadline;
    }

    @Override
    public String getVariables() {
        return this.variables;
    }

    @Override
    public Map<String, Object> getVariablesAsMap() {
        try {
            return Json.mapper.readValue(this.variables, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Unable to convert variables to a Map", e);
        }
    }

    @Override
    public <T> T getVariablesAsType(Class<T> variableType) {
        try {
            return Json.mapper.readValue(this.variables, variableType);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to convert variables to type " + variableType.getName(), e);
        }
    }

    @Override
    public String toJson() {
        try {
           return Json.mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to convert to Json String", e);
        }
    }

    public JsonObject toJsonObject() {
        return JsonObject.mapFrom(this);
    }


    public void setKey(long key) {
        this.key = key;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }

    public void setWorkflowInstanceKey(long workflowInstanceKey) {
        this.workflowInstanceKey = workflowInstanceKey;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public void setWorkflowDefinitionVersion(int workflowDefinitionVersion) {
        this.workflowDefinitionVersion = workflowDefinitionVersion;
    }

    public void setWorkflowKey(long workflowKey) {
        this.workflowKey = workflowKey;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public void setElementInstanceKey(long elementInstanceKey) {
        this.elementInstanceKey = elementInstanceKey;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }
}
