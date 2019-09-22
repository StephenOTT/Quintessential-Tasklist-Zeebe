package com.github.stephenott.form.validator.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

public class ValidationRequestResultException extends RuntimeException {

    private ErrorType errorType;

    public enum ErrorType {
        UNEXPECTED_STATUS_CODE,
        HTTP_REQ_FAILURE
    }

    @JsonCreator
    public ValidationRequestResultException(@JsonProperty(value = "errorType", required = true) ErrorType errorType,
                                            @JsonProperty(value = "internalErrorMessage", required = true) String internalErrorMessage,
                                            @JsonProperty(value = "endUserMessage", required = true) String endUserMessage) {
        super(endUserMessage, new IllegalStateException(internalErrorMessage));
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public ValidationRequestResultException setErrorType(ErrorType errorType) {
        this.errorType = errorType;
        return this;
    }


}
