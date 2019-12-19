package com.github.stephenott.qtz.workers.python

import com.github.stephenott.qtz.workers.GenericWorker
import com.github.stephenott.qtz.zeebe.ZeebeManagementClientConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonExecutorZeebeWorker() {

    @Inject
    lateinit var zClientConfig: ZeebeManagementClientConfiguration

    @Inject
    lateinit var workerConfig: PythonExecutorWorkerConfiguration

    @Inject
    private lateinit var jobProcessor: PythonExecutorJobProcessor

    @Inject
    private lateinit var jobFailedProcessor: PythonExecutorFailedJobProcessor

    private lateinit var worker: GenericWorker

    fun create(): GenericWorker {
        worker = GenericWorker(jobProcessor,
                jobFailedProcessor,
                zClientConfig,
                workerConfig)
        return worker
    }

    fun getWorker(): GenericWorker {
        return worker
    }
}