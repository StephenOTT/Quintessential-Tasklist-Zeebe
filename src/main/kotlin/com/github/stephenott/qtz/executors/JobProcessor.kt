package com.github.stephenott.qtz.executors

import io.reactivex.Single
import io.zeebe.client.api.response.ActivatedJob

interface JobProcessor {
    fun processJob(job: ActivatedJob): Single<*>
}