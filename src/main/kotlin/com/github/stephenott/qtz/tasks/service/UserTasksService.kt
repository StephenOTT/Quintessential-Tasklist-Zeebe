package com.github.stephenott.qtz.tasks.service

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.stephenott.qtz.forms.domain.FormSchema
import com.github.stephenott.qtz.forms.domain.FormEntity
import com.github.stephenott.qtz.forms.repository.FormsRepository
import com.github.stephenott.qtz.forms.repository.FormSchemasRepository
import com.github.stephenott.qtz.forms.validator.client.FormValidatorServiceClient
import com.github.stephenott.qtz.forms.validator.client.ValidationResponseInvalid
import com.github.stephenott.qtz.forms.validator.domain.FormSubmission
import com.github.stephenott.qtz.forms.validator.domain.FormSubmissionData
import com.github.stephenott.qtz.forms.validator.exception.FormValidationException
import com.github.stephenott.qtz.tasks.domain.UserTaskEntity
import com.github.stephenott.qtz.tasks.domain.UserTaskMetadata
import com.github.stephenott.qtz.tasks.domain.UserTaskState
import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.tasks.repository.UserTasksRepository
import com.github.stephenott.qtz.zeebe.management.repository.ZeebeManagementRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Sort
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.reactivex.Completable
import io.reactivex.Single
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTasksService() {

    @Inject
    lateinit var userTasksRepository: UserTasksRepository

    @Inject
    lateinit var formSchemaRepository: FormSchemasRepository

    @Inject
    lateinit var formRepository: FormsRepository

    @Inject
    lateinit var zeebeManagementRepository: ZeebeManagementRepository

    @Inject
    lateinit var formValidatorService: FormValidatorServiceClient

    fun createCustomTask(taskId: UUID, task: CreateCustomTaskRequest): Single<UserTaskEntity> {
        return Single.just(task).map {
            UserTaskEntity(title = it.title,
                    description = it.description,
                    priority = it.priority,
                    assignee = it.assignee,
                    candidateGroups = it.candidateGroups,
                    candidateUsers = it.candidateUsers,
                    dueDate = it.dueDate,
                    formKey = it.formKey,
                    metadata = it.metadata)

        }.flatMap {
            userTasksRepository.update(it).onErrorResumeNext { e ->
                Single.error(e) //@TODO add proper error handling
            }
        }
    }

    fun updateTask(entity: UserTaskEntity): Single<UserTaskEntity> {
        requireNotNull(entity.taskId, lazyMessage = { "taskId cannot be null when updating a User Task." })
        return userTasksRepository.update(entity)
    }

    fun deleteTask(taskId: UUID): Completable {
        return userTasksRepository.deleteById(taskId)
    }

    /**
     * An Administrative endpoint that lets you submit the data directly into the UserTask DB and Zeebe without any Form Validation
     */
    fun completeTask(taskId: UUID, variables: ZeebeVariables): Single<UserTaskEntity> {
        return userTasksRepository.findById(taskId)
                .map { task ->
                    require(task.state != UserTaskState.COMPLETED, lazyMessage = { "User Task is already completed." })
                    task.completeVariables = variables
                    task.state = UserTaskState.COMPLETED
                    task.completedAt = Instant.now()
                    task
                    //@TODO review if this should be sent into another .map{} :
                }.flatMapSingle { task ->
                    reportCompletionToZeebe(task.zeebeJobKey!!, variables).toSingleDefault(task)
                }.flatMap { task ->
                    userTasksRepository.update(task)
                            .onErrorResumeNext {
                                Single.error(it) //@TODO add better error handling
                            }
                }
    }

    class UserTaskNotFoundException(val taskId: UUID) : RuntimeException("Provided Task ID $taskId could not be found") {}
    class FormKeyNotFoundException(val formKey: String) : RuntimeException("Provided formKey $formKey could not be found") {}
    class NoSchemasFoundForFormKeyException(val formKey: String) : RuntimeException("Provided formKey $formKey could not be found") {}
    class UnableToCompleteZeebeJobException(val taskId: UUID, zeebeJobKey: Long) : RuntimeException("Unable to complete Zeebe job $zeebeJobKey for task $taskId") {}
    class UnableToUpdateUserTaskWithCompletionException(val taskId: UUID) : RuntimeException("Unable to update user task $taskId to completed state.")

    /**
     * Full flow of UserTask and Form Validation and Zebee Completion.
     * The typical function used for submiting completed User Tasks.
     */
    fun submitTask(taskId: UUID, submissionVariables: FormSubmissionData): Single<UserTaskEntity> {
        return userTasksRepository.findById(taskId)
                .switchIfEmpty(Single.error(UserTaskNotFoundException(taskId)))
                .map { task ->
                    //Get User Task Entity and make some updates
                    require(task.state != UserTaskState.COMPLETED, lazyMessage = { "User Task is already completed." })

                    task.state = UserTaskState.COMPLETED
                    task.completedAt = Instant.now()

                    SubmitTaskRequest(task).apply {
                        formSubmissionData = submissionVariables
                    }

                }.flatMap { submitTaskRequest ->
                    // Get the Form Schema for the task
                    getMostRecentTaskForm(submitTaskRequest.userTaskEntity.taskId!!)
                            .map { schema ->
                                submitTaskRequest.apply {
                                    formSchema = schema
                                }
                            }
                }.flatMap { submitTaskRequest ->
                    // Validate the submission against the Schema
                    formValidatorService.validate(Single.just(FormSubmission(submitTaskRequest.formSchema!!, submitTaskRequest.formSubmissionData!!)))
                            .map { validatorResponse ->
                                submitTaskRequest.apply {
                                    val processedSubmission = validatorResponse.body()!!.processed_submission
                                    userTaskEntity.completeVariables = ZeebeVariables(variables = mapOf(
                                            Pair("${userTaskEntity.zeebeElementId!!}_task_submission", mapOf(
                                                    Pair("submission", processedSubmission),
                                                    Pair("formKey", userTaskEntity.formKey!!)))))

                                    validFormResponse = processedSubmission
                                }
                            }.onErrorResumeNext {
                                // @TODO Can eventually be replaced once micronaut-core fixes a issue where the response body is not passed to @Error handler when it catches the HttpClientResponseException
                                if (it is HttpClientResponseException) {
                                    val body = it.response.getBody(ValidationResponseInvalid::class.java)
                                    if (body.isPresent) {
                                        Single.error(FormValidationException(body.get()))
                                    } else {
                                        Single.error(IllegalStateException("Invalid Response Received", it))
                                    }
                                } else {
                                    Single.error(IllegalStateException("Unexpected Error received from Form Validation request.", it))
                                }
                            }
                }.flatMap { submitTaskRequest ->
                    //Report Completion to Zeebe
                    reportCompletionToZeebe(submitTaskRequest.userTaskEntity.zeebeJobKey!!, submitTaskRequest.userTaskEntity.completeVariables!!)
                            .onErrorResumeNext {
                                Completable.error(UnableToCompleteZeebeJobException(submitTaskRequest.userTaskEntity.taskId!!, submitTaskRequest.userTaskEntity.zeebeJobKey!!))
                            }.toSingleDefault(submitTaskRequest)
                            .doOnSuccess {
                                println("User Task ${submitTaskRequest.userTaskEntity.taskId!!} has been reported to Zeebe as Job Complete.")
                            }
                    //@TODO future consideration: The CompletedAt timestamp for the UserTaskEntity is from the original submission and not the Zeebe job completion.
                }.flatMap { submitTaskRequest ->
                    // Submit final result to DB
                    userTasksRepository.update(submitTaskRequest.userTaskEntity)
                            .onErrorResumeNext {
                                Single.error(UnableToUpdateUserTaskWithCompletionException(submitTaskRequest.userTaskEntity.taskId!!)) //@TODO add better error handling
                            }
                }
    }

    fun assignTask(taskId: UUID, assignee: AssignTaskRequest, requireUnassigned: Boolean = false): Single<UserTaskEntity> {
        return userTasksRepository.findById(taskId).flatMapSingle { ut ->

            if (requireUnassigned && ut.assignee != null) {
                throw IllegalStateException("Task is already assigned.")
            }

            ut.assignee = assignee.assignee

            userTasksRepository.update(ut)
                    .onErrorResumeNext { e ->
                        Single.error(e) // @TODO add update error handling
                    }
        }.onErrorResumeNext { e ->
            Single.error(e) // @TODO add error handling
        }
    }

    fun removeAssigneeFromTask(taskId: UUID, requestingUserId: String): Single<UserTaskEntity> {
        return userTasksRepository.findById(taskId).flatMapSingle { ut ->

            if (ut.assignee != requestingUserId) {
                throw IllegalStateException("Task is not assigned to requesting user.")
            }

            ut.assignee = null

            userTasksRepository.update(ut)
                    .onErrorResumeNext { e ->
                        Single.error(e) //@TODO
                    }
        }.onErrorResumeNext { e ->
            Single.error(e) //@TODO
        }
    }

    private fun reportCompletionToZeebe(jobKey: Long, completionVariables: ZeebeVariables): Completable {
        return zeebeManagementRepository.completeJob(jobKey, completionVariables)
    }

    private fun reportFailureToZeebe(jobKey: Long, errorMessage: String, remainingRetries: Int = 0): Completable {
        return zeebeManagementRepository.reportJobFailure(jobKey, errorMessage, remainingRetries)
    }

    fun getMostRecentTaskForm(taskId: UUID): Single<FormSchema> {
        return userTasksRepository.findById(taskId)
                .switchIfEmpty(Single.error(UserTaskNotFoundException(taskId)))
                .flatMap {
                    formRepository.findByFormKey(it.formKey!!)
                            .switchIfEmpty(Single.error(FormKeyNotFoundException(it.formKey!!)))
                }.flatMap { formEntity ->
                    formSchemaRepository.findByForm(FormEntity(id = formEntity.id!!),
                            Pageable.from(0, 1, Sort.of(Sort.Order.desc("version"))))
                            .map {
                                if (it.isEmpty){
                                    throw NoSchemasFoundForFormKeyException(formEntity.formKey!!)
                                } else {
                                    require(it.numberOfElements == 1,
                                            lazyMessage = { "Could not find a form schema for the formKey of the provided Task ID." })
                                    it.content[0].schema!!
                                }
                            }
                }
    }
}


data class AssignTaskRequest(
        val assignee: String
)

data class SubmitTaskRequest(
        @JsonIgnore var userTaskEntity: UserTaskEntity
) {
    @JsonIgnore
    var formSchema: FormSchema? = null

    @JsonIgnore
    var formSubmissionData: FormSubmissionData? = null

    @JsonIgnore
    var validFormResponse: Map<String, Any?>? = null

}

data class CreateCustomTaskRequest(
        val title: String,
        val description: String? = null,
        val priority: Int? = 0,
        val assignee: String? = null,
        val candidateGroups: Set<String>? = null,
        val candidateUsers: Set<String>? = null,
        val dueDate: Instant? = null,
        val formKey: String,
        val metadata: UserTaskMetadata? = null
)