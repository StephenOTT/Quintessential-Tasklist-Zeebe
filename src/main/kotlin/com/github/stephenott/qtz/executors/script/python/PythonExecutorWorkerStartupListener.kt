package com.github.stephenott.qtz.executors.script.python

import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonExecutorWorkerStartupListener : ApplicationEventListener<ServerStartupEvent> {

    @Inject
    lateinit var pythonExecutorWorker: PythonExecutorZeebeWorker

    @Inject
    lateinit var workerConfig: PythonExecutorWorkerConfiguration

    override fun onApplicationEvent(event: ServerStartupEvent?) {
        if (workerConfig.enabled) {
            println("Creating and Starting Python Executor Worker...")

            pythonExecutorWorker.create().start()
                    .doOnComplete {
                        println("Python Executor Worker has started")
                    }
                    .subscribeOn(Schedulers.io()).subscribe()

        } else {
            println("Python Executor Worker is disabled in configuration.")
        }
    }
}