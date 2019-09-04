package com.github.stephenott.usertask;


import com.github.stephenott.Common;
import com.github.stephenott.configuration.ApplicationConfiguration;
import com.github.stephenott.DoneJob;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

public class UserTaskExecutorVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(UserTaskExecutorVerticle.class);

    private EventBus eb;
    private ApplicationConfiguration.UserTaskExecutorConfiguration utWorkerConfig;

    @Override
    public void start() throws Exception {
        utWorkerConfig = config().mapTo(ApplicationConfiguration.UserTaskExecutorConfiguration.class);

        eb = vertx.eventBus();

        String address = Common.JOB_ADDRESS_PREFIX + utWorkerConfig.getAddress();

        eb.<JsonObject>consumer(address, handler -> {

            log.info("User Task has captured some Work...");

            String sourceClient = handler.headers().get("sourceClient");

            UserTaskConfiguration utConfig = handler.body()
                    .getJsonObject("customHeaders")
                    .mapTo(UserTaskConfiguration.class);

            UserTaskEntity utEntity = new UserTaskEntity()
                    .setZeebeSource(sourceClient)
                    .setTaskId("user-task--" + UUID.randomUUID().toString())
                    .setTitle(utConfig.getTitle())
                    .setDescription(utConfig.getDescription())
                    .setPriority(utConfig.getPriority())
                    .setAssignee(utConfig.getAssignee())
                    .setCandidateGroups((utConfig.getCandidateGroups() != null) ? new HashSet<String>(Arrays.asList(utConfig.getCandidateGroups().split(","))):null)
                    .setCandidateUsers((utConfig.getCandidateUsers() != null)?new HashSet<String>(Arrays.asList(utConfig.getCandidateUsers().split(","))):null)
                    .setDueDate((utConfig.getDueDate() != null)?Instant.parse(utConfig.getDueDate()): null)
                    .setFormKey(utConfig.getFormKey())
                    .setZeebeDeadline(Instant.ofEpochMilli(handler.body().getLong("deadline")))
                    .setZeebeJobKey(handler.body().getLong("key"))
                    .setBpmnProcessId(handler.body().getString("bpmnProcessId"))
                    .setZeebeVariables(((JsonObject)Json.decodeValue(handler.body().getString("variables"))).getMap());

            log.info("User Task created: {}", JsonObject.mapFrom(utEntity).toString());

            DoneJob doneJob = new DoneJob(
                    utEntity.getZeebeJobKey(),
                    DoneJob.Result.COMPLETE, 0);

            eb.send(sourceClient + ".job-action.completion", doneJob.toJsonObject());

        });

        log.info("User Task Verticle consuming tasks at: {}", utWorkerConfig.getAddress());
    }

    public void saveEntitytoDb(UserTaskEntity entity){

    }
}
