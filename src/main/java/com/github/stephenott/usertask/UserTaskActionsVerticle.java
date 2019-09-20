package com.github.stephenott.usertask;

import com.github.stephenott.usertask.entity.FormSchemaEntity;
import com.github.stephenott.usertask.entity.UserTaskEntity;
import com.github.stephenott.usertask.mongo.MongoManager;
import com.github.stephenott.usertask.mongo.Subscribers.SimpleSubscriber;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.github.stephenott.usertask.DbActionResult.FailedAction;
import static com.github.stephenott.usertask.DbActionResult.SuccessfulAction;

public class UserTaskActionsVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(UserTaskActionsVerticle.class);

    private EventBus eb;

    private MongoCollection<UserTaskEntity> tasksCollection = MongoManager.getDatabase().getCollection("tasks", UserTaskEntity.class);
    private MongoCollection<FormSchemaEntity> formsCollection = MongoManager.getDatabase().getCollection("forms", FormSchemaEntity.class);

    @Override
    public void start() throws Exception {
        log.info("Starting UserTaskActionsVerticle");

        eb = vertx.eventBus();

        establishCompleteActionConsumer();
        establishGetActionConsumer();
        establishGetFormSchemaWithDefaultsForTaskIdConsumer();
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

    private void establishGetFormSchemaWithDefaultsForTaskIdConsumer() {
        String address = "ut.action.get-form-schema-with-defaults";

        eb.<GetTasksFormSchemaReqRes.Request>consumer(address, ebHandler -> {

            getFormSchemaWithDefaultsForTaskId(ebHandler.body()).setHandler(mHandler -> {

                if (mHandler.succeeded()) {
                    log.info("Get Form Schema With Defaults for Task ID completed");
                    ebHandler.reply(SuccessfulAction(Collections.singletonList(mHandler.result())));

                } else {
                    log.error("Could not complete Get Form Schema with Defaults for Task ID", mHandler.cause());
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

        Document doc = new Document();
        doc.putAll(completionRequest.getCompletionVariables());

        Bson updateDoc = Updates.combine(
                Updates.set("state", UserTaskEntity.State.COMPLETED.toString()),
                Updates.set("completeVariables", doc),
                Updates.currentDate("completedAt")
        );

        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER);

        tasksCollection.findOneAndUpdate(findQuery, updateDoc, options)
                .subscribe(new SimpleSubscriber<UserTaskEntity>().singleResult(ar -> {
                    if (ar.succeeded()) {
                        promise.complete(ar.result());
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

        tasksCollection.find().filter(queryFilter)
                .subscribe(new SimpleSubscriber<>(ar -> {
                    if (ar.succeeded()) {
                        promise.complete(ar.result());

                    } else {
                        promise.fail(ar.cause());
                    }
                }));
        return promise.future();
    }

    public Future<GetTasksFormSchemaReqRes.Response> getFormSchemaWithDefaultsForTaskId(GetTasksFormSchemaReqRes.Request request) {
        Promise<GetTasksFormSchemaReqRes.Response> promise = Promise.promise();

        promise.future().compose(task -> {
            Promise<String> stepProm = Promise.promise();

            tasksCollection.find().filter(Filters.eq(request.getTaskId()))
                    .subscribe(new SimpleSubscriber<UserTaskEntity>().singleResult(result -> {
                        if (result.succeeded()) {
                            stepProm.complete(result.result().getFormKey());

                        } else {
                            stepProm.fail(result.cause());
                        }
                    }));
            return stepProm.future();

        }).compose(formKey -> {
            Promise<FormSchemaEntity> stepProm = Promise.promise();

            formsCollection.find().filter(Filters.eq(request.getTaskId()))
                    .subscribe(new SimpleSubscriber<FormSchemaEntity>().singleResult(onDone -> {
                        if (onDone.succeeded()) {
                            stepProm.complete(onDone.result());

//                        } else {
                            stepProm.fail(onDone.cause());
                        }
                    }));
            return stepProm.future();

        }).compose(formSchema -> {
            promise.complete(
                    new GetTasksFormSchemaReqRes.Response()
                            .setDefaultValues(new HashMap<>())
                            .setFormKey(formSchema.getKey())
                            .setSchema(new JsonObject(formSchema.getSchema()).getMap())
                            .setTaskId(request.getTaskId())
            );
            return Future.succeededFuture();
        });

        return promise.future();
    }

}
