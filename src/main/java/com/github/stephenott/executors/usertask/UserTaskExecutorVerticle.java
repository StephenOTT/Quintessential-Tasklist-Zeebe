package com.github.stephenott.executors.usertask;

import com.github.stephenott.common.Common;
import com.github.stephenott.conf.ApplicationConfiguration;
import com.github.stephenott.executors.JobResult;
import com.github.stephenott.usertask.mongo.MongoManager;
import com.github.stephenott.usertask.entity.UserTaskEntity;
import com.github.stephenott.usertask.mongo.Subscribers.AsyncResultSubscriber;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.Success;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import static com.github.stephenott.usertask.mongo.Subscribers.AsyncResultSubscriber.isSuccessResult;

public class UserTaskExecutorVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(UserTaskExecutorVerticle.class);

    private EventBus eb;

    private ApplicationConfiguration.UserTaskExecutorConfiguration utExecutorConfig;

    private MongoCollection<UserTaskEntity> tasksCollection = MongoManager.getDatabase().getCollection("tasks", UserTaskEntity.class);

    @Override
    public void start() throws Exception {
        try{
            utExecutorConfig = config().mapTo(ApplicationConfiguration.UserTaskExecutorConfiguration.class);
        } catch (Exception e){
            log.error("Unable to parse Ut Executor Config", e);
            throw e;
        }

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
                    .setCandidateGroups((utConfig.getCandidateGroups() != null) ? new HashSet<String>(Arrays.asList(utConfig.getCandidateGroups().split(","))) : null)
                    .setCandidateUsers((utConfig.getCandidateUsers() != null) ? new HashSet<String>(Arrays.asList(utConfig.getCandidateUsers().split(","))) : null)
                    .setDueDate((utConfig.getDueDate() != null) ? Instant.parse(utConfig.getDueDate()) : null)
                    .setFormKey(utConfig.getFormKey())
                    .setZeebeDeadline(Instant.ofEpochMilli(handler.body().getLong("deadline")))
                    .setZeebeJobKey(handler.body().getLong("key"))
                    .setBpmnProcessId(handler.body().getString("bpmnProcessId"))
                    .setBpmnProcessVersion(handler.body().getInteger("workflowDefinitionVersion"))
                    .setZeebeVariables(((JsonObject) Json.decodeValue(handler.body().getString("variables"))).getMap())
                    .setTaskOriginalCapture(Instant.now());

            log.info("User Task created: {}", JsonObject.mapFrom(utEntity).toString());

            saveToDb(utEntity).setHandler(res -> {
                if (res.succeeded()) {
                    log.info("UserTaskEntity has been saved to DB...");
                    JobResult jobResult = new JobResult(
                            utEntity.getZeebeJobKey(),
                            JobResult.Result.COMPLETE, 0);

                    eb.send(sourceClient + ".job-action.completion", jobResult.toJsonObject());

                } else {
                    //@TODO update with better error
                    throw new IllegalStateException("Unable to save to DB", res.cause());
                }
            });

        });

        log.info("User Task Executor Verticle consuming tasks at: {}", utExecutorConfig.getAddress());
    }

    public Future<Void> saveToDb(UserTaskEntity entity) {
        Promise<Void> promise = Promise.promise();

        tasksCollection.insertOne(entity)
                .subscribe(new AsyncResultSubscriber<Success>().setOnCompleteHandler(ar -> {
                    if (ar.succeeded()) {
                        if (isSuccessResult(ar.result())) {
                            promise.complete();

                        } else {
                            promise.fail(new IllegalStateException("DB did not return a Success for InsertOne."));
                        }
                    } else {
                        log.error("Could not insert doc...", ar.cause());
                        promise.fail(ar.cause());
                    }

                }).setOnSubscribeHandler(ar -> {
                    if (ar.succeeded()) {
                        log.info("InsertOne Subscription has been created.");
                    } else {
                        log.error("InsertOne Subscription failed", ar.cause());
                    }
                }));

        return promise.future();
    }
}
