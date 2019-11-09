package com.github.stephenott.usertask;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.stephenott.common.EventBusableReplyException;

public class FailedDbActionException extends EventBusableReplyException {

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
