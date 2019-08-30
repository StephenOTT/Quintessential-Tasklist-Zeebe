package com.github.stephenott;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorVerticle extends AbstractVerticle {

    private Logger log;
    private EventBus eb;
    private ApplicationConfiguration.ExecutorConfiguration executorConfiguration;

    @Override
    public void start() throws Exception {
        executorConfiguration = config().mapTo(ApplicationConfiguration.ExecutorConfiguration.class);

        log = LoggerFactory.getLogger("WorkerVerticle." + executorConfiguration.getName());

        eb = vertx.eventBus();

        eb.<JsonObject>consumer(Common.JOB_ADDRESS_PREFIX + executorConfiguration.getAddress(), handler -> {
            log.info("doing some work!!!");
            JsonObject job = handler.body();

            //@TODO Add polyexecutor

            DoneJob doneJob = new DoneJob(
                    job.getLong("key"),
                    DoneJob.Result.COMPLETE,
                    (job.getInteger("retries") > 0) ? job.getInteger("retries") - 1 : 0);

            handler.reply(JsonObject.mapFrom(doneJob));

        });
    }
}