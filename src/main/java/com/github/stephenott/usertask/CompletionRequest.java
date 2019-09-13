package com.github.stephenott.usertask;

import java.util.Map;

public class CompletionRequest {

    private long zeebeJobKey;
    private String zeebeSource;
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
