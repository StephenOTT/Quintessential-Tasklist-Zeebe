package com.github.stephenott.qtz.tasks.service

import com.github.stephenott.qtz.forms.FormSchema
import com.github.stephenott.qtz.forms.persistence.FormEntity
import com.github.stephenott.qtz.forms.persistence.FormSchemaRepository
import com.github.stephenott.qtz.tasks.domain.UserTaskEntity
import com.github.stephenott.qtz.tasks.domain.UserTaskMetadata
import com.github.stephenott.qtz.tasks.domain.UserTaskState
import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.tasks.repository.UserTasksRepository
import com.github.stephenott.qtz.zeebe.management.repository.ZeebeManagementRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Sort
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTasksService() {

    @Inject
    lateinit var userTasksRepository: UserTasksRepository

    @Inject
    lateinit var formSchemaRepository: FormSchemaRepository

    @Inject
    lateinit var zeebeManagementRepository: ZeebeManagementRepository

//    fun createTaskZeebe(){
//
//    }

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

    fun updateTask(entity: UserTaskEntity):Single<UserTaskEntity> {
        requireNotNull(entity.taskId, lazyMessage = {"taskId cannot be null when updating a User Task."})
        return userTasksRepository.update(entity)
    }

    fun deleteTask(taskId: UUID): Completable {
        return userTasksRepository.deleteById(taskId)
    }

    fun completeTask(taskId: UUID, variables: ZeebeVariables): Single<UserTaskEntity> {
        return Single.just(variables).flatMap { vars ->
            userTasksRepository.findById(taskId)
                    .flatMapSingle { task ->
                        require(task.state != UserTaskState.COMPLETED, lazyMessage = { "User Task is already completed." })
                        task.completeVariables = vars
                        task.state = UserTaskState.COMPLETED
                        task.completedAt = Instant.now()
                        //@TODO review if this should be sent into another .map{} :
                        reportCompletionToZeebe(task.zeebeJobKey!!, vars)
                                .subscribeOn(Schedulers.io()).subscribe()

                        userTasksRepository.update(task)
                                .onErrorResumeNext {
                                    Single.error(it)
                                }

                    }.onErrorResumeNext {
                        Single.error(it) // @TODO add better error message
                    }
        }
    }

    fun assignTask(taskId: UUID, assignee: AssignTaskRequest): Single<UserTaskEntity> {
        return userTasksRepository.findById(taskId).flatMapSingle { ut ->
            ut.assignee = assignee.assignee
            userTasksRepository.update(ut)
                    .onErrorResumeNext { e ->
                        Single.error(e) // @TODO add update error handling
                    }
        }.onErrorResumeNext { e ->
            Single.error(e) // @TODO add error handling
        }
    }

    private fun reportCompletionToZeebe(jobKey: Long, completionVariables: ZeebeVariables): Completable {
            return zeebeManagementRepository.completeJob(jobKey, completionVariables)
    }

    private fun reportFailureToZeebe(jobKey: Long, errorMessage: String, remainingRetries: Int = 0): Completable {
        return zeebeManagementRepository.reportJobFailure(jobKey, errorMessage, remainingRetries)
    }

    fun getMostRecentTaskForm(taskId: UUID): Single<FormSchema> {
        //@TODO consider adding a wrapper object that has metadata such as formKey
        return userTasksRepository.findById(taskId)
                .flatMapSingle { _ ->
                    val formEntity = FormEntity(id = taskId)
                    val pageConfig = Pageable.from(0, 1, Sort.of(Sort.Order.desc("version")))

                    formSchemaRepository.findByForm(formEntity, pageConfig)
                            .onErrorResumeNext {
                                Single.error(it) //@TODO add error for schema was not found

                            }.map { page ->
                                check(page.numberOfElements == 1, lazyMessage = { "Number of elements returned expected not equal to 1" })
                                page.content[0].schema!!
                            }
                }
    }

}


data class AssignTaskRequest(
        val assignee: String
)

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