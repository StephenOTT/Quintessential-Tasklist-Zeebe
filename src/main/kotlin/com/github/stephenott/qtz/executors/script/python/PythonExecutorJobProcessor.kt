package com.github.stephenott.qtz.executors.script.python

import com.github.stephenott.qtz.executors.JobProcessor
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.zeebe.client.api.response.ActivatedJob
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonExecutorJobProcessor : JobProcessor {

    @Inject
    private lateinit var pythonConfigWorker: PythonExecutorWorkerConfiguration

    @Inject
    lateinit var executor: PythonExecutor

    override fun processJob(job: ActivatedJob): Single<Map<String, Any?>> {
        return Single.fromCallable {
            println("Processing Python Job ${job.key}...")

            val fileName = job.customHeaders["script"] ?: throw IllegalArgumentException("missing script parameter in job header")

            val scriptFile = File(pythonConfigWorker.scriptFolder, fileName)

            println("VARIABLES1: ${job.variables}")

            val inputs = job.variablesAsMap

            executor.execute(scriptFile, inputs)
                    .doOnSuccess {
                        println("Result: $it")

                    }.doOnError {
                        println("Script Execution resulted in a error (script may have successfully executed, but parsing result failed: see stacktrace.)")
                        it.printStackTrace()

                    }.subscribeOn(Schedulers.io()).blockingGet()
        }
    }
}