package com.github.stephenott.qtz.workers

import io.reactivex.Completable
import io.zeebe.client.ZeebeClient
import io.zeebe.client.api.response.ActivatedJob

interface JobFailedProcessor {
    fun processFailedJob(zClient: ZeebeClient, job: ActivatedJob, errorMessage: String): Completable
}