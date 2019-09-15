package com.github.stephenott.form.validator;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ValidationSubmissionObject {

    @JsonProperty(required = true)
    private Map<String, Object> data;

    private Map<String, Object> metadata;

    public ValidationSubmissionObject() {
    }

    public Map<String, Object> getData() {
        return data;
    }

    public ValidationSubmissionObject setData(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public ValidationSubmissionObject setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }
}
