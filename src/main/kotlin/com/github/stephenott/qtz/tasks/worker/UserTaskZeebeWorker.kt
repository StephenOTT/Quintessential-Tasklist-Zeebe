package com.github.stephenott.qtz.tasks.worker

import com.github.stephenott.qtz.tasks.domain.UserTaskEntity
import com.github.stephenott.qtz.tasks.domain.UserTaskState
import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.tasks.repository.UserTasksRepository
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.zeebe.client.ZeebeClient
import io.zeebe.client.api.response.ActivatedJob
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupListener : ApplicationEventListener<ServerStartupEvent> {

    @Inject
    lateinit var worker: UserTaskZeebeWorker

    override fun onApplicationEvent(event: ServerStartupEvent?) {
        println("Creating User Task Worker...")
        worker.startWorker()
    }
}

@Singleton
class UserTaskZeebeWorker(
        val config: ZeebeUserTaskClientConfiguration
) {

    @Inject
    lateinit var userTaskRepository: UserTasksRepository

    fun startWorker() {
        val zClient = createDefaultUserTaskZeebeClient(config)
        println("<-------STARTING!!!------->")
        Completable.fromCallable {
            activateWorker(zClient)
        }.doOnSubscribe {
            println("Starting User Task Worker Polling...")
        }.subscribeBy(
                onError = {
                    println("activate worker error.......")
                },
                onComplete = {
                    println("User Task Long Poll has completed, restarting long poll...")
                    startWorker()
                })
    }

    private fun activateWorker(zClient: ZeebeClient) {
        println("Starting Polling 1")
        pollForZeebeJobs(zClient,
                config.taskType,
                config.workerName,
                config.exclusiveLockTimeout,
                config.maxTasksToLock)
                .doOnError {
                    println("ERROR when handling jobs...")
                }.doOnComplete {
                    println("polling complete")
                }
                .forEach { job ->
                    println("procesing job...")
                    val entity = zeebeJobToUserTaskEntity(job)
                    userTaskRepository.save(entity)
                            .doOnSuccess { ut ->
                                println("User Task was captured from Zeebe and saved: ${ut.taskId}")
                            }.doOnError { e ->
                                println("User Task that was capture from Zeebe has failed to save in DB: Zeebe job ${job.key}, from workflow: ${job.workflowInstanceKey}.  Reporting failure to Zeebe cluster...")
                                reportJobFailureToZeebe(zClient, job, "Failed to save User Task: ${e.message}")
                            }.subscribe()
                }
    }

    private fun reportJobFailureToZeebe(zClient: ZeebeClient, job: ActivatedJob, errorMessage: String): Completable {
        return Completable.fromCallable {
            zClient.newFailCommand(job.key)
                    .retries(job.retries.minus(1))
                    .errorMessage(errorMessage)
                    .send().join() //@TODO Add timeouts

        }.doOnSubscribe {
            println("Attempting to report failure to Zeebe for job: ${job.key} with error message: ${errorMessage}.")
        }.doOnComplete {
            println("Successfully reported Failure of Job: ${job.key} with error message: ${errorMessage}.")
        }.doOnError {
            println("Unable to report failure of ${job.key}: Error was: ${it.message}")
        }
    }


    private fun zeebeJobToUserTaskEntity(job: ActivatedJob): UserTaskEntity {
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
//                dueDate = Instant.parse(job.customHeaders["dueDate"]) ?: null,
                dueDate = null,
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
    }

    private fun pollForZeebeJobs(zeebeClient: ZeebeClient,
                                 jobType: String,
                                 workerName: String,
                                 taskLockTimeout: Duration,
                                 maxJobsToActivate: Int): Flowable<ActivatedJob> {
        println("starting polling 2")

        return Flowable.fromCallable {
            zeebeClient.newActivateJobsCommand().jobType(jobType)
                    .maxJobsToActivate(maxJobsToActivate)
                    .timeout(taskLockTimeout)
                    .workerName(workerName)
                    .send().join()
        }.doOnSubscribe {
            println("Starting long poll for User Tasks...")
        }.flatMap {
            Flowable.fromIterable(it.jobs)
        }
    }

    private fun createDefaultUserTaskZeebeClient(config: ZeebeUserTaskClientConfiguration): ZeebeClient {
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
    var zeebeBroker: String = "localhost:26500"
    var zeebeClusterName: String = "zeebe:${UUID.randomUUID()}"
}