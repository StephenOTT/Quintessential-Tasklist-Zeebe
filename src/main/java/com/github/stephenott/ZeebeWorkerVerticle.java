package com.github.stephenott;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ZeebeWorkerVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger("Worker-Verticle");
    private EventBus eb;
    private String verticleId = vertx.getOrCreateContext().deploymentID();

    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();

        log.info("Worker Deployed on: " + verticleId);

        //@TODO make this dynamic based on config
        eb.<JsonObject>consumer("job-job1", handler -> {

            JsonObject job = handler.body();

            DoneJob doneJob = new DoneJob(job.getLong("key"),
                    DoneJob.Result.COMPLETE,
                    (job.getInteger("retries") > 0) ? job.getInteger("retries") - 1 : 0);

            handler.reply(JsonObject.mapFrom(doneJob));

        });
    }
}