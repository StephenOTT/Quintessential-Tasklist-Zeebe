package com.github.stephenott.usertask;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.stephenott.common.EventBusable;

import java.util.Map;

public class CompletionRequest implements EventBusable {

    @JsonProperty(value = "job", required = true)
    private long zeebeJobKey;

    @JsonProperty(value = "source", required = true)
    private String zeebeSource;

    @JsonProperty("variables")
    private Map<String, Object> completionVariables;

    private boolean bypassFormSubmission = false;

    private Object formSubmission;


    public CompletionRequest() {
    }

    public long getZeebeJobKey() {
        return zeebeJobKey;
    }

    public CompletionRequest setZeebeJobKey(long zeebeJobKey) {
        this.zeebeJobKey = zeebeJobKey;
        return this;
    }

    public String getZeebeSource() {
        return zeebeSource;
    }

    public CompletionRequest setZeebeSource(String zeebeSource) {
        this.zeebeSource = zeebeSource;
        return this;
    }

    public Map<String, Object> getCompletionVariables() {
        return completionVariables;
    }

    public CompletionRequest setCompletionVariables(Map<String, Object> completionVariables) {
        this.completionVariables = completionVariables;
        return this;
    }

    public boolean isBypassFormSubmission() {
        return bypassFormSubmission;
    }

    public CompletionRequest setBypassFormSubmission(boolean bypassFormSubmission) {
        this.bypassFormSubmission = bypassFormSubmission;
        return this;
    }

    public Object getFormSubmission() {
        return formSubmission;
    }

    public CompletionRequest setFormSubmission(Object formSubmission) {
        this.formSubmission = formSubmission;
        return this;
    }
}
