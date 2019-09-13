package com.github.stephenott.usertask;

import com.github.stephenott.usertask.mongo.MongoManager;
import com.github.stephenott.usertask.mongo.Subscribers.AsyncResultSubscriber;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.stephenott.usertask.DbActionResult.ActionResult.*;

public class UserTaskActionsVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(UserTaskActionsVerticle.class);

    private EventBus eb;

    private MongoCollection<UserTaskEntity> tasksCollection = MongoManager.getDatabase().getCollection("tasks", UserTaskEntity.class);

    @Override
    public void start() throws Exception {

        log.info("Starting UserTaskActionsVerticle");

        eb = vertx.eventBus();


        establishCompleteActionConsumer();

    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void establishCompleteActionConsumer() {
        String address = "ut.action.complete";

        eb.<CompletionRequest>consumer(address, ebHandler -> {
//            CompletionRequest completionRequest;
//            try {
//                completionRequest = ebHandler.body().mapTo(CompletionRequest.class);

            completeTask(ebHandler.body()).setHandler(mHandler -> {
                DeliveryOptions options = new DeliveryOptions().setCodecName("com.github.stephenott.usertask.DbActionResult");
                if (mHandler.succeeded()) {
                    DbActionResult result = new DbActionResult(SUCCESS, JsonObject.mapFrom(mHandler.result()));
                    ebHandler.reply(result, options);
                    log.info("Document was updated with Task Completion, new doc: " + mHandler.result().toString());

                } else {
                    DbActionResult result = new DbActionResult(FAIL, mHandler.cause());
                    ebHandler.reply(result, options);
                    log.error("Could not complete Mongo command to Update doc to COMPLETE", mHandler.cause());
                }
            });

//            } catch (Exception e) {
//                throw new IllegalArgumentException("Unable to parse JSON into CompletionRequest");
//            }

        }).exceptionHandler(error -> log.error("Could not read eb message", error));

    }

    private Future<UserTaskEntity> completeTask(CompletionRequest completionRequest) {
        Promise<UserTaskEntity> promise = Promise.promise();

        Bson findQuery = Filters.and(
                Filters.eq("zeebeSource", completionRequest.getZeebeSource()),
                Filters.eq("zeebeJobKey", completionRequest.getZeebeJobKey()),
                Filters.ne("state", UserTaskEntity.State.COMPLETED.toString()) // Should not be able to complete a task that is already completed
        );

        Bson updateDoc = Updates.combine(
                Updates.set("state", UserTaskEntity.State.COMPLETED.toString()),
                Updates.set("completeVariables", completionRequest.getCompletionVariables()),
                Updates.currentDate("completedAt")
        );

        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER);

        tasksCollection.findOneAndUpdate(findQuery, updateDoc, options)
                .subscribe(new AsyncResultSubscriber<UserTaskEntity>().setOnCompleteHandler(ar -> {
                    if (ar.succeeded()) {
                        if (ar.result().size() == 1) {
                            promise.complete(ar.result().get(0));
                        } else {
                            promise.fail("Zero Tasks matched the find query. Possible incorrect source, job key, or task is already completed");
                            log.error("Unable to Complete the task, the findOneAndUpdate query returned 0 results: likely the find query failed to find a match.");
                        }
                    } else {
                        promise.fail(ar.cause());
                    }
                }));

        return promise.future();
    }

}
