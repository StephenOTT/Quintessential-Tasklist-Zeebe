package com.github.stephenott.form.validator;

public class ValidationServiceRequest {

    private ValidationSchemaObject schema;
    private ValidationSubmissionObject submission;

    public ValidationServiceRequest() {
    }

    public ValidationServiceRequest(ValidationRequest validationRequest) {
        this.schema = validationRequest.getSchema();
        this.submission = validationRequest.getSubmission();
    }


    public ValidationSchemaObject getSchema() {
        return schema;
    }

    public ValidationServiceRequest setSchema(ValidationSchemaObject schema) {
        this.schema = schema;
        return this;
    }

    public ValidationSubmissionObject getSubmission() {
        return submission;
    }

    public ValidationServiceRequest setSubmission(ValidationSubmissionObject submission) {
        this.submission = submission;
        return this;
    }
}
