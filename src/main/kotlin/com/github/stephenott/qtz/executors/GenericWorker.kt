package com.github.stephenott.qtz.executors

import com.github.stephenott.qtz.tasks.worker.*
import com.github.stephenott.qtz.zeebe.management.ZeebeClientConfiguration
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

    fun getWorkerName(): String{
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
                        .subscribeOn(Schedulers.io())
                        .subscribeBy(
                                onError = {
                                    if (it is ClientStatusException && it.message == "Channel shutdownNow invoked" && !workerActive){
                                        workerActive = false
                                        println("Worker was stopped due to requested stop command.")
                                    } else {
                                        workerActive = false
                                        zClient.close()
                                        throw IllegalStateException("Worker has unexpectedly stopped due to: ${it.message}", it)
                                    }
                                },
                                //@TODO add error catching that caches when Channel ShutdownNow invoked error occurs (from when management controller stops the worker.
                                onComplete = { println("Looping complete...") },
                                onNext = {
                                    if (it.isEmpty()) {
                                        Thread.sleep(1000) // Wait for 1 second so looping does not occur at overly-aggressive rate
                                    } else {
                                        processJobs(it).subscribeBy(
                                                onComplete = { println("Batch for processing jobs complete") },
                                                onError = { println("Major error with processing jobs: ${it.message}") })
                                    }
                                }
                        )
            } else {
                println("worker is already active, no need to start the worker.")
            }
        }
    }

    private fun processJobs(jobs: List<ActivatedJob>): Completable {
        return Completable.fromAction {
            Flowable.fromIterable(jobs).forEach { job ->
                jobProcessor.processJob(job)
                        .subscribeOn(Schedulers.io())
                        .subscribeBy(
                                onSuccess = {
                                    this.currentActiveJobs.decrementAndGet()
                                    println("Worker ${getWorkerName()} has completed processing Job ${job.key} processing.")
                                },
                                onError = {
                                    this.currentActiveJobs.decrementAndGet()

                                    val errorMessage = it.message ?: "Job ${job.key} failed to complete successfully."

                                    jobFailedProcessor.processFailedJob(this.zClient, job, errorMessage)
                                }
                        )
            }
        }
    }

    fun stop(): Completable {
        return Completable.fromAction {
            if (!workerActive) {
                println("Deactivation of worker was requested, but worker is already stopped.")
            } else {
                println("Stopping worker...")
                this.workerActive = false
                this.zClient.close()
            }
        }
    }

    private fun startTaskCapture(): Single<List<ActivatedJob>> {
        return Single.fromCallable {
            val currentActiveJobsSnapshot: Int = this.currentActiveJobs.get()
            val maxJobsToActivate: Int = this.workerConfig.maxBatchSize - currentActiveJobsSnapshot

            if (maxJobsToActivate != 0) {
                check(currentActiveJobsSnapshot in 0..this.workerConfig.maxBatchSize,
                        lazyMessage = { "Max Jobs bound/limit was out breached!" })

                val jobs = pollForZeebeJobs(
                        this.workerConfig.taskType,
                        this.workerConfig.workerName,
                        this.workerConfig.taskMaxZeebeLock,
                        maxJobsToActivate)

                        .toList()
                        .doOnSuccess {
                            println("Polling returned: ${it.size} jobs.")
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
            println("Starting Long Polling for $maxJobsToActivate jobs (${zClientConfig.longPollTimeout.seconds} second cycle)...")
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