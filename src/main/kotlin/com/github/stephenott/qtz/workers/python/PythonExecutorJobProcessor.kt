package com.github.stephenott.qtz.workers.python

import com.github.stephenott.qtz.executors.script.python.PythonExecutor
import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.workers.JobProcessor
import com.github.stephenott.qtz.workers.JobResult
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.zeebe.client.api.response.ActivatedJob
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonExecutorJobProcessor : JobProcessor {

    @Inject
    private lateinit var workerConfig: PythonExecutorWorkerConfiguration

    @Inject
    lateinit var executor: PythonExecutor

    override fun processJob(job: ActivatedJob): Single<JobResult> {
        return Single.fromCallable {
            println("${workerConfig.workerName} Processing Python Job ${job.key}...")

            val fileName = job.customHeaders["script"] ?: throw IllegalArgumentException("missing script parameter in job header")

            val scriptFile = File(workerConfig.scriptFolder, fileName)

            val inputs = job.variablesAsMap

            val executionResult = executor.execute(scriptFile, inputs)
                    .doOnSuccess {
                        println("${workerConfig.workerName} Python Execution Result: $it")

                    }.doOnError {
                        println("${workerConfig.workerName} Python Script Execution resulted in a error (script may have successfully executed, but parsing result failed: see stacktrace.)")
                        it.printStackTrace()

                    }.subscribeOn(Schedulers.io()).blockingGet()

            JobResult(resultVariables = ZeebeVariables(executionResult))
        }
    }
}