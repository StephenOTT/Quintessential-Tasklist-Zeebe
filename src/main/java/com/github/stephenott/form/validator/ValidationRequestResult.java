package com.github.stephenott.form.validator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.stephenott.common.EventBusable;
import com.github.stephenott.form.validator.exception.ValidationRequestResultException;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public class ValidationRequestResult implements EventBusable {

    @JsonProperty(required = true)
    private Result result;

    private ValidResult validResultObject = null;
    private InvalidResult invalidResultObject = null;
    private ErrorResult errorResult = null;

    public enum Result {
        VALID,
        INVALID,
        ERROR
    }

    public ValidationRequestResult() {
    }

    public static ValidationRequestResult GenerateValidResult(ValidResult validResultObject){
        return new ValidationRequestResult()
                .setResult(Result.VALID)
                .setValidResultObject(validResultObject);
    }

    public static ValidationRequestResult GenerateInvalidResult(InvalidResult invalidResultObject){
        return new ValidationRequestResult()
                .setResult(Result.INVALID)
                .setInvalidResultObject(invalidResultObject);
    }

    public static ValidationRequestResult GenerateErrorResult(ErrorResult errorResult){
        return new ValidationRequestResult()
                .setResult(Result.ERROR)
                .setErrorResult(errorResult);
    }

    public Result getResult() {
        return result;
    }

    public ValidationRequestResult setResult(Result result) {
        this.result = result;
        return this;
    }

    public ValidResult getValidResultObject() {
        return validResultObject;
    }

    public ValidationRequestResult setValidResultObject(ValidResult validResultObject) {
        this.validResultObject = validResultObject;
        return this;
    }

    public InvalidResult getInvalidResultObject() {
        return invalidResultObject;
    }

    public ValidationRequestResult setInvalidResultObject(InvalidResult invalidResultObject) {
        this.invalidResultObject = invalidResultObject;
        return this;
    }

    public ErrorResult getErrorResult() {
        return errorResult;
    }

    public ValidationRequestResult setErrorResult(ErrorResult errorResult) {
        this.errorResult = errorResult;
        return this;
    }

    public static class ValidResult {

        @JsonProperty("processed_submission")
        private Map<String, Object> processedSubmission;

        public ValidResult() {
        }

        public Map<String, Object> getProcessedSubmission() {
            return processedSubmission;
        }

        public ValidResult setProcessedSubmission(Map<String, Object> processedSubmission) {
            this.processedSubmission = processedSubmission;
            return this;
        }
    }


    public static class InvalidResult{

        private boolean isJoi;

        private String name;

        private List<Map<String, Object>> details;

        @JsonProperty("_object")
        private Map<String, Object> object;

        @JsonProperty("_validated")
        private Map<String, Object> validated;

        public InvalidResult() {
        }

        public boolean isJoi() {
            return isJoi;
        }

        public InvalidResult setJoi(boolean joi) {
            isJoi = joi;
            return this;
        }

        public String getName() {
            return name;
        }

        public InvalidResult setName(String name) {
            this.name = name;
            return this;
        }

        public List<Map<String, Object>> getDetails() {
            return details;
        }

        public InvalidResult setDetails(List<Map<String, Object>> details) {
            this.details = details;
            return this;
        }

        public Map<String, Object> getObject() {
            return object;
        }

        public InvalidResult setObject(Map<String, Object> object) {
            this.object = object;
            return this;
        }

        public Map<String, Object> getValidated() {
            return validated;
        }

        public InvalidResult setValidated(Map<String, Object> validated) {
            this.validated = validated;
            return this;
        }
    }

    public static class ErrorResult {

        ValidationRequestResultException.ErrorType errorType;
        String internalErrorMessage;
        String endUserMessage;

        private ErrorResult() {
        }

        public ErrorResult(ValidationRequestResultException.ErrorType errorType, String internalErrorMessage, String endUserMessage) {
            this.errorType = errorType;
            this.internalErrorMessage = internalErrorMessage;
            this.endUserMessage = endUserMessage;
        }

        public ValidationRequestResultException.ErrorType getErrorType() {
            return errorType;
        }

        public ErrorResult setErrorType(ValidationRequestResultException.ErrorType errorType) {
            this.errorType = errorType;
            return this;
        }

        public String getInternalErrorMessage() {
            return internalErrorMessage;
        }

        public ErrorResult setInternalErrorMessage(String internalErrorMessage) {
            this.internalErrorMessage = internalErrorMessage;
            return this;
        }

        public String getEndUserMessage() {
            return endUserMessage;
        }

        public ErrorResult setEndUserMessage(String endUserMessage) {
            this.endUserMessage = endUserMessage;
            return this;
        }
    }
}

