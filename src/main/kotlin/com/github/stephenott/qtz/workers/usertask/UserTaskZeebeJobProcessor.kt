package com.github.stephenott.qtz.workers.usertask

import com.github.stephenott.qtz.tasks.domain.UserTaskEntity
import com.github.stephenott.qtz.tasks.domain.UserTaskState
import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.tasks.repository.UserTasksRepository
import com.github.stephenott.qtz.workers.JobProcessor
import com.github.stephenott.qtz.workers.JobResult
import com.github.stephenott.qtz.zeebe.ZeebeManagementClientConfiguration
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.zeebe.client.api.response.ActivatedJob
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTaskZeebeJobProcessor: JobProcessor {

    @Inject
    private lateinit var userTaskRepository: UserTasksRepository

    @Inject
    private lateinit var workerConfig: ZeebeUserTaskWorkerConfiguration

    @Inject
    private lateinit var zClientConfig: ZeebeManagementClientConfiguration

    override fun processJob(job: ActivatedJob): Single<JobResult> {
        return Single.fromCallable {
            println("${workerConfig.workerName} Processing User Task Job...")
            val entity = zeebeJobToUserTaskEntity(job, this.zClientConfig)
            val taskEntity = userTaskRepository.save(entity)
                    .subscribeOn(Schedulers.io())
                    .doOnSuccess { ut ->
                        println("User Task was captured from Zeebe and saved: ${ut.taskId}")
                    }.blockingGet()

            JobResult(resultVariables = ZeebeVariables(mapOf(Pair("createdUserTask", taskEntity))),
                    reportResult = false)
        }

    }

    private fun zeebeJobToUserTaskEntity(job: ActivatedJob, config: ZeebeManagementClientConfiguration): UserTaskEntity {
        return UserTaskEntity(
                state = UserTaskState.NEW,
                taskOriginalCapture = Instant.now(),
                title = job.customHeaders["title"]
                        ?: throw IllegalArgumentException("Missing task title configuration"),
                description = job.customHeaders["description"],
                priority = job.customHeaders["priority"]?.toInt() ?: 0,
                assignee = job.customHeaders["assignee"],
                candidateGroups = job.customHeaders["candidateGroups"]?.split(",")?.toSet(),
                candidateUsers = job.customHeaders["candidateGroups"]?.split(",")?.toSet(),
//                dueDate = Instant.parse(job.customHeaders["dueDate"]) ?: null, //@TODO!!!
                dueDate = null,
                formKey = job.customHeaders["formKey"]
                        ?: throw IllegalArgumentException("formKey is missing."),
                zeebeJobKey = job.key,
                zeebeVariablesAtCapture = ZeebeVariables(job.variablesAsMap),
                zeebeSource = config.clusterName,
                zeebeBpmnProcessId = job.bpmnProcessId,
                zeebeBpmnProcessVersion = job.workflowDefinitionVersion,
                zeebeBpmnProcessKey = job.workflowKey,
                zeebeElementInstanceKey = job.elementInstanceKey,
                zeebeElementId = job.elementId,
                zeebeJobDealine = Instant.ofEpochMilli(job.deadline),
                zeebeJobRetriesRemaining = job.retries)
    }

}