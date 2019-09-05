package com.github.stephenott.form.validator;

import io.vertx.core.json.JsonObject;

public class ValidationRequest {

    private Object schema;
    private Object submission;

    public ValidationRequest() {
    }

    public JsonObject getSchema() {
        return JsonObject.mapFrom(schema);
    }

    public ValidationRequest setSchema(Object schema) {
        this.schema = schema;
        return this;
    }

    public JsonObject getSubmission() {
        return JsonObject.mapFrom(submission);
    }

    public ValidationRequest setSubmission(Object submission) {
        this.submission = submission;
        return this;
    }
}
