package com.github.stephenott.form.validator;

import com.github.stephenott.conf.ApplicationConfiguration;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormValidationServerHttpVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(FormValidationServerHttpVerticle.class);

    WebClient webClient;

    EventBus eb;

    private ApplicationConfiguration.FormValidationServerConfiguration formValidationServerConfig;

    @Override
    public void start() throws Exception {
        formValidationServerConfig = config().mapTo(ApplicationConfiguration.FormValidationServerConfiguration.class);

        eb = vertx.eventBus();

        WebClientOptions webClientOptions = new WebClientOptions();
        webClient = WebClient.create(vertx, webClientOptions);

        Router mainRouter = Router.router(vertx);
        HttpServer server = vertx.createHttpServer();

        mainRouter.route().failureHandler(failure -> {

            int statusCode = failure.statusCode();

            HttpServerResponse response = failure.response();
            response.setStatusCode(statusCode)
                    .end("DOG" + failure.failure().getLocalizedMessage());
        });

        establishFormValidationRoute(mainRouter);

        server.requestHandler(mainRouter)
                .listen(8082);

        log.info("Form Validation Server deployed at: localhost:...., CORS is .... ");
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void establishFormValidationRoute(Router router) {
        Route route = router.route(HttpMethod.POST, "/validate")
                .consumes("application/json")
                .produces("application/json");

//        if (managementConfig.getCorsRegex() != null) {
//            route.handler(CorsHandler.create(managementConfig.getCorsRegex()));
//        }

        route.handler(BodyHandler.create());

        route.handler(rc -> { //routing context
            //@TODO add a generic helper to parse and handler errors or look at using a "throws" in the method def
            // without this try the error is hidden from the logs
            ValidationRequest request;
            try {
                request = rc.getBodyAsJson().mapTo(ValidationRequest.class);

            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse body", e);
            }

            validateFormSubmission(request, handler -> {
                if (handler.succeeded()) {
                    if (handler.result().submissionIsValid()) {
                        rc.response()
                                .setStatusCode(202)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(handler.result().toJsonObject().toBuffer());
                    } else {
                        rc.response()
                                .setStatusCode(400)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(handler.result().toJsonObject().toBuffer());
                    }

                } else {
                    log.error("Unable to execute validation request");
                    rc.fail(403, handler.cause());
                }
            });
        });
    }

    private void establishFormValidationEbConsumer(){
        //@TODO
    }

    public void validateFormSubmission(ValidationRequest validationRequest, Handler<AsyncResult<ValidationResult.Type>> handler) {
        //@TODO Refactor this to reduce the wordiness...
        ApplicationConfiguration.FormValidatorServiceConfiguration validatorConfig = formValidationServerConfig.getFormValidatorService();

        String host = validatorConfig.getHost();
        int port = validatorConfig.getPort();
        long requestTimeout = validatorConfig.getRequestTimeout();
        String validateUri = validatorConfig.getValidateUri();
        log.info("HAHAHA1");
        //@TODO look at using the .expect predicate methods as part of .post() rather than using the if statusCode...
        webClient.post(port, host, validateUri)
                .timeout(requestTimeout)
                .sendJson(JsonObject.mapFrom(validationRequest), res -> {
                    if (res.succeeded()) {
                        log.info("HAHAHA2");
                        int statusCode = res.result().statusCode();

                        if (statusCode == 202) {
                            ValidationResult.Valid responseValid = res.result().bodyAsJson(ValidationResult.Valid.class);
                            handler.handle(Future.succeededFuture(responseValid));

                        } else if (statusCode == 400) {
                            ValidationResult.Fail responseFail = res.result().bodyAsJson(ValidationResult.Fail.class);
                            handler.handle(Future.succeededFuture(responseFail));

                        } else {
                            handler.handle(Future.failedFuture("Unexpected response returned by form validator: code:" + res.result().statusCode() + ".  Body: " + res.result().bodyAsString()));
                            log.error("Unexpected response returned by form validator: code:" + res.result().statusCode() + ".  Body: " + res.result().bodyAsString());
                        }

                    } else {
                        log.info("HAHAHA3");
                        handler.handle(Future.failedFuture(res.cause()));
                        log.error("Unable to complete HTTP request to validation server", res.cause());
                    }
                });
    }
}
