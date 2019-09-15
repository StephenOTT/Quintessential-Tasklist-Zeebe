package com.github.stephenott.usertask;

import com.github.stephenott.usertask.entity.UserTaskEntity;
import com.github.stephenott.usertask.mongo.MongoManager;
import com.github.stephenott.usertask.mongo.Subscribers.AsyncResultSubscriber;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.stephenott.usertask.DbActionResult.FailedAction;
import static com.github.stephenott.usertask.DbActionResult.SuccessfulAction;

public class UserTaskActionsVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(UserTaskActionsVerticle.class);

    private EventBus eb;

    private MongoCollection<UserTaskEntity> tasksCollection = MongoManager.getDatabase().getCollection("tasks", UserTaskEntity.class);

    @Override
    public void start() throws Exception {
        log.info("Starting UserTaskActionsVerticle");

        eb = vertx.eventBus();

        establishCompleteActionConsumer();
        establishGetActionConsumer();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void establishCompleteActionConsumer() {
        String address = "ut.action.complete";

        eb.<CompletionRequest>consumer(address, ebHandler -> {

            completeTask(ebHandler.body()).setHandler(mHandler -> {

                if (mHandler.succeeded()) {
                    ebHandler.reply(SuccessfulAction(Collections.singletonList(mHandler.result())));
                    log.info("Document was updated with Task Completion, new doc: " + mHandler.result().toString());

                } else {
                    ebHandler.reply(FailedAction(mHandler.cause()));
                    log.error("Could not complete Mongo command to Update doc to COMPLETE", mHandler.cause());
                }
            });

        }).exceptionHandler(error -> log.error("Could not read eb message", error));
    }

    private void establishGetActionConsumer() {
        String address = "ut.action.get";

        eb.<GetRequest>consumer(address, ebHandler -> {

            getTasks(ebHandler.body()).setHandler(mHandler -> {

                if (mHandler.succeeded()) {
                    log.info("Get Tasks command was completed");
                    ebHandler.reply(SuccessfulAction(mHandler.result()));

                } else {
                    log.error("Could not complete Mongo command to Get Tasks", mHandler.cause());
                    ebHandler.reply(FailedAction(mHandler.cause()));

                }
            });

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

    private Future<List<UserTaskEntity>> getTasks(GetRequest getRequest) {
        Promise<List<UserTaskEntity>> promise = Promise.promise();

        List<Bson> findQueryItems = new ArrayList<>();
        log.info("GET REQUEST: " + getRequest.toJsonObject().toString());

        getRequest.getTaskId().ifPresent(v -> findQueryItems.add(Filters.eq(v)));
        getRequest.getState().ifPresent(v -> findQueryItems.add(Filters.eq("state", v.toString())));
        getRequest.getTitle().ifPresent(v -> findQueryItems.add(Filters.eq("title", v)));
        getRequest.getAssignee().ifPresent(v -> findQueryItems.add(Filters.eq("assignee", v)));
        getRequest.getDueDate().ifPresent(v -> findQueryItems.add(Filters.eq("dueDate", v)));
        getRequest.getBpmnProcessId().ifPresent(v -> findQueryItems.add(Filters.eq("bpmnProcessId", v)));
        getRequest.getZeebeJobKey().ifPresent(v -> findQueryItems.add(Filters.eq("zeebeJobKey", v)));
        getRequest.getZeebeSource().ifPresent(v -> findQueryItems.add(Filters.eq("zeebeSource", v)));

        Bson queryFilter = (findQueryItems.isEmpty()) ? null : Filters.and(findQueryItems);

        tasksCollection.find().filter(queryFilter).subscribe(new AsyncResultSubscriber<UserTaskEntity>().setOnCompleteHandler(ar -> {
                    if (ar.succeeded()) {
                        promise.complete(ar.result());
                    } else {
                        promise.fail(ar.cause());
                    }
                }));

        return promise.future();
    }

}
