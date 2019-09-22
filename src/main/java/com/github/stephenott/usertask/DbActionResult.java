package com.github.stephenott.usertask;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.stephenott.common.EventBusable;
import com.github.stephenott.common.EventBusableReplyException;

import java.util.List;

public class DbActionResult implements EventBusable {

    private ActionResult result;
    private List<?> resultObject;
    private Error error;

    public DbActionResult() {
    }

    public DbActionResult(ActionResult result, List<?> resultObject) {
        this.result = result;
        this.resultObject = resultObject;
    }

    public static DbActionResult FailedAction(Throwable throwable) {
        return new DbActionResult()
                .setResult(ActionResult.FAIL)
                .setError(new Error()
                        .setExceptionClass(throwable.getClass().getCanonicalName())
                        .setLocalizedMessage(throwable.getLocalizedMessage())
                        .setMessage(throwable.getMessage()));

    }

    public static DbActionResult SuccessfulAction(List<?> resultObject) {
        return new DbActionResult()
                .setResult(ActionResult.SUCCESS)
                .setResultObject(resultObject);
    }

    public ActionResult getResult() {
        return result;
    }

    public DbActionResult setResult(ActionResult result) {
        this.result = result;
        return this;
    }

    public List<?> getResultObject() {
        return resultObject;
    }

    public DbActionResult setResultObject(List<?> resultObject) {
        this.resultObject = resultObject;
        return this;
    }

    public Error getError() {
        return error;
    }

    public DbActionResult setError(Error error) {
        this.error = error;
        return this;
    }

    public enum ActionResult {
        SUCCESS,
        FAIL
    }




    public static class FailedDbActionException extends EventBusableReplyException {

        public enum FailureType {
            CANT_CREATE,
            CANT_READ,
            CANT_FIND,
            CANT_UPDATE,
            CANT_DELETE,
            CANT_CONTACT_DB,
            FILTER_PARSE_ERROR,
            CANT_COMPLETE_COMMAND
        }

        @JsonCreator
        public FailedDbActionException(@JsonProperty(value = "failureType", required = true) FailureType failureType,
                                       @JsonProperty(value = "internalErrorMessage", required = true) String internalErrorMessage,
                                       @JsonProperty(value = "userErrorMessage", required = true) String userErrorMessage) {
            super(failureType, internalErrorMessage, userErrorMessage);
        }
    }

    public static class Error {
        private String message;
        private String localizedMessage;
        private String exceptionClass;

        public Error() {
        }

        public String getMessage() {
            return message;
        }

        public Error setMessage(String message) {
            this.message = message;
            return this;
        }

        public String getLocalizedMessage() {
            return localizedMessage;
        }

        public Error setLocalizedMessage(String localizedMessage) {
            this.localizedMessage = localizedMessage;
            return this;
        }

        public String getExceptionClass() {
            return exceptionClass;
        }

        public Error setExceptionClass(String exceptionClass) {
            this.exceptionClass = exceptionClass;
            return this;
        }
    }
}
