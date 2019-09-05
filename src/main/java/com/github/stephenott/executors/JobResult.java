package com.github.stephenott.executors;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JobResult {

    private Result result;
    private long jobKey;
    private Map<String, Object> variables = new HashMap<>();
    private int retries;
    private String errorMessage = "An error occurred";

    public enum Result {
        COMPLETE,
        FAIL
    }

    private JobResult() {
    }

    public JobResult(long jobKey, Result result, Map<String, Object> variables, int retries, String errorMessage) {
        Objects.requireNonNull(result);

        this.result = result;
        this.jobKey = jobKey;
        this.variables = variables;
        this.retries = retries;
        this.errorMessage = errorMessage;
    }

    public JobResult(long jobKey, Result result, int retries) {
        this(jobKey, result, null, retries, null);
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public long getJobKey() {
        return jobKey;
    }

    public void setJobKey(long jobKey) {
        this.jobKey = jobKey;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public JsonObject toJsonObject(){
        return JsonObject.mapFrom(this);
    }

    public static JobResult fromJsonObject(JsonObject doneJob){
        return doneJob.mapTo(JobResult.class);
    }

}