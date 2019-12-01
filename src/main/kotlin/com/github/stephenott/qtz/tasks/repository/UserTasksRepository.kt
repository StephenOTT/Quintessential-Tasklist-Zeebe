package com.github.stephenott.qtz.tasks.repository

import com.github.stephenott.qtz.tasks.domain.UserTaskEntity
import com.github.stephenott.qtz.tasks.domain.UserTaskState
import io.micronaut.data.annotation.Repository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.reactive.RxJavaCrudRepository
import io.reactivex.Single
import java.util.*

@Repository
interface UserTasksRepository: RxJavaCrudRepository<UserTaskEntity, UUID>{
    fun update(entity: UserTaskEntity): Single<UserTaskEntity>
    fun findAll(pageable: Pageable): Single<Page<UserTaskEntity>>
    fun findByStateInList(state: List<UserTaskState>?, pageable: Pageable): Single<Page<UserTaskEntity>>
    fun findByAssigneeAndStateInList(assignee: String?, state: List<UserTaskState>?, pageable: Pageable): Single<Page<UserTaskEntity>>
    fun findByCandidateGroupsInListAndStateInList(candidateGroups: List<String>?, state: List<UserTaskState>?, pageable: Pageable): Single<Page<UserTaskEntity>>
    fun findByCandidateUsersInListAndStateInList(candidateUsers: List<String>?, state: List<UserTaskState>?, pageable: Pageable): Single<Page<UserTaskEntity>>
    fun findByStateInListAndAssigneeInListOrCandidateGroupsInListOrCandidateUsersInList(state: List<UserTaskState>?, assignee: List<String>?, candidateGroups: List<String>?, candidateUsers: List<String>?, pageable: Pageable): Single<Page<UserTaskEntity>>
}