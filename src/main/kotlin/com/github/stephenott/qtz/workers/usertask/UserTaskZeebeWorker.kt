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
}