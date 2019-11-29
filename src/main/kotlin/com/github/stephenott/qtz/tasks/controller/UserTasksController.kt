package com.github.stephenott.qtz.tasks.controller

import com.github.stephenott.qtz.tasks.domain.UserTaskEntity
import com.github.stephenott.qtz.tasks.repository.UserTasksRepository
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.reactivex.Single
import java.util.*
import javax.inject.Inject

@Controller("/tasks")
class UserTasksController(): UserTasksOperations {

    @Inject
    lateinit var userTaskRepository: UserTasksRepository

    @Get("/")
    override fun getAllTasks(pageable: Pageable): Single<HttpResponse<List<UserTaskEntity>>> {
        return userTaskRepository.findAll(pageable ?: Pageable.from(0, 50)).map { page ->
            HttpResponse.ok(page.content)
                    .header("X-Total-Count", page.totalSize.toString())
                    .header("X-Page-Count", page.numberOfElements.toString())
        }
    }

    @Get("/{taskId}/")
    override fun getTaskById(taskId: UUID): Single<HttpResponse<UserTaskEntity>> {
        return Single.fromPublisher(userTaskRepository.findById(taskId)).map {
            HttpResponse.ok(it)
        }
    }

}

interface UserTasksOperations {

    fun getAllTasks(pageable:Pageable): Single<HttpResponse<List<UserTaskEntity>>>

    fun getTaskById(taskId: UUID): Single<HttpResponse<UserTaskEntity>>
}