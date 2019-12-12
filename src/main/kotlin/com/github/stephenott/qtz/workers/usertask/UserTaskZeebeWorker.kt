package com.github.stephenott.qtz.workers.usertask

import com.github.stephenott.qtz.workers.GenericWorker
import com.github.stephenott.qtz.zeebe.ZeebeManagementClientConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTaskZeebeWorker() {

    @Inject
    lateinit var zClientConfig: ZeebeManagementClientConfiguration

    @Inject
    lateinit var userTaskWorkerConfig: ZeebeUserTaskWorkerConfiguration

    @Inject
    private lateinit var jobProcessor: UserTaskZeebeJobProcessor

    @Inject
    private lateinit var jobFailedProcessor: UserTaskZeebeFailedJobProcessor

    private lateinit var worker: GenericWorker

    fun create(): GenericWorker {
        worker = GenericWorker(jobProcessor,
                jobFailedProcessor,
                zClientConfig,
                userTaskWorkerConfig)
        return worker
    }

    fun getWorker(): GenericWorker {
        return worker
    }

//    var workerActive: Boolean = false
//        private set
//
//    private lateinit var zClient: ZeebeClient
//
//    private var currentActiveJobs: AtomicInteger = AtomicInteger(0)
//
//    fun start(): Completable {
//        return Completable.fromAction {
//            if (!workerActive) {
//                this.zClient = createDefaultUserTaskZeebeClient()
//
//                this.workerActive = true
//
//                //@TODO add retryWhen logic to support specific expected errors such as inability to connect to Zeebe broker
//
//                startTaskCapture()
//                        .repeatUntil { !this.workerActive }
//                        .subscribeOn(Schedulers.io())
//                        .subscribeBy(
//                                onError = {
//                                    if (it is ClientStatusException && it.message == "Channel shutdownNow invoked" && !workerActive){
//                                        workerActive = false
//                                        println("Worker was stopped due to requested stop command.")
//                                    } else {
//                                        workerActive = false
//                                        zClient.close()
//                                        throw IllegalStateException("Worker has unexpectedly stopped due to: ${it.message}", it)
//                                    }
//                                },
//                                //@TODO add error catching that caches when Channel ShutdownNow invoked error occurs (from when management controller stops the worker.
//                                onComplete = { println("Looping complete...") },
//                                onNext = {
//                                    if (it.isEmpty()) {
//                                        Thread.sleep(1000) // Wait for 1 second so looping does not occur at overly-aggressive rate
//                                    } else {
//                                        processJobs(it).subscribeBy(
//                                                onComplete = { println("Batch for processing jobs complete") },
//                                                onError = { println("Major error with processing jobs: ${it.message}") })
//                                    }
//                                }
//                        )
//            } else {
//                println("worker is already active, no need to start the worker.")
//            }
//        }
//    }
//
//    private fun processJobs(jobs: List<ActivatedJob>): Completable {
//        return Completable.fromAction {
//            Flowable.fromIterable(jobs).forEach { job ->
//                jobProcessor.processJob(job)
//                        .subscribeOn(Schedulers.io())
//                        .subscribeBy(
//                                onSuccess = {
//                                    this.currentActiveJobs.decrementAndGet()
//                                    println("User Task Job ${job.key} processing complete.")
//                                },
//                                onError = {
//                                    this.currentActiveJobs.decrementAndGet()
//
//                                    val errorMessage = it.message ?: "User Task failed to persist in the DB."
//
//                                    jobFailedProcessor.processFailedJob(this.zClient, job, errorMessage)
//                                }
//                        )
//            }
//        }
//    }
//
//    fun stop(): Completable {
//        return Completable.fromAction {
//            if (!workerActive) {
//                println("Deactivation of worker was requested, but worker is already stopped.")
//            } else {
//                println("Stopping worker...")
//                this.workerActive = false
//                this.zClient.close()
//            }
//        }
//    }
//
//    private fun startTaskCapture(): Single<List<ActivatedJob>> {
//        return Single.fromCallable {
//            val currentActiveJobsSnapshot: Int = this.currentActiveJobs.get()
//            val maxJobsToActivate: Int = userTaskWorkerConfig.maxBatchSize - currentActiveJobsSnapshot
//
//            if (maxJobsToActivate != 0) {
//                check(currentActiveJobsSnapshot in 0..userTaskWorkerConfig.maxBatchSize,
//                        lazyMessage = { "Max Jobs bound/limit was out breached!" })
//
//                val jobs = pollForZeebeJobs(
//                        userTaskWorkerConfig.taskType,
//                        userTaskWorkerConfig.workerName,
//                        userTaskWorkerConfig.taskMaxZeebeLock,
//                        maxJobsToActivate)
//
//                        .toList()
//                        .doOnSuccess {
//                            println("Polling returned: ${it.size} jobs.")
//                        }
//                        .subscribeOn(Schedulers.io()).blockingGet()
//
//                this.currentActiveJobs.addAndGet(jobs.size)
//
//                jobs
//
//            } else {
//                listOf()
//            }
//        }
//    }
//
//    private fun pollForZeebeJobs(jobType: String, workerName: String, taskLockTimeout: Duration, maxJobsToActivate: Int): Flowable<ActivatedJob> {
//        return Flowable.fromCallable {
//            this.zClient.newActivateJobsCommand().jobType(jobType)
//                    .maxJobsToActivate(maxJobsToActivate)
//                    .timeout(taskLockTimeout)
//                    .workerName(workerName)
//                    .requestTimeout(zClientConfig.longPollTimeout)
//                    .send()
//                    .join(zClientConfig.longPollTimeout.seconds.plus(5), TimeUnit.SECONDS)
//
//        }.doOnSubscribe {
//            println("Starting Long Polling for $maxJobsToActivate jobs (${zClientConfig.longPollTimeout.seconds} second cycle)...")
//        }.flatMap {
//            Flowable.fromIterable(it.jobs)
//        }
//    }
//
//    private fun createDefaultUserTaskZeebeClient(): ZeebeClient {
//        return ZeebeClient.newClientBuilder()
//                .brokerContactPoint(zClientConfig.brokerContactPoint) //@TODO add rest of default configurations
//                .defaultRequestTimeout(zClientConfig.commandTimeout)
//                .defaultMessageTimeToLive(zClientConfig.messageTimeToLive)
//                .usePlaintext() //@TODO remove and replace with cert  /-/SECURITY/-/
//                .build()
//    }
}