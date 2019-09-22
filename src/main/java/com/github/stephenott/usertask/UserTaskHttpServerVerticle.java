package com.github.stephenott.usertask;

import com.github.stephenott.common.EventBusableReplyException;
import com.github.stephenott.conf.ApplicationConfiguration;
import com.github.stephenott.executors.JobResult;
import com.github.stephenott.form.validator.*;
import com.github.stephenott.form.validator.ValidationRequestResult.Result;
import com.github.stephenott.form.validator.exception.InvalidFormSubmissionException;
import com.github.stephenott.form.validator.exception.ValidationRequestResultException;
import com.github.stephenott.usertask.entity.FormSchemaEntity;
import com.github.stephenott.usertask.entity.UserTaskEntity;
import com.github.stephenott.usertask.mongo.MongoManager;
import com.github.stephenott.usertask.mongo.Subscribers.SimpleSubscriber;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.stephenott.usertask.DbActionResult.ActionResult.*;
import static com.github.stephenott.usertask.UserTaskHttpServerVerticle.HttpUtils.addCommonHeaders;

public class UserTaskHttpServerVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(UserTaskHttpServerVerticle.class);

    private EventBus eb;

    private ApplicationConfiguration.UserTaskHttpServerConfiguration serverConfiguration;

    private MongoCollection<FormSchemaEntity> formsCollection = MongoManager.getDatabase().getCollection("forms", FormSchemaEntity.class);
    private MongoCollection<UserTaskEntity> tasksCollection = MongoManager.getDatabase().getCollection("tasks", UserTaskEntity.class);


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        try {
            serverConfiguration = config().mapTo(ApplicationConfiguration.UserTaskHttpServerConfiguration.class);
        } catch (Exception e) {
            log.error("Unable to start User Task HTTP Server Verticle because config cannot be parsed", e);
            stop();
        }

        int port = serverConfiguration.getPort();

        log.info("Starting UserTaskHttpServerVerticle on port: {}", port);

        eb = vertx.eventBus();

        Router mainRouter = Router.router(vertx);

        HttpServer server = vertx.createHttpServer();

        mainRouter.route().failureHandler( rc -> {
            addCommonHeaders(rc.response());
            log.info("PROCESSING ERROR: " + rc.failure().getClass().getCanonicalName());
            rc.response().setStatusCode(rc.statusCode());
            rc.response().end(new JsonObject().put("error", rc.failure().getMessage()).toBuffer());
        });

        mainRouter.errorHandler(500, rc -> {
            log.error("HTTP FAILURE!!!", rc.failure());
            rc.fail(500);
        });

        establishCompleteActionRoute(mainRouter);
        establishGetTasksRoute(mainRouter);
        establishSubmitTaskRoute(mainRouter);
        establishSaveFormSchemaRoute(mainRouter);

        server.requestHandler(mainRouter).listen(port);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void establishSaveFormSchemaRoute(Router router) {
        //@TODO move to common
        String path = "/form/schema";

        Route saveFormSchemaRoute = router.post(path)
                .handler(BodyHandler.create()); //@TODO add cors

        saveFormSchemaRoute.handler(rc -> {
            FormSchemaEntity formSchemaEntity = rc.getBodyAsJson().mapTo(FormSchemaEntity.class);
            log.info("SCHEMA: " + formSchemaEntity.getSchema());
            ReplaceOptions options = new ReplaceOptions().upsert(true);

            Bson findQuery = Filters.eq(formSchemaEntity.getId());

            formsCollection.replaceOne(findQuery, formSchemaEntity, options)
                    .subscribe(new SimpleSubscriber<UpdateResult>().singleResult(handler -> {
                        if (handler.succeeded()) {
                            addCommonHeaders(rc.response());
                            rc.response()
                                    .setStatusCode(201)
                                    .end(new JsonObject()
                                            .put("id", handler.result().getUpsertedId().asString().getValue())
                                            .toBuffer());
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
//                    if (reply.result().body().getResult().equals(SUCCESS)) {
                        addCommonHeaders(rc.response());
                        rc.response().end(new JsonArray(reply.result().body().getResultObject()).toBuffer());

//                    } else {
//                        log.error("FIND FAILED: " + JsonObject.mapFrom(reply.result().body().getError()).toString());
//                        throw new IllegalStateException("Find Failed: " + reply.result().body().getError().getLocalizedMessage());
//                    }

                } else {
                    if (reply.cause().getClass().equals(DbActionResult.FailedDbActionException.class)){
                        rc.fail(500, reply.cause());
                    } else {
                        rc.fail(500, new IllegalStateException("Something went wrong", reply.cause()));
                    }

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

            SubmitTaskComposeDto dto = new SubmitTaskComposeDto();
            getUserTaskByTaskId(taskId).compose( userTaskEntity -> {
                dto.setUserTaskEntity(userTaskEntity);
                return Future.succeededFuture();

            }).compose(s2 -> {
                return getFormSchemaByFormKey(dto.getUserTaskEntity().getFormKey());

            }).compose(formSchema -> {
                dto.setFormSchemaEntity(formSchema);
                return Future.succeededFuture();

            }).compose(s3 ->{
                ValidationSchemaObject schema = new JsonObject(dto.getFormSchemaEntity().getSchema())
                        .mapTo(ValidationSchemaObject.class);

                ValidationRequest validationRequest = new ValidationRequest()
                        .setSchema(schema)
                        .setSubmission(submissionObject);

                return validateFormSchema(validationRequest);

            }).compose(validationResult -> {
                if (validationResult.getResult().equals(Result.VALID)) {
                    dto.setValidationRequestResult(validationResult);
                    dto.validForm = true;
                    return Future.succeededFuture();
                } else {
                    return Future.failedFuture(new IllegalStateException("Something went wrong, should not be here"));
                }
//                else {
//                    dto.setValidationRequestResult(validationResult);
//                    dto.validForm = false;
//                    return Future.failedFuture(new IllegalArgumentException("Invalid Form Submission"));
//                }
            }).compose(s4 -> {
                    Map<String, Object> completionVariables = new HashMap<>();
                    String variableName = dto.getUserTaskEntity().getTaskId() + "_submission";
                    completionVariables.put(variableName, dto.getValidationRequestResult().getValidResultObject().getProcessedSubmission());

                    //@TODO Refactor this call bck to zeebe to have a proper response handling
                    String zeebeSource = dto.getUserTaskEntity().getZeebeSource();
                    JobResult jobResult = new JobResult(
                            dto.getUserTaskEntity().getZeebeJobKey(),
                            JobResult.Result.COMPLETE)
                            .setVariables(completionVariables);

                    dto.setJobResult(jobResult);

                    return completeZeebeJob(zeebeSource, jobResult);
            }).compose(s5 -> {
                    CompletionRequest completionRequest = new CompletionRequest()
                            .setZeebeJobKey(dto.getUserTaskEntity().getZeebeJobKey())
                            .setZeebeSource(dto.getUserTaskEntity().getZeebeSource())
                            .setCompletionVariables(dto.getJobResult().getVariables());

                    return completeUserTask(completionRequest);
            }).setHandler(result -> {
                if (result.succeeded()){
                    if (dto.validForm) {
                        // Valid Form Submsision and Everything went well
                        rc.response()
                                .setStatusCode(202)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(JsonObject.mapFrom(dto.getValidationRequestResult().getValidResultObject()).toBuffer());
                    } else {
                        log.error("Task Submission DTO Error", new IllegalStateException("Task Submission with Form Validation succeeded by the DTO had a validForm=false... That should never occur.. something went wrong..."));
                        rc.fail(500, new IllegalStateException("Something went wrong.  We are looking into it"));
                    }
                } else {
                    if (result.cause().getClass().equals(InvalidFormSubmissionException.class)){
//                        log.info("Form Submission was invalid: " + JsonObject.mapFrom(dto.getValidationRequestResult().getInvalidResultObject()).toString());
                        InvalidFormSubmissionException exception = (InvalidFormSubmissionException)result.cause();
                        rc.response()
                                .setStatusCode(400)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(JsonObject.mapFrom(exception.getInvalidResult()).toBuffer());

                    } else if (result.cause().getClass().equals(ValidationRequestResultException.class)) {
                        rc.fail (500, result.cause());

                    } else if (result.cause().getClass().equals(IllegalArgumentException.class)){
                        rc.fail(400, result.cause());

                    } else {
                        log.error("Task Form Submission processing failed." , result.cause());
                        rc.fail(500, new IllegalStateException("Something went wrong during task submission processing.  We are looking into it"));
                    }
                }
            });



            //************
            //@TODO look at refactor with more fluent and compose
            //@TODO DO MAJOR REFACTOR TO CLEAN THIS JUNK UP: WAY TOO DEEP of a PYRAMID
//            getUserTaskByTaskId(taskId).setHandler(taskIdHandler -> {
//                if (taskIdHandler.succeeded()) {
//
//                    log.info("Found Form Key: " + taskIdHandler.result().getFormKey());
//
//                    getFormSchemaByFormKey(taskIdHandler.result().getFormKey()).setHandler(formKeyHandler -> {
//                        if (formKeyHandler.succeeded()) {
//
//                            String ebAddress = "forms.action.validate";
//
//                            ValidationSchemaObject schema = new JsonObject(formKeyHandler.result().getSchema()).mapTo(ValidationSchemaObject.class);
//
//                            ValidationRequest validationRequest = new ValidationRequest()
//                                    .setSchema(schema)
//                                    .setSubmission(submissionObject);
//
//                            eb.<ValidationRequestResult>request(ebAddress, validationRequest, ebHandler -> {
//                                if (ebHandler.succeeded()) {
//
//                                    if (ebHandler.result().body().getResult().equals(Result.VALID)) {
//
//                                        Map<String, Object> completionVariables = new HashMap<>();
//                                        String variableName = taskIdHandler.result().getTaskId() + "_submission";
//                                        completionVariables.put(variableName, ebHandler.result()
//                                                .body().getValidResultObject().getProcessedSubmission());
//
//                                        CompletionRequest completionRequest = new CompletionRequest()
//                                                .setZeebeJobKey(taskIdHandler.result().getZeebeJobKey())
//                                                .setZeebeSource(taskIdHandler.result().getZeebeSource())
//                                                .setCompletionVariables(completionVariables);
//
//                                        //@TODO Refactor this call bck to zeebe to have a proper response handling
//                                        String zeebeSource = taskIdHandler.result().getZeebeSource();
//                                        JobResult jobResult = new JobResult(
//                                                taskIdHandler.result().getZeebeJobKey(),
//                                                JobResult.Result.COMPLETE)
//                                                .setVariables(completionVariables);
//
//                                        //Complete the job in Zeebe:
//                                        //@TODO Refactor this to use the future
//                                        completeZeebeJob(zeebeSource, jobResult);
//
//
//                                        eb.<DbActionResult>request("ut.action.complete", completionRequest, dbCompleteHandler -> {
//                                            if (dbCompleteHandler.succeeded()) {
//
//
//                                            } else {
//                                                // EB Never returned a result for Completion
//                                                log.error("Never received a response from DB Completion request over EB");
//                                                rc.response()
//                                                        .setStatusCode(400)
//                                                        .end();
//                                            }
//                                        });
//
//                                    } else if (ebHandler.result().body().getResult().equals(Result.INVALID)){
//                                        rc.response()
//                                                .setStatusCode(400)
//                                                .putHeader("content-type", "application/json; charset=utf-8")
//                                                .end(JsonObject.mapFrom(ebHandler.result().body().getInvalidResultObject()).toBuffer());
//
//                                    } else {
//                                        ValidationRequestResultException exception = new JsonObject(ebHandler.result().body().getErrorResult()).mapTo(ValidationRequestResultException.class);
//                                        rc.fail(500, exception);
//                                    }
//                                } else {
//                                    // Did not receive message back on EB from Validation Service
//                                    throw new IllegalStateException("Did not receive a message back from the validation service");
//                                }
//                            }); // End of Validation Request EB send
//
//                        } else {
//                            // Could not find Form Schema in DB
//                            throw new IllegalArgumentException("Unable to find Form Schema for provided Form key, or multiple keys were returned.");
//                        }
//                    }); // end of getFormSchemaByFormKey
//
//                } else {
//                    // Could not find User Task in DB for the provided TaskId
//                    throw new IllegalArgumentException("Unable to find User Task for provided Task ID");
//                }
//            });
        });
    }

    private Future<Void> completeUserTask(CompletionRequest completionRequest){
        Promise<Void> promise = Promise.promise();

        String ebAddress = "ut.action.complete";

        eb.<DbActionResult>request(ebAddress, completionRequest, dbCompleteHandler -> {
            if (dbCompleteHandler.succeeded()) {
                if (dbCompleteHandler.result().body().getResult().equals(SUCCESS)) {
                   promise.complete();
                } else {
                    promise.fail(new IllegalStateException("DB Action returned a Fail: " + Json.encode(dbCompleteHandler.result().body().getError())));
                }
            } else {
                promise.fail(new IllegalStateException("EB User Task Completion Handler failed.", dbCompleteHandler.cause()));
            }
        });
        return promise.future();
    }

    private Future<ValidationRequestResult> validateFormSchema(ValidationRequest validationRequest){
        Promise<ValidationRequestResult> promise = Promise.promise();

        String ebAddress = "forms.action.validate"; //@TODO move to common

        eb.<ValidationRequestResult>request(ebAddress, validationRequest, ebHandler -> {
            if (ebHandler.succeeded()) {
                Result result = ebHandler.result().body().getResult();

                if (result.equals(Result.VALID)){
                    promise.complete(ebHandler.result().body());

                } else if (result.equals(Result.INVALID)) {
                    promise.fail(new InvalidFormSubmissionException("Form submission was invalid", ebHandler.result().body().getInvalidResultObject()));

                } else { //if it was a ERROR that was returned:
                    ValidationRequestResult.ErrorResult errorResult = ebHandler.result().body().getErrorResult();
                    promise.fail(new ValidationRequestResultException(
                            errorResult.getErrorType(),
                            errorResult.getInternalErrorMessage(),
                            errorResult.getEndUserMessage()));
                }
            } else {
                promise.fail(new IllegalStateException("Eb Response Failed for Validation Request", ebHandler.cause()));
            }
        });

        return promise.future();
    }

    private Future<Void> completeZeebeJob(String zeebeSource, JobResult jobResult){
        Promise<Void> promise = Promise.promise();

        String ebAddress = ".job-action.completion";

        // @TODO Refactor to have a proper response
        eb.<Void>request(zeebeSource + ebAddress, jobResult, result -> {
            if (result.succeeded()){
                promise.complete();
            } else {
                promise.fail(result.cause());
            }
        });
        return promise.future();
    }

    private Future<UserTaskEntity> getUserTaskByTaskId(String taskId) {
        //@TODO look at moving this into the UserTaskActionsVerticle
        Promise<UserTaskEntity> promise = Promise.promise();

        // @TODO future refactor to use a projection to only return the single Form Key field rather than the entire entity.

        if (taskId == null) {
            promise.fail(new IllegalArgumentException("taskId cannot be null"));
        }

        Bson findQuery = Filters.eq(taskId);

        tasksCollection.find().filter(findQuery)
                .subscribe(new SimpleSubscriber<UserTaskEntity>().singleResult(result -> {
                    if (result.succeeded()) {
                        promise.complete(result.result());
                    } else {
                        promise.fail(new IllegalArgumentException("Unable to find requested Task ID", result.cause()));
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
                .subscribe(new SimpleSubscriber<FormSchemaEntity>().singleResult(result -> {
                    if (result.succeeded()) {
                        promise.complete(result.result());

                    } else {
                        promise.fail(new IllegalArgumentException("Unable to find Form Schema that was configured for the requested task.",result.cause()));
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
