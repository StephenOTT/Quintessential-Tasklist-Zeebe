package com.github.stephenott.usertask;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

//@TODO Refactor to enable proper serialization of Instant for JsonObject
@BsonDiscriminator
public class UserTaskEntity {

    @BsonId
    private String taskId;

    private long olVersion = 1L;

    private Instant taskOriginalCapture = Instant.now();

    private State state = State.NEW;

    private String title;
    private String description;
    private int priority = 0;
    private String assignee;
    private Set<String> candidateGroups;
    private Set<String> candidateUsers;
    private Instant dueDate;
    private String formKey;
    private Instant newAt = Instant.now();
    private Instant assignedAt;
    private Instant delegatedAt;
    private Instant completedAt;
    private Map<String, Object> variablesSubmittedOnCompletion;
    private String zeebeSource;
    private Instant zeebeDeadline;
    private long zeebeJobKey;
    private String bpmnProcessId;
    private int bpmnProcessVersion;
    private Map<String, Object> zeebeVariables;

    private Map<String, Object> metadata;

    public UserTaskEntity() {
    }

    public enum State {
        NEW,
        ASSIGNED,
        UNASSIGNED,
        DELEGATED,
        COMPLETED
    }

    public String getTaskId() {
        return taskId;
    }

    public UserTaskEntity setTaskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    /**
     * Gets the optimistic locking version number
     * @return
     */
    public long getOlVersion() {
        return olVersion;
    }

    /**
     * Set the optimistic locking version number
     * @param olVersion
     * @return
     */
    public UserTaskEntity setOlVersion(long olVersion) {
        this.olVersion = olVersion;
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

    public State getState() {
        return state;
    }

    public UserTaskEntity setState(State state) {
        this.state = state;
        return this;
    }

    public Instant getNewAt() {
        return newAt;
    }

    public UserTaskEntity setNewAt(Instant newAt) {
        this.newAt = newAt;
        return this;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public UserTaskEntity setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
        return this;
    }

    public Instant getDelegatedAt() {
        return delegatedAt;
    }

    public UserTaskEntity setDelegatedAt(Instant delegatedAt) {
        this.delegatedAt = delegatedAt;
        return this;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public UserTaskEntity setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public Map<String, Object> getVariablesSubmittedOnCompletion() {
        return variablesSubmittedOnCompletion;
    }

    public UserTaskEntity setVariablesSubmittedOnCompletion(Map<String, Object> variablesSubmittedOnCompletion) {
        this.variablesSubmittedOnCompletion = variablesSubmittedOnCompletion;
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

    public int getBpmnProcessVersion() {
        return bpmnProcessVersion;
    }

    public UserTaskEntity setBpmnProcessVersion(int bpmnProcessVersion) {
        this.bpmnProcessVersion = bpmnProcessVersion;
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

    public Instant getTaskOriginalCapture() {
        return taskOriginalCapture;
    }

    public UserTaskEntity setTaskOriginalCapture(Instant taskOriginalCapture) {
        this.taskOriginalCapture = taskOriginalCapture;
        return this;
    }
}