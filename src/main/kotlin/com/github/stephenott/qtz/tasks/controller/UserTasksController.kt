package com.github.stephenott.qtz.tasks.controller

import com.github.stephenott.qtz.forms.FormSchema
import com.github.stephenott.qtz.forms.persistence.FormEntity
import com.github.stephenott.qtz.forms.persistence.FormSchemaRepository
import com.github.stephenott.qtz.tasks.domain.UserTaskEntity
import com.github.stephenott.qtz.tasks.domain.UserTaskMetadata
import com.github.stephenott.qtz.tasks.domain.UserTaskState
import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.tasks.repository.UserTasksRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Sort
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.reactivex.Maybe
import io.reactivex.Single
import java.time.Instant
import java.util.*
import javax.inject.Inject

@Controller("/tasks")
open class UserTasksController() : UserTasksOperations {

    @Inject
    lateinit var userTaskRepository: UserTasksRepository

    @Inject
    lateinit var formSchemaRepository: FormSchemaRepository

    @Get("/")
    override fun getAllTasks(pageable: Pageable?): Single<HttpResponse<List<UserTaskEntity>>> {
        val UT1 = UserTaskEntity(
                taskOriginalCapture = Instant.now(),
                state = UserTaskState.NEW,
                title = "My Custom Title",
                description = "some description goes here",
                priority = 10,
                assignee = "steve",
                candidateGroups = setOf("g1", "g2"),
                candidateUsers = setOf("u1"),
                dueDate = Instant.now().plusSeconds(2000),
                formKey = "someKey",
                completedAt = Instant.now(),
                completeVariables = ZeebeVariables(mapOf(Pair("DOG", 1234))),
                zeebeSource = "some-source-zeebe",
                zeebeJobKey = 3432432432432432,
                zeebeBpmnProcessId = "some-p-id-43423",
                zeebeBpmnProcessVersion = 222,
                zeebeVariablesAtCapture = ZeebeVariables(mapOf(Pair("CAR", 1234))),
                metadata = UserTaskMetadata(mapOf(Pair("CAR", 1234)))
        )
        userTaskRepository.save(UT1).blockingGet() //@TODO remove after testing

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
    override fun assignTask(taskId: UUID, @Body assignee: AssignTaskRequest): Single<HttpResponse<UserTaskEntity>> {
        return userTaskRepository.findById(taskId).flatMapSingle { ut ->
            ut.assignee = assignee.assignee
            userTaskRepository.update(ut)
                    .onErrorResumeNext { e ->
                        Single.error(e) // @TODO add update error handling
                    }
        }.onErrorResumeNext { e ->
            Single.error(e) // @TODO add error handling
        }.map {
            HttpResponse.ok(it)
        }
    }


    @Get("/{taskId}/form")
    override fun getTaskForm(taskId: UUID): Single<HttpResponse<FormSchema>> {
        //@TODO consider adding a wrapper object that has metadata such as formKey
        return userTaskRepository.findById(taskId)
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

                }.map {
                    HttpResponse.ok(it)
                }
    }

    @Post("/{taskId}/complete")
    override fun completeTask(taskId: UUID, @Body variables: ZeebeVariables): Single<HttpResponse<UserTaskEntity>> {
        return userTaskRepository.findById(taskId)
                .flatMapSingle { task ->
                    require(task.state != UserTaskState.COMPLETED, lazyMessage = { "User Task is already completed." })
                    task.completeVariables = variables
                    task.state = UserTaskState.COMPLETED

                    userTaskRepository.update(task)
                            .onErrorResumeNext {
                                Single.error(it)
                            }

                }.onErrorResumeNext {
                    Single.error(it) // @TODO add better error message
                }.map {
                    HttpResponse.ok(it)
                }
    }

    @Post("/")
    override fun createCustomTask(taskId: UUID, @Body task: Single<CreateCustomTaskRequest>): Single<HttpResponse<UserTaskEntity>> {
        return task.map {
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
            userTaskRepository.update(it).onErrorResumeNext { e->
                Single.error(e) //@TODO add proper error handling
            }

        }.map {
            HttpResponse.created(it)
        }
    }
}

interface UserTasksOperations {

    fun getAllTasks(pageable: Pageable?): Single<HttpResponse<List<UserTaskEntity>>>

    fun getTaskById(taskId: UUID): Maybe<HttpResponse<UserTaskEntity>>

    fun claimTask(taskId: UUID): Single<HttpResponse<UserTaskEntity>>

    fun unClaimTask(taskId: UUID): Single<HttpResponse<UserTaskEntity>>

    fun assignTask(taskId: UUID, assignee: AssignTaskRequest): Single<HttpResponse<UserTaskEntity>>

    fun getTaskForm(taskId: UUID): Single<HttpResponse<FormSchema>>

    fun completeTask(taskId: UUID, variables: ZeebeVariables): Single<HttpResponse<UserTaskEntity>>

    fun createCustomTask(taskId: UUID, task: Single<CreateCustomTaskRequest>): Single<HttpResponse<UserTaskEntity>>
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