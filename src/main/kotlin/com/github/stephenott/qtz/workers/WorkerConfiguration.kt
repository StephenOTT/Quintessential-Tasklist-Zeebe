package com.github.stephenott.qtz.workers

import java.time.Duration

interface WorkerConfiguration {
    var enabled: Boolean
    var taskType: String
    var workerName: String
    var taskMaxZeebeLock: Duration
    var maxBatchSize: Int
}