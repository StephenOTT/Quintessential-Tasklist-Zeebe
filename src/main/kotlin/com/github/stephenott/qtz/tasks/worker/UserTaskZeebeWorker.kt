package com.github.stephenott.qtz.tasks.worker

import com.github.stephenott.qtz.tasks.domain.UserTaskEntity
import com.github.stephenott.qtz.tasks.domain.UserTaskState
import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.tasks.repository.UserTasksRepository
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.zeebe.client.ZeebeClient
import io.zeebe.client.api.response.ActivatedJob
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.inject.Inject

class UserTaskZeebeWorker(
        val config: ZeebeUserTaskClientConfiguration
) {

    val zClient = createDefaultUserTaskZeebeClient(config)

    @Inject
    lateinit var userTaskRepository: UserTasksRepository

    init {
        pollForZeebeJobs(zClient,
                config.taskType,
                config.workerName,
                config.exclusiveLockTimeout,
                config.maxTasksToLock).map {
            Flowable.fromIterable(it)
        } //flow over each item

                    it.forEach { job ->

                        val userTask = UserTaskEntity(
                                state = UserTaskState.NEW,
                                taskOriginalCapture = Instant.now(),
                                title = job.customHeaders["title"]
                                        ?: throw IllegalArgumentException("Missing task title configuration"),
                                description = job.customHeaders["description"],
                                priority = job.customHeaders["priority"]?.toInt() ?: 0,
                                assignee = job.customHeaders["assignee"],
                                candidateGroups = job.customHeaders["candidateGroups"]?.split(",")?.toSet(),
                                candidateUsers = job.customHeaders["candidateGroups"]?.split(",")?.toSet(),
                                dueDate = job.customHeaders["dueDate"].let { Instant.parse(it) },
                                formKey = job.customHeaders["formKey"]
                                        ?: throw IllegalArgumentException("formKey is missing."),
                                zeebeJobKey = job.key,
                                zeebeVariablesAtCapture = ZeebeVariables(job.variablesAsMap),
                                zeebeSource = config.zeebeClusterName,
                                zeebeBpmnProcessId = job.bpmnProcessId,
                                zeebeBpmnProcessVersion = job.workflowDefinitionVersion,
                                zeebeBpmnProcessKey = job.workflowKey,
                                zeebeElementInstanceKey = job.elementInstanceKey,
                                zeebeElementId = job.elementId,
                                zeebeJobDealine = Instant.ofEpochMilli(job.deadline),
                                zeebeJobRetriesRemaining = job.retries)

                        userTaskRepository.save(userTask)
                                .doOnSuccess { ut ->
                                    println("User Task was captured from Zeebe and saved: ${ut.taskId}")
                                }.doOnError {
                                    println("User Task that was capture from Zeebe has failed to save in DB: Zeebe job ${job.key}, from workflow: ${job.workflowInstanceKey}")
                                }

                    }

                }
    }

    fun pollForZeebeJobs(zeebeClient: ZeebeClient,
                         jobType: String,
                         workerName: String,
                         taskLockTimeout: Duration,
                         maxJobsToActivate: Int): Single<List<ActivatedJob>> {

        return Single.fromCallable {
            zeebeClient.newActivateJobsCommand().jobType(jobType)
                    .maxJobsToActivate(maxJobsToActivate)
                    .timeout(taskLockTimeout)
                    .workerName(workerName)
                    .send().join()
        }.map {
            it.jobs
        }
    }

    fun createDefaultUserTaskZeebeClient(config: ZeebeUserTaskClientConfiguration): ZeebeClient {
        return ZeebeClient.newClientBuilder()
                .brokerContactPoint(config.zeebeBroker) //@TODO add rest of default configurations
                .usePlaintext() //@TODO remove and replace with cert  /-/SECURITY/-/
                .build()
    }

}


@ConfigurationProperties("userTask")
@Context
class ZeebeUserTaskClientConfiguration {
    var taskType: String = "user-task"
    var workerName: String = "user-task-worker:${UUID.randomUUID()}"
    var exclusiveLockTimeout: Duration = Duration.ofDays(30)
    var maxTasksToLock: Int = 1
    var zeebeBroker: String = "localhost:25600"
    var zeebeClusterName: String = "zeebe:${UUID.randomUUID()}"
}