package com.github.stephenott.usertask;

import com.github.stephenott.executors.JobResult;
import com.github.stephenott.form.validator.ValidationRequestResult;
import com.github.stephenott.usertask.entity.FormSchemaEntity;
import com.github.stephenott.usertask.entity.UserTaskEntity;

public class SubmitTaskComposeDto {

    UserTaskEntity userTaskEntity;
    FormSchemaEntity formSchemaEntity;
    ValidationRequestResult validationRequestResult;
    DbActionResult dbActionResult;
    JobResult jobResult;
    boolean validForm;

    public SubmitTaskComposeDto() {
    }

    public UserTaskEntity getUserTaskEntity() {
        return userTaskEntity;
    }

    public SubmitTaskComposeDto setUserTaskEntity(UserTaskEntity userTaskEntity) {
        this.userTaskEntity = userTaskEntity;
        return this;
    }

    public FormSchemaEntity getFormSchemaEntity() {
        return formSchemaEntity;
    }

    public SubmitTaskComposeDto setFormSchemaEntity(FormSchemaEntity formSchemaEntity) {
        this.formSchemaEntity = formSchemaEntity;
        return this;
    }

    public ValidationRequestResult getValidationRequestResult() {
        return validationRequestResult;
    }

    public SubmitTaskComposeDto setValidationRequestResult(ValidationRequestResult validationRequestResult) {
        this.validationRequestResult = validationRequestResult;
        return this;
    }

    public DbActionResult getDbActionResult() {
        return dbActionResult;
    }

    public SubmitTaskComposeDto setDbActionResult(DbActionResult dbActionResult) {
        this.dbActionResult = dbActionResult;
        return this;
    }

    public JobResult getJobResult() {
        return jobResult;
    }

    public SubmitTaskComposeDto setJobResult(JobResult jobResult) {
        this.jobResult = jobResult;
        return this;
    }

    public boolean isValidForm() {
        return validForm;
    }

    public SubmitTaskComposeDto setValidForm(boolean validForm) {
        this.validForm = validForm;
        return this;
    }
}
