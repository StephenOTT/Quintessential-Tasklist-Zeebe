package com.github.stephenott.usertask;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.stephenott.common.EventBusable;
import com.github.stephenott.usertask.entity.UserTaskEntity;

import java.time.Instant;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GetRequest implements EventBusable {

    private Optional<String> taskId = Optional.empty();

    private Optional<UserTaskEntity.State> state = Optional.empty();

    private Optional<String> title = Optional.empty();

    private Optional<String> assignee = Optional.empty();

    private Optional<Instant> dueDate = Optional.empty();

    private Optional<Long> zeebeJobKey = Optional.empty();

    private Optional<String> zeebeSource = Optional.empty();

    private Optional<String> bpmnProcessId = Optional.empty();

    public GetRequest() {
    }

    public Optional<String> getTaskId() {
        return taskId;
    }

    public GetRequest setTaskId(Optional<String> taskId) {
        this.taskId = taskId;
        return this;
    }

    public Optional<UserTaskEntity.State> getState() {
        return state;
    }

    public GetRequest setState(Optional<UserTaskEntity.State> state) {
        this.state = state;
        return this;
    }

    public Optional<String> getTitle() {
        return title;
    }

    public GetRequest setTitle(Optional<String> title) {
        this.title = title;
        return this;
    }

    public Optional<String> getAssignee() {
        return assignee;
    }

    public GetRequest setAssignee(Optional<String> assignee) {
        this.assignee = assignee;
        return this;
    }

    public Optional<Instant> getDueDate() {
        return dueDate;
    }

    public GetRequest setDueDate(Optional<Instant> dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public Optional<Long> getZeebeJobKey() {
        return zeebeJobKey;
    }

    public GetRequest setZeebeJobKey(Optional<Long> zeebeJobKey) {
        this.zeebeJobKey = zeebeJobKey;
        return this;
    }

    public Optional<String> getZeebeSource() {
        return zeebeSource;
    }

    public GetRequest setZeebeSource(Optional<String> zeebeSource) {
        this.zeebeSource = zeebeSource;
        return this;
    }

    public Optional<String> getBpmnProcessId() {
        return bpmnProcessId;
    }

    public GetRequest setBpmnProcessId(Optional<String> bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
        return this;
    }
}
