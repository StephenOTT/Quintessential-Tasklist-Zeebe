package com.github.stephenott.executors.usertask;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.impl.codec.json.JsonObjectCodec;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class UserTaskEntity {

    private String taskId;
    private String title;
    private String description;
    private int priority = 0;
    private String assignee;
    private Set<String> candidateGroups;
    private Set<String> candidateUsers;
    private Instant dueDate;
    private String formKey;
    private String zeebeSource;
    private Instant zeebeDeadline;
    private long zeebeJobKey;
    private String bpmnProcessId;
    private Map<String, Object> metadata;
    private Map<String, Object> zeebeVariables;

    public UserTaskEntity() {
    }

    public String getTaskId() {
        return taskId;
    }

    public UserTaskEntity setTaskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public UserTaskEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public UserTaskEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public UserTaskEntity setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public String getAssignee() {
        return assignee;
    }

    public UserTaskEntity setAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    public Set<String> getCandidateGroups() {
        return candidateGroups;
    }

    public UserTaskEntity setCandidateGroups(Set<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
        return this;
    }

    public Set<String> getCandidateUsers() {
        return candidateUsers;
    }

    public UserTaskEntity setCandidateUsers(Set<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
        return this;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public UserTaskEntity setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public String getFormKey() {
        return formKey;
    }

    public UserTaskEntity setFormKey(String formKey) {
        this.formKey = formKey;
        return this;
    }

    public String getZeebeSource() {
        return zeebeSource;
    }

    public UserTaskEntity setZeebeSource(String zeebeSource) {
        this.zeebeSource = zeebeSource;
        return this;
    }

    public Instant getZeebeDeadline() {
        return zeebeDeadline;
    }

    public UserTaskEntity setZeebeDeadline(Instant zeebeDeadline) {
        this.zeebeDeadline = zeebeDeadline;
        return this;
    }

    public long getZeebeJobKey() {
        return zeebeJobKey;
    }

    public UserTaskEntity setZeebeJobKey(long zeebeJobKey) {
        this.zeebeJobKey = zeebeJobKey;
        return this;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public UserTaskEntity setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
        return this;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public UserTaskEntity setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Map<String, Object> getZeebeVariables() {
        return zeebeVariables;
    }

    public UserTaskEntity setZeebeVariables(Map<String, Object> zeebeVariables) {
        this.zeebeVariables = zeebeVariables;
        return this;
    }

    public JsonObject toMongoJson() {
        return JsonObject.mapFrom(this)
                .put("zeebeDeadline", new JsonObject().put(JsonObjectCodec.DATE_FIELD, this.getZeebeDeadline()));
    }
}