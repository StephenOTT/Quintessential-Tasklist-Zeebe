package com.github.stephenott.qtz.tasks.controller

import com.github.stephenott.qtz.forms.domain.FormSchema
import com.github.stephenott.qtz.forms.validator.domain.FormSubmissionData
import com.github.stephenott.qtz.forms.validator.exception.FormValidationException
import com.github.stephenott.qtz.forms.validator.client.ValidationResponseInvalid
import com.github.stephenott.qtz.tasks.domain.UserTaskEntity
import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.tasks.repository.UserTasksRepository
import com.github.stephenott.qtz.tasks.service.AssignTaskRequest
import com.github.stephenott.qtz.tasks.service.CreateCustomTaskRequest
import com.github.stephenott.qtz.tasks.service.UserTasksService
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.*
import javax.inject.Inject

@Controller("/tasks")
open class UserTasksController() : UserTasksControllerOperations {

    @Inject
    lateinit var userTaskRepository: UserTasksRepository

    @Inject
    lateinit var userTasksService: UserTasksService

    @Get("/")
    override fun getAllTasks(pageable: Pageable?): Single<HttpResponse<List<UserTaskEntity>>> {
        return userTaskRepository.findAll(pageable ?: Pageable.from(0, 50)).map { page ->
            HttpResponse.ok(page.content)
                    .header("X-Total-Count", page.totalSize.toString())
                    .header("X-Page-Count", page.numberOfElements.toString())
        }
    }

    @Get("/{taskId}/")
    override fun getTaskById(taskId: UUID): Maybe<HttpResponse<UserTaskEntity>> {
        return userTaskRepository.findById(taskId)
                .map {
                    //@TODO add error handling for cannot find taskid
                    HttpResponse.ok(it)
                }
    }

    override fun claimTask(taskId: UUID): Single<HttpResponse<UserTaskEntity>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unClaimTask(taskId: UUID): Single<HttpResponse<UserTaskEntity>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Post("/{taskId}/assign")
    override fun assignTask(taskId: UUID, @Body assignee: Single<AssignTaskRequest>): Single<HttpResponse<UserTaskEntity>> {
        return assignee.flatMap {
            userTasksService.assignTask(taskId, it)
        }.map {
            HttpResponse.ok(it)
        }
    }


    @Get("/{taskId}/form")
    override fun getTaskForm(taskId: UUID): Single<HttpResponse<FormSchema>> {
        //@TODO consider adding a wrapper object that has metadata such as formKey
        return userTasksService.getMostRecentTaskForm(taskId)
                .map {
                    HttpResponse.ok(it)
                }
    }

    @Post("/{taskId}/complete")
    override fun completeTask(taskId: UUID, @Body variables: Single<ZeebeVariables>): Single<HttpResponse<UserTaskEntity>> {
        return variables.flatMap { vars ->
            userTasksService.completeTask(taskId, vars)
        }.doOnError {
            println("Error occurred when trying to complete task ${taskId}: ${it.message}")
            it.printStackTrace()
        }.map {
            HttpResponse.ok(it)
        }
    }

    @Post("/{taskId}/submit")
    override fun submitTask(taskId: UUID, @Body submission: Single<FormSubmissionData>): Single<HttpResponse<UserTaskEntity>> {
        return submission.flatMap {
            userTasksService.submitTask(taskId, it)
        }.map {
            HttpResponse.ok(it)
        }
    }

    @Post("/")
    override fun createCustomTask(taskId: UUID, @Body task: Single<CreateCustomTaskRequest>): Single<HttpResponse<UserTaskEntity>> {
        return task.flatMap { taskRequest ->
            userTasksService.createCustomTask(taskId, taskRequest).map { entity ->
                HttpResponse.created(entity)
            }
        }
    }

    @Error
    fun formValidationError(request: HttpRequest<*>, exception: FormValidationException): HttpResponse<ValidationResponseInvalid> {
        return HttpResponse.badRequest(exception.responseBody)
    }

}

interface UserTasksControllerOperations {

    fun getAllTasks(pageable: Pageable?): Single<HttpResponse<List<UserTaskEntity>>>

    fun getTaskById(taskId: UUID): Maybe<HttpResponse<UserTaskEntity>>

    fun claimTask(taskId: UUID): Single<HttpResponse<UserTaskEntity>>

    fun unClaimTask(taskId: UUID): Single<HttpResponse<UserTaskEntity>>

    fun assignTask(taskId: UUID, assignee: Single<AssignTaskRequest>): Single<HttpResponse<UserTaskEntity>>

    fun getTaskForm(taskId: UUID): Single<HttpResponse<FormSchema>>

    fun completeTask(taskId: UUID, variables: Single<ZeebeVariables>): Single<HttpResponse<UserTaskEntity>>

    fun submitTask(taskId: UUID, submission: Single<FormSubmissionData>): Single<HttpResponse<UserTaskEntity>>

    fun createCustomTask(taskId: UUID, task: Single<CreateCustomTaskRequest>): Single<HttpResponse<UserTaskEntity>>
}