package com.github.stephenott.qtz.workers.usertask

import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTaskWorkerStartupListener : ApplicationEventListener<ServerStartupEvent> {

    @Inject
    lateinit var userTaskWorker: UserTaskZeebeWorker

    @Inject
    lateinit var workerConfig: ZeebeUserTaskWorkerConfiguration

    override fun onApplicationEvent(event: ServerStartupEvent?) {
        if (workerConfig.enabled) {
            println("Creating and Starting User Task Worker...")

            userTaskWorker.create().start()
                    .doOnComplete {
                        println("User Task Worker has started")
                    }
                    .subscribeOn(Schedulers.io()).subscribe()

        } else {
            println("User Task Worker is disabled in configuration.")
        }
    }
}