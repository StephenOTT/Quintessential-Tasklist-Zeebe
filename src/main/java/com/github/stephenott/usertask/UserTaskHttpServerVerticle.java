package com.github.stephenott.usertask;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.github.stephenott.usertask.UserTaskHttpServerVerticle.HttpUtils.*;

public class UserTaskHttpServerVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(UserTaskHttpServerVerticle.class);

    EventBus eb;


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
                    .end("DOG-task: " + failure.failure().getLocalizedMessage());
        });

        mainRouter.errorHandler(500, rc -> {
            log.error("HTTP FAILURE!!!", rc.failure());
            rc.fail(500);
        });

        establishCompleteActionRoute(mainRouter);
        establishGetTasksRoute(mainRouter);

        server.requestHandler(mainRouter)
                .listen(port);

    }

    @Override
    public void stop() throws Exception {
        super.stop();
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
            DeliveryOptions options = new DeliveryOptions().setCodecName("com.github.stephenott.usertask.CompletionRequest");
            eb.<DbActionResult>request(address, completionRequest, options, reply -> {
                if (reply.succeeded()) {
                    addCommonHeaders(rc.response());

                    if (reply.result().body().getResultObject().size() == 1){
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

            DeliveryOptions options = new DeliveryOptions().setCodecName("com.github.stephenott.usertask.GetRequest");

            eb.<DbActionResult>request(address, getRequest, options, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().body().getResult().equals(DbActionResult.ActionResult.SUCCESS)){
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
