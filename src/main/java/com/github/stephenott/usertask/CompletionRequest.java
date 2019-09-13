package com.github.stephenott.usertask;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.stephenott.common.EventBusable;

import java.util.Map;

public class CompletionRequest implements EventBusable {

    @JsonProperty("job")
    private long zeebeJobKey;

    @JsonProperty("source")
    private String zeebeSource;

    @JsonProperty("variables")
    private Map<String, Object> completionVariables;


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
}
