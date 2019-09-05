package com.github.stephenott.executors.polyglot;

import com.github.stephenott.common.Common;
import com.github.stephenott.executors.JobResult;
import com.github.stephenott.conf.ApplicationConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(ExecutorVerticle.class);

    private EventBus eb;
    private ApplicationConfiguration.ExecutorConfiguration executorConfiguration;

    @Override
    public void start() throws Exception {
        executorConfiguration = config().mapTo(ApplicationConfiguration.ExecutorConfiguration.class);

        eb = vertx.eventBus();

        String address = Common.JOB_ADDRESS_PREFIX + executorConfiguration.getAddress();

        eb.<JsonObject>consumer(address, handler -> {
            log.info("doing some work!!!");

            String sourceClient = handler.headers().get("sourceClient");

            JsonObject job = handler.body();

            //@TODO Add polyexecutor

            JobResult jobResult = new JobResult(
                    job.getLong("key"),
                    JobResult.Result.COMPLETE,
                    (job.getInteger("retries") > 0) ? job.getInteger("retries") - 1 : 0);

            eb.send(sourceClient + ".job-action.completion", jobResult.toJsonObject());

        });
    }
}
