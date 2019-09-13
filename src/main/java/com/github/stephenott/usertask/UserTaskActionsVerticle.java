package com.github.stephenott.usertask;

import com.github.stephenott.usertask.mongo.MongoManager;
import com.github.stephenott.usertask.mongo.Subscribers.AsyncResultSubscriber;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserTaskActionsVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(UserTaskActionsVerticle.class);

    private EventBus eb;

    private MongoCollection<UserTaskEntity> tasksCollection = MongoManager.getDatabase().getCollection("tasks", UserTaskEntity.class);

    @Override
    public void start() throws Exception {

        log.info("Starting UserTaskActionsVerticle");

        eb = vertx.eventBus();

        JsonObject mongoConfig = new JsonObject();

        establishCompleteActionConsumer();


    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void establishCompleteActionConsumer() {

        String address = "ut.action.complete";

        eb.<JsonObject>consumer(address, ebHandler -> {
            CompletionRequest completionRequest;
            try {
                completionRequest = ebHandler.body().mapTo(CompletionRequest.class);

                completeTask(completionRequest).setHandler(mHandler -> {
                    if (mHandler.succeeded()) {
                        ebHandler.reply(JsonObject.mapFrom(mHandler.result())); //@TODO
                        log.info("Document was updated with Task Completion, new doc: " + mHandler.result().toString());
                    } else {
                        log.error("Could not complete Mongo command to Update doc to COMPLETE", mHandler.cause());
                    }
                });

            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse JSON into CompletionRequest");
            }

        });

    }

    private Future<UserTaskEntity> completeTask(CompletionRequest completionRequest) {
        Promise<UserTaskEntity> promise = Promise.promise();

        Bson findQuery = Filters.and(
                Filters.eq("zeebeSource", completionRequest.getZeebeSource()),
                Filters.eq("zeebeJobKey", completionRequest.getZeebeJobKey())
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
                            promise.fail("Update did not return proper result");
                        }
                    } else {
                        promise.fail(ar.cause());
                    }
                }));

        return promise.future();
    }

}
