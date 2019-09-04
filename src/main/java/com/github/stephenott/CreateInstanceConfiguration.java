package com.github.stephenott;

import java.util.HashMap;
import java.util.Map;

public class CreateInstanceConfiguration {

    private Long workflowKey;
    private String bpmnProcessId;
    private Integer bpmnProcessVersion;
    private Map<String, Object> variables = new HashMap<>();

    public CreateInstanceConfiguration() {
    }

    public Long getWorkflowKey() {
        return workflowKey;
    }

    public void setWorkflowKey(Long workflowKey) {
        this.workflowKey = workflowKey;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public Integer getBpmnProcessVersion() {
        return bpmnProcessVersion;
    }

    public void setBpmnProcessVersion(Integer bpmnProcessVersion) {
        if (bpmnProcessVersion < 1) {
            throw new IllegalArgumentException("version cannot be less than 1");
        } else {
            this.bpmnProcessVersion = bpmnProcessVersion;
        }
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
