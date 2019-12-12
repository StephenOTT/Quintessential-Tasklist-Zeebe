package com.github.stephenott.qtz.workers.usertask

import com.github.stephenott.qtz.workers.WorkerConfiguration
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import java.time.Duration
import java.util.*

@ConfigurationProperties("userTask")
@Context
class ZeebeUserTaskWorkerConfiguration: WorkerConfiguration {

    /**
     * The default state of the worker.
     */
    override var enabled: Boolean = true

    /**
     * The Zeebe Task Topic that will be queried for.  Represents the "User Task" type.
     */
    override var taskType: String = "user-task"

    /**
     * The worker name that Zeebe Jobs will be locked with.
     */
    override var workerName: String = "User-Task-Worker:${UUID.randomUUID()}"

    /**
     * The Zeebe job exclusiveLockTimeout value.  Represents the User Task lock.
     */
    override var taskMaxZeebeLock: Duration = Duration.ofDays(30)

    /**
     * The maximum number of Jobs that can be locked on each poll request.
     */
    override var maxBatchSize: Int = 1
}