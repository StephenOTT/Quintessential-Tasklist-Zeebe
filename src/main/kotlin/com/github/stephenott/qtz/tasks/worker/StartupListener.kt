package com.github.stephenott.qtz.tasks.worker

import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupListener : ApplicationEventListener<ServerStartupEvent> {

    @Inject
    lateinit var worker: UserTaskZeebeWorker

    override fun onApplicationEvent(event: ServerStartupEvent?) {
        println("Creating User Task Worker...")
//        Completable.fromAction {
        worker.start().doOnComplete {
            println("Worker has started")
        }.subscribeOn(Schedulers.io()).subscribe()
    }
}