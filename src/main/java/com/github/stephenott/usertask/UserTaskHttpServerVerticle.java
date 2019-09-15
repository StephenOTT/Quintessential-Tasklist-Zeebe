package com.github.stephenott.usertask;

import com.github.stephenott.form.validator.ValidationRequest;
import com.github.stephenott.form.validator.ValidationRequestResult;
import com.github.stephenott.form.validator.ValidationSchemaObject;
import com.github.stephenott.form.validator.ValidationSubmissionObject;
import com.github.stephenott.usertask.entity.FormSchemaEntity;
import com.github.stephenott.usertask.entity.UserTaskEntity;
import com.github.stephenott.usertask.mongo.MongoManager;
import com.github.stephenott.usertask.mongo.Subscribers;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.github.stephenott.usertask.UserTaskHttpServerVerticle.HttpUtils.addCommonHeaders;

public class UserTaskHttpServerVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(UserTaskHttpServerVerticle.class);

    private EventBus eb;

    private MongoCollection<FormSchemaEntity> formsCollection = MongoManager.getDatabase().getCollection("forms", FormSchemaEntity.class);
    private MongoCollection<UserTaskEntity> tasksCollection = MongoManager.getDatabase().getCollection("tasks", UserTaskEntity.class);

    @Override
    public void start() throws Exception {
        int port = 8088;

        log.info("Starting UserTaskHttpServerVerticle on port: {}", port);

        eb = vertx.eventBus();

        Router mainRouter = Router.router(vertx);

        HttpServer server = vertx.createHttpServer();

        mainRouter.route().failureHandler(failure -> {
            int statusCode = failure.statusCode();

            HttpServerResponse response = failure.response();
            response.setStatusCode(statusCode)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("DOG-task: " + failure.failure().getLocalizedMessage());
        });

        mainRouter.errorHandler(500, rc -> {
            log.error("HTTP FAILURE!!!", rc.failure());
            rc.fail(500);
        });

        establishCompleteActionRoute(mainRouter);
        establishGetTasksRoute(mainRouter);
        establishSubmitTaskRoute(mainRouter);
        establishSaveFormSchemaRoute(mainRouter);

        server.requestHandler(mainRouter)
                .listen(port);

    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void establishSaveFormSchemaRoute(Router router){
        //@TODO move to common
        String path = "/forms/schema";

        Route saveFormSchemaRoute = router.post(path)
                .handler(BodyHandler.create()); //@TODO add cors

        saveFormSchemaRoute.handler(rc -> {
            FormSchemaEntity formSchemaEntity = rc.getBodyAsJson().mapTo(FormSchemaEntity.class);

            ReplaceOptions options = new ReplaceOptions().upsert(true);

            Bson findQuery = Filters.eq(formSchemaEntity.getId());

            formsCollection.replaceOne(findQuery, formSchemaEntity, options)
                    .subscribe(new Subscribers.AsyncResultSubscriber<>(handler -> {
                        if (handler.succeeded()){
                            if (handler.result().size() == 1){

                                addCommonHeaders(rc.response());
                                rc.response()
                                        .setStatusCode(201)
                                        .end(new JsonObject().put("id", handler.result().get(0).getUpsertedId().asString().getValue()).toBuffer());

                            } else {
                                throw new IllegalStateException("Unable to complete Form Schema Save, multiple documents were returned instead of 1.");
                            }

                        } else {
                            throw new IllegalStateException("Unable to complete Form Schema Save");
                        }
            }));
        });
    }

    private void establishCompleteActionRoute(Router router) {
        //@TODO move to common
        String path = "/task/complete";

        Route completeRoute = router.post(path)
                .handler(BodyHandler.create()); //@TODO add cors

        completeRoute.handler(rc -> {
            CompletionRequest completionRequest = rc.getBodyAsJson().mapTo(CompletionRequest.class);

            //@TODO move to common
            String address = "ut.action.complete";

            eb.<DbActionResult>request(address, completionRequest, reply -> {
                if (reply.succeeded()) {
                    addCommonHeaders(rc.response());

                    if (reply.result().body().getResultObject().size() == 1) {
                        rc.response().end(JsonObject.mapFrom(reply.result().body().getResultObject().get(0)).toBuffer());
                    } else {
                        log.error("No objects were returned in the resultObject of the Complete request");
                        throw new IllegalStateException("Something went wrong");
                    }

                } else {
                    throw new IllegalStateException(reply.cause());
                }
            });

        });


    }

    private void establishGetTasksRoute(Router router) {
        //@TODO move to common
        String address = "ut.action.get";
        //@TODO move to common
        String path = "/task";

        Route getRoute = router.get(path)
                .handler(BodyHandler.create()); //@TODO add cors

        getRoute.handler(rc -> {
            GetRequest getRequest = rc.getBodyAsJson().mapTo(GetRequest.class);

            eb.<DbActionResult>request(address, getRequest, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().body().getResult().equals(DbActionResult.ActionResult.SUCCESS)) {
                        addCommonHeaders(rc.response());
                        rc.response().end(new JsonArray(reply.result().body().getResultObject()).toBuffer());

                    } else {
                        log.error("FIND FAILED: " + JsonObject.mapFrom(reply.result().body().getError()).toString());
                        throw new IllegalStateException("Find Failed: " + reply.result().body().getError().getLocalizedMessage());
                    }

                } else {
                    throw new IllegalStateException(reply.cause());
                }
            });

        });
    }


    private void establishSubmitTaskRoute(Router router) {
        String path = "/task/id/:taskId/submit";

        Route submitRoute = router.post(path)
                .handler(BodyHandler.create()); //@TODO add cors

        submitRoute.handler(rc -> {
            ValidationSubmissionObject submissionObject = rc.getBodyAsJson().mapTo(ValidationSubmissionObject.class);

            String taskId = Optional.of(rc.request().getParam("taskId"))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid task id"));

            log.info("TASK ID: " + taskId);

            //@TODO look at refactor with more fluent and compose
            getFormKeyFromUserTask(taskId).setHandler(taskIdHandler -> {
                if (taskIdHandler.succeeded()) {

                    log.info("Found Form Key: " + taskIdHandler.result().getFormKey());

                    getFormSchemaByFormKey(taskIdHandler.result().getFormKey()).setHandler( formKeyHandler -> {
                        if (formKeyHandler.succeeded()){

                            String ebAddress = "forms.action.validate";

                            ValidationSchemaObject schema = new JsonObject(formKeyHandler.result().getSchema()).mapTo(ValidationSchemaObject.class);

                            ValidationRequest validationRequest = new ValidationRequest()
                                    .setSchema(schema)
                                    .setSubmission(submissionObject);

                            eb.<ValidationRequestResult>request(ebAddress, validationRequest, ebHandler ->{
                                if (ebHandler.succeeded()){

                                    if (ebHandler.result().body().getResult().equals(ValidationRequestResult.Result.VALID)){
                                        rc.response()
                                                .setStatusCode(202)
                                                .putHeader("content-type", "application/json; charset=utf-8")
                                                .end(JsonObject.mapFrom(ebHandler.result().body().getValidResultObject()).toBuffer());

                                    } else {
                                        rc.response()
                                                .setStatusCode(400)
                                                .putHeader("content-type", "application/json; charset=utf-8")
                                                .end(JsonObject.mapFrom(ebHandler.result().body().getInvalidResultObject()).toBuffer());
                                    }
                                } else {
                                    throw new IllegalStateException("Did not receive a message back from the validation service");
                                }
                            });

                        } else {
                            throw new IllegalArgumentException("Unable to find Form Schema for provided Form key");
                        }
                    });
                } else {
                    throw new IllegalArgumentException("Unable to find User Task for provided Task ID");
                }
            });


        });
    }

    private Future<UserTaskEntity> getFormKeyFromUserTask(String taskId) {
        //@TODO look at moving this into the UserTaskActionsVerticle
        Promise<UserTaskEntity> promise = Promise.promise();

        // @TODO future refactor to use a projection to only return the single Form Key field rather than the entire entity.

        if (taskId == null) {
            promise.fail(new IllegalArgumentException("taskId cannot be null"));
        }

        Bson findQuery = Filters.eq(taskId);

        tasksCollection.find().filter(findQuery)
                .subscribe(new Subscribers.AsyncResultSubscriber<>(result -> {
                    if (result.succeeded()) {

                        if (result.result().size() == 1) {
                            promise.complete(result.result().get(0));

                        } else {
                            log.error("DB Result returned more than 1 User Task Entity for the Task Id... something went wrong");
                            promise.fail(new IllegalStateException("DB Result returned more than 1 User Task... something went wrong"));
                        }

                    } else {
                        log.error("Mongo Query did not complete for Finding User Task by ID", result.cause());
                        promise.fail(result.cause());
                    }
                }));
        return promise.future();
    }

    private Future<FormSchemaEntity> getFormSchemaByFormKey(String formKey) {
        //@TODO look at moving this its own verticle for a FormSchemaEntity Verticle
        Promise<FormSchemaEntity> promise = Promise.promise();

        if (formKey == null) {
            promise.fail(new IllegalArgumentException("formKey cannot be null"));
        }

        Bson findQuery = Filters.eq("key", formKey);

        formsCollection.find().filter(findQuery)
                .subscribe(new Subscribers.AsyncResultSubscriber<>(result -> {
                    if (result.succeeded()) {

                        if (result.result().size() == 1) {
                            log.info("ENTITY ID: " + result.result().get(0).getId());
                            log.info("SCHEMA FROM ENTITY RAW: " + result.result().get(0).getSchema());
                            promise.complete(result.result().get(0));

                        } else {
                            log.error("DB Result returned more than 1 Form Schema for the Form Key... something went wrong");
                            promise.fail(new IllegalStateException("DB Result returned more than 1 Form Schema... something went wrong"));
                        }

                    } else {
                        log.error("Mongo Query did not complete for Finding Form Schema", result.cause());
                        promise.fail(result.cause());
                    }
                }));
        return promise.future();
    }

    private void establishGetTasksRoute() {
        //@TODO
    }

    private void establishDeleteTaskRoute() {
        //@TODO
    }

    private void establishClaimTaskRoute() {
        //@TODO
    }

    private void establishUnClaimTaskRoute() {
        //@TODO
    }

    private void establishAssignTaskRoute() {
        //@TODO
    }

    private void establishCreateCustomTaskRoute() {
        //@TODO
        // Will use a custom BPMN that allows a custom single step task to be created.
        // Create a config for this so the BPMN Process ID can be set in the YAML config
    }


    public static class HttpUtils {

        public static String applicationJson = "application/json";

        public static HttpServerResponse addCommonHeaders(HttpServerResponse httpServerResponse) {
            httpServerResponse.headers()
                    .add("content-type", applicationJson);

            return httpServerResponse;
        }

    }

}
