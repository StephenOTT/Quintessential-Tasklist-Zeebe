package com.github.stephenott.form.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.stephenott.conf.ApplicationConfiguration;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.stephenott.form.validator.ValidationRequestResult.*;
import static com.github.stephenott.form.validator.ValidationRequestResult.InvalidResult;
import static com.github.stephenott.form.validator.ValidationRequestResult.ValidResult;

public class FormValidationServerHttpVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(FormValidationServerHttpVerticle.class);

    private WebClient webClient;

    private EventBus eb;

    private ApplicationConfiguration.FormValidationServerConfiguration formValidationServerConfig;

    @Override
    public void start() throws Exception {
        formValidationServerConfig = config().mapTo(ApplicationConfiguration.FormValidationServerConfiguration.class);

        eb = vertx.eventBus();

        if (formValidationServerConfig.isEnabled()) {

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
                    .listen(formValidationServerConfig.getPort());

        }

        //@TODO Add a EB config toggle
        establishFormValidationEbConsumer();

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

        if (formValidationServerConfig.getCorsRegex() != null) {
            route.handler(CorsHandler.create(formValidationServerConfig.getCorsRegex()));
        }

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
                    if (handler.result().getResult().equals(Result.VALID)) {
                        rc.response()
                                .setStatusCode(202)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(JsonObject.mapFrom(handler.result().getValidResultObject()).toBuffer());
                    } else {
                        rc.response()
                                .setStatusCode(400)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(JsonObject.mapFrom(handler.result().getInvalidResultObject()).toBuffer());
                    }

                } else {
                    log.error("Unable to execute validation request");
                    rc.fail(403, handler.cause());
                }
            });
        });
    }

    private void establishFormValidationEbConsumer(){

        String address = "forms.action.validate";

        eb.<ValidationRequest>consumer(address, ebHandler -> {

            validateFormSubmission(ebHandler.body(), valResult -> {
                if (valResult.succeeded()){
                    ebHandler.reply(valResult.result());
                } else {
                    log.error("Form Validation method failed to provide a succeeded future", valResult.cause());
                }
            });

        }).exceptionHandler(error -> log.error("Could not read Validation Request message from EB", error));;
    }

    public void validateFormSubmission(ValidationRequest validationRequest, Handler<AsyncResult<ValidationRequestResult>> handler) {
        //@TODO Refactor this to reduce the wordiness...
        ApplicationConfiguration.FormValidatorServiceConfiguration validatorConfig = formValidationServerConfig.getFormValidatorService();

        String host = validatorConfig.getHost();
        int port = validatorConfig.getPort();
        long requestTimeout = validatorConfig.getRequestTimeout();
        String validateUri = validatorConfig.getValidateUri();

        log.info("BODY: " + JsonObject.mapFrom(new ValidationServiceRequest(validationRequest)).toString());

        //@TODO look at using the .expect predicate methods as part of .post() rather than using the if statusCode...
        webClient.post(port, host, validateUri)
                .timeout(requestTimeout)
                .sendJson(new ValidationServiceRequest(validationRequest), res -> {
                    if (res.succeeded()) {

                        int statusCode = res.result().statusCode();

                        if (statusCode == 202) {
                            log.info("FORMIO 202 RESULT: " + res.result().bodyAsString());
                            handler.handle(Future.succeededFuture(GenerateValidResult(res.result().bodyAsJson(ValidResult.class))));

                        } else if (statusCode == 400) {
                            log.info("FORMIO 400 RESULT: " + res.result().bodyAsString());
                            handler.handle(Future.succeededFuture(GenerateInvalidResult(res.result().bodyAsJson(InvalidResult.class))));

                        } else {
                            log.error("Unexpected response returned by form validator: code:" + res.result().statusCode() + ".  Body: " + res.result().bodyAsString());
                            handler.handle(Future.failedFuture("Unexpected response returned by form validator: code:" + res.result().statusCode() + ".  Body: " + res.result().bodyAsString()));
                        }

                    } else {
                        log.error("Unable to complete HTTP request to validation server", res.cause());
                        handler.handle(Future.failedFuture(res.cause()));
                    }
                });
    }
}
