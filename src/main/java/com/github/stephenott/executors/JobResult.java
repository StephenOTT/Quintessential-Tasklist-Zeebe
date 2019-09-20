package com.github.stephenott.executors;

import com.github.stephenott.common.EventBusable;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JobResult implements EventBusable {

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

    /**
     * Will set retries to 0.
     * @param jobKey
     * @param result
     */
    public JobResult(long jobKey, Result result) {
        setJobKey(jobKey);
        setResult(result);
    }

    public Result getResult() {
        return result;
    }

    public JobResult setResult(Result result) {
        this.result = result;
        return this;
    }

    public long getJobKey() {
        return jobKey;
    }

    public JobResult setJobKey(long jobKey) {
        this.jobKey = jobKey;
        return this;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public JobResult setVariables(Map<String, Object> variables) {
        this.variables = variables;
        return this;
    }

    public int getRetries() {
        return retries;
    }

    public JobResult setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JobResult setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    //    public JsonObject toJsonObject(){
//        return JsonObject.mapFrom(this);
//    }
//
//    public static JobResult fromJsonObject(JsonObject doneJob){
//        return doneJob.mapTo(JobResult.class);
//    }

}