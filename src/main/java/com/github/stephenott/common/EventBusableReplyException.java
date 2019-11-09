package com.github.stephenott.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.stephenott.usertask.FailedDbActionException;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;

@JsonAutoDetect(fieldVisibility = Visibility.NONE,
        getterVisibility = Visibility.NONE,
        setterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE)
public class EventBusableReplyException extends ReplyException implements EventBusable {

    @JsonProperty()
    private Enum<?> failureType;

    @JsonProperty()
    private String internalErrorMessage;

    @JsonProperty()
    private String userErrorMessage;
//
//    public EventBusableReplyException(FailedDbActionException.FailureType failureType, Throwable internalError, String UserErrorMessage){
//        super(ReplyFailure.RECIPIENT_FAILURE, UserErrorMessage);
//        this.internalErrorMessage
//    }

    public EventBusableReplyException(Enum<?> failureType, String internalErrorMessage, String userErrorMessage) {
        super(ReplyFailure.RECIPIENT_FAILURE, userErrorMessage);
        this.internalErrorMessage = internalErrorMessage;
        this.userErrorMessage = userErrorMessage;
        this.failureType = failureType;
    }

    public String getInternalErrorMessage() {
        return internalErrorMessage;
    }

    public EventBusableReplyException setInternalErrorMessage(String internalErrorMessage) {
        this.internalErrorMessage = internalErrorMessage;
        return this;
    }

    public String getUserErrorMessage() {
        return userErrorMessage;
    }

    public EventBusableReplyException setUserErrorMessage(String userErrorMessage) {
        this.userErrorMessage = userErrorMessage;
        return this;
    }

    public Enum<?> getFailureType() {
        return failureType;
    }

    public EventBusableReplyException setFailureType(Enum<?> failureType) {
        this.failureType = failureType;
        return this;
    }
}