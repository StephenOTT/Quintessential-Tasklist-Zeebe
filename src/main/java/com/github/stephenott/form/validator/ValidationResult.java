package com.github.stephenott.form.validator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class ValidationResult {

    public interface Type {

        @JsonIgnore
        JsonObject toJsonObject();

        @JsonIgnore
        boolean submissionIsValid();
    }

    public static class Valid implements Type {

        @JsonProperty("processed_submission")
        private Object processedSubmission;

        public Valid() {
        }

        public Object getProcessedSubmission() {
            return processedSubmission;
        }

        public Valid setProcessedSubmission(Object processedSubmission) {
            this.processedSubmission = processedSubmission;
            return this;
        }

        @Override
        public JsonObject toJsonObject() {
            return JsonObject.mapFrom(this);
        }

        @Override
        public boolean submissionIsValid() {
            return true;
        }
    }


    public static class Fail implements Type {

        private boolean isJoi;

        private String name;

        private List<Object> details;

        @JsonProperty("_object")
        private Object object;

        @JsonProperty("_validated")
        private Object validated;

        public Fail() {
        }

        @JsonProperty("isJoi")
        public boolean isJoi() {
            return isJoi;
        }

        public Fail setJoi(boolean joi) {
            isJoi = joi;
            return this;
        }

        public String getName() {
            return name;
        }

        public Fail setName(String name) {
            this.name = name;
            return this;
        }

        public List<Object> getDetails() {
            return details;
        }

        public Fail setDetails(List<Object> details) {
            this.details = details;
            return this;
        }

        public Object getObject() {
            return object;
        }

        public Fail setObject(Object object) {
            this.object = object;
            return this;
        }

        public Object getValidated() {
            return validated;
        }

        public Fail setValidated(Object validated) {
            this.validated = validated;
            return this;
        }

        @Override
        public JsonObject toJsonObject() {
            return JsonObject.mapFrom(this);
        }

        @Override
        public boolean submissionIsValid() {
            return false;
        }
    }
}

