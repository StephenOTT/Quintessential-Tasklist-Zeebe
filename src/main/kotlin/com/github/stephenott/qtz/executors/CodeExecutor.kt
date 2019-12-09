package com.github.stephenott.qtz.executors

import io.reactivex.Single
import java.io.File
import java.time.Duration

interface CodeExecutor {
    fun execute(script: File,
                inputs: Map<String, Any?> = mapOf(),
                executionTimeout: Duration = Duration.ofSeconds(60)
    ): Single<Map<String, Any?>>
}