package com.github.stephenott.qtz.workers

import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import io.reactivex.Single
import io.zeebe.client.api.response.ActivatedJob

interface JobProcessor {
    fun processJob(job: ActivatedJob): Single<JobResult>
}

data class JobResult(
        val resultVariables: ZeebeVariables = ZeebeVariables(),

        /**
         * Indicates if the Worker should report the successful processing of the job right away.
         * Typically set to True.
         * Use Case for setting to False is for User Tasks where a Job Completion only occurs sometime in the future.
         */
        val reportResult: Boolean = true
)