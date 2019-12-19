package com.github.stephenott.qtz.executors.script.python

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.stephenott.qtz.executors.CodeExecutor
import com.github.stephenott.qtz.workers.python.PythonExecutorWorkerConfiguration
import io.reactivex.Single
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonExecutor: CodeExecutor {

    @Inject
    private lateinit var executorConfig: PythonExecutorWorkerConfiguration

    private val jsonMapper: ObjectMapper = jacksonObjectMapper()

    override fun execute(script: File,
                         inputs: Map<String, Any?>,
                         executionTimeout: Duration): Single<Map<String, Any?>> {
        return Single.fromCallable {
            val inputFile: File = File.createTempFile(UUID.randomUUID().toString(), "-python-executor-input" )

            jsonMapper.writeValue(inputFile, inputs)

            val executor: ExecutorService = Executors.newSingleThreadExecutor()

            val execution = executor.submit<Process> {
                val pythonPath = executorConfig.pythonPath
                val pythonFileToExecute = script.absolutePath
                ProcessBuilder(listOf(pythonPath, pythonFileToExecute, "--inputs", inputFile.absolutePath))
                        .start()
            }

            val process = execution.get(executionTimeout.seconds, TimeUnit.SECONDS)

            process.waitFor(executionTimeout.seconds, TimeUnit.SECONDS)

            val exitCode:Int = process.exitValue()

            if (exitCode == 0){
                val output = process.inputStream.bufferedReader().use { it.readText() }
                try{
                    inputFile.delete()
                    jsonMapper.readValue<Map<String, Any?>>(output)
                } catch(e: Exception) {
                    println("${executorConfig.workerName} Python Script Output: $output")
                    throw IllegalStateException("Script successfully executed but orchestrator failed to read result into JSON...", e)
                }

            } else {
                println("${executorConfig.workerName} Python Script Execution Exit Code: $exitCode")

                val errorOutput: String = process.errorStream.bufferedReader().use { it.readText() }
                println("${executorConfig.workerName} Python Script Execution returned a failing error code: $errorOutput")
                throw IllegalStateException("Failed Script Execution")
            }
        }
    }
}