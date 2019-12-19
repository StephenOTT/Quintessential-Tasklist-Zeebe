package com.github.stephenott.qtz.workers

import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.zeebe.ZeebeClientConfiguration
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.zeebe.client.ZeebeClient
import io.zeebe.client.api.command.ClientStatusException
import io.zeebe.client.api.response.ActivatedJob
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


class GenericWorker(
        private val jobProcessor: JobProcessor,
        private val jobFailedProcessor: JobFailedProcessor,
        private val zClientConfig: ZeebeClientConfiguration,
        private val workerConfig: WorkerConfiguration
) {

    private var workerActive: Boolean = false

    private lateinit var zClient: ZeebeClient

    private var currentActiveJobs: AtomicInteger = AtomicInteger(0)

    fun getWorkerName(): String {
        return workerConfig.workerName
    }

    fun workerIsActive(): Boolean {
        return workerActive
    }

    fun start(): Completable {
        return Completable.fromAction {
            if (!workerActive) {
                this.zClient = createDefaultZeebeClient()

                this.workerActive = true

                //@TODO add retryWhen logic to support specific expected errors such as inability to connect to Zeebe broker

                startTaskCapture()
                        .repeatUntil { !this.workerActive }
                        .subscribeOn(Schedulers.io()).subscribeBy(
                                onError = {
                                    if (it is ClientStatusException && it.message == "Channel shutdownNow invoked" && !workerActive) {
                                        workerActive = false

                                        println("${getWorkerName()} worker was stopped due to requested stop command.")

                                    } else {
                                        workerActive = false

                                        zClient.close()

                                        throw IllegalStateException("${getWorkerName()} Worker has unexpectedly stopped due to: ${it.message}", it)
                                    }
                                },
                                //@TODO add error catching that caches when Channel ShutdownNow invoked error occurs (from when management controller stops the worker.
                                onComplete = {
                                    println("${getWorkerName()} Looping complete...")
                                },
                                onNext = {
                                    if (it.isEmpty()) {
                                        Thread.sleep(1000) // Wait for 1 second so looping does not occur at overly-aggressive rate
                                    } else {
                                        processJobs(it).subscribeBy(
                                                onComplete = { println("${getWorkerName()} batch for processing jobs complete") },
                                                onError = { println("Major error with ${getWorkerName()} processing jobs: ${it.message}") })
                                    }
                                }
                        )
            } else {
                println("${getWorkerName()} worker is already active, no need to start the worker.")
            }
        }
    }

    fun stop(): Completable {
        return Completable.fromAction {
            if (!workerActive) {
                println("${getWorkerName()} Deactivation of worker was requested, but worker is already stopped.")
            } else {
                println("${getWorkerName()} Stopping worker...")
                this.workerActive = false
                this.zClient.close()
            }
        }
    }

    private fun processJobs(jobs: List<ActivatedJob>): Completable {
        return Completable.fromAction {
            Flowable.fromIterable(jobs).forEach { job ->
                jobProcessor.processJob(job)
                        .subscribeOn(Schedulers.io()).subscribeBy(
                                onSuccess = { jobResult ->
                                    this.currentActiveJobs.decrementAndGet()

                                    println("${getWorkerName()} Worker has completed processing Job ${job.key}.")

                                    if (jobResult.reportResult) {
                                        reportJobSuccess(job, jobResult.resultVariables)
                                                .subscribeOn(Schedulers.io())
                                                .subscribeBy(
                                                        onError = { error ->
                                                            //@TODO add retry support
                                                            println("${getWorkerName()} Failed to report success of job ${job.key} back to Zeebe: ${error.message}")
                                                            error.printStackTrace()
                                                        },
                                                        onComplete = {
                                                            println("${getWorkerName()} Job ${job.key} was successfully reported as a success to Zeebe.")
                                                        })
                                    }
                                },
                                onError = {
                                    this.currentActiveJobs.decrementAndGet()

                                    val errorMessage = it.message
                                            ?: "${getWorkerName()} Job ${job.key} failed to complete successfully."

                                    jobFailedProcessor.processFailedJob(this.zClient, job, errorMessage)
                                }
                        )
            }
        }
    }

    private fun reportJobSuccess(job: ActivatedJob, resultVariables: ZeebeVariables): Completable {
        return Completable.fromAction {
            zClient.newCompleteCommand(job.key)
                    .variables(resultVariables.variables)
                    .send().join(zClientConfig.commandTimeout.seconds.plus(1), TimeUnit.SECONDS)
        }
    }

    private fun startTaskCapture(): Single<List<ActivatedJob>> {
        return Single.fromCallable {
            val currentActiveJobsSnapshot: Int = this.currentActiveJobs.get()
            val maxJobsToActivate: Int = this.workerConfig.maxBatchSize - currentActiveJobsSnapshot

            if (maxJobsToActivate != 0) {
                check(currentActiveJobsSnapshot in 0..this.workerConfig.maxBatchSize,
                        lazyMessage = { "${getWorkerName()} Max Jobs bound/limit was out breached!" })

                val jobs = pollForZeebeJobs(
                        this.workerConfig.taskType,
                        this.workerConfig.workerName,
                        this.workerConfig.taskMaxZeebeLock,
                        maxJobsToActivate)

                        .toList()
                        .doOnSuccess {
                            println("${getWorkerName()} Polling returned: ${it.size} jobs.")
                        }
                        .subscribeOn(Schedulers.io()).blockingGet()

                this.currentActiveJobs.addAndGet(jobs.size)

                jobs

            } else {
                listOf()
            }
        }
    }

    private fun pollForZeebeJobs(jobType: String, workerName: String, taskLockTimeout: Duration, maxJobsToActivate: Int): Flowable<ActivatedJob> {
        return Flowable.fromCallable {
            this.zClient.newActivateJobsCommand().jobType(jobType)
                    .maxJobsToActivate(maxJobsToActivate)
                    .timeout(taskLockTimeout)
                    .workerName(workerName)
                    .requestTimeout(zClientConfig.longPollTimeout)
                    .send()
                    .join(zClientConfig.longPollTimeout.seconds.plus(5), TimeUnit.SECONDS)

        }.doOnSubscribe {
            println("${getWorkerName()} Starting Long Polling for $maxJobsToActivate jobs (${zClientConfig.longPollTimeout.seconds} second cycle)...")
        }.flatMap {
            Flowable.fromIterable(it.jobs)
        }
    }

    private fun createDefaultZeebeClient(): ZeebeClient {
        return ZeebeClient.newClientBuilder()
                .brokerContactPoint(zClientConfig.brokerContactPoint) //@TODO add rest of default configurations
                .defaultRequestTimeout(zClientConfig.commandTimeout)
                .defaultMessageTimeToLive(zClientConfig.messageTimeToLive)
                .usePlaintext() //@TODO remove and replace with cert  /-/SECURITY/-/
                .build()
    }
}