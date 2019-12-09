package com.github.stephenott.qtz.executors.script.python

import com.github.stephenott.qtz.executors.JobFailedProcessor
import com.github.stephenott.qtz.zeebe.management.ZeebeManagementClientConfiguration
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.zeebe.client.ZeebeClient
import io.zeebe.client.api.response.ActivatedJob
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonExecutorFailedJobProcessor: JobFailedProcessor {

    @Inject
    private lateinit var zClientConfig: ZeebeManagementClientConfiguration

    override fun processFailedJob(zClient: ZeebeClient, job: ActivatedJob, errorMessage: String): Completable {
        return Completable.fromCallable {
            zClient.newFailCommand(job.key)
                    .retries(job.retries.minus(1))
                    .errorMessage(errorMessage)
                    .send()
                    .join(zClientConfig.commandTimeout.seconds.plus(5), TimeUnit.SECONDS)

        }.subscribeOn(Schedulers.io())
                .doOnSubscribe {
                    println("Attempting to report failure of Python Executor job: ${job.key} with error message: ${errorMessage}.")
                }.doOnComplete {
                    println("Successfully reported Failure of Python Executor Job: ${job.key} with error message: ${errorMessage}.")
                }.doOnError {
                    println("Unable to report Python Executor job ${job.key} failure: Error was: ${it.message}")
                }
    }


}