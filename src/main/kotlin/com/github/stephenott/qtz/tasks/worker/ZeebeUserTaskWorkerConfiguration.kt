package com.github.stephenott.qtz.tasks.worker

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import java.time.Duration
import java.util.*

@ConfigurationProperties("userTask")
@Context
class ZeebeUserTaskWorkerConfiguration {
    /**
     * The Zeebe Task Topic that will be queried for.  Represents the "User Task" type.
     */
    var taskType: String = "user-task"

    /**
     * The worker name that Zeebe Jobs will be locked with.
     */
    var workerName: String = "user-task-worker:${UUID.randomUUID()}"

    /**
     * The Zeebe job exclusiveLockTimeout value.  Represents the User Task lock.
     */
    var taskMaxZeebeLock: Duration = Duration.ofDays(30)

    /**
     * The maximum number of Jobs that can be locked on each poll request.
     */
    var maxBatchSize: Int = 1
}