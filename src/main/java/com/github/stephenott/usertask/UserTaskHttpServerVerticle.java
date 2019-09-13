package com.github.stephenott.usertask;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        establishCompleteActionRoute(mainRouter);

        server.requestHandler(mainRouter)
                .listen(port);

    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void establishCompleteActionRoute(Router router) {

        String address = "ut.action.complete";

        DeliveryOptions options = new DeliveryOptions();

        Route route = router.route(HttpMethod.POST, "/task/complete")
                .consumes("application/json")
                .produces("application/json");

        //@TODO add cors

        route.handler(BodyHandler.create());

        route.handler(rc -> {
            CompletionRequest completionRequest;
            try {
                completionRequest = rc.getBodyAsJson().mapTo(CompletionRequest.class);

                eb.<JsonObject>request(address, JsonObject.mapFrom(completionRequest), options, handler -> {
                    if (handler.succeeded()) {
                        rc.response()
                                .putHeader("content-type", "application/json")
                                .end(handler.result().body().toBuffer());
                    } else {
                        rc.fail(403, handler.cause());
                    }
                });

            } catch (Exception e) {
                log.error("Unable to process completion request payment from HTTP request", e);
                rc.fail(403, e);
            }

        });


    }

    private void establishGetTaskRoute() {
        //@TODO
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
}
