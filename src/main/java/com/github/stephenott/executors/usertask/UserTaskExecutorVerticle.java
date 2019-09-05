package com.github.stephenott.executors.usertask;

import com.github.stephenott.common.Common;
import com.github.stephenott.conf.ApplicationConfiguration;
import com.github.stephenott.executors.JobResult;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

public class UserTaskExecutorVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(UserTaskExecutorVerticle.class);

    private EventBus eb;

    private ApplicationConfiguration.UserTaskExecutorConfiguration utExecutorConfig;

    MongoClient mClient;

    @Override
    public void start() throws Exception {
        utExecutorConfig = config().mapTo(ApplicationConfiguration.UserTaskExecutorConfiguration.class);

        JsonObject mongoConfig = new JsonObject();
        mClient = MongoClient.createShared(vertx, mongoConfig);

        eb = vertx.eventBus();

        String address = Common.JOB_ADDRESS_PREFIX + utExecutorConfig.getAddress();

        eb.<JsonObject>consumer(address, handler -> {

            log.info("User Task({}) has captured some Work.", address);

            String sourceClient = handler.headers().get("sourceClient");
            //@TODO add handler if sourceClient is missing then reject job.

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

            saveEntitytoDb(utEntity);

            JobResult jobResult = new JobResult(
                    utEntity.getZeebeJobKey(),
                    JobResult.Result.COMPLETE, 0);

            eb.send(sourceClient + ".job-action.completion", jobResult.toJsonObject());

        });

        log.info("User Task Executor Verticle consuming tasks at: {}", utExecutorConfig.getAddress());
    }

    public void saveEntitytoDb(UserTaskEntity entity){
        mClient.save("tasks", entity.toMongoJson(), res -> {
            if (res.succeeded()) {
                String id = res.result();
                log.info("Saved entity with id " + id);

            } else {
                log.error("could not save entity",res.cause());
            }
        });
    }
}
