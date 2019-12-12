package com.github.stephenott.qtz.workers.usertask

import com.github.stephenott.qtz.workers.JobFailedProcessor
import com.github.stephenott.qtz.zeebe.ZeebeManagementClientConfiguration
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.zeebe.client.ZeebeClient
import io.zeebe.client.api.response.ActivatedJob
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UserTaskZeebeFailedJobProcessor: JobFailedProcessor {

    @Inject
    private lateinit var zClientConfig: ZeebeManagementClientConfiguration

    override fun processFailedJob(zClient: ZeebeClient, job: ActivatedJob, errorMessage: String): Completable {
        return Completable.fromCallable {
            zClient.newFailCommand(job.key)
                    .retries(job.retries.minus(1))
                    .errorMessage(errorMessage)
                    .send()
                    .join(zClientConfig.commandTimeout.seconds.plus(2), TimeUnit.SECONDS)

        }.subscribeOn(Schedulers.io())
                .doOnSubscribe {
                    println("Attempting to report failure to Zeebe for job: ${job.key} with error message: ${errorMessage}.")
                }.doOnComplete {
                    println("Successfully reported Failure of Job: ${job.key} with error message: ${errorMessage}.")
                }.doOnError {
                    println("Unable to report failure of ${job.key}: Error was: ${it.message}")
                }
    }


}