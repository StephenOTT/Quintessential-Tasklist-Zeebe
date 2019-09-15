package com.github.stephenott.form.validator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.stephenott.common.EventBusable;


public class ValidationRequest implements EventBusable {

    @JsonProperty(required = true)
    private ValidationSchemaObject schema;

    @JsonProperty(required = true)
    private ValidationSubmissionObject submission;

    public ValidationRequest() {
    }

    public ValidationSchemaObject getSchema() {
        return schema;
    }

    public ValidationRequest setSchema(ValidationSchemaObject schema) {
        this.schema = schema;
        return this;
    }

    public ValidationSubmissionObject getSubmission() {
        return submission;
    }

    public ValidationRequest setSubmission(ValidationSubmissionObject submission) {
        this.submission = submission;
        return this;
    }
}
