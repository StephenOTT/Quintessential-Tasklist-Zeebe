package com.github.stephenott.form.validator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.stephenott.common.EventBusable;

import java.util.List;
import java.util.Map;

public class ValidationRequestResult implements EventBusable {

    @JsonProperty(required = true)
    private Result result;

    private ValidResult validResultObject = null;
    private InvalidResult invalidResultObject = null;

    public enum Result {
        VALID,
        INVALID
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
}

